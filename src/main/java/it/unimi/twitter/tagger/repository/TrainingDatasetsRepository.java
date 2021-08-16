package it.unimi.twitter.tagger.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.unimi.twitter.tagger.domain.TrainingDatasets;

@Repository
public interface TrainingDatasetsRepository extends JpaRepository<TrainingDatasets, Long> {

	List<TrainingDatasets> findAllByOrderByTopicAsc();
	List<TrainingDatasets> findByBearerOrderByTopicAsc(String bearer);
	                       
}