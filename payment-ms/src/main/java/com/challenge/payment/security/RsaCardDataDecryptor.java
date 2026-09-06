package com.challenge.payment.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

@Component
public class RsaCardDataDecryptor {

    private static final OAEPParameterSpec OAEP_SHA_256 = new OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT
    );

    private final PrivateKey privateKey;
    private final ObjectMapper objectMapper;

    public RsaCardDataDecryptor(
            @Value("${app.rsa.private-key-path}") Path privateKeyPath,
            ObjectMapper objectMapper
    ) {
        this.privateKey = loadPrivateKey(privateKeyPath);
        this.objectMapper = objectMapper;
    }

    public CardData decrypt(String encryptedCardData) {
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedCardData);
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey, OAEP_SHA_256);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return objectMapper.readValue(decryptedBytes, CardData.class);
        } catch (GeneralSecurityException | IllegalArgumentException | IOException exception) {
            throw new CardDataDecryptionException(exception);
        }
    }

    private static PrivateKey loadPrivateKey(Path path) {
        try {
            return PrivateKeyLoader.load(path);
        } catch (Exception exception) {
            throw new IllegalStateException("No fue posible cargar la clave privada RSA", exception);
        }
    }
}
