package lets_play;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "lets_play.repository")
public class LetsPlayApplication {
    public static void main(String[] args) {
        SpringApplication.run(LetsPlayApplication.class, args);
    }
}