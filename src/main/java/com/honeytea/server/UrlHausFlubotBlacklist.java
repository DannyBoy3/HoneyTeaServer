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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class UrlHausFlubotBlacklist implements LinkProvider {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final URI apiUrl;
	private final HttpClient client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(3))
			.sslContext(Util.insecureContext())
			.proxy(ProxySelector.of(new InetSocketAddress("127.0.0.1", 3129)))
			.build();

	private final Set<String> cache = new HashSet<>();

	public UrlHausFlubotBlacklist() {
		try {
			apiUrl = new URI("https://urlhaus-api.abuse.ch/v1/tag/");
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
		HttpRequest request = HttpRequest.newBuilder(apiUrl)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString("tag=Flubot"))
				.build();

		try {
			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
			JsonNode jsonNode = objectMapper.readTree(response.body());
			ArrayNode urls = (ArrayNode) jsonNode.get("urls");

			Collection<String> results = new HashSet<>();

			urls.forEach(node -> {
				try {
					String link = node.get("url").asText().replace("\\/", "/");
					if (cache.contains(link)) {
						return;
					}
					cache.add(link);
					results.add(link);
				} catch (Exception e) {
					//do nothing
				}
			});

			return results;
		}
		catch (IOException | InterruptedException e) {
			System.err.println("Failed to fetch url db: " + e.getMessage());
		}
		return emptyList();
	}

}
