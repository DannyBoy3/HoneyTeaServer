package com.honeytea.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Stack;

public class LinkProcessor {

	//todo add more links
	private final Collection<String> whitelist = Arrays.asList("bitly.", "google.", "tinyurl.", "adf.", "bit.ly");

	private final Registry registry;
	private final HttpClient client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(3))
			.followRedirects(HttpClient.Redirect.ALWAYS)
			.sslContext(Util.insecureContext())
			.build();

	private final Stack<URI> jobQueue = new Stack<>();
	private final Thread worker;

	public LinkProcessor(Registry registry) {
		this.registry = registry;
		this.worker = new Thread(this::runAnalysis);
		worker.start();
	}

	private void runAnalysis() {
		while (true) {
			try {
				if (jobQueue.isEmpty()) {
					Thread.sleep(1000);
					continue;
				}

				URI uri = jobQueue.pop();

				HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
				String host = getHost(uri);
				HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

				//check if redirects happened
				if (response.previousResponse().isPresent()) {
					String endHost = getHost(response.uri());
					boolean sameDomain = endHost.contains(host) || host.contains(endHost);
					if (!sameDomain) {
						//is a redirect link, block url only
						String redirectLink = uri.toString().split("://")[1];
						registry.save(Arrays.asList(redirectLink, endHost));
					}
				}

			}
			catch (Exception e) {
				//
			}
		}
	}

	private boolean isWhitelisted(URI uri) {
		String host = uri.getHost();
		for (String part : whitelist) {
			if (host.contains(part)) {
				return true;
			}
		}
		return false;
	}

	public void process(Collection<String> links) {
		Collection<String> results = new ArrayList<>();
		for (String link : links) {
			try {

				URI uri = new URI(link);
				if (uri.getScheme() == null) {
					uri = withSchema(uri, "http");
				}

				if (registry.contains(uri.getHost())) {
					//skip already saved
					continue;
				}

				if (!isWhitelisted(uri)) {
					results.add(uri.getHost());
				}

				scheduleAnalysis(uri);
			}
			catch (Exception e) {
				System.err.println("Failed to process " + link + " : " + e.getMessage());
			}
		}
		registry.save(results);
	}

	private void scheduleAnalysis(URI uri) {
		jobQueue.push(uri);
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