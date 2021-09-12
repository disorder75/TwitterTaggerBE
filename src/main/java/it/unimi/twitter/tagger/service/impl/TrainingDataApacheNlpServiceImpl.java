package it.unimi.twitter.tagger.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.unimi.twitter.tagger.configuration.TwitterAuthApi;
import it.unimi.twitter.tagger.domain.TrainingDatasets;
import it.unimi.twitter.tagger.dto.ApacheNlpCategoryPredictionDto;
import it.unimi.twitter.tagger.repository.TrainingDatasetsRepository;
import it.unimi.twitter.tagger.service.TrainingDataApacheNlpService;
import it.unimi.twitter.tagger.utils.ReadUtils;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.doccat.BagOfWordsFeatureGenerator;
import opennlp.tools.doccat.DoccatFactory;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.doccat.FeatureGenerator;
import opennlp.tools.tokenize.TokenSample;
import opennlp.tools.tokenize.TokenSampleStream;
import opennlp.tools.tokenize.TokenizerFactory;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.util.MarkableFileInputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.model.ModelUtil;

@Service("ApacheNlpTrainingDatasetsService")
@Slf4j
public class TrainingDataApacheNlpServiceImpl implements TrainingDataApacheNlpService {

	private static final String TMP_DOCUMENTCATEGORIZER_BIN = "/documentcategorizer.bin";
	private static final String TMP_TOKENIZERDATA_TXT = "/tokenizerdata.txt";
	private static final String TMP_TRAINDATA_TXT = "/traindata.txt";

	private static String DOCUMENTCATEGORIZER_BIN;
	private static String TOKENIZERDATA_TXT;
	private static String TRAINDATA_TXT;

	
	private DoccatModel trainedModel;
	private File fTrainedModelSerialized;
	private File fTrainData;
	private File fTokenizerData;
	
	@Autowired
	private TrainingDatasetsRepository tdr;
	@Autowired
	private TwitterAuthApi twitterAuthApi;

	@PostConstruct
	public void init() throws IOException {
		log.info("start training for ApacheNlp (official support)");

		String home = System.getenv("HOME");
		DOCUMENTCATEGORIZER_BIN = home + TMP_DOCUMENTCATEGORIZER_BIN;
		TOKENIZERDATA_TXT = home + TMP_TOKENIZERDATA_TXT;
		TRAINDATA_TXT = home + TMP_TRAINDATA_TXT;

		writeTokenizeRulesOnFilesystem();
		
	}

	@Override
	public File writeTrainDataOnFilesystem(String bearer) throws IOException {
		List<TrainingDatasets> data = tdr.findByBearerOrderByTopicAsc(bearer);
		fTrainData = new File(TRAINDATA_TXT);
		fTrainData.createNewFile();
		FileOutputStream oFile = new FileOutputStream(fTrainData, false);
		data.parallelStream().forEach(d -> {
			try {
				String line = d.getTopic() + '\t' + d.getValue() + System.lineSeparator();
				oFile.write(line.getBytes());
			} catch (IOException e) {
				log.error("Failed to create training data on filesystem. Err {}", e.getMessage());
			}
		});
		oFile.close();
		log.info("trainining data dumped in " + TRAINDATA_TXT);
		return fTrainData;
	}

	@Override
	public File writeTokenizeRulesOnFilesystem() throws IOException {
		fTokenizerData = new File(TOKENIZERDATA_TXT);
		fTokenizerData.createNewFile();
		FileOutputStream otokenfos = new FileOutputStream(fTokenizerData, false);
		String tokenData = "This is one example of tokenizer.\n" + "I<SPLIT>, you<SPLIT>, everyone can tokenize.\n"
				+ "Triangle<SPLIT>, rectangle<SPLIT>, circle<SPLIT>, line are shapes.\n"
				+ "opennlp<SPLIT>.<SPLIT>tools<SPLIT>.<SPLIT>tokenize\n"
				+ "java<SPLIT>.<SPLIT>util<SPLIT>.<SPLIT>ArrayList\n" + "java<SPLIT>.<SPLIT>lang<SPLIT>.<SPLIT>Object";

		otokenfos.write(tokenData.getBytes());
		otokenfos.close();
		log.info("rules for tokens dumped in " + TOKENIZERDATA_TXT);
		return fTokenizerData;
	}

	@Override
	public DoccatModel train(File of) throws IOException {

		// Read file with classifications samples of sentences.
		// InputStreamFactory inputStreamFactory = new
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
		trainedModel = DocumentCategorizerME.train("it", sampleStream, params, factory);

		// Serialize model to some file so that next time we don't have to again train a
		// model. Next time We can just load this file directly into model.
		// NB: Use this if the production machine keeps you files on filesystem
		fTrainedModelSerialized = new File(DOCUMENTCATEGORIZER_BIN);
		fTrainedModelSerialized.createNewFile();
		trainedModel.serialize(fTrainedModelSerialized);
		return trainedModel;
	}

