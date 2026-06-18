package com.test;
import javax.net.ssl.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.*;
import java.security.KeyStore;
import java.security.SecureRandom;



//It watches the certificate automatically, installs the new one when needed, and keeps the application running
/*This is a standalone demo that simulates an untrusted environment and shows how to hot-reload the 
trust store without restarting the application*/

//his code solves that. It watches the certificate automatically, 
//installs the new one when needed, and keeps the application running — no restart, no downtime.
public class Standalone{

    private static final String KEYSTORE_PATH = "D:\\tBits-workspace\\keystore\\truststore1.jks";  // PKCS12
    private static final String KEYSTORE_PASS = "changeit";
    private static final String TARGET_URL    = "https://annotate.mytbits.com";
    private static final long   SLEEP_SECONDS = 10;

    public static void main(String[] args) throws Exception {
        System.out.println("=== STARTING SSL CERTIFICATE ROTATION DAEMON ===\n");

        // 1. Create empty PKCS12 trust store (simulates untrusted environment)
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        File file = new File(KEYSTORE_PATH);
        if (file.exists()) file.delete();
        keyStore.load(null, null);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            keyStore.store(fos, KEYSTORE_PASS.toCharArray());
        }
        System.out.println("[INIT] Empty trust store created at: " + KEYSTORE_PATH);

        // 2. Boot the Dynamic Trust Manager with the empty store
        DynamicTrustManager dynamicTrustManager = new DynamicTrustManager(keyStore);

        // 3. Build SSLContext once — reuse forever, inner TM swaps live
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null,
            new TrustManager[]{dynamicTrustManager},
            new SecureRandom());

        // 4. Build Jersey client once — backed by our dynamic SSLContext
        Client client = ClientBuilder.newBuilder()
            .sslContext(sslContext)
            .build();

        // 5. Monitoring loop
        int cycle = 0;
        while (true) {
            cycle++;
            System.out.println("\n--- [CYCLE " + cycle + "] Scheduled monitor check ---");

            // Phase A: check expiry + fingerprint, rotate if needed
            CertificateRotationService.checkAndRotateCertificate(
                dynamicTrustManager, TARGET_URL, KEYSTORE_PATH, KEYSTORE_PASS);

            // Phase B: test actual application connection
            try {
                Response response = client.target(TARGET_URL).request().get();
                System.out.println("   [APP] Connection: SUCCESS (HTTP "
                    + response.getStatus() + ")");
                response.close();

                System.out.println("\n[DEMO COMPLETE] Hot-reload confirmed — no restart was performed.");
                break;

            } catch (Exception e) {
                System.out.println("   [APP] Connection: FAILED — "
                    + e.getMessage());
                System.out.println("   [DAEMON] Retrying in "
                    + SLEEP_SECONDS + "s...");
                Thread.sleep(SLEEP_SECONDS * 1000);
            }
        }

        client.close();
    }
}