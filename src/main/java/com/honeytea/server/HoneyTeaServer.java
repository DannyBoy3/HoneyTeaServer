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

    private final static int BATCH_SIZE = 20;

    private final long refreshPeriod;
    private final int port;
    private final Javalin javalin;
    private final Registry registry = new Registry();
    private final ScheduledExecutorService scheduler;
    private final Collection<LinkProvider> providers = new ArrayList<>();
    private final LinkProcessor processor = new LinkProcessor();

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
                this::fetchLinks,
                1000,
                refreshPeriod,
                TimeUnit.MILLISECONDS);
    }

    private void fetchLinks() {
        Collection<String> result = new HashSet<>();
        for (LinkProvider provider : providers) {
            String name = provider.getName();
            System.out.println("Fetching links from " + name);
            Collection<String> links = provider.fetchLinks();
            System.out.println("Received " + links.size() + " links from " + name + " processing...");

            long startTime = System.currentTimeMillis();
            long batchTime = System.currentTimeMillis();
            long cycles = 0;

            for (String link : links) {
                try {
                    if (registry.contains(link)) {
                        continue;
                    }
                    result.add(processor.process(link));
                } catch (Exception e) {
                    System.err.println("Failed to process " + link + " " + e.getMessage());
                }
                if (result.size() >= BATCH_SIZE) {
                    // batch is processed
                    long now = System.currentTimeMillis();
                    long deltaBatchTime = now - batchTime;
                    long deltaStartTime = now - startTime;
                    batchTime = now;

                    cycles++;
                    long averageBatchTime = deltaStartTime / cycles;

                    System.out.println("Time from start elapsed: " + Duration.ofMillis(deltaStartTime));
                    System.out.println("Time for processing last " + BATCH_SIZE + " links: " + Duration.ofMillis(deltaBatchTime));
                    System.out.println("Average " + BATCH_SIZE + " batch time: " + Duration.ofMillis(averageBatchTime));

                    registry.save(result);
                    result.clear();
                }
            }
        }

        registry.save(result);
    }

}
