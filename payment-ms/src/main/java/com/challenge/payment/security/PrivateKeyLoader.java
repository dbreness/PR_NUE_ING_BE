package com.challenge.payment.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

final class PrivateKeyLoader {

    private static final String BEGIN_MARKER = "-----BEGIN PRIVATE KEY-----";
    private static final String END_MARKER = "-----END PRIVATE KEY-----";

    private PrivateKeyLoader() {
    }

    static PrivateKey load(Path path) throws IOException, GeneralSecurityException {
        String pem = Files.readString(path, StandardCharsets.US_ASCII);
        if (!pem.contains(BEGIN_MARKER) || !pem.contains(END_MARKER)) {
            throw new GeneralSecurityException("La clave privada no tiene formato PKCS#8 PEM");
        }

        String encodedKey = pem
                .replace(BEGIN_MARKER, "")
                .replace(END_MARKER, "")
                .replaceAll("\\s", "");
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decodedKey));
    }
}
