# ADR-005: OCR — Tesseract + OpenCV Python Service

**Date:** 2026-11-15  
**Status:** Accepted  
**Milestone:** M0

## Context

Scanned land records need OCR to extract fields for officer review.

## Decision

A small **FastAPI** service runs **Tesseract** with **OpenCV** preprocessing. EasyOCR is a fallback if accuracy proves too low after officer review data is available.

## Consequences

- Heavy image libraries stay out of the Spring Boot JVM.
- The officer reviews every result, so perfect OCR is not required for the MVP.
- The service is containerised; Tesseract version is pinned in the Dockerfile.
