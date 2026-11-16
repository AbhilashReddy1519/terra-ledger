package com.terraledger.repository;

import com.terraledger.domain.LandRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LandRecordRepository extends JpaRepository<LandRecord, String> {
    List<LandRecord> findByUploadedByOrderByCreatedAtDesc(String uploadedBy);
    List<LandRecord> findAllByOrderByCreatedAtDesc();
}
