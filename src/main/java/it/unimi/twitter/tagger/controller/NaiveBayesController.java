package it.unimi.twitter.tagger.controller;

import java.io.IOException;
import java.net.URISyntaxException;

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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.unimi.twitter.tagger.configuration.TwitterAuthApi;
import it.unimi.twitter.tagger.configuration.TwitterStreamedData;
import it.unimi.twitter.tagger.configuration.classifiers.NaiveBayesTwitterClassifier;
import it.unimi.twitter.tagger.dto.ApacheNlpCategoryPredictionDto;
import it.unimi.twitter.tagger.dto.ClassificationDto;
import it.unimi.twitter.tagger.service.TrainingDatasetsService;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.doccat.DocumentCategorizerME;

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
		 *		Get the tweet from the cached queue 
		 */
		if (twStreamingData.isEmpty())
			return new ResponseEntity<String>("", HttpStatus.OK);
		
		String tweet = twStreamingData.remove();
		
		/*
		 *		Predict 
		 */
		
		ApacheNlpCategoryPredictionDto prediction = null;
		try {
			prediction = naiveBayesTwitterClassifier.naiveBayesPredictSentenceByApacheNlp(tweet);
		} catch (IOException e) {
			StringBuilder sbErrMsg = new StringBuilder("Prediction failed. Err. ").append(e.getMessage());
			return new ResponseEntity<String>(sbErrMsg.toString(), HttpStatus.FORBIDDEN);
		} catch (URISyntaxException e) {
			StringBuilder sbErrMsg = new StringBuilder("Invalid configuration. Err. ").append(e.getMessage());
			return new ResponseEntity<String>(sbErrMsg.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		DocumentCategorizerME categories = prediction.getDocumentCategorizerME();
		double[] probability = prediction.getProbabilitiesOfOutcomes();
		log.info("getNumberOfCategories {}", categories.getNumberOfCategories());
		log.info("probabilitiesOfOutcomes {}", categories.getAllResults(probability));
		log.info("getBestCategory {}", categories.getBestCategory(probability));

		/*
		 *		Add the prediction result to the tweet (convert json-string and add the new property) 
		 */
		JsonParser jsonParser = new JsonParser();
		JsonObject jsonObject = (JsonObject) jsonParser.parse(tweet);
		jsonObject.addProperty("prediction", categories.getBestCategory(probability));
		jsonObject.addProperty("predictionsWithProbabilities", categories.getAllResults(probability));
		
		return new ResponseEntity<String>(jsonObject.toString(), HttpStatus.OK);
	}

	@PostMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> training(@RequestHeader("Authorization") String bearer, @RequestBody ClassificationDto classification) {

		/*
		 *		Verify current running bearer 
		 */
		if(twitterAuthApi.getBearer() == null) {
			return new ResponseEntity<String>("Please perform a new login.", HttpStatus.FORBIDDEN);
		}
		
		if(twitterAuthApi.getBearer().compareToIgnoreCase(bearer) != 0) {
			return new ResponseEntity<String>("NaiveBayes engine is running for a different bearer, please perform a new login.", HttpStatus.FORBIDDEN);
		}
		
		/*
		 *		Store the classified text 
		 */
		log.info("storing classification for {}", classification.getClassification());		
		trainingDatasetService.setClassification(classification, bearer);

		/*
		 *		Re-training the model
		 */
		log.info("Re-training model on the fly");
		boolean trained = false;
		try {
			naiveBayesTwitterClassifier.trainNaiveBayesKnowledgeBaseApacheNlp(bearer);
			trained = true;
		} catch (IOException e) {
			log.error("failed to re-train the model", e);
		}
		
		if(!trained)
			return new ResponseEntity<String>("Erron on training NaiveBayes engine", HttpStatus.INTERNAL_SERVER_ERROR);
		
		return new ResponseEntity<String>(classification.toString(), HttpStatus.OK); 
	}

}
