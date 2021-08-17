package it.unimi.twitter.tagger.streaming.receiver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import it.unimi.twitter.tagger.configuration.TwitterAuthApi;
import it.unimi.twitter.tagger.configuration.TwitterStreamedData;
import lombok.extern.slf4j.Slf4j;

@Service("twitterStreamingService")
@Slf4j
public class TwitterStreamingReceiver {

	private final static String queryStr = "?";
	private final static String querySep = "&";
	private final static String twitterSearchStreamApiUrl = "https://api.twitter.com/2/tweets/search/stream";
	private final static String twitterSampleStreamApiUrl = "https://api.twitter.com/2/tweets/sample/stream";
	private final static String twitterExpansionQuery = "expansions=attachments.media_keys,author_id,geo.place_id";
	private final static String twitterMediaFieldsQuery = "media.fields=duration_ms,height,media_key,preview_image_url,type,url,width,public_metrics";
	private final static String twitterPlaceFieldsQuery = "place.fields=contained_within,country,country_code,full_name,geo,id,name,place_type";
	private final static String twitterUserFieldsQuery = "user.fields=created_at,description,entities,id,location,name,pinned_tweet_id,profile_image_url,protected,public_metrics,url,username,verified,withheld";
	
	@Autowired
	TwitterStreamedData<String> twStreamingData;
	@Autowired
	TwitterAuthApi twAuthApi;

	@Scheduled(fixedDelay = 1000)
	public void streamTweets() {
		/*
		 *		Twitter API restriction:
		 *		Rate limit	
		 *		Maximum 50 requests per 15-minute window (app auth) 
		 */
		if (StringUtils.isBlank(twAuthApi.getBearer())) {
			twStreamingData.clear();
			//twStreamingData.add("{\"Authorization\" : \"invalid credentials\"}");
			return;
		}
		
		log.info("starting http twitter streaming at {}", ZonedDateTime.now());
		boolean onError = false;
		try {
			String url = twitterSearchStreamApiUrl + queryStr + twitterExpansionQuery + querySep + twitterMediaFieldsQuery + querySep + twitterPlaceFieldsQuery + querySep + twitterUserFieldsQuery;
			twitterStream(twAuthApi.getBearer(), url, twStreamingData);
		} catch (Exception e) {
			log.error("failed to retrieve streamed tweets. Err {}", e.getMessage());
			onError = true;
		} finally {
			if(onError) {
				twStreamingData.clear();
				String errDesc = "failed to connect to twitter at " + ZonedDateTime.now() + "  Try later";
				twStreamingData.add("{\"Error\" : \"" + errDesc + "\"}");
			}
		}
		
		log.info("end http twitter streaming at {}", ZonedDateTime.now());
	}

	public static void twitterStream(String token, String url, TwitterStreamedData<String> queue) throws IOException, URISyntaxException, ParseException, JSONException, InterruptedException, HttpException {
		String bearerToken = token;
		if (null != bearerToken) {
			Map<String, String> rules = new HashMap<>();
			rules.put("italiani OR #italiani -is:retweet -is:reply -has:links lang:it","rule_italia_001");
			rules.put("governo italiano OR #governoitaliano OR #politica -is:retweet -is:reply -has:links lang:it","rule_italia_002");
			rules.put("economia italiana OR #economiaitaliana OR #economia OR #sviluppo -is:retweet -is:reply -has:links lang:it","rule_italia_003");
			rules.put("industria italiana OR #industriaitaliana OR #aziende -is:retweet -is:reply -has:links lang:it","rule_italia_004");
			rules.put("istruzione OR #istruzione OR #scuola OR #scuolapubblica -is:retweet -is:reply -has:links lang:it","rule_italia_005");
			rules.put("occupazione OR #occupazione OR #lavoro -is:retweet -is:reply -has:links lang:it","rule_italia_006");
			rules.put("disoccupazione OR #disoccupazione OR #redditocittadinanza -is:retweet -is:reply -has:links lang:it","rule_italia_007");
			rules.put("pensioni OR #pensioni OR #pensionati -is:retweet -is:reply -has:links lang:it","rule_italia_008");
			rules.put("immigrazione OR #immigrazione -is:retweet -is:reply -has:links lang:it","rule_italia_009");
			rules.put("mortalita OR #morti -is:retweet -is:reply -has:links lang:it","rule_italia_010");
			rules.put("natalita OR #natatalita -is:retweet -is:reply -has:links lang:it","rule_italia_011");
			rules.put("clandestini OR #clandestini OR #sbarchi -is:retweet -is:reply -has:links lang:it","rule_italia_012");
			rules.put("emigrazione OR #emigrazione -is:retweet -is:reply -has:links lang:it","rule_italia_013");
			rules.put("immigrazione OR #immigrazione -is:retweet -is:reply -has:links lang:it","rule_italia_014");
			rules.put("scienza OR #scienza OR #innovazione OR #ricerca -is:retweet -is:reply -has:links lang:it","rule_italia_015");
			rules.put("cervelli OR #cervelliinfuga -is:retweet -is:reply -has:links lang:it","rule_italia_016");
			setupRules(bearerToken, rules);
			connectStream(bearerToken, url, queue);
		} else {
			log.error("There was a problem getting you bearer token. Please make sure you set the BEARER_TOKEN environment variable");
		}
	}
	
