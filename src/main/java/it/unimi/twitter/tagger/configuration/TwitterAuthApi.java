package it.unimi.twitter.tagger.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
public class TwitterAuthApi {

	private String bearer;
	
	@Bean(name="TwitterAuthApi")
	public void twitterAuth() {}
	
}
