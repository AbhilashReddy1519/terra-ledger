package com.terraledger.service;

import com.terraledger.dto.ChainRecord;
import com.terraledger.dto.ChainRecordEvent;
import com.terraledger.dto.CommitResult;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gateway service for Web3j smart contract interactions with LandRegistry.sol.
 * Manages raw transactions, role authorization checks, event listening, and state queries.
 * Includes simulated in-memory ledger fallback for offline test environments.
 */
@Service
public class ContractGatewayService {

    private static final Logger log = LoggerFactory.getLogger(ContractGatewayService.class);

    private static final BigInteger GAS_PRICE = BigInteger.valueOf(1_000_000_000L); // 1 Gwei
    private static final BigInteger GAS_LIMIT = BigInteger.valueOf(3_000_000L);

    private final String rpcUrl;
    private final String privateKey;
    private String contractAddress;

    private Web3j web3j;
    private Credentials credentials;
    private StaticGasProvider gasProvider;
    private boolean isLiveChain = false;

    // In-memory simulation fallback for offline test environments
    private final Map<String, List<SimulatedVersion>> simulatedLedger = new ConcurrentHashMap<>();
    private final Set<String> simulatedOfficers = ConcurrentHashMap.newKeySet();
    private final List<ChainRecordEvent> simulatedEvents = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger simulatedBlockNumber = new AtomicInteger(100);

    public record SimulatedVersion(int version, String cid, String sha256Hex, String approver, Instant at, String txHash) {}

    public ContractGatewayService(
            @Value("${app.hardhat.rpc-url:http://localhost:8545}") String rpcUrl,
            @Value("${app.hardhat.private-key:0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80}") String privateKey,
            @Value("${app.hardhat.contract-address:${LAND_REGISTRY_ADDRESS:}}") String contractAddress) {
        this.rpcUrl = rpcUrl;
        this.privateKey = privateKey;
        this.contractAddress = contractAddress;
    }

    @PostConstruct
    public void init() {
        this.gasProvider = new StaticGasProvider(GAS_PRICE, GAS_LIMIT);
        try {
            this.credentials = Credentials.create(privateKey);
            if (this.credentials != null) {
                simulatedOfficers.add(this.credentials.getAddress().toLowerCase());
            }

            this.web3j = Web3j.build(new HttpService(rpcUrl));

            // Test connectivity
            BigInteger blockNum = web3j.ethBlockNumber().send().getBlockNumber();
            this.isLiveChain = true;
            log.info("ContractGatewayService connected to EVM node at {}. Current block={}", rpcUrl, blockNum);

            // Resolve contract address if not provided
            if (this.contractAddress == null || this.contractAddress.isBlank()) {
                resolveContractAddress();
            }

            if (this.contractAddress != null && !this.contractAddress.isBlank()) {
                log.info("ContractGatewayService bound to LandRegistry at {}", this.contractAddress);
                // Ensure backend credentials address is registered as officer if needed
                ensureOfficerRegistered(this.credentials.getAddress());
            }

        } catch (Exception e) {
            log.warn("Could not connect to EVM RPC at {} ({}). Using simulated contract ledger for offline/test mode.",
                    rpcUrl, e.getMessage());
            this.isLiveChain = false;
            // Setup default officer in simulation
            if (this.credentials != null) {
                simulatedOfficers.add(this.credentials.getAddress().toLowerCase());
            }
        }
    }

    private void resolveContractAddress() {
        String[] possiblePaths = {
                "/deployments/localhost.json",
                "deployments/localhost.json",
                "contracts/deployments/localhost.json",
                "../contracts/deployments/localhost.json"
        };

        for (String p : possiblePaths) {
            File f = new File(p);
            if (f.exists()) {
                try {
                    String content = Files.readString(f.toPath());
                    Matcher m = Pattern.compile("\"address\"\\s*:\\s*\"([^\"]+)\"").matcher(content);
                    if (m.find()) {
                        this.contractAddress = m.group(1);
                        log.info("Resolved LandRegistry address from {}: {}", p, this.contractAddress);
                        return;
                    }
                } catch (Exception ex) {
                    log.warn("Error reading deployment file {}: {}", p, ex.getMessage());
                }
            }
        }
    }

    private void ensureOfficerRegistered(String address) {
        try {
            if (!isOfficerOnChain(address)) {
                log.info("Granting officer role to backend address {}...", address);
                grantOfficerOnChain(address);
            }
        } catch (Exception e) {
            log.warn("Could not auto-verify officer role on chain: {}", e.getMessage());
        }
    }

