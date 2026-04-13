package com.aui.translator.bootstrap;

import java.io.IOException;
import java.net.URI;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

public final class TranslatorServer {
    private static final String BASE_URI = "http://0.0.0.0:8081/api/";

    private TranslatorServer() {
    }

    public static HttpServer startServer() {
        return GrizzlyHttpServerFactory.createHttpServer(
                URI.create(BASE_URI),
                new TranslatorApplication()
        );
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = startServer();
        System.out.println("Translator service running at " + BASE_URI);
        System.out.println("Try POST /translator/translate with basic auth.");
        System.in.read();
        server.shutdownNow();
    }
}
