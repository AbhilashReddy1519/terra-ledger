# ADR-003: Document Privacy — AES-256-GCM Encryption Before IPFS

**Date:** 2026-11-15  
**Status:** Accepted  
**Milestone:** M0

## Context

IPFS content is readable by anyone with the CID. Land records contain personal data.

## Decision

Encrypt every document with **AES-256-GCM** before writing to IPFS. The SHA-256 hash is computed on the **original plaintext bytes** before encryption, and that hash is what goes on-chain for verification.

## Consequences

- CID leakage does not expose document content.
- Verification is: hash(uploaded file) == chain hash.
- Key rotation procedure required (documented in M4).
