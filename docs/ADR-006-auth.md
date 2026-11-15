# ADR-006: Authentication — Email/Password + Wallet Challenge + JWT

**Date:** 2026-11-15  
**Status:** Accepted  
**Milestone:** M1

## Context

The system needs role-based access control (Citizen, Revenue Officer, Admin) and wallet-linked identity.

## Decision

- **Email + password** for initial registration and login.
- **Wallet challenge-signature** (EIP-191 `personal_sign`) to link a wallet or sign in with a Web3 wallet.
- **Cryptographic verification**: Java Web3j `Sign.signedPrefixedMessageToKey` validates the EIP-191 signature against the nonce challenge message.
- **Single-use replay protection**: Each challenge record has a random UUID nonce, 5-minute expiry, and is flagged as verified/consumed upon verification. Replayed or expired challenges are rejected with HTTP 400.
- **Short-lived JWT** (1 hour) issued by Spring Security containing user id, email, and role.
- Roles: `CITIZEN`, `OFFICER`, `ADMIN` enforced via Spring Security method security and HTTP request matchers.

## Consequences

- Wallet-only login can be added later without breaking existing sessions.
- Challenge nonces are single-use and expire (prevents replay).
- JWT is stateless; revocation requires short expiry or a denylist (added in hardening).