    /**
     * Commits a record to the LandRegistry contract using backend service credentials.
     */
    public CommitResult commitRecord(String recordUuid, String cid, String sha256Hex, String approver) {
        return commitWithCredentials(this.credentials, recordUuid, cid, sha256Hex, approver);
    }

    /**
     * Commits a record using specified wallet credentials (allows testing unauthorized calls).
     */
    public CommitResult commitWithCredentials(Credentials senderCredentials, String recordUuid, String cid, String sha256Hex, String approver) {
        if (recordUuid == null || cid == null || sha256Hex == null) {
            throw new IllegalArgumentException("recordUuid, cid, and sha256Hex cannot be null");
        }

        byte[] recordIdBytes = toBytes32(recordUuid);
        byte[] sha256Bytes = Numeric.hexStringToByteArray(sha256Hex);
        String senderAddress = senderCredentials.getAddress();

        if (isLiveChain && contractAddress != null && !contractAddress.isBlank()) {
            try {
                int existingCount = getVersionCountOnChain(recordIdBytes);
                String funcName = (existingCount == 0) ? "createRecord" : "addVersion";

                Function function = new Function(
                        funcName,
                        Arrays.asList(
                                new Bytes32(recordIdBytes),
                                new Utf8String(cid),
                                new Bytes32(sha256Bytes)
                        ),
                        Collections.emptyList()
                );

                String encodedFunction = FunctionEncoder.encode(function);
                TransactionManager txManager = new RawTransactionManager(web3j, senderCredentials);
                TransactionReceipt receipt = sendTransaction(txManager, contractAddress, encodedFunction);

                if (!receipt.isStatusOK()) {
                    throw new RuntimeException("Transaction reverted on chain: " + receipt.getTransactionHash());
                }

                int newVersion = existingCount + 1;
                log.info("Committed record {} (v{}) to chain. TxHash={}, Block={}",
                        recordUuid, newVersion, receipt.getTransactionHash(), receipt.getBlockNumber());

                return new CommitResult(
                        receipt.getTransactionHash(),
                        newVersion,
                        receipt.getBlockNumber(),
                        senderAddress,
                        cid,
                        sha256Hex
                );

            } catch (Exception e) {
                log.error("Failed to commit record {} to blockchain: {}", recordUuid, e.getMessage(), e);
                throw new RuntimeException("Blockchain commit failed: " + e.getMessage(), e);
            }
        }

        // ── Simulated ledger execution ──────────────────────────────────────────
        return simulatedCommit(senderAddress, recordUuid, cid, sha256Hex);
    }

    private CommitResult simulatedCommit(String senderAddress, String recordUuid, String cid, String sha256Hex) {
        if (!simulatedOfficers.contains(senderAddress.toLowerCase())) {
            throw new RuntimeException("Transaction reverted with error: NotOfficer()");
        }

        List<SimulatedVersion> versions = simulatedLedger.computeIfAbsent(recordUuid, k -> new ArrayList<>());
        int newVersion = versions.size() + 1;
        String txHash = "0x" + Hash.sha3String(recordUuid + ":" + newVersion + ":" + System.currentTimeMillis()).substring(2);
        BigInteger blockNumber = BigInteger.valueOf(simulatedBlockNumber.incrementAndGet());

        SimulatedVersion simV = new SimulatedVersion(newVersion, cid, sha256Hex, senderAddress, Instant.now(), txHash);
        versions.add(simV);

        ChainRecordEvent event = new ChainRecordEvent(
                Numeric.toHexString(toBytes32(recordUuid)),
                newVersion,
                sha256Hex,
                cid,
                senderAddress,
                txHash,
                blockNumber
        );
        simulatedEvents.add(event);

        log.info("[SIMULATED CHAIN] Committed record {} v{} tx={}", recordUuid, newVersion, txHash);
        return new CommitResult(txHash, newVersion, blockNumber, senderAddress, cid, sha256Hex);
    }

