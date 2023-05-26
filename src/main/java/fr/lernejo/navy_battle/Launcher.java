// Launcher.java
package fr.lernejo.navy_battle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.lang.model.util.Elements;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.io.BufferedReader;
import java.util.concurrent.atomic.AtomicInteger;

public class Launcher {
    public static void main(String[] args) throws IOException, InterruptedException {
        int port = Integer.parseInt(args[0]);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        server.setExecutor(threadPoolExecutor);
        server.createContext("/ping", new PingHandler());
        server.createContext("/api/game/start", new ApiGameStartHandler(port));
        server.createContext("/api/game/fire", new ApiGameFireHandler(port));
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
        System.out.println("Requete à envoyé : "+ "{\"id\":\"1\", \"url\":\"http://localhost:" + port + "\", \"message\":\"hello\"}");
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
        System.out.println("Réponse à envoyé : "+ responseJson);
        try {sendRequestFire(requestBody);} catch (InterruptedException e) {throw new RuntimeException(e);}
    }
    static String getRequestString(HttpExchange exchange) throws IOException {
        try (InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            StringBuilder requestBody = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                requestBody.append(line);
            }
            System.out.println("Requete recu : " + requestBody.toString());
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
    String processGameStartRequest(String requestBody) throws IOException, InterruptedException {
        return "{\"id\": \"2aca7611-0ae4-49f3-bf63-75bef4769028\", \"url\": \"http://localhost:" + this.port + "\", \"message\": \"May the best code win\"}";
    }

    public void sendRequestFire(String requestReceive) throws IOException, InterruptedException {
        String jsonString = requestReceive.replaceAll("\\s+", "");
        int urlStartIndex = jsonString.indexOf("\"url\":\"") + 7;
        int urlEndIndex = jsonString.indexOf("\",", urlStartIndex);
        String url = jsonString.substring(urlStartIndex, urlEndIndex);
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url+"/api/game/fire?cell=B2"))
            .setHeader("Accept", "application/json")
            .setHeader("Origin", String.valueOf(this.port))
            .setHeader("nbRequest", String.valueOf(1))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Requete envoyé en GET: "+ "cell=B2");
        String responseBody = response.body();System.out.println("Response recu: " + responseBody);
    }
}

class ApiGameFireHandler implements HttpHandler {
    private final int port;
    public ApiGameFireHandler(int port){
        this.port = port;
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            try {sendResponse(exchange, 404, "Not Found");} catch (InterruptedException e) {throw new RuntimeException(e);}return;
        }
        //int clientPort = exchange.getRemoteAddress().getPort();
        System.out.println("request fire receive from : " +this.port);
        String cell = exchange.getRequestURI().getQuery();
        String responseJson = processGameFireRequest(cell);
        System.out.println("Réponse à envoyé: " + responseJson);
        try {sendResponse(exchange, 200, responseJson);} catch (InterruptedException e) {throw new RuntimeException(e);}
        String originHeader = exchange.getRequestHeaders().getFirst("Origin");
        String nb_request = exchange.getRequestHeaders().getFirst("nbRequest");
        int int_nb_request = Integer.parseInt(nb_request)+1;
        if(int_nb_request < 7){try {sendRequestFire2(originHeader, int_nb_request);} catch (InterruptedException e) {throw new RuntimeException(e);}}
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

    public void sendRequestFire2(String Origin, int nb_request) throws IOException, InterruptedException {
        String[] tableau = {"B2", "C3", "A2", "D3", "D4", "D8", "A5", "E3"};
        Random random = new Random();
        int index = random.nextInt(tableau.length);
        String valeurTiree = tableau[index];
        System.out.println("fonction parametre :"+ Origin);
        String url = "http://localhost:"+Origin;
        System.out.println("url : "+url);
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url+"/api/game/fire?cell="+valeurTiree))
            .setHeader("Accept", "application/json")
            .setHeader("Origin", String.valueOf(this.port))
            .setHeader("nbRequest", String.valueOf(nb_request))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Requete envoyé to"+url+ "en GET: "+ "cell=B2");
        int statusCode = response.statusCode();
        String responseBody = response.body();
        System.out.println("Status code: " + statusCode);
        System.out.println("Response recu: " + responseBody);
    }

}




