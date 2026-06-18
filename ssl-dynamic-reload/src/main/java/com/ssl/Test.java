package com.ssl;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

public class Test {

    private static final String KEYSTORE_PATH = "truststore.jks";
    private static final String KEYSTORE_PASS = "changeit";
    private static final String TARGET_URL = "https://annotate.mytbits.com";

    public static void main(String[] args) throws Exception {
        System.out.println("=== STARTING CONTINUOUS SSL MONITORING DAEMON ===");

        // 1. Create a completely empty keystore to simulate an untrusted environment
        KeyStore keyStore = KeyStore.getInstance("JKS");
        File file = new File(KEYSTORE_PATH);
        if (file.exists()) {
            file.delete();
        } 

        keyStore.load(null, null);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            keyStore.store(fos, KEYSTORE_PASS.toCharArray());
        }

        // 2. Initialize our Dynamic Engine with the empty vault
        DynamicTrustManager dynamicTrustManager = new DynamicTrustManager(keyStore);

        // 3. Initialize Java 8 SSL Context
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, new TrustManager[] { dynamicTrustManager }, new SecureRandom());

        // 4. Build the Jersey Client
        Client client = ClientBuilder.newBuilder().sslContext(sslContext).build();

        // 5. THE CONTINUOUS MONITORING LOOP
        while (true) {
            System.out.println("\n--- [CRON] Executing Scheduled Monitor Check ---");
            
            // Phase A: Check the date and rotate ONLY if necessary
            CertificateRotationService.checkAndRotateCertificate(dynamicTrustManager, TARGET_URL, KEYSTORE_PATH, KEYSTORE_PASS);

            // Phase B: Test the application connection
            try {
                Response response = client.target(TARGET_URL).request().get();
                System.out.println("   [APP] Connection Test: SUCCESS (" + response.getStatus() + ")");
                
                
             // --- ADD THIS TO STOP THE LOOP ---
                System.out.println("\n[SUCCESS] Hot-reload confirmed! Exiting monitor loop to finish.");
                break; // This breaks the infinite loop and stops the program!
            } catch (Exception e) {
                System.out.println("   [APP] Connection Test: FAILED (PKIX path building failed)");
            }

            // Phase C: Sleep for 10 seconds before checking again (In reality, sleep for 12 or 24 hours)
            System.out.println("   [DAEMON] Sleeping for 10 seconds...");
            Thread.sleep(10000);
        }
    }
}