	@Override
	public ApacheNlpCategoryPredictionDto getPrediction(String sentence) throws IOException, URISyntaxException {
		
		if (trainedModel == null && 
			(twitterAuthApi.getBearer() == null || twitterAuthApi.getBearer().length() == 0) ) {
			throw new IOException("file of trained document model not available yet ");
		} else if (trainedModel == null) {
			File fPLainTrainData = writeTrainDataOnFilesystem(twitterAuthApi.getBearer());
			train(fPLainTrainData);
		}
			
		
		log.info("Predict sentence: {}", sentence);

		// Load serialized trained model
		InputStream modelIn = new FileInputStream(fTrainedModelSerialized);
		setTokenize(sentence);

		// Initialize document categorizer tool
		ApacheNlpCategoryPredictionDto apacheNlpPrediction = new ApacheNlpCategoryPredictionDto();
		DocumentCategorizerME myCategorizer = new DocumentCategorizerME(trainedModel);
		apacheNlpPrediction.setDocumentCategorizerME(myCategorizer);
		
		// Get the probabilities of all outcome i.e. positive & negative
		double[] probabilitiesOfOutcomes = myCategorizer.categorize(getTokens(sentence));
		apacheNlpPrediction.setProbabilitiesOfOutcomes(probabilitiesOfOutcomes);
		return apacheNlpPrediction;
	}
	
	/**
	 * Tokenize sentence into tokens.
	 * 
	 * @param sentence
	 * @return
	 * @throws URISyntaxException 
	 */
	public static String[] getTokens(String sentence) throws URISyntaxException {

		// Use model that was created in earlier tokenizer tutorial
		URL urlTokenFile = ReadUtils.class.getResource("/datasets/tokenizermodel.bin");
		File fileTokenBin = new File(urlTokenFile.toURI());

		try (InputStream modelIn = new FileInputStream(fileTokenBin)) {

			TokenizerME myCategorizer = new TokenizerME(new TokenizerModel(modelIn));

			String[] tokens = myCategorizer.tokenize(sentence);

			for (String t : tokens) {
				System.out.println("Tokens: " + t);
			}
			return tokens;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	
	public static void setTokenize(String sentence) throws IOException, URISyntaxException {
		/**
		 * Lets tokenize
		 */
		URL urlTokenizer = ReadUtils.class.getResource("/datasets/tokenizerdata.txt");
		File fileTokenizer = new File(urlTokenizer.toURI());
//		BufferedWriter writer = new BufferedWriter(new FileWriter(fileTokenizer));
//	    writer.write(sentence);	    
//	    writer.close();
		
		/**
		 * Read human understandable data & train a model
		 */
		// Read file with examples of tokenization.
	    InputStreamFactory inputStreamFactory = new MarkableFileInputStreamFactory(fileTokenizer);
		ObjectStream<String> lineStream = new PlainTextByLineStream(inputStreamFactory, StandardCharsets.UTF_8);
		ObjectStream<TokenSample> sampleStream = new TokenSampleStream(lineStream);

		// Train a model from the file read above
		TokenizerFactory factory = new TokenizerFactory("en", null, false, null);
		TokenizerModel model = TokenizerME.train(sampleStream, factory, TrainingParameters.defaultParams());

		// Serialize model to some file so that next time we don't have to again train a
		// model. Next time We can just load this file directly into model.
		URL urlBin = ReadUtils.class.getResource("/datasets/tokenizermodel.bin");
		File fileBin = new File(urlBin.toURI());
		model.serialize(fileBin);
	}
	

	public static void setTokenize(File fileTokenizer) throws IOException, URISyntaxException {
		/**
		 * Read human understandable data & train a model
		 */
		// Read file with examples of tokenization.
	    InputStreamFactory inputStreamFactory = new MarkableFileInputStreamFactory(fileTokenizer);
		ObjectStream<String> lineStream = new PlainTextByLineStream(inputStreamFactory, StandardCharsets.UTF_8);
		ObjectStream<TokenSample> sampleStream = new TokenSampleStream(lineStream);

		// Train a model from the file read above
		TokenizerFactory factory = new TokenizerFactory("it", null, false, null);
		TokenizerModel model = TokenizerME.train(sampleStream, factory, TrainingParameters.defaultParams());

		// Serialize model to some file so that next time we don't have to again train a
		// model. Next time We can just load this file directly into model.
		URL urlBin = ReadUtils.class.getResource("/datasets/tokenizermodel.bin");
		File fileBin = new File(urlBin.toURI());
		model.serialize(fileBin);
	}	
}
