package it.unimi.twitter.tagger.configuration;

//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.builders.WebSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
//import org.springframework.security.config.web.server.ServerHttpSecurity;
//import org.springframework.security.web.server.SecurityWebFilterChain;
//
//import lombok.extern.slf4j.Slf4j;
//
//@Configuration
//@EnableWebSecurity
//@EnableWebFluxSecurity
//@Slf4j
//public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
//	
//	@Bean
//	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
//		log.info("exposing actuators endpoints");
//	    return http.authorizeExchange()
//	      .pathMatchers("/actuator/**").permitAll()
//	      .anyExchange().authenticated()
//	      .and().build();
//	}
//	
//	@Override
//	public void configure(WebSecurity web) throws Exception {
//		// TODO Auto-generated method stub
//		super.configure(web);
//	}
//
//	@Override
//	protected void configure(HttpSecurity http) throws Exception {
//		// TODO Auto-generated method stub
//		super.configure(http);
//	}
//
//}
