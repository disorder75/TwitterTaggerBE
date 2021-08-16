package it.unimi.twitter.tagger.test.controller.twitter.gateway;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

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

import it.unimi.twitter.tagger.controller.twitter.gateway.TwitterGatewayController;
import it.unimi.twitter.tagger.dto.TwitterRequestDto;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "prod")
@Slf4j
class TwitterGatewayControllerTest {
	
	private MockMvc webTestClient;
	
	@Autowired
	private WebApplicationContext webApplicationContext;

	private TwitterRequestDto twitterRequestDto;
		
	@BeforeEach
	void setUp() throws Exception {
		webTestClient = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		twitterRequestDto = new TwitterRequestDto();
		twitterRequestDto.setBearer("AAAAAAAAAAAAAAAAAAAAACj2PQEAAAAAaPyCSHbaQXIGpYKKY19dATZxntQ%3DrALIU5jai8zyDTvBTFN9n7BNcZ3NMJbrAsuCcBv5ZSqUkLQKVf");
	}

	@Test
	void test01_retrieveUserInfo() throws Exception {

		// set twitter request for user information data
		twitterRequestDto.setRequest("https://api.twitter.com/2/users/by/username/_CaptainUnlucky?expansions=pinned_tweet_id&"
																										   + "user.fields=profile_image_url,created_at,description,url,protected,location,entities,username,verified"
																										   + "&tweet.fields=created_at");

		// set bearer
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonTwitterRequest = objectMapper.writeValueAsString(twitterRequestDto);

		// call the controlelr 
		String url = TwitterGatewayController.TWITTERV2_API_V1 + "request" + "/";
		ResultActions ret = webTestClient.perform(MockMvcRequestBuilders.post(url)
																		.header("Authorization", twitterRequestDto.getBearer())
																		.content(jsonTwitterRequest)
																        .contentType(MediaType.APPLICATION_JSON)
																        .accept(MediaType.APPLICATION_JSON))
																        .andExpect(status().is2xxSuccessful());
		log.info("Backend response:" + System.lineSeparator() + ret.andReturn().getResponse().getContentAsString());
	}

	@Test
	void test02_retrieveUserTweetsSimpleQuery() throws Exception {
	    	    
		// set twitter request for the timeline
		twitterRequestDto.setRequest("https://api.twitter.com/2/users/2212009997/tweets");

		// set bearer
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonTwitterRequest = objectMapper.writeValueAsString(twitterRequestDto);

		// call the controlelr 
		String url = TwitterGatewayController.TWITTERV2_API_V1 + "request" + "/";
		ResultActions ret = webTestClient.perform(MockMvcRequestBuilders.post(url)
																		.header("Authorization", twitterRequestDto.getBearer())
																		.content(jsonTwitterRequest)
																        .contentType(MediaType.APPLICATION_JSON)
																        .accept(MediaType.APPLICATION_JSON))
																        .andExpect(status().is2xxSuccessful());
		log.info("Backend response:" + System.lineSeparator() + ret.andReturn().getResponse().getContentAsString());
	}

	
	@Test
	void test03_retrieveUserTweetsComplexQuery01() throws Exception {

	    
	    String fiveMinutesAgo = ZonedDateTime.now(ZoneOffset.UTC).minusHours(4).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_INSTANT);
	    String now = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_INSTANT);

	    
		// set twitter request for the timeline
		twitterRequestDto.setRequest("https://api.twitter.com/2/users/2212009997/tweets?start_time=" + fiveMinutesAgo + "&end_time=" + now
																								+ "&expansions=attachments.poll_ids,attachments.media_keys,author_id,entities.mentions.username,geo.place_id,in_reply_to_user_id,referenced_tweets.id,referenced_tweets.id.author_id"
																								+ "&tweet.fields=attachments,author_id,context_annotations,conversation_id,created_at,entities,geo,id,in_reply_to_user_id,lang,possibly_sensitive,public_metrics,referenced_tweets,reply_settings,source,text,withheld"
																								+ "&user.fields=created_at,description,entities,id,location,name,pinned_tweet_id,profile_image_url,protected,public_metrics,url,username,verified,withheld&place.fields=contained_within,country,country_code,full_name,geo,id,name,place_type"
																								+ "&poll.fields=duration_minutes,end_datetime,id,options,voting_status&media.fields=duration_ms,height,media_key,preview_image_url,type,url,width,public_metrics,non_public_metrics,organic_metrics,promoted_metrics&max_results=5");		
		// set bearer
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonTwitterRequest = objectMapper.writeValueAsString(twitterRequestDto);

		// call the controlelr 
		String url = TwitterGatewayController.TWITTERV2_API_V1 + "request" + "/";
		ResultActions ret = webTestClient.perform(MockMvcRequestBuilders.post(url)
																		.header("Authorization", twitterRequestDto.getBearer())
																		.content(jsonTwitterRequest)
																        .contentType(MediaType.APPLICATION_JSON)
																        .accept(MediaType.APPLICATION_JSON))
																        .andExpect(status().is2xxSuccessful());
		log.info("Backend response:" + System.lineSeparator() + ret.andReturn().getResponse().getContentAsString());
	}

	@Test
	void test04_retrieveStreamedData() throws Exception {

		// set bearer
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonTwitterRequest = objectMapper.writeValueAsString(twitterRequestDto);

		// call the controller 
		String url = TwitterGatewayController.TWITTERV2_API_V1 + "streamed" + "/";
		ResultActions ret = webTestClient.perform(MockMvcRequestBuilders.post(url)
																		.header("Authorization", twitterRequestDto.getBearer())
																		.content(jsonTwitterRequest)
																        .contentType(MediaType.APPLICATION_JSON)
																        .accept(MediaType.APPLICATION_JSON))
																        .andExpect(status().is2xxSuccessful());
		log.info("Backend response:" + System.lineSeparator() + ret.andReturn().getResponse().getContentAsString());
	}

}
