package it.unimi.twitter.tagger.test.classification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.datumbox.framework.applications.nlp.TextClassifier;
import com.datumbox.framework.common.ConfigurableFactory;
import com.datumbox.framework.common.Configuration;
import com.datumbox.framework.core.Datasets;
import com.datumbox.framework.core.common.dataobjects.Dataframe;
import com.datumbox.framework.core.common.dataobjects.Record;
import com.datumbox.framework.core.common.text.extractors.NgramsExtractor;
import com.datumbox.framework.core.machinelearning.MLBuilder;
import com.datumbox.framework.core.machinelearning.classification.BernoulliNaiveBayes;
import com.datumbox.framework.core.machinelearning.common.abstracts.featureselectors.AbstractFeatureSelector;
import com.datumbox.framework.core.machinelearning.common.abstracts.modelers.AbstractClassifier;
import com.datumbox.framework.core.machinelearning.common.abstracts.transformers.AbstractScaler;
import com.datumbox.framework.core.machinelearning.featureselection.ChisquareSelect;
import com.datumbox.framework.core.machinelearning.modelselection.metrics.ClassificationMetrics;
import com.datumbox.framework.old.naive.bayes.dataobjects.NaiveBayesKnowledgeBase;
import com.datumbox.framework.old.naive.bayes.machinelearning.classifiers.NaiveBayes;
import com.datumbox.framework.tests.Constants;

import it.unimi.twitter.tagger.utils.ReadUtils;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@ActiveProfiles(profiles = "prod")
@Slf4j
class TwitterTaggerClassificationTests {

	@Test
	void contextLoads() {
	}
	
