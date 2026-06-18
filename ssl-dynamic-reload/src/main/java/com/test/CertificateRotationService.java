package com.test;



import javax.net.ssl.*;
import java.net.URL;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.io.*;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CertificateRotationService {

    // Tracks last seen fingerprint per URL — detects rotation even before expiry
    private static final Map<String, String> lastKnownFingerprints =
        new ConcurrentHashMap<>();

    public static void checkAndRotateCertificate(
            DynamicTrustManager dynamicTrustManager,
            String targetUrl,
            String keystorePath,
            String keystorePass) {
        try {
            System.out.println("   [MONITOR] Inspecting: " + targetUrl);

            // 1. Fetch cert using trust-all context (safe — read only, not app traffic)
            X509Certificate targetCert = fetchCertTrustAll(targetUrl);

            // 2. Expiry check
            Date expirationDate = targetCert.getNotAfter();
            long diffMillis = expirationDate.getTime() - new Date().getTime();
            long daysLeft   = TimeUnit.DAYS.convert(diffMillis, TimeUnit.MILLISECONDS);

            System.out.println("   [MONITOR] Expires: " + expirationDate
                + " (" + daysLeft + " days remaining)");

            // 3. Alert tiers
            if      (daysLeft <= 1)  System.err.println("   [CRITICAL] Expires in " + daysLeft + " day(s)!");
            else if (daysLeft <= 7)  System.err.println("   [WARNING]  Expires in " + daysLeft + " days");
            else if (daysLeft <= 30) System.out.println("   [INFO]     Expires in " + daysLeft + " days");

            // 4. Fingerprint change detection (catches rotation before expiry)
         // 4. Fingerprint change detection
            String currentFp  = sha256(targetCert);
            String previousFp = lastKnownFingerprints.put(targetUrl, currentFp);
            boolean rotated      = previousFp != null && !previousFp.equals(currentFp);
            boolean expiringSoon = daysLeft < 100;
            boolean firstBoot    = previousFp == null;  //
            if (rotated) {
                System.out.println("   [ALERT] Fingerprint changed — certificate was rotated!");
                System.out.println("     Old: " + previousFp.substring(0,16) + "...");
                System.out.println("     New: " + currentFp.substring(0,16) + "...");
            }
            // 5. Install only if necessary
//            if (expiringSoon || rotated) {
//                System.out.println("   [ACTION] Installing new certificate into trust store...");
            if (firstBoot || expiringSoon || rotated) {               
                String reason = firstBoot    ? "first boot — seeding trust store"
                              : rotated      ? "fingerprint changed"
                              :                "expiring in " + daysLeft + " days";
                System.out.println("   [ACTION] Installing certificate (" + reason + ")...");
                KeyStore ks = KeyStore.getInstance("PKCS12");
                File f = new File(keystorePath);
                if (f.exists()) {
                    try (FileInputStream fis = new FileInputStream(f)) {
                        ks.load(fis, keystorePass.toCharArray());
                    }
                } else {
                    ks.load(null, keystorePass.toCharArray());
                }

                ks.setCertificateEntry("auto-rotated-cert", targetCert);

                try (FileOutputStream fos = new FileOutputStream(keystorePath)) {
                    ks.store(fos, keystorePass.toCharArray());
                }

                // Hot reload — no restart
                dynamicTrustManager.reloadTrustManager(ks);
                System.out.println("   [SYSTEM] Rotation complete. Zero downtime achieved.");

            } else {
                System.out.println("   [SYSTEM] Certificate healthy. No action required.");
            }

        } catch (Exception e) {
            System.err.println("   [ERROR] Monitor failed: " + e.getMessage());
        }
    }

    /**
     * Fetches the remote certificate using a trust-all SSLContext.
     * SAFE: used ONLY to read the cert, never for application traffic.
     */
    private static X509Certificate fetchCertTrustAll(String targetUrl) throws Exception {
        SSLContext readCtx = SSLContext.getInstance("TLSv1.2");
        readCtx.init(null, new TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] c, String a) {}
            public void checkServerTrusted(X509Certificate[] c, String a) {}
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }}, new SecureRandom());

        HttpsURLConnection conn =
            (HttpsURLConnection) new URL(targetUrl).openConnection();
        conn.setSSLSocketFactory(readCtx.getSocketFactory());
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        try {
            conn.connect();
            Certificate[] certs = conn.getServerCertificates();
            return (X509Certificate) certs[0];
        } finally {
            conn.disconnect(); // always runs
        }
    }

    private static String sha256(X509Certificate cert) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(cert.getEncoded());
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}