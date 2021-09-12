package it.unimi.twitter.tagger.dto;

import lombok.Data;
import opennlp.tools.doccat.DocumentCategorizerME;

@Data
public class ApacheNlpCategoryPredictionDto {

	double[] probabilitiesOfOutcomes;
	DocumentCategorizerME documentCategorizerME;
	
}
