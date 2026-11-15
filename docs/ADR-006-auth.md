# ADR-006: Authentication — Email/Password + Wallet Challenge + JWT

**Date:** 2026-11-15  
**Status:** Accepted  
**Milestone:** M0

## Context

The system needs role-based access control (Citizen, Revenue Officer, Admin) and wallet-linked identity.

## Decision

- **Email + password** for initial login.
- **Wallet challenge-signature** (EIP-191) to link a wallet or as a second factor.
- **Short-lived JWT** (1 hour) issued by Spring Security.
- Roles: `CITIZEN`, `OFFICER`, `ADMIN`.

## Consequences

- Wallet-only login can be added later without breaking existing sessions.
- Challenge nonces are single-use and expire (prevents replay).
- JWT is stateless; revocation requires short expiry or a denylist (added in hardening).
