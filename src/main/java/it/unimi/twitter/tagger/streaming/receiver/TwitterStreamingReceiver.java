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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.configurationprocessor.json.JSONArray;
//import org.springframework.boot.configurationprocessor.json.JSONException;
//import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import it.unimi.twitter.tagger.configuration.TwitterAuthApi;
import it.unimi.twitter.tagger.configuration.TwitterStreamedData;
import lombok.extern.slf4j.Slf4j;

@Service("twitterStreamingService")
@Slf4j
public class TwitterStreamingReceiver {

	public static final String FAILED_TO_CONNECT_TO_TWITTER_AT = "failed to connect to twitter at ";
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

	@Scheduled(fixedDelay = 1500 * 10)
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
		} catch (ParseException | IOException | URISyntaxException | JSONException | InterruptedException | HttpException e) {
			log.error("failed to retrieve streamed tweets. Err {}", e.getMessage());
			onError = true;
		} finally {
			if(onError) {
				twStreamingData.clear();
				String errDesc = FAILED_TO_CONNECT_TO_TWITTER_AT + ZonedDateTime.now() + "  Try later";
				twStreamingData.add("{\"Error\" : \"" + errDesc + "\"}");
			}
		}
		
		log.info("end http twitter streaming at {}", ZonedDateTime.now());
	}

	public static void twitterStream(String token, String url, TwitterStreamedData<String> queue) throws IOException, URISyntaxException, ParseException, JSONException, InterruptedException, HttpException {
		String bearerToken = token;
		if (null != bearerToken) {
			/*
			 *		free developer account has maximum 25 rules. 
			 */
			
			Map<String, String> rules = new HashMap<>();
//			rules.put("(italiani OR Italia OR Patria OR popoloitaliano) lang:IT -is:retweet -is:reply -has:links","rule_italia_001");
//			rules.put("(senato OR parlamento OR senatori OR parlamentari) lang:IT -is:retweet -is:reply -has:links","rule_italia_002");
//			rules.put("(economia italiana OR mercati OR economia) lang:IT -is:retweet -is:reply -has:links","rule_italia_003");
//			rules.put("(industriali OR industria italiana OR industria OR fabbriche OR produzione) lang:IT -is:retweet -is:reply -has:links","rule_italia_004");
//			rules.put("(istruzione OR insegnamento OR maestre OR maestro OR #maestri) lang:IT -is:retweet -is:reply -has:links","rule_italia_005");
//			rules.put("(occupazione OR lavoro OR prospettive OR industrializzazione OR sviluppo OR futuro OR operai OR disoccupazione) lang:IT -is:retweet -is:reply -has:links","rule_italia_006");
//			rules.put("(pensioni OR lavoro OR pensioneintegrativa OR terzaeta OR #terzaeta OR #pensioni) lang:IT -is:retweet -is:reply -has:links","rule_italia_007");
//			rules.put("(invasione OR emigrazione OR immigrazione OR clandestini OR sbarchi OR accoglienza) lang:IT -is:retweet -is:reply -has:links","rule_italia_008");
//			rules.put("(matrimoni OR divorzi OR natalita OR mortalita OR nascite OR popolazione) lang:IT -is:retweet -is:reply -has:links","rule_italia_009");
//			rules.put("(motociclismo OR motociclette OR piloti OR automobilismo OR bolidi OR formula1) lang:IT -is:retweet -is:reply -has:links","rule_italia_010");
//			rules.put("(sperimentazione OR vaccini OR scienza OR ricerca OR laboratori OR ricercatori) lang:IT -is:retweet -is:reply -has:links","rule_italia_011");
//			rules.put("(pianeti OR esplorazione OR galassia OR spazio OR stelle OR alieni) lang:IT -is:retweet -is:reply -has:links","rule_italia_012");
//			rules.put("(malattie OR cancro OR terapie OR dottori OR ospedali) lang:IT -is:retweet -is:reply -has:links","rule_italia_013");
//			rules.put("(bambini OR asili OR infanzia) lang:IT -is:retweet -is:reply -has:links","rule_italia_014");
//			rules.put("(ingegneria OR informatica OR tecnologia) lang:IT -is:retweet -is:reply -has:links","rule_italia_015");
//			rules.put("(formazione OR didattica OR medie OR superiori OR laurea) lang:IT -is:retweet -is:reply -has:links","rule_italia_016");
//			rules.put("(criminalita OR carceri OR delinquenza OR illegalita OR giustizia) lang:IT -is:retweet -is:reply -has:links","rule_italia_017");
//			rules.put("(olimpiadi) lang:IT -is:retweet -is:reply -has:links","rule_italia_018");
//			rules.put("(campionato OR calcio OR legacalcio OR seriea OR coppe) lang:IT -is:retweet -is:reply -has:links","rule_italia_019");
//			rules.put("(cinema OR televisione OR radioitaliana OR attori OR registi OR modelle OR fotografia OR arte) lang:IT -is:retweet -is:reply -has:links","rule_italia_020");

			rules.put("(italiani OR Italia OR popoloitaliano) OR (#Italia OR #popoloitaliano OR #italiani) lang:IT -is:retweet -is:reply -has:links","rule_italia_001");
			//rules.put("(senato OR parlamento OR senatori OR parlamentari) OR (#senato OR #deputati OR #cameradeputati OR #governoitaliano OR #politica) lang:IT -is:retweet -is:reply -has:links","rule_italia_002");
			rules.put("(economia italiana OR mercati OR economia) OR (#borsa OR #economiaitaliana OR #economiaitalia OR #sviluppo OR #investimenti) lang:IT -is:retweet -is:reply -has:links","rule_italia_003");
			//rules.put("(industriali OR industria italiana OR industria OR fabbriche OR produzione) OR (#industria OR #confindustria OR #industriali OR #industriaitaliana OR #aziende) lang:IT -is:retweet -is:reply -has:links","rule_italia_004");
			rules.put("(istruzione OR insegnamento OR maestre OR maestro) OR (#maestri OR #istruzione OR #scuola OR #scuolapubblica OR #scuolaitaliana) lang:IT -is:retweet -is:reply -has:links","rule_italia_005");
			rules.put("(occupazione OR lavoro OR prospettive OR industrializzazione OR sviluppo OR prospettiva OR operai OR disoccupazione) OR (#occupazione OR #lavoro OR #prospettiva OR #lavoro OR #inps OR #sindataci OR #cgil OR #delocalizzazione OR #disoccupazione) lang:IT -is:retweet -is:reply -has:links","rule_italia_006");
			rules.put("(pensioni OR lavoro OR pensioneintegrativa OR terzaeta) OR (#terzaeta OR #pensioni OR #pensionati OR #pensionamento OR #quotacento OR #contributivo OR #retributivo OR #sistemapensionistico OR #pensioneintegrativa) lang:IT -is:retweet -is:reply -has:links","rule_italia_007");
			rules.put("(invasione OR emigrazione OR immigrazione OR clandestini OR sbarchi OR accoglienza) OR (#accoglienza OR #immigrazione OR #clandestini OR #sbarchi OR #emigrazione) lang:IT -is:retweet -is:reply -has:links","rule_italia_008");
			rules.put("(matrimoni OR divorzi OR natalita OR mortalita OR nascite OR popolazione) OR (#matrimoni OR #divorzi OR #popolazione OR #mortalita OR #natatalita OR #nascite) lang:IT -is:retweet -is:reply -has:links","rule_italia_009");
			rules.put("(motociclismo OR motociclette OR piloti OR automobilismo OR bolidi OR formula1) OR (#classeregina OR #motociclismo OR #piloti OR #casecostruttrici) lang:IT -is:retweet -is:reply -has:links","rule_italia_010");
			rules.put("(sperimentazione OR vaccini OR scienza OR ricerca OR laboratori OR ricercatori) OR (#vaccini OR #ricercatori OR #scienza OR #innovazione OR #ricerca OR #cervelli OR #laboratori) lang:IT -is:retweet -is:reply -has:links","rule_italia_011");
			//rules.put("(pianeti OR esplorazione OR galassia OR spazio OR stelle OR alieni) OR (#esplorazionespaziale OR #missionispaziali OR #agenziaspaziale OR #astronauti OR #alieni OR #pianeti OR #galassia OR #spazio OR #stelle OR #viaggispaziali) lang:IT -is:retweet -is:reply -has:links","rule_italia_012");
			rules.put("(malattie OR cancro OR terapie OR dottori OR ospedali) OR (#cancro OR #terapie OR #dottori OR #ospedali OR #malattie OR #alzheimer) lang:IT -is:retweet -is:reply -has:links","rule_italia_013");
			rules.put("(bambini OR asili OR infanzia) OR (#formazioneprimaria OR #scuolaprimaria OR #asili OR #infanzia OR #bambini) lang:IT -is:retweet -is:reply -has:links","rule_italia_014");
			//rules.put("(ingegneria OR informatica OR tecnologia) OR (#ingegneria OR #informatica OR #tecnologia OR #innovazione) lang:IT -is:retweet -is:reply -has:links","rule_italia_015");
			rules.put("(formazione OR didattica OR medie OR superiori OR laurea) OR (#didattica OR #formazione OR #dad OR #scuolemedie OR #scuolesuperiori OR #univerisita OR #laurea OR #formazioneadistanza) lang:IT -is:retweet -is:reply -has:links","rule_italia_016");
			rules.put("(criminalita OR carceri OR delinquenza OR illegalita OR giustizia) OR (#giustizia OR #criminalita OR #carceri OR #illegalita OR #delinquenza OR #sicurezza) lang:IT -is:retweet -is:reply -has:links","rule_italia_017");
			//rules.put("(olimpiadi) OR (#olimpiadi) lang:IT -is:retweet -is:reply -has:links","rule_italia_018");
			rules.put("(campionato OR calcio OR legacalcio OR seriea OR coppeeuropee) OR (#campionato OR #calcio) lang:IT -is:retweet -is:reply -has:links","rule_italia_019");
			rules.put("(autori OR televisione OR radioitaliana OR attori OR registi OR modelle OR fotografia OR arteitaliana) OR (#cinemaitaliano OR #televisione OR #radioitaliana OR #attori OR #registi OR #modelle OR #fotografia OR #arteitaliana) lang:IT -is:retweet -is:reply -has:links","rule_italia_020");

			/*
			//rules.put("occupazione OR #occupazione OR #lavoro -is:retweet -is:reply -has:links lang:it","rule_italia_006");
			//rules.put("disoccupazione OR #disoccupazione OR #redditocittadinanza -is:retweet -is:reply -has:links lang:it","rule_italia_007");
			//rules.put("mortalita OR #morti -is:retweet -is:reply -has:links lang:it","rule_italia_010");
			//rules.put("emigrazione OR #emigrazione -is:retweet -is:reply -has:links lang:it","rule_italia_013");
			//rules.put("immigrazione OR #immigrazione -is:retweet -is:reply -has:links lang:it","rule_italia_014");
			//rules.put("cervelli OR #cervelliinfuga -is:retweet -is:reply -has:links lang:it","rule_italia_016");
			//rules.put("tecnologia OR #tecnologia -is:retweet -is:reply -has:links lang:it","rule_italia_017");
			//rules.put("investimenti OR #investimenti -is:retweet -is:reply -has:links lang:it","rule_italia_018");
			//rules.put("mercati OR #mercati -is:retweet -is:reply -has:links lang:it","rule_italia_019");
			rules.put("ospedali OR #ospedali -is:retweet -is:reply -has:links lang:it","rule_italia_020");
			rules.put("decessi OR #decessi -is:retweet -is:reply -has:links lang:it","rule_italia_021");
			//rules.put("informatica OR #informatica -is:retweet -is:reply -has:links lang:it","rule_italia_025");
			rules.put("vacanze OR #vacanze -is:retweet -is:reply -has:links lang:it","rule_italia_026");
			rules.put("presidenziali OR #presidenziali -is:retweet -is:reply -has:links lang:it","rule_italia_027");
			//rules.put("formazione OR #formazione -is:retweet -is:reply -has:links lang:it","rule_italia_028");
			//rules.put("accademia OR #accademia -is:retweet -is:reply -has:links lang:it","rule_italia_029");
			//rules.put("università OR #universita -is:retweet -is:reply -has:links lang:it","rule_italia_030");
			//rules.put("laurea OR #laurea -is:retweet -is:reply -has:links lang:it","rule_italia_031");
			//rules.put("universo OR #universo -is:retweet -is:reply -has:links lang:it","rule_italia_032");
			//rules.put("galassia OR #galassia -is:retweet -is:reply -has:links lang:it","rule_italia_034");
			//rules.put("spazio OR #spazio -is:retweet -is:reply -has:links lang:it","rule_italia_035");
			//rules.put("stelle OR #stelle -is:retweet -is:reply -has:links lang:it","rule_italia_036");
			//rules.put("asili OR #asili -is:retweet -is:reply -has:links lang:it","rule_italia_038");
			//rules.put("donne OR #donne -is:retweet -is:reply -has:links lang:it","rule_italia_039");
			//rules.put("uomini OR #uomini -is:retweet -is:reply -has:links lang:it","rule_italia_040");
			//rules.put("cancro OR #cancro -is:retweet -is:reply -has:links lang:it","rule_italia_042");
			//rules.put("alzheimer OR #alzheimer -is:retweet -is:reply -has:links lang:it","rule_italia_043");
			//rules.put("terapie OR #terapie -is:retweet -is:reply -has:links lang:it","rule_italia_044");
			//rules.put("dottori OR #dottori -is:retweet -is:reply -has:links lang:it","rule_italia_045");
			//rules.put("medici OR #medici -is:retweet -is:reply -has:links lang:it","rule_italia_046");
			//rules.put("infermieri OR #infermieri -is:retweet -is:reply -has:links lang:it","rule_italia_047");
			//rules.put("amore OR #amore -is:retweet -is:reply -has:links lang:it","rule_italia_048");
			//rules.put("musica OR #musica -is:retweet -is:reply -has:links lang:it","rule_italia_049");
			//rules.put("giocatori OR #giocatori -is:retweet -is:reply -has:links lang:it","rule_italia_050");
			//rules.put("atleti OR #atleti -is:retweet -is:reply -has:links lang:it","rule_italia_051");
			//rules.put("giovani OR #giovani -is:retweet -is:reply -has:links lang:it","rule_italia_052");
			//rules.put("esplorazione OR #esplorazione -is:retweet -is:reply -has:links lang:it","rule_italia_053");
			//rules.put("concerti OR #concerti -is:retweet -is:reply -has:links lang:it","rule_italia_054");
			//rules.put("stelle OR #stelle -is:retweet -is:reply -has:links lang:it","rule_italia_055");			
			//rules.put("cinema OR #cinema OR #film OR #televisione -is:retweet -is:reply -has:links lang:it","rule_italia_057");
 */
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
