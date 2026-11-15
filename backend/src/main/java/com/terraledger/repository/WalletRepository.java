package com.terraledger.repository;

import com.terraledger.domain.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, String> {
    List<Wallet> findByUserId(String userId);
    Optional<Wallet> findByAddressAndRevokedAtIsNull(String address);
    Optional<Wallet> findByUserIdAndAddressAndRevokedAtIsNull(String userId, String address);
}
