package com.terraledger.repository;

import com.terraledger.domain.AuthChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AuthChallengeRepository extends JpaRepository<AuthChallenge, String> {
    Optional<AuthChallenge> findByNonce(String nonce);
}
