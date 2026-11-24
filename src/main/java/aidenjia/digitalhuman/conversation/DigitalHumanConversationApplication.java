package con.aidenjia.digitalhuman.conversation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DigitalHumanConversationApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalHumanConversationApplication.class, args);
    }
}
