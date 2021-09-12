package it.unimi.twitter.tagger.test.nlp;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import it.unimi.twitter.tagger.domain.TrainingDatasets;
import it.unimi.twitter.tagger.service.TrainingDataApacheNlpService;
import it.unimi.twitter.tagger.service.TrainingDatasetsService;
import it.unimi.twitter.tagger.service.impl.TrainingDataApacheNlpServiceImpl;
import it.unimi.twitter.tagger.utils.ReadUtils;
import lombok.extern.slf4j.Slf4j;

import opennlp.tools.doccat.BagOfWordsFeatureGenerator;
import opennlp.tools.doccat.DoccatFactory;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.doccat.FeatureGenerator;
import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.util.MarkableFileInputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.model.ModelUtil;
import opennlp.tools.util.TrainingParameters;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "prod")
@Slf4j
class TwitterApacheNlpTest {
	
	@Autowired
	TrainingDatasetsService trainingDatasetsService;
	@Autowired
	TrainingDataApacheNlpService trainingDataApacheNlpService;
	
	@BeforeEach
	void setUp() throws Exception {
		assertNotNull(trainingDatasetsService);
	}

	@Test
	void test01_source_form_files() throws Exception {
		
		URL url = ReadUtils.class.getResource("/datasets/apacheNlpCategoriesClassifycation.txt");
		File file = new File(url.toURI());
				
				
		// Read file with classifications samples of sentences.
			//InputStreamFactory inputStreamFactory = new MarkableFileInputStreamFactory(new File("documentcategorizer.txt"));
		InputStreamFactory inputStreamFactory = new MarkableFileInputStreamFactory(file);
		ObjectStream<String> lineStream = new PlainTextByLineStream(inputStreamFactory, StandardCharsets.UTF_8);
		
		ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream);
		
		// Use CUT_OFF as zero since we will use very few samples.
		// BagOfWordsFeatureGenerator will treat each word as a feature. Since we have
		// few samples, each feature/word will have small counts, so it won't meet high
		// cutoff.
		TrainingParameters params = ModelUtil.createDefaultTrainingParameters();
		params.put(TrainingParameters.CUTOFF_PARAM, 0);
		DoccatFactory factory = new DoccatFactory(new FeatureGenerator[] { new BagOfWordsFeatureGenerator() });
		// Train a model with classifications from above file.
		DoccatModel model = DocumentCategorizerME.train("it", sampleStream, params, factory);

		// Serialize model to some file so that next time we don't have to again train a
		// model. Next time We can just load this file directly into model.
		URL urlBin = ReadUtils.class.getResource("/datasets/documentcategorizer.bin");
		File fileBin = new File(urlBin.toURI());
		model.serialize(fileBin);

