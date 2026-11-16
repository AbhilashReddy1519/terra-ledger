package com.terraledger.repository;

import com.terraledger.domain.OcrExtraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OcrExtractionRepository extends JpaRepository<OcrExtraction, String> {
    List<OcrExtraction> findByRecordId(String recordId);
}
