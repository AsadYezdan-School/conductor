package com.github.asadyezdanschool.conductor.submitter;

import com.sun.net.httpserver.HttpServer;
import com.zaxxer.hikari.HikariDataSource;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        AppComponent component = DaggerAppComponent.create();
        ResourceConfig jerseyConfig = component.resourceConfig();

        URI baseUri = URI.create("http://0.0.0.0:" + port + "/");
        HttpServer server = JdkHttpServerFactory.createHttpServer(baseUri, jerseyConfig, false);

        // Use virtual threads for all request handling
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            server.stop(5);
            if (component.dataSource() instanceof HikariDataSource ds) {
                ds.close();
            }
        }));

        log.info("Submitter service listening on " + baseUri);
        Thread.currentThread().join();
    }
}