	@Test
	void test01_ClassificationText() throws IOException {

		log.info("Classification Text old implementation");
		
		//map of dataset files
        Map<String, URL> trainingFiles = new HashMap<>();
        trainingFiles.put("English", ReadUtils.class.getResource("/datasets/training.language.en.txt"));
        trainingFiles.put("French", ReadUtils.class.getResource("/datasets/training.language.fr.txt"));
        trainingFiles.put("German", ReadUtils.class.getResource("/datasets/training.language.de.txt"));
        trainingFiles.put("Computer", ReadUtils.class.getResource("/datasets/computers.txt"));
        
        //loading examples in memory
        Map<String, String[]> trainingExamples = new HashMap<>();
        for(Map.Entry<String, URL> entry : trainingFiles.entrySet()) {
            trainingExamples.put(entry.getKey(), ReadUtils.readLines(entry.getValue()));
        }
        
        //train classifier
        NaiveBayes nb = new NaiveBayes();
        nb.setChisquareCriticalValue(6.63); //0.01 pvalue
        nb.train(trainingExamples);
        
        //get trained classifier knowledgeBase
        NaiveBayesKnowledgeBase knowledgeBase = nb.getKnowledgeBase();
        
        nb = null;
        trainingExamples = null;
        
        //Use classifier
        nb = new NaiveBayes(knowledgeBase);
        String exampleEn = "I am English";
        String outputEn = nb.predict(exampleEn);
        log.info("The sentense \"{}\" was classified as \"{}\"", exampleEn, outputEn);
        
        String exampleFr = "Je suis Français";
        String outputFr = nb.predict(exampleFr);
        log.info("The sentense \"{}\" was classified as \"{}\"", exampleFr, outputFr);

        String exampleDe = "Ich bin Deutsch";
        String outputDe = nb.predict(exampleDe);
        log.info("The sentense \"{}\" was classified as \"{}\"", exampleDe, outputDe);

        String examplePc0 = "Ogni volta che utilizzo un computer mi sento felice";
        String outputPc0 = nb.predict(examplePc0);
        log.info("The sentense \"{}\" was classified as \"%s\"{}", examplePc0, outputPc0);

        String examplePc1 = "I floppy sono un vecchio sistema di memorizzazione delle informazioni";
        String outputPc1 = nb.predict(examplePc1);
        log.info("The sentense \"{}\" was classified as \"{}\"", examplePc1, outputPc1);

        String examplePc2 = "Una delle interfaccie uomo-macchine è stata il joystick";
        String outputPc2 = nb.predict(examplePc2);
        log.info("The sentense \"{}\" was classified as \"{}\"", examplePc2, outputPc2);

        String examplePc3 = "La multimedialità è stata una delle caratteristiche nella generazione dopo i microprocessori ad 8bit";
        String outputPc3 = nb.predict(examplePc3);
        log.info("The sentense \"{}\" was classified as \"{}\"", examplePc3, outputPc3);

        String examplePc4 = "I primi pc avevano una velocità di clock decisamente bassa";
        String outputPc4 = nb.predict(examplePc4);
        log.info("The sentense \"{}\" was classified as \"{}\"", examplePc4, outputPc4);
	}

	
	@Test
	void test02_ClassificationTextBernoulliNaiveBayes(TestInfo testInfo) throws IOException {

		log.info("Classification Text BernoulliNaiveBayes");

        BernoulliNaiveBayes.TrainingParameters mlParams = new BernoulliNaiveBayes.TrainingParameters();
        
        ChisquareSelect.TrainingParameters fsParams = new ChisquareSelect.TrainingParameters();
        fsParams.setALevel(0.05);
        fsParams.setMaxFeatures(1000);
        fsParams.setRareFeatureThreshold(3);
        
//trainAndValidate(mlParams, fsParams, null, 0.8393075950598075, 1);

        Configuration configuration = getConfiguration();

        String storageName = this.getClass().getSimpleName() + testInfo.getDisplayName();

//Map<Object, URI> dataset = Datasets.sentimentAnalysis();

        Map<Object, URI> dataset = new HashMap<>();
        dataset.put("English", Datasets.inputStreamToURI(Datasets.class.getClassLoader().getResourceAsStream("datasets/training.language.en.txt")));
        dataset.put("French", Datasets.inputStreamToURI(Datasets.class.getClassLoader().getResourceAsStream("datasets/training.language.fr.txt")));
        dataset.put("German", Datasets.inputStreamToURI(Datasets.class.getClassLoader().getResourceAsStream("datasets/training.language.de.txt")));
//        dataset.put("Computer", Datasets.inputStreamToURI(Datasets.class.getClassLoader().getResourceAsStream("datasets/computers.txt")));
        
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
//assertEquals(0.7393075950598075, vm.getMacroF1(), Constants.DOUBLE_ACCURACY_HIGH);

        instance.close();
        
        instance = MLBuilder.load(TextClassifier.class, storageName, configuration);
//Dataframe validationData = instance.predict(Datasets.sentimentAnalysisUnlabeled());

        
        Dataframe validationData_fr = instance.predict(Datasets.inputStreamToURI(Datasets.class.getClassLoader().getResourceAsStream("datasets/unlabeled.language.fr.txt")));
        Dataframe validationData_de = instance.predict(Datasets.inputStreamToURI(Datasets.class.getClassLoader().getResourceAsStream("datasets/unlabeled.language.de.txt")));
        Dataframe validationData_en = instance.predict(Datasets.inputStreamToURI(Datasets.class.getClassLoader().getResourceAsStream("datasets/unlabeled.language.en.txt")));
        //Dataframe validationData_game = instance.predict(Datasets.inputStreamToURI(Datasets.class.getClassLoader().getResourceAsStream("datasets/unlabeled.game.review.txt")));

        validationData_fr.forEach(p -> {
        	log.info("validationData_fr prediction {} probabilites {}", p.getYPredicted(), p.getYPredictedProbabilities());        	
        });

        validationData_de.forEach(p -> {
        	log.info("validationData_de prediction {} probabilites {}", p.getYPredicted(), p.getYPredictedProbabilities());
        });

        validationData_en.forEach(p -> {
        	log.info("validationData_en prediction {} probabilites {}", p.getYPredicted(), p.getYPredictedProbabilities());
        });

//        validationData_game.forEach(p -> {
//        	log.info("validationData_game prediction {} probabilites {}", p.getYPredicted(), p.getYPredictedProbabilities());
//        });

        
//        List<Object> expResult = Arrays.asList("negative","positive");
//        int i = 0;
//        for(Record r : validationData.values()) {
//            assertEquals(expResult.get(i), r.getYPredicted());
//            ++i;
//        }
        
        instance.delete();
//validationData.close();

        validationData_fr.close();
        validationData_de.close();
        validationData_en.close();
//        validationData_game.close();


	}

	
    /**
     * Test of train and validate method, of class TextClassifier using BernoulliNaiveBayes.
     */ 
    @Test
    public void testTrainAndValidateBernoulliNaiveBayes() {
        log.info("testTrainAndValidateBernoulliNaiveBayes");
        
        BernoulliNaiveBayes.TrainingParameters mlParams = new BernoulliNaiveBayes.TrainingParameters();
        
        ChisquareSelect.TrainingParameters fsParams = new ChisquareSelect.TrainingParameters();
        fsParams.setALevel(0.05);
        fsParams.setMaxFeatures(1000);
        fsParams.setRareFeatureThreshold(3);
        
        trainAndValidate(
                mlParams,
                fsParams,
                null,
                0.8393075950598075,
                1
        );
    }

