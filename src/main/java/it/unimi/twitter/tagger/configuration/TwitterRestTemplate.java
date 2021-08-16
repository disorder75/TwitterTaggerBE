package it.unimi.twitter.tagger.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class TwitterRestTemplate {

	@Bean(name="TwitterRestTemplate")
	public RestTemplate restTemplate() {
	    return new RestTemplate();
	}
	
}