	/*
	 * This method calls the filtered stream endpoint and streams Tweets from it
	 * */
	private static void connectStream(String bearerToken, String url, TwitterStreamedData<String> queue) throws IOException, URISyntaxException, InterruptedException, HttpException {

		log.info("stream connection {}", url);
		
		HttpClient httpClient = HttpClients.custom()
										   .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
										   .build();

		URIBuilder uriBuilder = new URIBuilder(url);

		HttpGet httpGet = new HttpGet(uriBuilder.build());
		httpGet.setHeader("Authorization", String.format("Bearer %s", bearerToken));

		HttpResponse response = httpClient.execute(httpGet);

		if(response.getStatusLine().getStatusCode() < 200 || 
		   response.getStatusLine().getStatusCode() >= 300) {
			StringBuilder sb = new StringBuilder("Twitter request refused: ").append(response.getStatusLine().getStatusCode())
																			 .append(" - ").append(response.getStatusLine().getReasonPhrase());
			throw new HttpException(sb.toString());
		}
		
		log.warn("Twitter request accepted: {} - {}", response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
		HttpEntity entity = response.getEntity();
		if (null != entity) {
			InputStreamReader is = new InputStreamReader(entity.getContent());
			BufferedReader reader = new BufferedReader(is);
			String line = reader.readLine();
			while (line != null) {
				// watch-out this is not thread safe!
				if (StringUtils.isNotBlank(line)) {
					log.info(line);
					if (queue.remainingCapacity() > 0)
						queue.add(line);
					else {
						try {
							queue.poll();
							queue.put(line);
						} catch (InterruptedException e) {
							log.error("error on cached tweets, flushing");
							queue.clear();
						}
					}
				}
				/*
				 *		Next data 
				 */
				line = reader.readLine();
				/*
				 *		We don't need performance  
				 */
			    TimeUnit.SECONDS.sleep(1);
			}
		}
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
		HttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
										   .setCookieSpec(CookieSpecs.STANDARD).build()).build();
		URIBuilder uriBuilder = new URIBuilder(twitterSearchStreamApiUrl + "/rules");

		HttpPost httpPost = new HttpPost(uriBuilder.build());
		httpPost.setHeader("Authorization", String.format("Bearer %s", bearerToken));
		httpPost.setHeader("content-type", "application/json");
		StringEntity body = new StringEntity(getFormattedString("{\"add\": [%s]}", rules));
		httpPost.setEntity(body);
		HttpResponse response = httpClient.execute(httpPost);
		HttpEntity entity = response.getEntity();
		if (null != entity) {
			log.info("Rule created {}",EntityUtils.toString(entity, "UTF-8"));
		}
	}

	/*
	 * Helper method to get existing rules
	 * */
	private static List<String> getRules(String bearerToken) throws URISyntaxException, IOException, ParseException, JSONException {
		List<String> rules = new ArrayList<>();
		CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
										 			.setCookieSpec(CookieSpecs.STANDARD).build())
										 			.build();

		URIBuilder uriBuilder = new URIBuilder(twitterSearchStreamApiUrl + "/rules");
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

		URIBuilder uriBuilder = new URIBuilder(twitterSearchStreamApiUrl +"/rules");

		HttpPost httpPost = new HttpPost(uriBuilder.build());
		httpPost.setHeader("Authorization", String.format("Bearer %s", bearerToken));
		httpPost.setHeader("content-type", "application/json");
		StringEntity body = new StringEntity(getFormattedString("{ \"delete\": { \"ids\": [%s]}}", existingRules));
		httpPost.setEntity(body);
		HttpResponse response = httpClient.execute(httpPost);
		HttpEntity entity = response.getEntity();
		if (null != entity) {
			log.info("Rule deleted {}", EntityUtils.toString(entity, "UTF-8"));
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
