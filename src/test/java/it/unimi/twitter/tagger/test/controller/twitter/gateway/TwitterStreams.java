package it.unimi.twitter.tagger.test.controller.twitter.gateway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import it.unimi.twitter.tagger.configuration.TwitterAuthApi;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "prod")
@Slf4j
class TwitterStreams {

	private static ArrayBlockingQueue<String> abqFifoPublictweets;
	
	@Autowired
	private TwitterAuthApi twitterAuthApi;
	
	@BeforeEach
	void setUp() throws Exception {
		abqFifoPublictweets = new ArrayBlockingQueue<>(5);
		twitterAuthApi.setBearer("AAAAAAAAAAAAAAAAAAAAACj2PQEAAAAAaPyCSHbaQXIGpYKKY19dATZxntQ%3DrALIU5jai8zyDTvBTFN9n7BNcZ3NMJbrAsuCcBv5ZSqUkLQKVf");
	}

	@Test
	void test01_retrieveUserInfo() throws Exception {		
		twitterStream(twitterAuthApi.getBearer(), "https://api.twitter.com/2/tweets/search/stream");
	}


	public static void twitterStream(String token, String url) throws IOException, URISyntaxException, ParseException, JSONException {
		String bearerToken = token;
		if (null != bearerToken) {
			Map<String, String> rules = new HashMap<>();
//			rules.put("cats has:images", "cat images");
//			rules.put("dogs has:images", "dog images");
			rules.put("Italia","tag: italia");
			setupRules(bearerToken, rules);
			connectStream(bearerToken, url);
		} else {
			log.error("There was a problem getting you bearer token. Please make sure you set the BEARER_TOKEN environment variable");
		}
	}

	/*
	 * This method calls the filtered stream endpoint and streams Tweets from it
	 * */
	private static void connectStream(String bearerToken, String url) throws IOException, URISyntaxException {

		Integer numberOfRead = (int) (Math.random() * 25);
		Integer cnt = 0;
		
		HttpClient httpClient = HttpClients.custom()
										   .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
										   .build();

		URIBuilder uriBuilder = new URIBuilder(url);

		HttpGet httpGet = new HttpGet(uriBuilder.build());
		httpGet.setHeader("Authorization", String.format("Bearer %s", bearerToken));

		HttpResponse response = httpClient.execute(httpGet);
		HttpEntity entity = response.getEntity();
		if (null != entity) {
			BufferedReader reader = new BufferedReader(new InputStreamReader((entity.getContent())));
			String line = reader.readLine();
			while (line != null && cnt < numberOfRead) {
				cnt++;
				// watch-out this is not thread safe!
				if (abqFifoPublictweets.remainingCapacity() > 0)
					abqFifoPublictweets.add(line);
				else {
					try {
						abqFifoPublictweets.poll();
						abqFifoPublictweets.put(line);
					} catch (InterruptedException e) {
						log.error("error on cached tweets, flushing");
						abqFifoPublictweets.clear();
					}
				}
				line = reader.readLine();
			}
		}
		
		if (abqFifoPublictweets.size() > 0)
			abqFifoPublictweets.forEach(t -> log.info(t));
	}

	/*
	 * Helper method to setup rules before streaming data
	 * */
	private static void setupRules(String bearerToken, Map<String, String> rules) throws IOException, URISyntaxException, ParseException, JSONException {
		List<String> existingRules = getRules(bearerToken);
		if (existingRules.size() > 0) {
			deleteRules(bearerToken, existingRules);
		}
		createRules(bearerToken, rules);
	}

