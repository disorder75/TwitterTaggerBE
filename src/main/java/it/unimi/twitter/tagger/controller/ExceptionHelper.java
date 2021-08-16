package it.unimi.twitter.tagger.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class ExceptionHelper {

	@ExceptionHandler(value = {IllegalArgumentException.class})
	public ResponseEntity<Object> handleInvalidInputException(IllegalArgumentException ex) {
		log.error("Invalid Input Exception: ",ex.getMessage());
		return new ResponseEntity<Object>(ex.getMessage(),HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(value = {IOException.class})
	public ResponseEntity<Object> handleUnauthorizedException(IOException ex) {
		log.error("Error on i/o operations. Err: ", ex.getMessage());
		return new ResponseEntity<Object>(ex.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(value = { Exception.class })
	public ResponseEntity<Object> handleException(Exception ex) {
		log.error("Exception: ",ex.getMessage());
		return new ResponseEntity<Object>(ex.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
	}

}