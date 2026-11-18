package com.terraledger.dto;

import java.util.List;

public class ReconcileReport {
    private int totalChainEvents;
    private int matchedCount;
    private int rebuiltCount;
    private List<String> discrepancies;
    private boolean cacheMatchesChain;

    public ReconcileReport() {}

    public ReconcileReport(int totalChainEvents, int matchedCount, int rebuiltCount, List<String> discrepancies, boolean cacheMatchesChain) {
        this.totalChainEvents = totalChainEvents;
        this.matchedCount = matchedCount;
        this.rebuiltCount = rebuiltCount;
        this.discrepancies = discrepancies;
        this.cacheMatchesChain = cacheMatchesChain;
    }

    public int getTotalChainEvents() { return totalChainEvents; }
    public void setTotalChainEvents(int totalChainEvents) { this.totalChainEvents = totalChainEvents; }

    public int getMatchedCount() { return matchedCount; }
    public void setMatchedCount(int matchedCount) { this.matchedCount = matchedCount; }

    public int getRebuiltCount() { return rebuiltCount; }
    public void setRebuiltCount(int rebuiltCount) { this.rebuiltCount = rebuiltCount; }

    public List<String> getDiscrepancies() { return discrepancies; }
    public void setDiscrepancies(List<String> discrepancies) { this.discrepancies = discrepancies; }

    public boolean isCacheMatchesChain() { return cacheMatchesChain; }
    public void setCacheMatchesChain(boolean cacheMatchesChain) { this.cacheMatchesChain = cacheMatchesChain; }

    public int totalChainEvents() { return totalChainEvents; }
    public int matchedCount() { return matchedCount; }
    public int rebuiltCount() { return rebuiltCount; }
    public List<String> discrepancies() { return discrepancies; }
    public boolean cacheMatchesChain() { return cacheMatchesChain; }
}
