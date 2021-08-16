package it.unimi.twitter.tagger.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import it.unimi.twitter.tagger.service.TrainingDatasetsService;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(NaiveBayesController.ROOT_API)
@CrossOrigin
@Slf4j
public class UploaderController {

	@Autowired
	private TrainingDatasetsService tds;
	
	@PostMapping("/upload/{topic}")
	public String handlePostFileUpload(@PathVariable String topic, @RequestParam("file") MultipartFile file) throws IllegalArgumentException, IOException {
		log.info("uploading file {} for topic {}", file.getOriginalFilename(), topic);
		tds.uploadTopicDataset(topic, file.getInputStream());
		return "redirect:/";
	}

	@PutMapping("/upload/{topic}")
	public String handlePutFileUpload(@PathVariable String topic, @RequestParam("file") MultipartFile file) throws IllegalArgumentException, IOException {
		log.info("uploading file {} for topic {}", file.getOriginalFilename(), topic);
		tds.uploadTopicDataset(topic, file.getInputStream());
		return "redirect:/";
	}
	
}