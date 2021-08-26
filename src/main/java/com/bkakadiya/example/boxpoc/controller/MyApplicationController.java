package com.bkakadiya.example.boxpoc.controller;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.bkakadiya.example.boxpoc.util.BoxUtility;
import com.bkakadiya.example.boxpoc.util.MultipartByteArrayResource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api/v1/my-app")
@Api("These are My Application Apis (Internal/External)")
public class MyApplicationController {

	private final RestTemplate restTemplate;

    @Autowired
    public MyApplicationController(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }
    
    @Autowired
    private BoxUtility boxUtility;
    
	private final Logger log = LoggerFactory.getLogger(MyApplicationController.class);
	
	private static final String BOX_FILES_URL = "https://upload.box.com/api/2.0/files/content";
	
	//TODO: UPDATE FOLDER ID HERE - BETTER TO GET IT FROM TEMPLATE/DB 
	private static final String BOX_FILE_UPLOAD_PAYLOAD = "{\"name\":\"___FILENAME__\", \"parent\":{\"id\":\"<folderId>\"}}";
	
	@PostMapping("/documents")
    @ApiOperation(value = "Handles new document request")
    public ResponseEntity<String> createDocRequest(
			@RequestParam(required = false) String sourceName,
			@RequestPart MultipartFile docFile) throws URISyntaxException, IOException {
    	
        log.debug("REST request to save DocRequest from source: {}", sourceName);
        
        Files.write(Paths.get("files", docFile.getOriginalFilename()), docFile.getBytes());
        
        log.info("File saved on my-app files folder");
        
        log.info("Sending file to Box.com");
        
        String eventInitiatedBy = "Abc";
    
        String payload = BOX_FILE_UPLOAD_PAYLOAD.replace("___FILENAME__", docFile.getOriginalFilename());
        
        MultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
		bodyMap.add("attributes", payload);
		//bodyMap.add("productId", request.getProductId());

		MultipartFile attachment = docFile;

		if (attachment != null && attachment.getOriginalFilename() != null) {
			MultipartByteArrayResource resource = new MultipartByteArrayResource(attachment.getBytes());
			resource.setFilename(attachment.getOriginalFilename());

			bodyMap.add("file", resource);
		}

		FormHttpMessageConverter converter = new FormHttpMessageConverter();
		restTemplate.getMessageConverters().add(converter);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		headers.set("Authorization", "Bearer " + boxUtility.getAccessToken(eventInitiatedBy));
        
		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);

		String response = restTemplate.postForObject(BOX_FILES_URL, requestEntity, String.class);

		
        return ResponseEntity.created(new URI("/location-of-creatd-doc"))        		
        		.body("Processed request for : " + docFile.getOriginalFilename() + "; Box response : " + response);
    }
	
	@GetMapping("/documents")
    @ApiOperation(value = "(Testing purpose only) Get All Document requests")
    public ResponseEntity<List<String>> getAllFiles() {
    	
        log.debug("REST request to get list of files");
        List<String> fileList = Arrays.asList("One", "Two");
        return ResponseEntity.ok().body(fileList);
    }


}
