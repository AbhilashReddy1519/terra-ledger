# ADR-001: Blockchain Path — Solidity on Hardhat (EVM)

**Date:** 2026-11-15  
**Status:** Accepted  
**Milestone:** M0

## Context

The system needs an immutable append-only ledger for land record commitments. Options evaluated:

- Hyperledger Fabric with Chaincode (Go/Java)
- Solidity on an EVM chain (Hardhat local → Hyperledger Besu permissioned)

## Decision

Use **Solidity on Hardhat** locally for development and MVP. Migrate to Hyperledger Besu (permissioned) in M8.

## Consequences

- One contract language across all environments.
- Standard secp256k1 wallet keys; mature Web3j Java client for the backend.
- Hardhat gives deterministic accounts and instant mining for tests.
- Fabric dropped entirely from the stack.

## Alternatives rejected

Hyperledger Fabric requires a separate chaincode language and a more complex network setup. It would only be chosen if a government partner mandated it.
