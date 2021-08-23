package it.unimi.twitter.tagger.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datumbox.framework.core.common.text.StringCleaner;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.unimi.twitter.tagger.configuration.TwitterAuthApi;
import it.unimi.twitter.tagger.configuration.TwitterStreamedData;
import it.unimi.twitter.tagger.configuration.classifiers.NaiveBayesTwitterClassifier;
import it.unimi.twitter.tagger.dto.ClassificationDto;
import it.unimi.twitter.tagger.service.TrainingDatasetsService;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(NaiveBayesController.ROOT_API)
@CrossOrigin
@Slf4j
public class NaiveBayesController {
	
	@Value("${application.version}")
	private String applicationVersion;
	
	public static final String ROOT_API = "/api/v1/naivebayes/";

	@Autowired
	private TrainingDatasetsService trainingDatasetService;
	@Autowired
	TwitterStreamedData<String> twStreamingData;
	@Autowired
	private NaiveBayesTwitterClassifier naiveBayesTwitterClassifier;
	@Autowired
	private TwitterAuthApi twitterAuthApi;

	
	@GetMapping(path = "version", produces = MediaType.APPLICATION_JSON_VALUE )
	public String getVerison() {
		log.info("retrieving all reservations ");
		return applicationVersion;
	}

	@GetMapping(path = "predict", produces = MediaType.APPLICATION_JSON_VALUE )
	public ResponseEntity<String> getTweetPredict() {
		log.info("predict tweet and send to the client");
		/*
		 *		Get the tweet 
		 */
		if (twStreamingData.isEmpty())
			return new ResponseEntity<String>("", HttpStatus.OK);
		
		//String tweet = twStreamingData.peek();
		String tweet = twStreamingData.remove();
		
		/*
		 *		Predict 
		 */
		String prediction = naiveBayesTwitterClassifier.naiveBayesKnowledgeBasePredict(tweet);
		/*
		 *		Add the prediction result to the tweet (convert json-string and add the new property) 
		 */
		JsonParser jsonParser = new JsonParser();
		JsonObject jsonObject = (JsonObject) jsonParser.parse(tweet);
		jsonObject.addProperty("prediction", prediction);

		return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.OK);
	}

	@PostMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> training(@RequestHeader("Authorization") String bearer, @RequestBody ClassificationDto classification) {

		
		if(twitterAuthApi.getBearer().compareToIgnoreCase(bearer) != 0) {
			return new ResponseEntity<String>("NaiveBayes engine is running for a different bearer, please perform a new login.", HttpStatus.FORBIDDEN);
		}
		
		log.info("classification: {}", classification);		
		String text = StringCleaner.tokenizeURLs(classification.getText()).replace(StringCleaner.TOKENIZED_URL, "");
		text = StringCleaner.tokenizeSmileys(text);
		text = StringCleaner.removeExtraSpaces(text);
		text = StringCleaner.removeExtraSpaces(StringCleaner.removeSymbols(text));
		text = StringCleaner.unifyTerminators(text);
		text = StringCleaner.removeAccents(text);
		classification.setText(text);
		classification.setBearer(bearer);
		trainingDatasetService.setClassification(classification);
		log.info("classified: {}", classification);
		/*
		 *		Re-training 
		 */
		boolean trained = false;
		try {
			naiveBayesTwitterClassifier.trainNaiveBayesKnowledgeBase(bearer);
			trained = true;
		} catch (IOException e) {
			log.error("failed to re-train the model", e);
		}
		
		if(!trained)
			return new ResponseEntity<String>("Erron on training NaiveBayes engine", HttpStatus.INTERNAL_SERVER_ERROR);
		
		return new ResponseEntity<String>(classification.toString(), HttpStatus.OK); 
	}

}
