package com.terraledger.service;

import org.springframework.stereotype.Service;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.Arrays;

@Service
public class SignatureService {

    /**
     * Recovers the Ethereum address that signed the message using EIP-191 personal_sign.
     *
     * @param message  The original plain text message that was signed
     * @param signatureHex 65-byte hex string (with or without 0x prefix)
     * @return 0x-prefixed lowercase Ethereum address
     */
    public String recoverAddress(String message, String signatureHex) {
        try {
            byte[] signatureBytes = Numeric.hexStringToByteArray(signatureHex);
            if (signatureBytes.length != 65) {
                throw new IllegalArgumentException("Invalid signature length: expected 65 bytes, got " + signatureBytes.length);
            }

            byte v = signatureBytes[64];
            if (v < 27) {
                v += 27; // normalize v
            }

            byte[] r = Arrays.copyOfRange(signatureBytes, 0, 32);
            byte[] s = Arrays.copyOfRange(signatureBytes, 32, 64);

            Sign.SignatureData signatureData = new Sign.SignatureData(v, r, s);
            BigInteger publicKey = Sign.signedPrefixedMessageToKey(
                    message.getBytes(StandardCharsets.UTF_8),
                    signatureData
            );

            String address = "0x" + Keys.getAddress(publicKey).toLowerCase();
            return address;
        } catch (SignatureException e) {
            throw new IllegalArgumentException("Could not recover address from signature: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid signature: " + e.getMessage(), e);
        }
    }
}
