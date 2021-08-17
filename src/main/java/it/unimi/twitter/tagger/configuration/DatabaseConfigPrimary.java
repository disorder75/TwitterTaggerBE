package it.unimi.twitter.tagger.configuration;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.zaxxer.hikari.HikariConfig;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class DatabaseConfigPrimary {

	@Value("${db.driver-class-name}")
	private String dbDriverClassName;
	@Value("${db.datasource.primary.username}")
	private String dbUsername;
	@Value("${db.datasource.primary.password}")
	private String dbPassword;
	@Value("${db.datasource.primary.baseSchema}")
	private String dbBaseSchema;
	@Value("${db.datasource.primary.url}")
	private String dbUrl;
	
	@Value("#{new Boolean(environment['spring.profiles.active']=='prod-heroku')}")
	private boolean isHerokuProfile;
	
	@Primary
	@Bean(name = "dataSourcePrimary")
	@ConfigurationProperties(prefix = "db.datasource.primary")
	public DataSource dataSource() {

		if (!isHerokuProfile) {
			DriverManagerDataSource dataSource = new DriverManagerDataSource();
			dataSource.setDriverClassName(dbDriverClassName);
			dataSource.setUrl(dbUrl);
			dataSource.setUsername(dbUsername);
			dataSource.setPassword(dbPassword);
			dataSource.setSchema(dbBaseSchema);
			return dataSource;

		} else {
			log.info("configuring jdbc connection for Heroku environment via HikariConfig: {}", dbUrl);			
			 HikariConfig config = new HikariConfig();
			 config.setJdbcUrl(dbUrl);
			 return (DataSource) config;
		}
	}
}