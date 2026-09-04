package com.annotator.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.Collections;

public class GoogleTokenVerifier {

    //provided Google Client ID
    private static final String CLIENT_ID = "6755583295-c5bc4f2ce6c1mh6mk4i725pmppra78qc.apps.googleusercontent.com";

    public static GoogleIdToken.Payload verifyToken(String jwtToken) {
        if (jwtToken == null || jwtToken.isEmpty()) return null;

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(CLIENT_ID))
                    .build();

            // Sends token signature to Google servers for strict verification
            GoogleIdToken idToken = verifier.verify(jwtToken);
            
            if (idToken != null) {
                return idToken.getPayload(); // Genuine Token
            } else {
                System.out.println("OIDC Validation Failed: Invalid token signature.");
                return null;
            }
        } catch (Exception e) {
            System.out.println(" OIDC Exception: " + e.getMessage());
            return null;
        }
    }
}