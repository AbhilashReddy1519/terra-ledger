package com.terraledger.service;

import com.terraledger.domain.LandRecord;
import com.terraledger.domain.RecordStatus;
import com.terraledger.domain.RecordVersion;
import com.terraledger.dto.ChainRecordEvent;
import com.terraledger.dto.ReconcileReport;
import com.terraledger.repository.LandRecordRepository;
import com.terraledger.repository.RecordVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reconciles the MySQL working cache against the smart contract truth.
 * Rule: "The chain wins. The reconcile job flags differences and never silently overwrites."
 * Rebuilds the MySQL record_versions cache from blockchain RecordCommitted events.
 */
@Service
public class ReconcileService {

    private static final Logger log = LoggerFactory.getLogger(ReconcileService.class);

    private final ContractGatewayService contractGatewayService;
    private final LandRecordRepository landRecordRepository;
    private final RecordVersionRepository recordVersionRepository;
    private final AuditService auditService;

    public ReconcileService(
            ContractGatewayService contractGatewayService,
            LandRecordRepository landRecordRepository,
            RecordVersionRepository recordVersionRepository,
            AuditService auditService) {
        this.contractGatewayService = contractGatewayService;
        this.landRecordRepository = landRecordRepository;
        this.recordVersionRepository = recordVersionRepository;
        this.auditService = auditService;
    }

    /**
     * Periodic reconciliation job (runs every 5 minutes in background).
     */
    @Scheduled(fixedDelayString = "${app.reconcile.interval-ms:300000}")
    public void scheduledReconcile() {
        try {
            ReconcileReport report = reconcileAll();
            if (!report.discrepancies().isEmpty()) {
                log.warn("Reconcile job detected discrepancies between MySQL cache and chain: {}", report.discrepancies());
            } else {
                log.debug("Reconcile job finished clean. Matched: {}, Rebuilt: {}", report.matchedCount(), report.rebuiltCount());
            }
        } catch (Exception e) {
            log.error("Scheduled reconciliation failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Reconciles all on-chain events against the MySQL database.
     * Rebuilds any missing cache records and reports discrepancies.
     */
    @Transactional
    public ReconcileReport reconcileAll() {
        List<ChainRecordEvent> events = contractGatewayService.getAllCommittedEvents();
        List<LandRecord> allRecords = landRecordRepository.findAll();

        // Build a mapping from bytes32 hex -> LandRecord
        Map<String, LandRecord> hexToRecord = new HashMap<>();
        for (LandRecord r : allRecords) {
            String hex = ContractGatewayService.toBytes32Hex(r.getId()).toLowerCase();
            hexToRecord.put(hex, r);
        }

        int matchedCount = 0;
        int rebuiltCount = 0;
        List<String> discrepancies = new ArrayList<>();

        for (ChainRecordEvent event : events) {
            String eventRecordHex = event.recordIdHex().toLowerCase();
            LandRecord record = hexToRecord.get(eventRecordHex);

            if (record == null) {
                log.info("Chain event for unindexed recordId bytes32: {} (v{}). Skipping.", event.recordIdHex(), event.version());
                continue;
            }

            Optional<RecordVersion> cachedVersionOpt = recordVersionRepository.findByRecordIdAndVersion(
                    record.getId(), event.version()
            );

            if (cachedVersionOpt.isPresent()) {
                RecordVersion cached = cachedVersionOpt.get();
                boolean shaMatches = cached.getSha256().equalsIgnoreCase(event.sha256Hex());
                boolean cidMatches = cached.getCid().equals(event.cid());

                if (shaMatches && cidMatches) {
                    matchedCount++;
                } else {
                    String diff = String.format("DISCREPANCY on record %s v%d: chain sha=%s cid=%s vs cache sha=%s cid=%s",
                            record.getId(), event.version(), event.sha256Hex(), event.cid(), cached.getSha256(), cached.getCid());
                    log.error(diff);
                    discrepancies.add(diff);
                    auditService.logEvent(
                            "SYSTEM_RECONCILE",
                            "CHAIN_DISCREPANCY",
                            "RecordVersion",
                            cached.getId(),
                            diff
                    );
                }
            } else {
                // Rebuild cache from chain truth!
                RecordVersion rebuilt = new RecordVersion(
                        record.getId(),
                        event.version(),
                        event.cid(),
                        event.sha256Hex(),
                        event.txHash(),
                        event.approver()
                );
                recordVersionRepository.save(rebuilt);

                // Update land_record current version and committed status
                if (record.getCurrentVersion() == null || record.getCurrentVersion() < event.version()) {
                    record.setCurrentVersion(event.version());
                    record.setIpfsCid(event.cid());
                    record.setSha256Hash(event.sha256Hex());
                    record.setStatus(RecordStatus.COMMITTED);
                    landRecordRepository.save(record);
                }

                rebuiltCount++;
                log.info("Rebuilt cache for record {} v{} from chain event (tx: {})", record.getId(), event.version(), event.txHash());
                auditService.logEvent(
                        "SYSTEM_RECONCILE",
                        "CACHE_REBUILT",
                        "LandRecord",
                        record.getId(),
                        "Rebuilt version " + event.version() + " from chain tx " + event.txHash()
                );
            }
        }

        boolean matchesChain = discrepancies.isEmpty();
        return new ReconcileReport(events.size(), matchedCount, rebuiltCount, discrepancies, matchesChain);
    }
}
