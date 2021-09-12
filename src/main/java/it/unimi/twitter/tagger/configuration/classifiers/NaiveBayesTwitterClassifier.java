package it.unimi.twitter.tagger.configuration.classifiers;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.datumbox.framework.applications.nlp.TextClassifier;
import com.datumbox.framework.core.common.text.extractors.NgramsExtractor;
import com.datumbox.framework.core.machinelearning.MLBuilder;
import com.datumbox.framework.core.machinelearning.classification.BernoulliNaiveBayes;
import com.datumbox.framework.core.machinelearning.featureselection.ChisquareSelect;
import com.datumbox.framework.core.machinelearning.modelselection.metrics.ClassificationMetrics;
import com.datumbox.framework.old.naive.bayes.dataobjects.NaiveBayesKnowledgeBase;
import com.datumbox.framework.old.naive.bayes.machinelearning.classifiers.NaiveBayes;

import it.unimi.twitter.tagger.domain.TrainingDatasets;
import it.unimi.twitter.tagger.dto.ApacheNlpCategoryPredictionDto;
import it.unimi.twitter.tagger.service.TrainingDataApacheNlpService;
import it.unimi.twitter.tagger.service.TrainingDatasetsService;
import it.unimi.twitter.tagger.utils.ReadUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Data
@Slf4j
public class NaiveBayesTwitterClassifier {
	
	private NaiveBayesKnowledgeBase knowledgeBase;
	
	@Autowired
	private TrainingDatasetsService trainingDatasetService;
	@Autowired
	private TrainingDataApacheNlpService trainingDataApacheNlpService;
	
	@PostConstruct
	public void init() throws IOException {
		log.info("start training for Datumbox (deprecated)");
		trainNaiveBayesKnowledgeBase();
	}

	@Deprecated
	public void trainNaiveBayesKnowledgeBase() throws IOException {
		/*
		 *		Load data for training from the database 
		 */
		List<TrainingDatasets> entitiesDataset = trainingDatasetService.findAllByOrderByTopicAsc();
		log.info("topics in dataset available nr. {}", entitiesDataset.size());

		/*
		 *		Dump dataset on filesystem for easy and quick integration with datumbox
		 *		TODO: add support into datumbox for the database 
		 */
		Map<String, URL> dataset = new HashMap<>();
        entitiesDataset.parallelStream().forEach(e ->{
        	/*
        	 *		Create temporary file 
        	 */
        	if (!dataset.containsKey(e.getTopic())) {
                File f;
				try {
					f = File.createTempFile(e.getTopic() + "2uri", "tmp");
	                f.deleteOnExit();
	                dataset.put(e.getTopic(), f.toURL());
				} catch (IOException err) {
					log.error("Failed to create temporary file", err);
				}
        	}
        	/*
        	 *		Append data (slow operation, todo: improve) 
        	 */
        	try {
        	    Files.write(Paths.get(dataset.get(e.getTopic()).getPath()), e.getValue().getBytes(), StandardOpenOption.APPEND);
        	}catch (IOException err) {
				log.error("Failed to write data into temporary file", err);
        	}
        });

		
        // Load train dataset in memory
        Map<String, String[]> trainingDataset = new HashMap<>();
        for(Map.Entry<String, URL> entry : dataset.entrySet()) {
            trainingDataset.put(entry.getKey(), ReadUtils.readLines(entry.getValue()));
        }

        //train classifier
        NaiveBayes nb = new NaiveBayes();
        nb.setChisquareCriticalValue(6.63); //0.01 pvalue
        nb.train(trainingDataset);
        
        //get trained classifier knowledgeBase
        knowledgeBase = nb.getKnowledgeBase();
	}

	public void trainNaiveBayesKnowledgeBaseDatumbox(String bearer) throws IOException {
		/*
		 *		Load data for training from the database 
		 */
		List<TrainingDatasets> entitiesDataset = trainingDatasetService.findByBearerOrderByTopicAsc(bearer);
		log.info("topics in dataset available nr. {}", entitiesDataset.size());

		/*
		 *		Dump dataset on filesystem for easy and quick integration with datumbox
		 *		TODO: add support into datumbox for the database 
		 */
		Map<String, URL> dataset = new HashMap<>();
        entitiesDataset.parallelStream().forEach(e ->{
        	/*
        	 *		Create temporary file 
        	 */
        	if (!dataset.containsKey(e.getTopic())) {
                File f;
				try {
					f = File.createTempFile(e.getTopic() + "2uri", "tmp");
	                f.deleteOnExit();
	                dataset.put(e.getTopic(), f.toURL());
				} catch (IOException err) {
					log.error("Failed to create temporary file", err);
				}
        	}
        	/*
        	 *		Append data (slow operation, todo: improve) 
        	 */
        	try {
        	    Files.write(Paths.get(dataset.get(e.getTopic()).getPath()), e.getValue().getBytes(), StandardOpenOption.APPEND);
        	}catch (IOException err) {
				log.error("Failed to write data into temporary file", err);
        	}
        });

		
        // Load train dataset in memory
        Map<String, String[]> trainingDataset = new HashMap<>();
        for(Map.Entry<String, URL> entry : dataset.entrySet()) {
            trainingDataset.put(entry.getKey(), ReadUtils.readLines(entry.getValue()));
        }

        //train classifier
        NaiveBayes nb = new NaiveBayes();
        nb.setChisquareCriticalValue(6.63); //0.01 pvalue
        nb.train(trainingDataset);
        
        //get trained classifier knowledgeBase
        knowledgeBase = nb.getKnowledgeBase();
	}

