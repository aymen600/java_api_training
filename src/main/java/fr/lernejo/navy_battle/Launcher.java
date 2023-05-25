// Launcher.java
package fr.lernejo.navy_battle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.io.BufferedReader;

public class Launcher {
    public static void main(String[] args) throws IOException, InterruptedException {
        int port = Integer.parseInt(args[0]);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        server.setExecutor(threadPoolExecutor);
        server.createContext("/ping", new PingHandler());
        server.createContext("/api/game/start", new ApiGameStartHandler(port));
        server.createContext("/api/game/fire", new ApiGameFireHandler());
        server.start();
        System.out.println("Server started on port " + port);
        if(args.length == 2){
            sendMessage(port, args[1]);
        }
    }
    public static void sendMessage(int port, String url) throws IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url + "/api/game/start"))
            .setHeader("Accept", "application/json")
            .setHeader("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"id\":\"1\", \"url\":\"http://localhost:" + port + "\", \"message\":\"hello\"}"))
            .build();
        //HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}

class PingHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String response = "OK";
        exchange.sendResponseHeaders(200, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}

class ApiGameStartHandler implements HttpHandler {
    private final int port;
    public ApiGameStartHandler(int port) {
        this.port = port;
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            try {
                sendResponse(exchange, 404, "Not Found");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        String requestBody = getRequestString(exchange);
        String responseJson;
        try {responseJson = processGameStartRequest(requestBody);} catch (InterruptedException e) {throw new RuntimeException(e);}
        try {sendResponse(exchange, 202, responseJson);} catch (InterruptedException e) {throw new RuntimeException(e);}
        try {sendRequestFire(requestBody);} catch (InterruptedException e) {throw new RuntimeException(e);}
    }
    private String getRequestString(HttpExchange exchange) throws IOException {
        try (InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            StringBuilder requestBody = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                requestBody.append(line);
            }
            System.out.println(requestBody.toString());
            return requestBody.toString();
        }
    }
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException, InterruptedException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            //System.out.println(responseBytes);
            os.write(responseBytes);
        }
    }
    private String processGameStartRequest(String requestBody) throws IOException, InterruptedException {
        return "{\"id\": \"2aca7611-0ae4-49f3-bf63-75bef4769028\", \"url\": \"http://localhost:" + this.port + "\", \"message\": \"May the best code win\"}";
    }

    private void sendRequestFire(String requestReceive) throws IOException, InterruptedException {
        String jsonString = requestReceive.replaceAll("\\s+", "");
        int urlStartIndex = jsonString.indexOf("\"url\":\"") + 7;
        int urlEndIndex = jsonString.indexOf("\",", urlStartIndex);
        String url = jsonString.substring(urlStartIndex, urlEndIndex);
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url+"/api/game/fire?cell=B2"))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();
        String responseBody = response.body();
        System.out.println("Status code: " + statusCode);
        System.out.println("Response body: " + responseBody);
    }
}

class ApiGameFireHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            try {sendResponse(exchange, 404, "Not Found");} catch (InterruptedException e) {throw new RuntimeException(e);}return;
        }
        String cell = exchange.getRequestURI().getQuery();
        String responseJson = processGameFireRequest(cell);
        System.out.println("Réponse à envoyé: " + responseJson);
        try {
            sendResponse(exchange, 200, responseJson);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException, InterruptedException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private String processGameFireRequest(String cell) {
        // réponse factice
        String consequence = "sunk";
        boolean shipLeft = true;
        return "{\"consequence\": \"" + consequence + "\", \"shipLeft\": " + shipLeft + "}";
    }
}




