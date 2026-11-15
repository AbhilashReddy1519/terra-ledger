# ADR-004: On-Chain Data — No Personal Data

**Date:** 2026-11-15  
**Status:** Accepted  
**Milestone:** M0

## Context

Personal data on an immutable ledger cannot be erased (GDPR/right-to-erasure conflict).

## Decision

On-chain: **record ID, version, CID, SHA-256, status, approver address, timestamp only**. No names, survey numbers, areas, or boundaries on-chain. All personal data stays in MySQL under access control.

## Consequences

- GDPR-compatible: personal data can be deleted from MySQL without touching the chain.
- The chain proves integrity of the committed file, not the content of the OCR fields.
