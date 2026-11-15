# ADR-002: Wallet Key Scheme — secp256k1 + EIP-191

**Date:** 2026-11-15  
**Status:** Accepted  
**Milestone:** M0

## Context

The browser needs to sign challenges and approvals. The Web Crypto API does not support secp256k1.

## Decision

Use **secp256k1 keys** in the browser via **ethers.js**, signing with EIP-191 `personal_sign`. The backend verifies signatures with `ecrecover` (via Web3j).

## Consequences

- Consistent with the EVM chain key scheme.
- ethers.js is a well-audited library.
- Backend verification is deterministic and stateless.
