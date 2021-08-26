package com.bkakadiya.example.boxpoc.controller;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.bkakadiya.example.boxpoc.util.BoxUtility;
import com.box.sdk.BoxWebHookSignatureVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;


@RestController
@RequestMapping("/api/v1")
@Api("Box.com interation APIs")
public class BoxController {
	
	private final Logger log = LoggerFactory.getLogger(BoxController.class);
	
	
	private final String BOX_DELIVERY_ID = "box-delivery-id";
	private final String BOX_DELIVERY_TIMESTAMP = "box-delivery-timestamp";
	private final String BOX_SIGNATURE_ALGORITHM = "box-signature-algorithm";
	private final String BOX_SIGNATURE_PRIMARY = "box-signature-primary";
	private final String BOX_SIGNATURE_SECONDARY = "box-signature-secondary";
	private final String BOX_SIGNATURE_VERSION = "box-signature-version";
	
	/**
	 * TODO: For production code, Get it from Secure place
	 */
	private final String primaryKey = "";
	private final String secondaryKey = "";
	
	private final RestTemplate restTemplate;

    @Autowired
    public BoxController(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }
    
    @Autowired
    private BoxUtility boxUtility;
    
	@GetMapping("/hello")
	public ResponseEntity<String> hello(){
		return ResponseEntity.ok()       		
        		.body("Got the request");
	}
	
	@PostMapping(value="/doc-requests", consumes={"application/json;charset=UTF-8"})
	@ApiOperation(value = "Box.com will post event here")
    public ResponseEntity<String> createDocRequest(@RequestBody String values, @RequestHeader Map<String, String> headers, HttpServletRequest request) throws URISyntaxException, IOException {
    	
        log.debug("REST request to save DocRequest ");
        log.info("Values:{}", values);
        log.info("Primary key: " + primaryKey);        
        log.info("Secondary key: " + secondaryKey);
        log.info("Headers: " + headers);
        log.info("Box Delivery id: " + headers.get(BOX_DELIVERY_ID));
        
        Map<String, Object> valueMap = new ObjectMapper ().readValue(values, HashMap.class); 
        
        String eventTrigger = (String) valueMap.get("trigger");
        String eventInitiatedBy = get2ndLevel(valueMap, "created_by", "login");
        log.info("Event Initited By: " +  eventInitiatedBy) ;
        
        
        //TODO: GET PRIMARY KEY AND SECONDARY KEY BASED ON THIS USER'S COMPANY ID 
        BoxWebHookSignatureVerifier verifier = new BoxWebHookSignatureVerifier(primaryKey, secondaryKey);
        boolean isValidMessage = verifier.verify(
            headers.get(BOX_SIGNATURE_VERSION),
            headers.get(BOX_SIGNATURE_ALGORITHM),
            headers.get(BOX_SIGNATURE_PRIMARY),
            headers.get(BOX_SIGNATURE_SECONDARY),
            values,
            headers.get(BOX_DELIVERY_TIMESTAMP)
        );
        
        if (isValidMessage) {
            log.info("Message is valid - " + headers.get(BOX_DELIVERY_ID));
            
            //TODO: Based on  
            // "trigger": "FILE.UPLOADED",  (Box Event)
            // "source": "id": "850219963408",  (Box file id) 
            // perform required operation
            // file can be pulled from box using https://api.box.com/2.0/files/:file_id/content/
            
            switch(eventTrigger) {
            
            case "FILE.UPLOADED":
            	log.info("File upload triggeed");
            	String fileId = get2ndLevel(valueMap, "source", "id");
            	String fileName = get2ndLevel(valueMap, "source", "name");
            	log.info("File Id : " + fileId + ", File Name : " + fileName);
            	
            	if(Files.notExists(Paths.get("files", fileName))) {
            		String fileContentUrl = "https://api.box.com/2.0/files/" + fileId + "/content/";
                	log.info("Fetching file : " + fileContentUrl);
                	
                	HttpHeaders boxHeaders = new HttpHeaders();
                	boxHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));
                	boxHeaders.set("Authorization", "Bearer " + boxUtility.getAccessToken(eventInitiatedBy));
                    HttpEntity<String> entity = new HttpEntity<>(boxHeaders);
                    ResponseEntity<byte[]> response = restTemplate.exchange(fileContentUrl, HttpMethod.GET, entity, byte[].class);
                    
                    Files.write(Paths.get("files", fileName), response.getBody());
                    log.info("Saved : " +  "files/" + fileName);    	
            	} else {
            		log.info("File Already exists with this name");
            	}
            	
            	break;
            	
            	
            //TODO: ADD OTHER TRIGGER DETAILS 
            	
            default:
            	log.info("Unknown trigger");
            }
            
            
            return ResponseEntity.created(new URI("/api/v1/doc-requests/1"))        		
            		.body("Got the request for : " + values);
            
            
        } else {
        	log.warn("Message is tempared -- " + headers.get(BOX_DELIVERY_ID) + " -- ip address: " + request.getRemoteAddr() + " -- x-forward-for: " + request.getHeader("X-FORWARDED-FOR"));
        	
        	return ResponseEntity.badRequest().body("This request is not initated from valid Box Client");
        }
        
    }

	private String get2ndLevel(Map<String, Object> valueMap, String first, String second) {
		return ( (Map<String, String>) valueMap.get(first)).get(second);
	}

}
