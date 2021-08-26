package com.bkakadiya.example.boxpoc.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.bkakadiya.example.boxpoc.beans.BoxOauth2TokenResponse;

@Component
public class BoxUtility {
	
	private final RestTemplate restTemplate;

	private static final String ACCESS_TOKEN_URL = "https://api.box.com/oauth2/token";
	
    @Autowired
    public BoxUtility(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    /**
     * Get Access Token from Box using client id and secret 
     * @param userId
     * @return
     */
	public String getAccessToken(String userId) {
		
		//TODO: GET CLIENT ID, SECRET, ENTERPRISE ID FROM db BASED on user's company id 
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		//headers.add("PRIVATE-TOKEN", "xyz");

		//TODO: GET CLIENT ID, SECRET, ENTERPRISE ID FROM Secure Vault based on user id 
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("client_id","<your_app_client_id>");
		map.add("client_secret","<your_app_client_secret>");
		map.add("grant_type","client_credentials");
		map.add("box_subject_type","enterprise");
		map.add("box_subject_id","<your_enterprise_id>");
		

		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

		ResponseEntity<BoxOauth2TokenResponse> response =
		    restTemplate.exchange(ACCESS_TOKEN_URL,
		                          HttpMethod.POST,
		                          entity,
		                          BoxOauth2TokenResponse.class);
		
		
		return response.getBody().getAccess_token();
	}
}
