import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

/**
 * GENERADOR DE LLAVES RSA (2048 bits) - COMPATIBILIDAD FRONT/BACK Y SEGURIDAD POSIX.
 * - Clave Pública: Formato SPKI/X.509 (Requerido por la API criptográfica de React).
 * - Clave Privada: Formato PKCS#8 (Requerido nativamente por Spring Boot / Java).
 */
public final class RsaKeyGenerator {

    private static final int RSA_KEY_SIZE = 2048;
    private static final Path DEFAULT_OUTPUT_DIRECTORY = Path.of("local-keys");
    private static final String PUBLIC_KEY_FILE = "public-key.pem";
    private static final String PRIVATE_KEY_FILE = "private-key.pem";

    private RsaKeyGenerator() {
    }

    public static void main(String[] args) {
        try {
            Configuration configuration = Configuration.parse(args);
            generate(configuration.outputDirectory(), configuration.force());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            System.err.println("Error: " + exception.getMessage());
            printUsage();
            System.exit(2);
        } catch (GeneralSecurityException | IOException exception) {
            System.err.println("No fue posible generar las claves RSA: " + exception.getMessage());
            System.exit(1);
        }
    }

    static void generate(Path outputDirectory, boolean force)
            throws GeneralSecurityException, IOException {
        Path publicDirectory = outputDirectory.resolve("public").toAbsolutePath().normalize();
        Path privateDirectory = outputDirectory.resolve("private").toAbsolutePath().normalize();
        Path publicKeyPath = publicDirectory.resolve(PUBLIC_KEY_FILE);
        Path privateKeyPath = privateDirectory.resolve(PRIVATE_KEY_FILE);

        // GUARDARRAÍL ANTIDESASTRE: Evita sobrescribir llaves activas y dejar datos de la DB ilegibles.
        rejectExistingKeys(publicKeyPath, privateKeyPath, force);

        Files.createDirectories(publicDirectory);
        Files.createDirectories(privateDirectory);

        // SEGURIDAD DEL DIRECTORIO PRIVADO: Permisos drásticos de lectura/escritura (Propietario único).
        setPosixPermissionsIfSupported(privateDirectory, "rwx------");

        // INICIALIZACIÓN: Uso del algoritmo asimétrico estándar RSA.
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(RSA_KEY_SIZE);
        KeyPair keyPair = generator.generateKeyPair();

        // ESCRITURA SEGURA EN DISCO: Se escribe primero en archivos temporales para evitar corrupción de datos.
        Path temporaryPublicKey = Files.createTempFile(publicDirectory, ".public-key-", ".tmp");
        Path temporaryPrivateKey = Files.createTempFile(privateDirectory, ".private-key-", ".tmp");

        try {
            // EXPORTACIÓN EN FORMATO PEM: Codificación compatible entre plataformas (React <-> Spring Boot).
            writePem(temporaryPublicKey, "PUBLIC KEY", keyPair.getPublic().getEncoded());
            writePem(temporaryPrivateKey, "PRIVATE KEY", keyPair.getPrivate().getEncoded());


            // HIGIENE DE ARCHIVOS EN UNIX: El dueño puede leer/escribir; el resto del sistema no tiene acceso.
            setPosixPermissionsIfSupported(temporaryPublicKey, "rw-r--r--");
            setPosixPermissionsIfSupported(temporaryPrivateKey, "rw-------");

            // MOVIMIENTO ATÓMICO: El archivo temporal reemplaza al definitivo en un único paso de hardware.
            moveIntoPlace(temporaryPublicKey, publicKeyPath, force);
            moveIntoPlace(temporaryPrivateKey, privateKeyPath, force);
        } finally {
            // LIMPIEZA DE BASURA: Garantiza que nunca queden residuos de claves expuestos en disco temporal.
            Files.deleteIfExists(temporaryPublicKey);
            Files.deleteIfExists(temporaryPrivateKey);
        }

        System.out.println("Claves RSA de " + RSA_KEY_SIZE + " bits generadas correctamente.");
        System.out.println("Clave publica: " + publicKeyPath);
        System.out.println("Clave privada: " + privateKeyPath);
    }

    private static void rejectExistingKeys(Path publicKeyPath, Path privateKeyPath, boolean force) {
        if (!force && (Files.exists(publicKeyPath) || Files.exists(privateKeyPath))) {
            throw new IllegalStateException(
                    "Ya existe al menos una clave. Use --force unicamente si desea reemplazar el par.");
        }
    }

    private static void writePem(Path destination, String type, byte[] encodedKey)
            throws IOException {
        // ENMASCARADO Base64 MIME: Formatea la clave en bloques legibles de 64 caracteres por línea.
        String encoded = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encodedKey);
        String pem = "-----BEGIN " + type + "-----\n"
                + encoded
                + "\n-----END " + type + "-----\n";
        Files.writeString(
                destination,
                pem,
                StandardCharsets.US_ASCII,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void moveIntoPlace(Path source, Path destination, boolean force)
            throws IOException {
        // ATOMIC_MOVE garantiza que el sistema operativo complete la operación sin interrupciones físicas.
        StandardCopyOption[] options = force
                ? new StandardCopyOption[]{StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING}
                : new StandardCopyOption[]{StandardCopyOption.ATOMIC_MOVE};
        try {
            Files.move(source, destination, options);
        } catch (AtomicMoveNotSupportedException exception) {
            if (force) {
                // Fallback de contingencia si el sistema de archivos del host no admite movimientos atómicos.
                Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, destination);
            }
        }
    }

    private static void setPosixPermissionsIfSupported(Path path, String permissions)
            throws IOException {
        // COMPATIBILIDAD MULTIPLATAFORMA: Solo aplica reglas POSIX en hosts Linux / macOS. Evita caídas en Windows.
        if (Files.getFileAttributeView(path, PosixFileAttributeView.class) != null) {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(permissions));
        }
    }

    private static void printUsage() {
        System.err.println(
                "Uso: java RsaKeyGenerator.java [--output-dir <directorio>] [--force]");
    }

    private record Configuration(Path outputDirectory, boolean force) {

        private static Configuration parse(String[] args) {
            Path outputDirectory = DEFAULT_OUTPUT_DIRECTORY;
            boolean force = false;

            for (int index = 0; index < args.length; index++) {
                switch (args[index]) {
                    case "--output-dir" -> {
                        if (index + 1 >= args.length || args[index + 1].isBlank()) {
                            throw new IllegalArgumentException(
                                    "--output-dir requiere una ruta no vacía.");
                        }
                        outputDirectory = Path.of(args[++index]);
                    }
                    case "--force" -> force = true;
                    case "--help", "-h" -> {
                        printUsage();
                        System.exit(0);
                    }
                    default -> throw new IllegalArgumentException(
                            "Opcion desconocida: " + args[index]);
                }
            }
            return new Configuration(outputDirectory, force);
        }
    }
}