    /**
     * Trains and validates a model with the provided modeler and feature selector.
     * 
     * @param <ML>
     * @param <FS>
     * @param <NS>
     * @param modelerTrainingParameters
     * @param featureSelectorTrainingParameters
     * @param numericalScalerTrainingParameters
     * @param testId
     */
    private <ML extends AbstractClassifier, FS extends AbstractFeatureSelector, NS extends AbstractScaler> 
    		void trainAndValidate(ML.AbstractTrainingParameters modelerTrainingParameters,
					              FS.AbstractTrainingParameters featureSelectorTrainingParameters,
					              NS.AbstractTrainingParameters numericalScalerTrainingParameters,
					              double expectedF1score,
					              int testId) {
    	
        Configuration configuration = getConfiguration();

        String storageName = this.getClass().getSimpleName() + testId;

        Map<Object, URI> dataset = Datasets.sentimentAnalysis();

        TextClassifier.TrainingParameters trainingParameters = new TextClassifier.TrainingParameters();

        //numerical scaling configuration
        trainingParameters.setNumericalScalerTrainingParameters(numericalScalerTrainingParameters);

        //feature selection configuration
        trainingParameters.setFeatureSelectorTrainingParametersList(Arrays.asList(featureSelectorTrainingParameters));

        //classifier configuration
        trainingParameters.setModelerTrainingParameters(modelerTrainingParameters);
        
        //text extraction configuration
        NgramsExtractor.Parameters exParams = new NgramsExtractor.Parameters();
        exParams.setMaxDistanceBetweenKwds(2);
        exParams.setExaminationWindowLength(6);
        trainingParameters.setTextExtractorParameters(exParams);

        TextClassifier instance = MLBuilder.create(trainingParameters, configuration);
        instance.fit(dataset);
        instance.save(storageName);


        ClassificationMetrics vm = instance.validate(dataset);
        assertEquals(expectedF1score, vm.getMacroF1(), Constants.DOUBLE_ACCURACY_HIGH);

        instance.close();
        
        instance = MLBuilder.load(TextClassifier.class, storageName, configuration);
        Dataframe validationData = instance.predict(Datasets.sentimentAnalysisUnlabeled());
        
        List<Object> expResult = Arrays.asList("negative","positive");
        int i = 0;
        for(Record r : validationData.values()) {
            assertEquals(expResult.get(i), r.getYPredicted());
            ++i;
        }
        
        instance.delete();
        validationData.close();
    }
    
    protected Configuration getConfiguration() {
        String storageEngine = System.getProperty("storageEngine");
        if(storageEngine == null) {
            return Configuration.getConfiguration();
        }
        else {
            Properties p = new Properties();
            if("InMemory".equals(storageEngine)) {
                p.setProperty("configuration.storageConfiguration", "com.datumbox.framework.storage.inmemory.InMemoryConfiguration");

            }
            else if("MapDB".equals(storageEngine)) {
                p.setProperty("configuration.storageConfiguration", "com.datumbox.framework.storage.mapdb.MapDBConfiguration");
            }
            else {
                throw new IllegalArgumentException("Unsupported option.");
            }
            return ConfigurableFactory.getConfiguration(Configuration.class, p);
        }
    }


}
