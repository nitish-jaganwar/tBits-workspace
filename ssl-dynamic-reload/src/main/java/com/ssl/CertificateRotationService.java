package com.ssl;



import javax.net.ssl.HttpsURLConnection;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class CertificateRotationService {

    public static void checkAndRotateCertificate(DynamicTrustManager dynamicTrustManager, String targetUrl, String jksPath, String jksPass) {
        try {
            System.out.println("   [MONITOR] Inspecting target: " + targetUrl);
            
            // 1. Connect and grab the certificate
            URL url = new URL(targetUrl);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.connect();
            
            Certificate[] certs = conn.getServerCertificates();
            X509Certificate targetCert = (X509Certificate) certs[0];
            conn.disconnect();
            
            // 2. CHECK EXPIRATION DATE
            Date expirationDate = targetCert.getNotAfter();
            long diffInMillies = expirationDate.getTime() - new Date().getTime();
            long daysUntilExpiration = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
            
            System.out.println("   [MONITOR] Certificate expires on: " + expirationDate + " (" + daysUntilExpiration + " days remaining)");

            // 3. DECISION LOGIC: Only rotate if less than 30 days remain!
            if (daysUntilExpiration<100) {
                System.out.println("   [ALERT] Certificate expiring soon! Initiating automated rotation...");
                
                KeyStore keyStore = KeyStore.getInstance("JKS");
                try (FileInputStream fis = new FileInputStream(jksPath)) {
                    keyStore.load(fis, jksPass.toCharArray());
                }

                keyStore.setCertificateEntry("auto-rotated-cert", targetCert);

                try (FileOutputStream fos = new FileOutputStream(jksPath)) {
                    keyStore.store(fos, jksPass.toCharArray());
                }
                
                // Hot reload the memory
                dynamicTrustManager.reloadTrustManager(keyStore);
                System.out.println("   [SYSTEM] Automated Rotation Complete. Zero Downtime Achieved.");
            } else {
                System.out.println("   [SYSTEM] Certificate is healthy. No action required.");
            }

        } catch (Exception e) {
            System.err.println("   [ERROR] Failed to monitor endpoint: " + e.getMessage());
        }
    }
}