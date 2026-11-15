# Land Records Wallet: Build Plan

Nov 5, 2026 · @Abhilash Reddy

Build the wallet in nine small milestones: a one-record walking skeleton first, then a full upload-to-verify MVP, then transfers and hardening.

## Decisions to lock first

Six choices change the code you write in every later milestone, so settle them before Milestone 0.

| Decision | Recommendation | Why |
| --- | --- | --- |
| Blockchain path | Solidity on an EVM chain: Hardhat locally for the MVP, Hyperledger Besu (permissioned) later | One contract language, standard wallet keys, mature Java client (Web3j). Choose Fabric only if a partner mandates it, and then drop Solidity entirely. |
| Wallet key scheme | secp256k1 keys in the browser using ethers.js, signing with EIP-191 `personal_sign` | The Web Crypto API does not support secp256k1, so the stack table's Web Crypto line would not match an EVM chain. The backend verifies with ecrecover (Web3j). |
| Document privacy | Encrypt the file (AES-256-GCM) before it goes to IPFS; hash the original bytes | IPFS content is readable by anyone who has the CID. The SHA-256 of the original file is what verification compares against. |
| On-chain data | Record ID, version, CID, SHA-256, status, approver address, timestamp. No names or areas. | Personal data on an immutable ledger cannot be erased. Keep it in MySQL under access control. |
| OCR | Tesseract with OpenCV preprocessing in a small Python service; EasyOCR only if accuracy is too low | Keeps heavy image libraries out of the Spring Boot app. The officer reviews every result, so perfect OCR is not required for the MVP. |
| Sign-in | Email and password plus wallet-signature challenge, short-lived JWT, Spring Security roles | Matches your plan. Wallet-only login can come later. |

Remove from the stack list whatever you do not choose (the Fabric Gateway client, if you take Solidity). A smaller stack is a faster build.

## Architecture at a glance

&#91;embedded content: architecture · 6 components\]

The React wallet never talks to MySQL, IPFS or the chain directly. Every request goes through Spring Boot, and the contract checks permissions again on its own.

## Skeleton: one thin path through every layer

The skeleton is Milestone 0: a hardcoded record travels from a React button through Spring Boot onto a local chain and back to the screen. It proves every service talks to every other before any real feature exists.

Start with this repository layout:

```
land-wallet/
  docker-compose.yml     # MySQL, IPFS (Kubo), Hardhat node, ocr-service
  backend/               # Spring Boot, Java 21, Flyway migrations
  frontend/              # React + Vite + ethers.js
  contracts/             # Hardhat project: LandRegistry.sol and its tests
  ocr-service/           # FastAPI + Tesseract + OpenCV
  sample-data/           # synthetic scanned records only
  docs/                  # this plan, decisions (ADRs), API spec
```

Rules that keep the build fast:

- One branch and one pull request per milestone; each ends with a working `docker compose up`.
- Continuous integration from day one: backend tests, contract tests and a frontend build on every push.
- A `.env.example` with every setting; no keys or secrets in the repository.
- A short decision record in `docs/` for each row of the decisions table, so the reasoning survives.

Done when: one command starts all services, and a button in the browser shows a record that was read back from the contract.

## MVP scope: upload, review, commit, verify

The MVP is done when one officer-approved land record can be committed to the chain and anyone with the file can verify it, with role checks enforced in both the API and the contract.

| In the MVP | Left out until after it |
| --- | --- |
| Registration, login, wallet link and signature challenge | Real identity checks (Aadhaar or DigiLocker style integrations) |
| Three roles: Citizen, Revenue Officer, Admin | Registrar, bank, court and public verifier roles |
| Scan upload, OCR, officer review and approval | Batch import, handwriting and multi-language OCR tuning |
| Encrypted storage in IPFS, SHA-256 hash, commit to the contract | Multi-node pinning and a permissioned multi-organisation network |
| Hash verification page (upload a file, see match or mismatch) | Public verification portal with access policies |
| Audit log of every action | Transfers, mutations and multi-party consent signing |
| Record versioning in the contract (append-only) | Key revocation and recovery workflows |

The MVP deliberately stops before transfers. Transfers add multi-party signing and legal rules, and they only make sense once a single record is trustworthy end to end.

## Milestones

