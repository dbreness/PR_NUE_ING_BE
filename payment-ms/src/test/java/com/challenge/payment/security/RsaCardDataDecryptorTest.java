package com.challenge.payment.security;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RsaCardDataDecryptorTest {

    private static final OAEPParameterSpec OAEP_SHA_256 = new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);

    @TempDir
    private Path directory;

    private KeyPair keyPair;
    private RsaCardDataDecryptor decryptor;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        Path privateKey = directory.resolve("private-key.pem");
        Files.writeString(privateKey, toPem("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
        decryptor = new RsaCardDataDecryptor(privateKey, JsonMapper.builder().build());
    }

    @Test
    void decryptsOaepSha256PayloadCompatibleWithBrowserContract() throws Exception {
        String json = """
                {"cardNumber":"test-number","expiration":"12/30","cvv":"test-code"}
                """;

        CardData result = decryptor.decrypt(encrypt(json));

        assertThat(result).isEqualTo(new CardData("test-number", "12/30", "test-code"));
        assertThat(result.toString()).doesNotContain("test-number", "12/30", "test-code");
    }

    @Test
    void rejectsInvalidCiphertextWithSanitizedMessage() {
        assertThatThrownBy(() -> decryptor.decrypt("not-base64"))
                .isInstanceOf(CardDataDecryptionException.class)
                .hasMessage("No fue posible descifrar los datos de pago");
    }

    @Test
    void rejectsDecryptedJsonWithMissingFields() throws Exception {
        assertThatThrownBy(() -> decryptor.decrypt(encrypt("{\"cardNumber\":\"test-number\"}")))
                .isInstanceOf(CardDataDecryptionException.class)
                .hasMessage("No fue posible descifrar los datos de pago");
    }

    @Test
    void rejectsNonPkcs8PrivateKeyAtStartup() throws Exception {
        Path invalidKey = directory.resolve("invalid.pem");
        Files.writeString(invalidKey, "-----BEGIN RSA PRIVATE KEY-----\ninvalid\n-----END RSA PRIVATE KEY-----\n");

        assertThatThrownBy(() -> new RsaCardDataDecryptor(invalidKey, JsonMapper.builder().build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No fue posible cargar la clave privada RSA");
    }

    private String encrypt(String plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic(), OAEP_SHA_256);
        return Base64.getEncoder().encodeToString(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
    }

    private static String toPem(String type, byte[] encoded) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded)
                + "\n-----END " + type + "-----\n";
    }
}
