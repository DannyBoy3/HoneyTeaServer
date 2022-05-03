package com.honeytea.server;

import static java.util.Collections.emptyList;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class UrlHausGenericBlacklist implements LinkProvider {

	private final URI apiUrl;
	private final HttpClient client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(3))
			.sslContext(Util.insecureContext())
			.build();

	private final Set<String> cache = new HashSet<>();

	public UrlHausGenericBlacklist() {
		try {
			apiUrl = new URI("https://urlhaus.abuse.ch/downloads/text_recent/");
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getName() {
		return apiUrl.toString();
	}

	@Override
	public Collection<String> fetchLinks() {
		HttpRequest request = HttpRequest.newBuilder(apiUrl).GET().build();

		try {
			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

			Collection<String> results = new HashSet<>();

			for (String link : response.body().split("\r\n")) {
				if (cache.contains(link)) {
					continue;
				}
				cache.add(link);
				results.add(link);
			}

			return results;
		}
		catch (IOException | InterruptedException e) {
			System.err.println("Failed to fetch url db: " + e.getMessage());
		}
		return emptyList();
	}

}
