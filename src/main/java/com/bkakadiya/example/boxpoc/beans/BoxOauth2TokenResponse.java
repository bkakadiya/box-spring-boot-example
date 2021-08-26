package com.bkakadiya.example.boxpoc.beans;

import java.io.Serializable;

public class BoxOauth2TokenResponse implements Serializable {
		
	private static final long serialVersionUID = 2539268944755847163L;

	private String access_token;
	
	private Long expiresIn;
	
	private String tokenType;

	public String getAccess_token() {
		return access_token;
	}

	public void setAccess_token(String access_token) {
		this.access_token = access_token;
	}

	public Long getExpiresIn() {
		return expiresIn;
	}

	public void setExpiresIn(Long expiresIn) {
		this.expiresIn = expiresIn;
	}

	public String getTokenType() {
		return tokenType;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}
	
	

}
