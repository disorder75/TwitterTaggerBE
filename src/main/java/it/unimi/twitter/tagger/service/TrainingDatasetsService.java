package it.unimi.twitter.tagger.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import it.unimi.twitter.tagger.domain.TrainingDatasets;
import it.unimi.twitter.tagger.dto.ClassificationDto;


public interface TrainingDatasetsService {

	public void uploadTopicDataset(String topic, InputStream is) throws IOException, IllegalArgumentException;
	public List<TrainingDatasets> findAllByOrderByTopicAsc();
	public List<TrainingDatasets> findByBearerOrderByTopicAsc(String bearer);
	void setClassification(ClassificationDto classification, String bearer);
	
}
