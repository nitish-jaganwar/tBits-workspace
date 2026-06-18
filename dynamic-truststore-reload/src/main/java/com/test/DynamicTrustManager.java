package com.test;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicReference;

public class DynamicTrustManager implements X509TrustManager {

    private final AtomicReference<X509TrustManager> currentTrustManager = new AtomicReference<>();

    public DynamicTrustManager(KeyStore initialKeyStore) throws Exception {
        reloadTrustManager(initialKeyStore);
    }

    public void reloadTrustManager(KeyStore newKeyStore) throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(newKeyStore);

        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                currentTrustManager.set((X509TrustManager) tm);
                System.out.println("   [SYSTEM] TrustManager successfully hot-reloaded in memory!");
                return;
            }
        }
        throw new IllegalStateException("No X509TrustManager found");
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        currentTrustManager.get().checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        currentTrustManager.get().checkServerTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return currentTrustManager.get().getAcceptedIssuers();
    }
}