package it.unimi.twitter.tagger.controller.twitter.gateway;


import java.time.LocalDate;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import it.unimi.twitter.tagger.configuration.TwitterAuthApi;
import it.unimi.twitter.tagger.configuration.TwitterStreamedData;
import it.unimi.twitter.tagger.dto.TwitterRequestDto;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(TwitterGatewayController.TWITTERV2_API_V1)
@CrossOrigin
@Slf4j
public class TwitterGatewayController {

	public static final String TWITTERV2_API_V1 = "/twitterv2api/v1/";

	@Autowired
	@Qualifier("TwitterRestTemplate")
	RestTemplate restTemplate;
	@Autowired
	private TwitterAuthApi twitterAuthApi;
	@Autowired
	private TwitterStreamedData<String> twStreamingData;
	
	@PostMapping(path = "request", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> postTwitterRequest(@RequestBody TwitterRequestDto twitterRequest) {
		twitterRequest.setTwitterRequestSent(LocalDate.now());
		log.info("received requested {}", twitterRequest);
		
		/*
		 *		Store the bearer for the streaming 
		 */
		twitterAuthApi.setBearer(twitterRequest.getBearer());
		
		/*
		 *		Twitter Communication 
		 */
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.setBearerAuth(twitterRequest.getBearer());
		
		ResponseEntity<String> entity = restTemplate.exchange(twitterRequest.getRequest(), HttpMethod.GET, new HttpEntity<Object>(headers), String.class);
				
		log.info("Twitter response:" + System.lineSeparator() + entity.getBody() + System.lineSeparator());
		
		/*
		 *		Response to the rest-api client 
		 */
		twitterRequest.setTwitterRequestReceived(LocalDate.now());
		return new ResponseEntity<String>(entity.getBody(), HttpStatus.OK); 
	}

	@PostMapping(path = "streamed", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> postTwitterStreamed(@RequestBody TwitterRequestDto twitterRequest) {
		twitterRequest.setTwitterRequestSent(LocalDate.now());
		log.info("received requested {}", twitterRequest);
		/*
		 *		Store the bearer for the streaming 
		 */
		twitterAuthApi.setBearer(twitterRequest.getBearer());
		
		/*
		 *		Response to the rest-api client 
		 */
		String ret = twStreamingData.toString();
		twStreamingData.clear();
		return new ResponseEntity<String>(ret, HttpStatus.OK); 
	}

}