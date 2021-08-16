package it.unimi.twitter.tagger.test.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.unimi.twitter.tagger.configuration.TwitterAuthApi;
import it.unimi.twitter.tagger.controller.twitter.gateway.TwitterGatewayController;
import it.unimi.twitter.tagger.dto.TwitterRequestDto;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "prod")
@Slf4j
class TwitterTaggerControllerTest {
	
	public static final String NAIVEBAYES_API_V1 = "/api/v1/naivebayes/";
	
	private MockMvc webTestClient;
	
	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private TwitterAuthApi twitterAuthApi;
		
	@BeforeEach
	void setUp() throws Exception {
		webTestClient = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		twitterAuthApi.setBearer("AAAAAAAAAAAAAAAAAAAAACj2PQEAAAAAaPyCSHbaQXIGpYKKY19dATZxntQ%3DrALIU5jai8zyDTvBTFN9n7BNcZ3NMJbrAsuCcBv5ZSqUkLQKVf");
		// give some time to the streamer
		TimeUnit.SECONDS.sleep(10);
	}
	
	@Test
	void test01_retrievePredictedTweet() throws Exception {
	    
	    // call the controller 
		String url = NAIVEBAYES_API_V1 + "predict" + "/";
		ResultActions ret = webTestClient.perform(MockMvcRequestBuilders.get(url)
																		.header("Authorization", twitterAuthApi.getBearer())
																        .contentType(MediaType.APPLICATION_JSON)
																        .accept(MediaType.APPLICATION_JSON))
																        .andExpect(status().is2xxSuccessful());

		log.info("Backend response:" + System.lineSeparator() + ret.andReturn().getResponse().getContentAsString());
	}

}
