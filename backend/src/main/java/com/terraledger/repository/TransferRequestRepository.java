package com.terraledger.repository;

import com.terraledger.domain.TransferRequest;
import com.terraledger.domain.TransferRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransferRequestRepository extends JpaRepository<TransferRequest, String> {

    List<TransferRequest> findByRecordId(String recordId);

    List<TransferRequest> findBySellerUserId(String sellerUserId);

    List<TransferRequest> findByStatus(TransferRequestStatus status);

    Optional<TransferRequest> findBySigningRequestId(String signingRequestId);
}
