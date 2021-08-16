package it.unimi.twitter.tagger.configuration;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@Slf4j
public class SwaggerConfig {      
	
    @Value("${swagger2.basePackage}")
	private String basePackage;
    @Value("${application.version}")
	private String version;
    
    @Bean
    public Docket api() {
    	
    	log.info("configuring base packer for Swagger: {}", basePackage);
    	
        return new Docket(DocumentationType.SWAGGER_2).select()
        											  .apis(RequestHandlerSelectors.basePackage(basePackage))
        											  .paths(PathSelectors.any())
        											  .build()
        											  .apiInfo(apiInfo());                                           
    }
    
    private ApiInfo apiInfo() {
        return new ApiInfo("NaiveBayesClassifier Rest API", "NaiveBayes Classifier", version, "", "POC - Unimi SSRI", "Nunzio Castelli", "");
    }
}