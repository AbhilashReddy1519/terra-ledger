package com.terraledger.repository;

import com.terraledger.domain.SigningRequest;
import com.terraledger.domain.SigningRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SigningRequestRepository extends JpaRepository<SigningRequest, String> {

    Optional<SigningRequest> findByNonce(String nonce);

    List<SigningRequest> findByExpectedSignerIgnoreCaseAndStatus(String expectedSigner, SigningRequestStatus status);

    List<SigningRequest> findByDocument(String document);

    List<SigningRequest> findByStatusAndExpiresAtBefore(SigningRequestStatus status, Instant now);
}