	/*
	 * Helper method to create rules for filtering
	 * */
	private static void createRules(String bearerToken, Map<String, String> rules) throws URISyntaxException, IOException {
		HttpClient httpClient = HttpClients.custom()
				.setDefaultRequestConfig(RequestConfig.custom()
						.setCookieSpec(CookieSpecs.STANDARD).build())
				.build();

		URIBuilder uriBuilder = new URIBuilder("https://api.twitter.com/2/tweets/search/stream/rules");

		HttpPost httpPost = new HttpPost(uriBuilder.build());
		httpPost.setHeader("Authorization", String.format("Bearer %s", bearerToken));
		httpPost.setHeader("content-type", "application/json");
		StringEntity body = new StringEntity(getFormattedString("{\"add\": [%s]}", rules));
		httpPost.setEntity(body);
		HttpResponse response = httpClient.execute(httpPost);
		HttpEntity entity = response.getEntity();
		if (null != entity) {
			System.out.println(EntityUtils.toString(entity, "UTF-8"));
		}
	}

	/*
	 * Helper method to get existing rules
	 * */
	private static List<String> getRules(String bearerToken) throws URISyntaxException, IOException, ParseException, JSONException {
		List<String> rules = new ArrayList<>();
		HttpClient httpClient = HttpClients.custom()
				.setDefaultRequestConfig(RequestConfig.custom()
						.setCookieSpec(CookieSpecs.STANDARD).build())
				.build();

		URIBuilder uriBuilder = new URIBuilder("https://api.twitter.com/2/tweets/search/stream/rules");

		HttpGet httpGet = new HttpGet(uriBuilder.build());
		httpGet.setHeader("Authorization", String.format("Bearer %s", bearerToken));
		httpGet.setHeader("content-type", "application/json");
		HttpResponse response = httpClient.execute(httpGet);
		HttpEntity entity = response.getEntity();
		if (null != entity) {
			JSONObject json = new JSONObject(EntityUtils.toString(entity, "UTF-8"));
			if (json.length() > 1) {
				JSONArray array = (JSONArray) json.get("data");
				for (int i = 0; i < array.length(); i++) {
					JSONObject jsonObject = (JSONObject) array.get(i);
					rules.add(jsonObject.getString("id"));
				}
			}
		}
		return rules;
	}

	/*
	 * Helper method to delete rules
	 * */
	private static void deleteRules(String bearerToken, List<String> existingRules) throws URISyntaxException, IOException {
		HttpClient httpClient = HttpClients.custom()
				.setDefaultRequestConfig(RequestConfig.custom()
						.setCookieSpec(CookieSpecs.STANDARD).build())
				.build();

		URIBuilder uriBuilder = new URIBuilder("https://api.twitter.com/2/tweets/search/stream/rules");

		HttpPost httpPost = new HttpPost(uriBuilder.build());
		httpPost.setHeader("Authorization", String.format("Bearer %s", bearerToken));
		httpPost.setHeader("content-type", "application/json");
		StringEntity body = new StringEntity(getFormattedString("{ \"delete\": { \"ids\": [%s]}}", existingRules));
		httpPost.setEntity(body);
		HttpResponse response = httpClient.execute(httpPost);
		HttpEntity entity = response.getEntity();
		if (null != entity) {
			System.out.println(EntityUtils.toString(entity, "UTF-8"));
		}
	}

	private static String getFormattedString(String string, List<String> ids) {
		StringBuilder sb = new StringBuilder();
		if (ids.size() == 1) {
			return String.format(string, "\"" + ids.get(0) + "\"");
		} else {
			for (String id : ids) {
				sb.append("\"" + id + "\"" + ",");
			}
			String result = sb.toString();
			return String.format(string, result.substring(0, result.length() - 1));
		}
	}

	private static String getFormattedString(String string, Map<String, String> rules) {
		StringBuilder sb = new StringBuilder();
		if (rules.size() == 1) {
			String key = rules.keySet().iterator().next();
			return String.format(string, "{\"value\": \"" + key + "\", \"tag\": \"" + rules.get(key) + "\"}");
		} else {
			for (Map.Entry<String, String> entry : rules.entrySet()) {
				String value = entry.getKey();
				String tag = entry.getValue();
				sb.append("{\"value\": \"" + value + "\", \"tag\": \"" + tag + "\"}" + ",");
			}
			String result = sb.toString();
			return String.format(string, result.substring(0, result.length() - 1));
		}
	}

}
