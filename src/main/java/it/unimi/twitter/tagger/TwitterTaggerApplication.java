package it.unimi.twitter.tagger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan("it.unimi.twitter.tagger")
@EnableJpaRepositories("it.unimi.twitter.tagger.repository")
@EntityScan("it.unimi.twitter.tagger.domain")
@EnableScheduling
public class TwitterTaggerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TwitterTaggerApplication.class, args);
	}

}
