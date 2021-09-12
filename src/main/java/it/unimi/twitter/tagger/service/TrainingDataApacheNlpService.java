package it.unimi.twitter.tagger.service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import it.unimi.twitter.tagger.dto.ApacheNlpCategoryPredictionDto;
import opennlp.tools.doccat.DoccatModel;

public interface TrainingDataApacheNlpService {
	public File writeTokenizeRulesOnFilesystem() throws IOException;
	public DoccatModel train(File trainingData) throws IOException;
	public File writeTrainDataOnFilesystem(String bearer) throws IOException;
	public ApacheNlpCategoryPredictionDto getPrediction(String sentence) throws IOException, URISyntaxException;
}
