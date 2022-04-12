package main;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication
@EnableJpaRepositories
public class Main {

    @Autowired
    private YAMLConfig config;

    public static void main(String[] args) {

        SpringApplication.run(Main.class);

    }
}
