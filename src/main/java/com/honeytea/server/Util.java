package com.honeytea.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

	public static URI toUri(String link) throws Exception {
		int indexOfFragment = link.indexOf("#");
		if (indexOfFragment > 0 && indexOfFragment + 1 < link.length()) {
			String fragment = URLEncoder.encode(link.substring(indexOfFragment + 1), StandardCharsets.UTF_8.toString());
			link = link.substring(0, indexOfFragment + 1) + fragment;
		}

		URI uri = new URI(link);
		if (uri.getScheme() == null) {
			uri = withSchema(uri, "http");
		}
		return uri;
	}

	public static String getHost(URI uri) {
		String host = uri.getHost();
		if (host.startsWith("www.")) {
			return host.split("\\.")[1];
		}
		return host;
	}

	public static URI withSchema(URI uri, String schema) throws URISyntaxException {
		return new URI(schema + "://" + uri.toString());
	}

}
