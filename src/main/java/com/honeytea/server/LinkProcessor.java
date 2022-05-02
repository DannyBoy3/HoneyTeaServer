package com.honeytea.server;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

public class LinkProcessor {

	private final HttpClient client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(3))
			.followRedirects(HttpClient.Redirect.ALWAYS)
			.sslContext(Util.insecureContext())
			.build();


	public String process(String link) {
		try {
			URI uri = new URI(link);
			if (uri.getScheme() == null) {
				uri = withSchema(uri, "http");
			}

			//check if malicious link is a redirect by traversing it
			HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
			String host = getHost(uri);
			try {
				HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

				//check if redirects happened
				if (response.previousResponse().isPresent()) {
					String endHost = getHost(response.uri());
					boolean sameDomain = endHost.contains(host) || host.contains(endHost);
					if (!sameDomain) {
						//is a redirect link, block url only
						return uri.toString().split("://")[1];
					}
				}
			}
			//no redirects happened, block entire domain
			catch (Exception e) {
				//do nothing
			}

			return host;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String getHost(URI uri) {
		String host = uri.getHost();
		if (host.startsWith("www.")) {
			return host.split("\\.")[1];
		}
		return host;
	}

	private URI withSchema(URI uri, String schema) throws URISyntaxException {
		return new URI(schema + "://" + uri.toString());
	}

}
