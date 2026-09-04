package com.example.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity; // ✅ THIS ONE
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form; // ✅ THIS ONE
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/hello")
public class HelloResource {

	public static void main(String[] args) {
  		String tokenResponse = getAccessToken();
		System.out.println("Access Token Response: " + tokenResponse);
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String sayHello() {
		return "Hello World from Jersey!";
	}

	public static String getAccessToken() {
		String url = "https://oauth2.googleapis.com/token";
		
		javax.ws.rs.client.Client client = ClientBuilder.newClient();

		WebTarget target = client.target(url);

//		Form form = new Form();
//		form.param("grant_type", "password");
//		form.param("username", "nitish.j@tbitsglobal.com");
//		form.param("password", "");
//		form.param("client_id", "");
//		form.param("client_secret", "");

		
		
		Form form = new Form();
		form.param("code", "AUTH_CODE");
		form.param("client_id", "");
		form.param("client_secret", "");
		form.param("redirect_uri", "http://localhost:8080/callback");
		form.param("grant_type", "authorization_code");

		Response response = target
		    .request()
		    .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));
//		Response response = target.request(MediaType.APPLICATION_JSON)
//				.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));

		String json = response.readEntity(String.class);

		System.out.println("Response: " + json);
		int status = response.getStatus();
		System.out.println("HTTP Status: " + status);
		return json; // extract access_token from this
	}

}