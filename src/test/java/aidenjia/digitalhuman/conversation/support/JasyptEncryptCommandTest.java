package con.aidenjia.digitalhuman.conversation.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.junit.jupiter.api.Test;

class JasyptEncryptCommandTest {

    @Test
    void encryptAndDecryptRoundTrip() {
        String password = "test-jasypt-key";
        String plain = "secret-value";
        String enc = JasyptEncryptCommand.encrypt(password, plain);

        assertThat(enc).startsWith("ENC(").endsWith(")");

        PooledPBEStringEncryptor decryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(password);
        config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setPoolSize("1");
        decryptor.setConfig(config);

        String cipher = enc.substring(4, enc.length() - 1);
        assertThat(decryptor.decrypt(cipher)).isEqualTo(plain);
    }
}