Milestones M0 to M6 make the MVP and take about 10 weeks for one person at 10 to 15 hours a week; M7 and M8 follow. The week counts are my estimates, so adjust them after M1 shows your real pace.

| # | Milestone | Deliverable | Done when | Weeks |
| --- | --- | --- | --- | --- |
| M0 | Walking skeleton | Docker Compose stack; hardcoded record goes UI to API to contract and back | `docker compose up` works and the UI shows a record read from the chain | 1 |
| M1 | Sign-in and roles | Registration, login, JWT, wallet link, challenge-signature login, role dashboards | A replayed or expired challenge is rejected; a citizen calling an officer endpoint gets 403 | 2 |
| M2 | Upload and OCR | Scan upload, OCR service, extracted fields shown to the user | Ten synthetic scans give editable fields; wrong file types are refused | 2 |
| M3 | Officer review | Review queue, field correction, approve or reject, audit log | Nothing moves forward without approval; every state change is logged with who and when | 1 |
| M4 | Storage and hash | Encrypt, store in IPFS, keep CID and SHA-256 | A stored file decrypts to bytes whose hash equals the stored hash | 1 |
| M5 | Contract and commit | `LandRegistry.sol`, Web3j gateway, MySQL cache, reconcile job | An approved record commits; a non-officer transaction reverts; a cache rebuilt from the chain matches | 2 |
| M6 | Verification (MVP complete) | Page that hashes an uploaded file and compares it with the chain | The original file matches; changing one byte gives a mismatch | 1 |
| M7 | Signing requests | Request ID, nonce, expiry, canonical payload, wallet signature, backend verification | Expired, replayed and wrong-signer signatures are all rejected | 2 |
| M8 | Mutation and transfer | Registrar role, consent signing, new record versions, move to a permissioned Besu network | A transfer needs seller consent and registrar approval; old versions stay readable | 3 |

Work each milestone in this order: write the test that defines "done", build the thinnest version that passes, then clean up. Do not start a milestone until the previous one is demonstrable.

## Data model, API and contract interface

MySQL holds working data, the contract holds committed truth, and the two meet at one table: `record_versions`.

| Table | Key columns | Added in |
| --- | --- | --- |
| `users` | id, email, password\_hash, role, status | M1 |
| `wallets` | user\_id, address, linked\_at, revoked\_at | M1 |
| `auth_challenges` | nonce, user\_id, expires\_at, used\_at | M1 |
| `land_records` | id, survey\_no, owner\_name, area, boundaries, status, current\_version | M2 |
| `ocr_extractions` | record\_id, field, extracted\_value, corrected\_value | M2 |
| `audit_events` | actor, action, entity, entity\_id, at, detail | M3 |
| `record_versions` | record\_id, version, cid, sha256, tx\_hash, approved\_by, committed\_at | M5 |
| `signing_requests` | request\_id, document, action, nonce, expires\_at, signature | M7 |

Core API, each checked by Spring Security role rules:

| Endpoint | Who | Purpose |
| --- | --- | --- |
| `POST /auth/register`, `POST /auth/login` | Anyone | Account and JWT |
| `POST /auth/wallet/challenge`, `POST /auth/wallet/verify` | Signed-in user | Link wallet or log in with a signature |
| `POST /records` (multipart) | Citizen, Officer | Upload a scan; triggers OCR |
| `GET /records/{id}` | Owner, Officer | View a record and its status |
| `POST /records/{id}/approve`, `/reject` | Officer | Review decision; approval starts IPFS and commit |
| `POST /verify` (file) | Any signed-in user | Hash the file and compare with the chain |

The contract stays small and has no update or delete function, so history cannot be rewritten even by an admin:

```solidity
contract LandRegistry {
    function grantOfficer(address a) external;                       // admin only
    function createRecord(bytes32 recordId, string cid, bytes32 sha256Hash) external;  // officer only
    function addVersion(bytes32 recordId, string cid, bytes32 sha256Hash) external;    // officer or registrar
    function getLatest(bytes32 recordId) external view returns (uint256 version, string memory cid, bytes32 sha256Hash, address approver, uint256 at);
    event RecordCommitted(bytes32 indexed recordId, uint256 version, bytes32 sha256Hash, string cid, address approver);
}
```

The backend signs commit transactions with an officer-approved service key held outside the code (environment secret in dev, a key vault later). The officer's own wallet signature on the approval is stored in the audit log as evidence.

