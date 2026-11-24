package con.aidenjia.digitalhuman.conversation.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiSecurityStartupValidatorTest {

    @Test
    @DisplayName("demo 开启且 token/userId 齐全时通过")
    void demoEnabledWithTokenPasses() {
        ApiSecurityProperties props = new ApiSecurityProperties();
        props.setIssuerUri("https://issuer/");
        props.setDemoTokenEnabled(true);
        props.setDemoToken("atk_x");
        props.setDemoUserId("demo-ue5");
        assertDoesNotThrow(() -> new ApiSecurityStartupValidator(props).validateOnReady());
    }

    @Test
    @DisplayName("demo 开启但缺 token 时启动失败")
    void demoEnabledMissingTokenFails() {
        ApiSecurityProperties props = new ApiSecurityProperties();
        props.setIssuerUri("https://issuer/");
        props.setDemoTokenEnabled(true);
        props.setDemoToken("");
        props.setDemoUserId("demo-ue5");
        assertThrows(
                IllegalStateException.class,
                () -> new ApiSecurityStartupValidator(props).validateOnReady());
    }
}