    /**
     * Fetches latest on-chain record for a given UUID.
     */
    public ChainRecord getLatest(String recordUuid) {
        byte[] recordIdBytes = toBytes32(recordUuid);

        if (isLiveChain && contractAddress != null && !contractAddress.isBlank()) {
            try {
                Function function = new Function(
                        "getLatest",
                        Collections.singletonList(new Bytes32(recordIdBytes)),
                        Arrays.asList(
                                new TypeReference<Uint256>() {},
                                new TypeReference<Utf8String>() {},
                                new TypeReference<Bytes32>() {},
                                new TypeReference<Address>() {},
                                new TypeReference<Uint256>() {}
                        )
                );

                String encoded = FunctionEncoder.encode(function);
                EthCall response = web3j.ethCall(
                        Transaction.createEthCallTransaction(credentials.getAddress(), contractAddress, encoded),
                        DefaultBlockParameterName.LATEST
                ).send();

                if (response.hasError() || response.getValue() == null || "0x".equals(response.getValue())) {
                    throw new RuntimeException("Record not found on chain: " + recordUuid);
                }

                List<Type> results = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
                int version = ((Uint256) results.get(0)).getValue().intValue();
                String cid = ((Utf8String) results.get(1)).getValue();
                byte[] hashBytes = ((Bytes32) results.get(2)).getValue();
                String sha256Hex = Numeric.toHexStringNoPrefix(hashBytes);
                String approver = ((Address) results.get(3)).getValue();
                long timestamp = ((Uint256) results.get(4)).getValue().longValue();

                return new ChainRecord(version, cid, sha256Hex, approver, Instant.ofEpochSecond(timestamp));

            } catch (Exception e) {
                log.error("Failed to query getLatest for {}: {}", recordUuid, e.getMessage());
                throw new RuntimeException("Failed to read record from chain: " + e.getMessage(), e);
            }
        }

        // Simulated query
        List<SimulatedVersion> versions = simulatedLedger.get(recordUuid);
        if (versions == null || versions.isEmpty()) {
            throw new RuntimeException("Record not found in simulated ledger: " + recordUuid);
        }
        SimulatedVersion latest = versions.get(versions.size() - 1);
        return new ChainRecord(latest.version(), latest.cid(), latest.sha256Hex(), latest.approver(), latest.at());
    }

    /**
     * Queries past RecordCommitted events emitted by LandRegistry.
     */
    public List<ChainRecordEvent> getAllCommittedEvents() {
        if (isLiveChain && contractAddress != null && !contractAddress.isBlank()) {
            try {
                Event event = new Event("RecordCommitted",
                        Arrays.asList(
                                new TypeReference<Bytes32>(true) {},
                                new TypeReference<Uint256>(false) {},
                                new TypeReference<Bytes32>(false) {},
                                new TypeReference<Utf8String>(false) {},
                                new TypeReference<Address>(false) {}
                        )
                );

                String eventSignature = EventEncoder.encode(event);
                EthFilter filter = new EthFilter(
                        DefaultBlockParameterName.EARLIEST,
                        DefaultBlockParameterName.LATEST,
                        contractAddress
                );
                filter.addSingleTopic(eventSignature);

                EthLog ethLog = web3j.ethGetLogs(filter).send();
                List<ChainRecordEvent> events = new ArrayList<>();

                for (EthLog.LogResult<?> logResult : ethLog.getLogs()) {
                    org.web3j.protocol.core.methods.response.Log logItem = (org.web3j.protocol.core.methods.response.Log) logResult.get();
                    String recordIdHex = logItem.getTopics().get(1);

                    List<Type> nonIndexedValues = FunctionReturnDecoder.decode(
                            logItem.getData(),
                            event.getNonIndexedParameters()
                    );

                    int version = ((Uint256) nonIndexedValues.get(0)).getValue().intValue();
                    byte[] hashBytes = ((Bytes32) nonIndexedValues.get(1)).getValue();
                    String sha256Hex = Numeric.toHexStringNoPrefix(hashBytes);
                    String cid = ((Utf8String) nonIndexedValues.get(2)).getValue();
                    String approver = ((Address) nonIndexedValues.get(3)).getValue();

                    events.add(new ChainRecordEvent(
                            recordIdHex,
                            version,
                            sha256Hex,
                            cid,
                            approver,
                            logItem.getTransactionHash(),
                            logItem.getBlockNumber()
                    ));
                }

                return events;

            } catch (Exception e) {
                log.warn("Failed to query on-chain events: {}. Falling back to simulation events.", e.getMessage());
            }
        }

        return new ArrayList<>(simulatedEvents);
    }

