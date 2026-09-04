package com.truststore.verifier;

import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.LinkedHashSet;
import java.util.Set;

public class Test {

	public static void main(String[] args) {

		// Uncomment for deep SSL handshake debugging if needed
		//System.setProperty("javax.net.debug", "ssl,handshake,trustmanager");

		// Set the combined truststore path and password - glashfish5 + java - jdk1.8.0_212
		System.setProperty("javax.net.ssl.trustStore", "C:\\Users\\NITISH JAGANWAR\\Downloads\\cacerts_combine.jks");
		System.setProperty("javax.net.ssl.trustStorePassword", "changeit");

	
		Set<String> urls = new LinkedHashSet<>();

		// Google Core & Workspace
		urls.add("https://www.google.com");
		urls.add("https://accounts.google.com");
		urls.add("https://www.googleapis.com");

		// Google Cloud APIs (Vision, Document AI, Gemini)
		urls.add("https://generativelanguage.googleapis.com");
		urls.add("https://storage.googleapis.com");
		urls.add("https://vision.googleapis.com");
		urls.add("https://documentai.googleapis.com");
		urls.add("https://vision.googleapis.com/v1/images:annotate");
		urls.add("https://vision.googleapis.com/$discovery/rest?version=v1");

		// chatbot service - tBits
		urls.add("https://annotate.mytbits.com");

		// Microsoft 365 & Azure
		urls.add("https://www.microsoft.com");
		urls.add("https://login.microsoftonline.com");
		urls.add("https://graph.microsoft.com");
		urls.add("https://outlook.office365.com");
		urls.add("https://management.azure.com");

		// AWS Infrastructure & AI Services
		urls.add("https://aws.amazon.com");
		urls.add("https://s3.amazonaws.com");
		urls.add("https://sts.amazonaws.com");
		urls.add("https://rekognition.us-east-1.amazonaws.com");
		urls.add("https://textract.us-east-1.amazonaws.com");
		urls.add("https://lambda.us-east-1.amazonaws.com");
		urls.add("https://bedrock-runtime.us-east-1.amazonaws.com");

		// Other Platforms
		urls.add("https://login.salesforce.com");
		urls.add("https://github.com");
		urls.add("https://api.openai.com");
		urls.add("https://www.cloudflare.com");
		urls.add("https://www.oracle.com");
		urls.add("https://www.mozilla.org");
		urls.add("https://www.apple.com");

		System.out.println("====================================================");
		System.out.println("TrustStore : " + System.getProperty("javax.net.ssl.trustStore"));
		System.out.println("====================================================");

		int success = 0;
		int failed = 0;

		for (String url : urls) {

			System.out.println("\n====================================================");
			System.out.println("Testing : " + url);

			try {
				HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
				conn.setConnectTimeout(10000);
				conn.setReadTimeout(10000);
				conn.connect();

				System.out.println("SUCCESS");
				System.out.println("Response Code : " + conn.getResponseCode());

				/*
				 * A cipher suite is a set of cryptographic algorithms used to establish a
				 * secure network connection. During the TLS/SSL handshake, clients and servers
				 * negotiate a matching cipher suite to ensure secure data exchange
				 */
				
				System.out.println("Cipher Suite  : " + conn.getCipherSuite());

				Certificate[] certs = conn.getServerCertificates();
				System.out.println("\nCertificate Chain:");

				for (int i = 0; i < certs.length; i++) {
					X509Certificate cert = (X509Certificate) certs[i];
					System.out.println("\nCertificate #" + (i + 1));
					System.out.println("Subject : " + cert.getSubjectX500Principal());
					System.out.println("Issuer  : " + cert.getIssuerX500Principal());
				}

				conn.disconnect();
				success++;

			} catch (Exception e) {
				failed++;
				System.out.println("FAILED");
				System.out.println("Exception: " + e.getClass().getName());
				System.out.println("Message: " + e.getMessage());
			}
		}

		System.out.println("\n====================================================");
		System.out.println("TEST SUMMARY");
		System.out.println("====================================================");
		System.out.println("Successful : " + success);
		System.out.println("Failed     : " + failed);
	}
}