package com.honeytea.server;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Util {

	public static SSLContext insecureContext() {
		TrustManager[] noopTrustManager = new TrustManager[] {
				new X509TrustManager() {
					public void checkClientTrusted(X509Certificate[] xcs, String string) {
					}

					public void checkServerTrusted(X509Certificate[] xcs, String string) {
					}

					public X509Certificate[] getAcceptedIssuers() {
						return null;
					}
				}
		};
		try {
			SSLContext sc = SSLContext.getInstance("ssl");
			sc.init(null, noopTrustManager, null);
			return sc;
		}
		catch (KeyManagementException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

}
