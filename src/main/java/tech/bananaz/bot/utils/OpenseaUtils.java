package tech.bananaz.bot.utils;

import java.net.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.*;
import net.minidev.json.*;

public class OpenseaUtils {
	
	private static final String OS_BASE			   	 = "https://api.opensea.io/api/v1/";
	private static final String OPENSEAEVENTSURL   	 = OS_BASE+"events?event_type=successful&only_opensea=false&limit=25&asset_contract_address=%s";
	private static final String OPENSEAEVENTSSLUGURL = OS_BASE+"events?event_type=successful&only_opensea=false&limit=25&collection_slug=%s";
	private static final String OPENSEATRANSFERS   	 = OS_BASE+"events?event_type=transfer&only_opensea=false&limit=20&asset_contract_address=%s&account_address=%s";
	private static final String NULLADDRESS        	 = "0x0000000000000000000000000000000000000000";
	private static final String APIKEYHEAD 		   	 = "x-api-key";
	private RestTemplate restTemplate 			   	 = new RestTemplate();
	private StringUtils sUtils  				   	 = new StringUtils();
	private JsonUtils jsonUtils 					 = new JsonUtils();
	private String apiKey;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenseaUtils.class);
	
	public OpenseaUtils(String apiKey) {
		if(!apiKey.equals("")) {
			this.apiKey = apiKey;
		} else {
			throw new RuntimeException("Opensea API key needed for library calls!");
		}
	}
	
	public JSONObject getCollectionListed(String contractAddress) throws Exception {
		String buildUrl = String.format(OPENSEAEVENTSURL, contractAddress);

		// Create header for validating
		HttpHeaders headers = new HttpHeaders();
		headers.set(APIKEYHEAD, this.apiKey);
		// Wrap headers in request body
		HttpEntity<String> wrappedRequest = new HttpEntity<>(headers);

		return getAllRequest(buildUrl, wrappedRequest);
	}
	
	public JSONObject getCollectionListedWithSlug(String slug) throws Exception {
		String buildUrl = String.format(OPENSEAEVENTSSLUGURL, slug);

		// Create header for validating
		HttpHeaders headers = new HttpHeaders();
		headers.set(APIKEYHEAD, this.apiKey);
		// Wrap headers in request body
		HttpEntity<String> wrappedRequest = new HttpEntity<>(headers);

		return getAllRequest(buildUrl, wrappedRequest);
	}
	
	public JSONObject getCollectionListedAfter(String contractAddress, String timestamp) throws Exception {
		String buildUrl = String.format(OPENSEAEVENTSURL + "&occurred_after=%s", contractAddress, timestamp);

		// Create header for validating
		HttpHeaders headers = new HttpHeaders();
		headers.set(APIKEYHEAD, this.apiKey);
		// Wrap headers in request body
		HttpEntity<String> wrappedRequest = new HttpEntity<>(headers);

		return getAllRequest(buildUrl, wrappedRequest);
	}
	
	public JSONObject getCollectionTransfers(String contractAddress) throws Exception {
		String buildUrl = String.format(OPENSEATRANSFERS, contractAddress, NULLADDRESS);

		// Create header for validating
		HttpHeaders headers = new HttpHeaders();
		headers.set(APIKEYHEAD, this.apiKey);
		// Wrap headers in request body
		HttpEntity<String> wrappedRequest = new HttpEntity<>(headers);

		return getAllRequest(buildUrl, wrappedRequest);
		
	}
	
	public JSONObject getCollectionTransfersAfter(String contractAddress, String timestamp) throws Exception {
		String buildUrl = String.format(OPENSEATRANSFERS + "&occurred_after=%s", contractAddress, NULLADDRESS, timestamp);

		// Create header for validating
		HttpHeaders headers = new HttpHeaders();
		headers.set(APIKEYHEAD, this.apiKey);
		// Wrap headers in request body
		HttpEntity<String> wrappedRequest = new HttpEntity<>(headers);

		return getAllRequest(buildUrl, wrappedRequest);
		
	}
	
	/**
	 * This is a helper method, in this method you can provide a String of the URL for 
	 * requesting and it will return a JSONObject of the response from Steam.
	 * 
	 * @param getURL Pass in a String of the API request URL.
	 * @return A json-smart object of the
	 * @throws HttpException 
	 * @throws InterruptedException
	 */
	private JSONObject getAllRequest(String getURL, HttpEntity<String> requestMetadata) throws Exception {
		// Variables for runtime
		URI createURI = sUtils.getURIFromString(getURL);
		ResponseEntity<String> result = null;
		JSONObject newResponse = new JSONObject();
		// Variables for timing
		long startTime = System.currentTimeMillis();
		long endTime = System.currentTimeMillis();
		try {
			// Runs for events endpoint, this to to append api key
			if(requestMetadata != null) {
				// Create HTTP Call
				result = restTemplate.exchange(createURI, HttpMethod.GET, requestMetadata, String.class);
				// Parse String response for JSONObject
				newResponse = jsonUtils.stringToJsonObject(result.getBody());
				endTime = System.currentTimeMillis();
			} else {
				result = restTemplate.getForEntity(createURI, String.class);
				newResponse = jsonUtils.stringToJsonObject(result.getBody());
			}
		} catch (HttpClientErrorException e) {
			LOGGER.error(String.format("Failed HTTP GET: [%s] %s - %s", e.getRawStatusCode(), e.getStatusText(), e.getResponseHeaders().toSingleValueMap()));
			throw new Exception(String.format("Failed HTTP GET: [%s] %s - %s", e.getRawStatusCode(), e.getStatusText(), e.getResponseHeaders().toSingleValueMap()));
		}
		LOGGER.debug(String.format("GET request took %sms", Long.valueOf(endTime-startTime).toString()));
		return newResponse;
	}
}
