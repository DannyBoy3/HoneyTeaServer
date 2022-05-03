package com.honeytea.server;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.javalin.Javalin;

public class HoneyTeaServer {

	private final long refreshPeriod;
	private final int port;
	private final Javalin javalin;
	private final Registry registry = new Registry();
	private final ScheduledExecutorService scheduler;
	private final Collection<LinkProvider> providers = new ArrayList<>();
	private final LinkProcessor processor = new LinkProcessor(registry);
	private boolean isWorking = false;

	public HoneyTeaServer(long refreshPeriod, int port) {
		this.refreshPeriod = refreshPeriod;
		this.port = port;

		providers.add(new UrlHausFlubotBlacklist());
		providers.add(new UrlHausGenericBlacklist());

		javalin = Javalin.create();
		scheduler = Executors.newSingleThreadScheduledExecutor();
	}

	public void start() {
		javalin.start(port);
		javalin.get("/links", ctx -> {
			String body = registry.getAll()
					.stream()
					.collect(Collectors.joining("\n"));
			ctx.result(body);
		});
		scheduler.scheduleAtFixedRate(
				() -> {
					if (isWorking) {
						return;
					}
					new Thread(this::fetchLinks).start();
				},
				1000,
				refreshPeriod,
				TimeUnit.MILLISECONDS);
	}

	private void fetchLinks() {
		try {
			isWorking = true;

			for (LinkProvider provider : providers) {
				String name = provider.getName();
				System.out.println("Fetching links from " + name);
				Collection<String> links = provider.fetchLinks();
				System.out.println("Received " + links.size() + " links from " + name + " processing...");

				processor.process(links);
			}

		} finally {
			isWorking = false;
		}
	}

}