    public boolean isOfficer(String address) {
        if (address == null) return false;
        if (isLiveChain && contractAddress != null && !contractAddress.isBlank()) {
            try {
                return isOfficerOnChain(address);
            } catch (Exception e) {
                log.warn("Checking officer failed on chain: {}", e.getMessage());
            }
        }
        return simulatedOfficers.contains(address.toLowerCase());
    }

    public void grantOfficer(String address) {
        if (address == null) return;
        if (isLiveChain && contractAddress != null && !contractAddress.isBlank()) {
            try {
                grantOfficerOnChain(address);
            } catch (Exception e) {
                log.warn("grantOfficer on chain failed: {}", e.getMessage());
            }
        }
        simulatedOfficers.add(address.toLowerCase());
    }

    public void revokeOfficer(String address) {
        if (address == null) return;
        if (isLiveChain && contractAddress != null && !contractAddress.isBlank()) {
            try {
                revokeOfficerOnChain(address);
            } catch (Exception e) {
                log.warn("revokeOfficer on chain failed: {}", e.getMessage());
            }
        }
        simulatedOfficers.remove(address.toLowerCase());
    }

    private boolean isOfficerOnChain(String address) throws Exception {
        Function function = new Function(
                "officers",
                Collections.singletonList(new Address(address)),
                Collections.singletonList(new TypeReference<org.web3j.abi.datatypes.Bool>() {})
        );

        String encoded = FunctionEncoder.encode(function);
        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(credentials.getAddress(), contractAddress, encoded),
                DefaultBlockParameterName.LATEST
        ).send();

        if (response.hasError() || response.getValue() == null) return false;
        List<Type> results = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        return !results.isEmpty() && ((org.web3j.abi.datatypes.Bool) results.get(0)).getValue();
    }

    private void grantOfficerOnChain(String address) throws Exception {
        Function function = new Function(
                "grantOfficer",
                Collections.singletonList(new Address(address)),
                Collections.emptyList()
        );
        String encoded = FunctionEncoder.encode(function);
        TransactionManager txManager = new RawTransactionManager(web3j, credentials);
        sendTransaction(txManager, contractAddress, encoded);
    }

    private void revokeOfficerOnChain(String address) throws Exception {
        Function function = new Function(
                "revokeOfficer",
                Collections.singletonList(new Address(address)),
                Collections.emptyList()
        );
        String encoded = FunctionEncoder.encode(function);
        TransactionManager txManager = new RawTransactionManager(web3j, credentials);
        sendTransaction(txManager, contractAddress, encoded);
    }

    private TransactionReceipt sendTransaction(TransactionManager txManager, String to, String data) throws Exception {
        EthSendTransaction ethSendTx = txManager.sendTransaction(
                gasProvider.getGasPrice(),
                gasProvider.getGasLimit(),
                to,
                data,
                BigInteger.ZERO
        );

        if (ethSendTx.hasError()) {
            throw new RuntimeException("Transaction send failed: " + ethSendTx.getError().getMessage());
        }

        String txHash = ethSendTx.getTransactionHash();
        return waitForReceipt(txHash);
    }

    private TransactionReceipt waitForReceipt(String txHash) throws Exception {
        for (int i = 0; i < 40; i++) {
            var receiptOpt = web3j.ethGetTransactionReceipt(txHash).send().getTransactionReceipt();
            if (receiptOpt.isPresent()) {
                return receiptOpt.get();
            }
            Thread.sleep(500);
        }
        throw new RuntimeException("Receipt timeout for tx: " + txHash);
    }

    private int getVersionCountOnChain(byte[] recordIdBytes) {
        try {
            Function function = new Function(
                    "versionCount",
                    Collections.singletonList(new Bytes32(recordIdBytes)),
                    Collections.singletonList(new TypeReference<Uint256>() {})
            );
            String encoded = FunctionEncoder.encode(function);
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(credentials.getAddress(), contractAddress, encoded),
                    DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError() || response.getValue() == null || "0x".equals(response.getValue())) {
                return 0;
            }
            List<Type> results = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
            if (results.isEmpty()) return 0;
            return ((Uint256) results.get(0)).getValue().intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    public static byte[] toBytes32(String uuidOrId) {
        return Hash.sha3(uuidOrId.getBytes(StandardCharsets.UTF_8));
    }

    public static String toBytes32Hex(String uuidOrId) {
        return Numeric.toHexString(toBytes32(uuidOrId));
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public boolean isLiveChain() {
        return isLiveChain;
    }
}
