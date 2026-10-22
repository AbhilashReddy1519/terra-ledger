# Blockchain-Based Land Records Wallet

A wallet-style application for accessing, signing, and verifying digital land records. It combines React, Spring Boot, IPFS, and a permissioned blockchain to maintain a tamper-evident history of approved records.

## How It Works

1. An authorized user uploads a scanned land document.
2. OCR extracts fields such as owner name, survey/khasra number, area, and boundaries.
3. A revenue officer reviews and approves the extracted information.
4. The approved document is stored in IPFS; IPFS returns a Content Identifier (CID).
5. The application calculates a SHA-256 hash of the document.
6. The approved metadata, CID, hash, and transaction details are submitted to the permissioned blockchain.
7. The network validates and commits the transaction.
8. Authorized users can access the record and verify documents by comparing hashes.

**Important:** Blockchain helps detect changes after a record is committed. It does not prove that the original document or OCR data was correct.

## User Sign-In

1. The user registers and completes the required identity checks.
2. The user creates or connects a wallet.
3. The verified account is linked to the wallet's public address.
4. On later sign-ins, the application authenticates the user and, when wallet authentication is used, issues a one-time challenge.
5. The wallet signs the challenge locally. The private key is never sent to the backend.
6. Spring Boot verifies the signature and issues a session or short-lived access token.
7. The dashboard displays records and actions allowed for that user.

A wallet signature proves control of a key; it does not by itself prove legal ownership of land.

## Authorities and Responsibilities

| Role | Responsibilities |
|---|---|
| **Citizen / Landowner** | View authorized land records, request changes or transfers, sign consent requests, and verify documents. |
| **Revenue Officer / VRO** | Review OCR output, correct errors, and approve or reject record submissions. |
| **Registrar** | Review and approve or reject eligible registration, mutation, and transfer requests under applicable rules. |
| **System Administrator** | Manage accounts, roles, configuration, and application operations. Should not be able to unilaterally alter approved ownership history. |
| **Blockchain Network Operator** | Maintain nodes, certificates, availability, and network upgrades. Operating a node does not automatically grant land-registration authority. |
| **Public / Authorized Verifier** | Check document integrity and view information permitted by access policy. |
| **Bank / Court / Authorized Institution** | Verify records for an authorized purpose, subject to privacy and access rules. |

Permissions must be enforced by Spring Security and the smart contract/chaincode, not only by hiding frontend controls.

## Document Signing and Verification

### Signing
1. A requester creates a request for a specific document and action.
2. The server creates a request ID, nonce, expiry, and canonical signing payload.
3. The user reviews the document and signs the payload in their wallet.
4. Spring Boot verifies the signature using the public key registered to the authenticated user.
5. The verified signature and audit event are saved.
6. Where required, approval evidence is submitted to the blockchain.

Never send or store private keys or seed phrases in the backend.

### Verification
1. Retrieve the approved document hash from the blockchain.
2. Calculate SHA-256 over the supplied document's exact bytes.
3. Compare the calculated hash with the committed hash.
4. A match means the file is byte-identical to the approved file; a mismatch indicates a different or potentially altered file.

A valid signature or matching hash does not independently establish legal validity or finalize a land transfer.

## Technology Stack

| Technology | Purpose |
|---|---|
| **React.js** | Wallet UI, dashboard, record search, document viewer, and signing screens |
| **Java + Spring Boot** | REST APIs and application logic |
| **Spring Security** | Authentication and role-based access control |
| **Spring Data JPA** | Database persistence |
| **MySQL** | Accounts, roles, request status, search indexes, and cached ledger data |
| **Solidity** | Smart-contract programming language for an Ethereum-compatible blockchain deployment |\n| **Ethereum-compatible blockchain / EVM test network** | Runs Solidity smart contracts for record rules and transactions; use this option if choosing the Solidity-based architecture |\n| **Hyperledger Fabric** | Alternative permissioned blockchain for authorized organizations; uses chaincode rather than Solidity by default |
| **Solidity smart contracts** | On-chain rules for authorized record creation, verification, approvals, and record versioning on an EVM-compatible network |\n| **Fabric chaincode** | Equivalent business rules if the project uses Hyperledger Fabric instead |
| **IPFS** | Off-chain storage for scanned documents |
| **SHA-256** | Document-integrity hashing |
| **Java Cryptography APIs / compatible library** | Backend signature verification |
| **Web Crypto API / compatible wallet library** | Local key operations and signing, using a format compatible with the backend |
| **Tesseract OCR / EasyOCR** | Extract text from scanned records |
| **OpenCV** | Image preprocessing before OCR |
| **Fabric Gateway Java client** | Connect Spring Boot to Hyperledger Fabric |
| **Docker Compose** | Run development services locally |
| **Flyway** | Versioned database migrations |
| **JUnit / Spring Boot Test** | Automated testing |

The stack is a recommendation; use only technologies that are actually included in the implementation.

## Why MySQL Is Still Needed

MySQL manages application data such as accounts, permissions, pending requests, notifications, search indexes, and cached ledger information. The blockchain stores the approved transaction history, while IPFS stores document files.

Do not treat a cached database value as an authoritative ownership change. Query or reconcile against the blockchain for authoritative committed state.

## Architecture

```text
React Wallet
    |
    v
Spring Boot REST API
    |------ MySQL: accounts, permissions, requests
    |------ OCR / SHA-256: extraction and integrity checks
    |------ IPFS: scanned documents
    |
    v
Blockchain Gateway
    |
    v
Permissioned Blockchain
    Smart contract / chaincode
    Approved metadata, hashes, CIDs, history
```

## Development Roadmap

1. Build sign-in, wallet connection, and role-based dashboards.
2. Implement document upload, OCR, validation, and officer review.
3. Add IPFS storage and SHA-256 verification.
4. Choose one blockchain path: deploy an Ethereum-compatible development network and write Solidity contracts, or deploy Hyperledger Fabric and implement chaincode. Do not treat Solidity and Fabric chaincode as interchangeable.
5. Implement document-signing requests, signature verification, and audit history.
6. Add authorized mutation/transfer workflows with append-only record versions.
7. Test access control, invalid signatures, tampered documents, duplicate requests, key revocation, and network failures.

## Security Notes

- Use synthetic or authorized records during development.
- Require officer review before committing OCR-derived data.
- Keep private keys and seed phrases under user control.
- Minimize personal data stored on-chain.
- Enforce authorization in both the API and blockchain business rules.
- Preserve earlier record versions instead of silently overwriting history.
- Legal recognition depends on applicable law and official processes, not software alone.