## Testing and security checks

Test the failure paths as hard as the happy path, because a records system is judged by what it refuses.

| Layer | What to test | Tooling |
| --- | --- | --- |
| Contract | Non-officer cannot commit; versions only append; events carry the right hash | Hardhat and Chai |
| Backend | Role rules per endpoint; replayed, expired and wrong-key challenges; duplicate uploads; invalid file types | JUnit, Spring Boot Test, Testcontainers (MySQL) |
| Integrity | One changed byte fails verification; cache rebuilt from chain equals MySQL | Integration tests against the local chain |
| OCR | Field accuracy on a fixed set of synthetic scans, tracked per milestone | A small script with expected values |
| Frontend | Login, upload, review and verify flows | Playwright end-to-end |
| Failure modes | Chain node down, IPFS down, OCR timeout: the record stays pending, nothing half-commits | Docker Compose stop and start in tests |

Security habits from day one: use only synthetic records, never log tokens or keys, rate-limit login and challenge endpoints, check authorization in the contract as well as the API, and keep private keys and seed phrases in the browser wallet only.

## How the finished MVP works

After M6, one synthetic land record can travel from a paper scan to a verified chain entry in about ten steps, with a person approving the key one.

1. A citizen signs in with email and password, then proves wallet control by signing a one-time challenge; the dashboard shows only what their role allows.
2. The citizen uploads a scanned record. The backend saves it and sends it to the OCR service.
3. OCR returns owner name, survey or khasra number, area and boundaries. The record shows as "pending review".
4. A revenue officer opens the review queue, compares the fields with the scan, corrects errors and approves or rejects. The decision is written to the audit log.
5. On approval, the backend encrypts the file, stores it in IPFS and gets a CID, and computes the SHA-256 of the original bytes.
6. The backend submits record ID, CID and hash to `LandRegistry`. The contract checks the sender is an officer-authorised address, then emits `RecordCommitted`.
7. The backend stores the transaction hash and version in MySQL and marks the record "committed".
8. Any signed-in user holding the file opens the verify page and uploads it. The backend hashes the bytes and compares them with the hash read from the chain.
9. A match shows "identical to the approved file" with the version, approver and time; a mismatch shows "different from the approved file".
10. A reconcile job periodically rebuilds the cache from chain events and flags any difference.

What a user will not see claimed: a match does not prove the original scan or OCR data was right, and a wallet signature does not prove legal ownership. The screens should say this in plain words, because that honesty is part of the design.

After M8, the same flow extends to a seller requesting a transfer, signing consent in their wallet, a registrar approving, and a new version appended with the old one still readable.

## Risks, prerequisites and what comes next

The biggest technical risk is not the blockchain; it is the backend key that commits records, because whoever holds it can commit anything.

| Risk | Mitigation |
| --- | --- |
| Backend commit key is stolen | Keep it in a secret store, never in the repository. In M8, move to transactions signed by the officer's own wallet. Store the officer's approval signature in the audit log meanwhile. |
| Officer approves wrong OCR data | Show the scan beside the fields, highlight low-confidence values, and sample-audit approved records. |
| Scope creep into transfers and legal rules | Freeze the MVP at M6. Transfers wait for M8. |
| Five services are hard to run | M0 forces one `docker compose up` before any feature work. |
| Lost encryption key makes files unreadable | Write a backup and rotation procedure in M4, and test a restore. |
| MySQL and chain disagree | The chain wins. The reconcile job flags differences and never silently overwrites. |
| Legal validity | Outside software scope; the app records evidence and does not decide ownership. |

**Prerequisites to check before M1.** Public-key signatures versus encryption and why a hash is not a signature; JWT and Spring Security filters; Solidity access-control modifiers, events and gas; IPFS content addressing (a CID names content, it does not protect it); Docker Compose networking.

**Skills this project builds that employers look for.** Threat modelling (who can do what, and what stops them), testing refusal paths, explaining trust boundaries clearly (the chain proves integrity after commit, not truth), and reproducible setup with documented decisions. Keep the decision records and the test suite tidy; they show your thinking better than the feature list.

**After the MVP.** A multi-organisation Besu network, registrar and bank roles, key revocation and recovery, a public verifier portal with access policy, and audit exports for officers.
