package it.unimi.twitter.tagger.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.unimi.twitter.tagger.domain.TrainingDatasets;
import it.unimi.twitter.tagger.dto.ClassificationDto;
import it.unimi.twitter.tagger.repository.TrainingDatasetsRepository;
import it.unimi.twitter.tagger.service.TrainingDatasetsService;
import it.unimi.twitter.tagger.utils.ReadUtils;
import lombok.extern.slf4j.Slf4j;


@Service("TrainingDatasetsService")
@Slf4j
public class TrainingDatasetsServiceImpl implements TrainingDatasetsService {
	
	@Autowired
	private TrainingDatasetsRepository tdr;
	
	@Override
	public void uploadTopicDataset(String topic, InputStream is) throws IOException, IllegalArgumentException {
		log.info("storing new values for topic {}", topic);
		Date now = new Date();
		try (Reader fr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
			List<String> lines = ReadUtils.readLinesFromFileReader(fr);
			lines.forEach(line -> {
				TrainingDatasets entity = new TrainingDatasets(null, "train-upload", topic, line, now);
				tdr.save(entity);
			});
		}
	}

	@Override
	public void setClassification(ClassificationDto classification) {
		TrainingDatasets entity = new TrainingDatasets(null, classification.getBearer(), classification.getClassification(), classification.getText(), new Date());
		tdr.saveAndFlush(entity);
	}

	@Override
	public List<TrainingDatasets> findAllByOrderByTopicAsc() {
		return tdr.findAllByOrderByTopicAsc();
	}

	@Override
	public List<TrainingDatasets> findByBearerOrderByTopicAsc(String bearer) {
		return tdr.findByBearerOrderByTopicAsc(bearer);
	}

}
