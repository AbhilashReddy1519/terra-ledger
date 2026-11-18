package com.terraledger.repository;

import com.terraledger.domain.RecordVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecordVersionRepository extends JpaRepository<RecordVersion, String> {

    List<RecordVersion> findByRecordIdOrderByVersionAsc(String recordId);

    Optional<RecordVersion> findByRecordIdAndVersion(String recordId, Integer version);

    Optional<RecordVersion> findTopByRecordIdOrderByVersionDesc(String recordId);

    Optional<RecordVersion> findByTxHash(String txHash);
}
