package it.unimi.twitter.tagger.domain;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.*;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "TRAINING_DATASETS")
@Getter 
@Setter 
@AllArgsConstructor
@NoArgsConstructor
public class TrainingDatasets implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)	
	private Long id;
	@Lob
	private String bearer;
	@Column(columnDefinition = "Topic")
	private String topic;
	@Lob
	private String value;
	@DateTimeFormat
	private Date creationDate;

	
}
