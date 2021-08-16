package it.unimi.twitter.tagger.dto;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ClassificationDto {
	private String classification;
	private String text;
	private String bearer;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private LocalDate twitterRequestSent;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private LocalDate twitterRequestReceived;
}