	public void trainNaiveBayesKnowledgeBaseApacheNlp(String bearer) throws IOException {
		log.info("start new training for user bearer {}", bearer);
		File fTrainingData = trainingDataApacheNlpService.writeTrainDataOnFilesystem(bearer);
		trainingDataApacheNlpService.train(fTrainingData);
		log.info("new training completed. Source data in {}", fTrainingData.getAbsoluteFile());
	}
	
	@Deprecated
	public String naiveBayesKnowledgeBasePredictByDatumbox(String sentence) {
        //Use classifier
		NaiveBayes nb = new NaiveBayes(knowledgeBase);
		return nb.predict(sentence);
	}

	
	public ApacheNlpCategoryPredictionDto naiveBayesPredictSentenceByApacheNlp(String sentence) throws IOException, URISyntaxException {
		return trainingDataApacheNlpService.getPrediction(sentence);
	}

	
	public void trainBernoulliNaiveBayes() {
		/*
		 *		Load data for training from the database 
		 */
		List<TrainingDatasets> entitiesDataset = trainingDatasetService.findAllByOrderByTopicAsc();
		log.info("topics in dataset available nr. {}", entitiesDataset.size());

		/*
		 *		Dump dataset on filesystem for easy and quick integration with datumbox
		 *		TODO: add support into datumbox for the database 
		 */
		Map<Object, URI> dataset = new HashMap<>();
        entitiesDataset.parallelStream().forEach(e ->{
        	/*
        	 *		Create temporary file 
        	 */
        	if (!dataset.containsKey(e.getTopic())) {
                File f;
				try {
					f = File.createTempFile(e.getTopic() + "2uri", "tmp");
	                f.deleteOnExit();
	                dataset.put(e.getTopic(), f.toURI());
				} catch (IOException err) {
					log.error("Failed to create temporary file", err);
				}
        	}
        	/*
        	 *		Append data (slow operation, todo: improve) 
        	 */
        	try {
        	    Files.write(Paths.get(dataset.get(e.getTopic()).getPath()), e.getValue().getBytes(), StandardOpenOption.APPEND);
        	}catch (IOException err) {
				log.error("Failed to write data into temporary file", err);
        	}
        });

		
		/*
		 *		Train the model 
		 */
		BernoulliNaiveBayes.TrainingParameters mlParams = new BernoulliNaiveBayes.TrainingParameters();
        ChisquareSelect.TrainingParameters fsParams = new ChisquareSelect.TrainingParameters();
        fsParams.setALevel(0.01);
        fsParams.setMaxFeatures(null);
        fsParams.setRareFeatureThreshold(3);
        com.datumbox.framework.common.Configuration configuration = com.datumbox.framework.common.Configuration.getConfiguration();
        String storageName = this.getClass().getSimpleName();
        log.info("training storageName: {}", storageName);        
        
        TextClassifier.TrainingParameters trainingParameters = new TextClassifier.TrainingParameters();
        //numerical scaling configuration
        trainingParameters.setNumericalScalerTrainingParameters(null);
        //feature selection configuration
        trainingParameters.setFeatureSelectorTrainingParametersList(Arrays.asList(fsParams));
        //classifier configuration
        trainingParameters.setModelerTrainingParameters(mlParams);
        //text extraction configuration
        NgramsExtractor.Parameters exParams = new NgramsExtractor.Parameters();
        exParams.setMaxDistanceBetweenKwds(2);
        exParams.setExaminationWindowLength(8);
        trainingParameters.setTextExtractorParameters(exParams);
        TextClassifier instance = MLBuilder.create(trainingParameters, configuration);
		instance.fit(dataset);
		instance.save(storageName);

		ClassificationMetrics vm = instance.validate(dataset);
        instance.close();
        instance = MLBuilder.load(TextClassifier.class, storageName, configuration);
        
//        Record pred = instance.predict("Gli italiani sono stanchi degli attuali politici");
//        log.info("prediction: {}", pred);

	}
}
