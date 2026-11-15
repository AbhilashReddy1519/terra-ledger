package com.terraledger.service;

import com.terraledger.domain.AuthChallenge;
import com.terraledger.domain.Role;
import com.terraledger.domain.User;
import com.terraledger.domain.Wallet;
import com.terraledger.dto.*;
import com.terraledger.repository.AuthChallengeRepository;
import com.terraledger.repository.UserRepository;
import com.terraledger.repository.WalletRepository;
import com.terraledger.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final AuthChallengeRepository challengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final SignatureService signatureService;

    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       WalletRepository walletRepository,
                       AuthChallengeRepository challengeRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       SignatureService signatureService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.challengeRepository = challengeRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.signatureService = signatureService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        String hash = passwordEncoder.encode(req.getPassword());
        User user = new User(req.getEmail(), hash, req.getRole());
        user = userRepository.save(user);

        String token = tokenProvider.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getRole().name(), null);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = tokenProvider.generateToken(user);
        String walletAddr = walletRepository.findByUserId(user.getId()).stream()
                .filter(w -> w.getRevokedAt() == null)
                .map(Wallet::getAddress)
                .findFirst()
                .orElse(null);

        return new AuthResponse(token, user.getId(), user.getEmail(), user.getRole().name(), walletAddr);
    }

    @Transactional
    public ChallengeResponse createChallenge(String userId) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String nonce = HexFormat.of().formatHex(randomBytes);

        Instant expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES);
        String message = "Terra Ledger Auth Challenge\nNonce: " + nonce + "\nExpires: " + expiresAt.toString();

        AuthChallenge challenge = new AuthChallenge(nonce, userId, message, expiresAt);
        challengeRepository.save(challenge);

        return new ChallengeResponse(nonce, message, expiresAt);
    }

    @Transactional
    public AuthResponse verifyChallenge(VerifyChallengeRequest req, String authenticatedUserId) {
        AuthChallenge challenge = challengeRepository.findByNonce(req.getNonce())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challenge not found"));

        if (challenge.isUsed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challenge already used");
        }

        if (challenge.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challenge has expired");
        }

        String recoveredAddress;
        try {
            recoveredAddress = signatureService.recoverAddress(challenge.getMessage(), req.getSignature());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid signature: " + e.getMessage());
        }

        // Mark challenge as used immediately to prevent replay
        challenge.setUsedAt(Instant.now());
        challengeRepository.save(challenge);

        // Determine user
        String targetUserId = authenticatedUserId != null ? authenticatedUserId : challenge.getUserId();
        User user = null;

        if (targetUserId != null) {
            user = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            // Link wallet if not already linked
            Optional<Wallet> existing = walletRepository.findByUserIdAndAddressAndRevokedAtIsNull(user.getId(), recoveredAddress);
            if (existing.isEmpty()) {
                Wallet wallet = new Wallet(user.getId(), recoveredAddress);
                walletRepository.save(wallet);
            }
        } else {
            // Find existing user linked to this wallet address
            Optional<Wallet> existingWallet = walletRepository.findByAddressAndRevokedAtIsNull(recoveredAddress);
            if (existingWallet.isPresent()) {
                user = userRepository.findById(existingWallet.get().getUserId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found for wallet"));
            } else {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No user linked to wallet address: " + recoveredAddress);
            }
        }

        String token = tokenProvider.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getRole().name(), recoveredAddress);
    }
}
