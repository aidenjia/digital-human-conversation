package con.aidenjia.digitalhuman.conversation.support;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

/**
 * 本地加密工具：生成 ENC(...) 写入 .env。
 *
 * <pre>
 * mvn -q test-compile exec:java \
 *   -Dexec.classpathScope=test \
 *   -Dexec.mainClass=con.aidenjia.digitalhuman.conversation.support.JasyptEncryptCommand \
 *   -Djasypt.password=你的主密钥 \
 *   -Djasypt.plain=要加密的明文
 * </pre>
 */
public final class JasyptEncryptCommand {

    private JasyptEncryptCommand() {}

    public static void main(String[] args) {
        String password = System.getProperty("jasypt.password");
        String plain = System.getProperty("jasypt.plain");
        if (password == null || password.isBlank() || plain == null) {
            System.err.println("Usage: -Djasypt.password=... -Djasypt.plain=...");
            System.exit(1);
        }
        System.out.println(encrypt(password, plain));
    }

    static String encrypt(String password, String plain) {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(password);
        config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setPoolSize("1");
        encryptor.setConfig(config);
        return "ENC(" + encryptor.encrypt(plain) + ")";
    }
}
