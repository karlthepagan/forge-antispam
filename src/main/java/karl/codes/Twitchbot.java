package karl.codes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 */
@SpringBootApplication
public class Twitchbot {
    public static void main(String[] argv) {
        SpringApplication app = new SpringApplication(Twitchbot.class);
        app.setAdditionalProfiles("secret");
        app.run(argv);
    }
}