		/**
		 * Load model from serialized file & lets categorize reviews.
		 */
		// Load serialized trained model
		try (InputStream modelIn = new FileInputStream(fileBin);
			 Scanner scanner = new Scanner(System.in);) {

			while (true) {
				// Get inputs in loop
				System.out.println("Enter a sentence:");
				String sentence = scanner.nextLine();

				TrainingDataApacheNlpServiceImpl.setTokenize(sentence);
				
				// Initialize document categorizer tool
				DocumentCategorizerME myCategorizer = new DocumentCategorizerME(model);

				// Get the probabilities of all outcome i.e. positive & negative
				double[] probabilitiesOfOutcomes = myCategorizer.categorize(TrainingDataApacheNlpServiceImpl.getTokens(sentence));

				// Get name of category which had high probability
				log.info("probabilitiesOfOutcomes {}", myCategorizer.getAllResults(probabilitiesOfOutcomes));
				log.info("getBestCategory {}", myCategorizer.getBestCategory(probabilitiesOfOutcomes));
				log.info("getNumberOfCategories {}", myCategorizer.getNumberOfCategories());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	@Test
	void test02_source_form_db() throws Exception {
		
		List<TrainingDatasets> data = trainingDatasetsService.findAllByOrderByTopicAsc();

		File of = new File("/tmp/traindata.txt");
		of.createNewFile(); 
		FileOutputStream oFile = new FileOutputStream(of, false); 
		data.parallelStream().forEach(d ->{
			try {
				String line = d.getTopic() + '\t' + d.getValue() + System.lineSeparator();
				oFile.write(line.getBytes());
			} catch (IOException e) {
				log.error("Failed to create training data on filesystem. Err {}", e.getMessage());
			}
		});
		oFile.close();		

		
		File otokenf = new File("/tmp/tokenizerdata.txt");
		otokenf.createNewFile(); 
		FileOutputStream otokenfos = new FileOutputStream(otokenf, false); 
		String tokenData = "This is one example of tokenizer.\n"
				+ "I<SPLIT>, you<SPLIT>, everyone can tokenize.\n"
				+ "Triangle<SPLIT>, rectangle<SPLIT>, circle<SPLIT>, line are shapes.\n"
				+ "opennlp<SPLIT>.<SPLIT>tools<SPLIT>.<SPLIT>tokenize\n"
				+ "java<SPLIT>.<SPLIT>util<SPLIT>.<SPLIT>ArrayList\n"
				+ "java<SPLIT>.<SPLIT>lang<SPLIT>.<SPLIT>Object";
		
		otokenfos.write(tokenData.getBytes());
		otokenfos.close();
				
		// Read file with classifications samples of sentences.
			//InputStreamFactory inputStreamFactory = new MarkableFileInputStreamFactory(new File("documentcategorizer.txt"));
		InputStreamFactory inputStreamFactory = new MarkableFileInputStreamFactory(of);
		ObjectStream<String> lineStream = new PlainTextByLineStream(inputStreamFactory, StandardCharsets.UTF_8);		
		ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream);
		
		// Use CUT_OFF as zero since we will use very few samples.
		// BagOfWordsFeatureGenerator will treat each word as a feature. Since we have
		// few samples, each feature/word will have small counts, so it won't meet high
		// cutoff.
		TrainingParameters params = ModelUtil.createDefaultTrainingParameters();
		params.put(TrainingParameters.CUTOFF_PARAM, 0);
		DoccatFactory factory = new DoccatFactory(new FeatureGenerator[] { new BagOfWordsFeatureGenerator() });
		// Train a model with classifications from above file.
		DoccatModel model = DocumentCategorizerME.train("it", sampleStream, params, factory);

		// Serialize model to some file so that next time we don't have to again train a
		// model. Next time We can just load this file directly into model.
		File fbin = new File("/tmp/documentcategorizer.bin");
		fbin.createNewFile(); 
		model.serialize(fbin);

		/**
		 * Load model from serialized file & lets categorize reviews.
		 */
		// Load serialized trained model
		try (InputStream modelIn = new FileInputStream(fbin);
			 Scanner scanner = new Scanner(System.in);) {

			while (true) {
				// Get inputs in loop
				System.out.println("Enter a sentence:");
				String sentence = scanner.nextLine();

				TrainingDataApacheNlpServiceImpl.setTokenize(otokenf);
				
				// Initialize document categorizer tool
				DocumentCategorizerME myCategorizer = new DocumentCategorizerME(model);

				// Get the probabilities of all outcome i.e. positive & negative
				double[] probabilitiesOfOutcomes = myCategorizer.categorize(TrainingDataApacheNlpServiceImpl.getTokens(sentence));

				// Get name of category which had high probability
				log.info("probabilitiesOfOutcomes {}", myCategorizer.getAllResults(probabilitiesOfOutcomes));
				log.info("getBestCategory {}", myCategorizer.getBestCategory(probabilitiesOfOutcomes));
				log.info("getNumberOfCategories {}", myCategorizer.getNumberOfCategories());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
