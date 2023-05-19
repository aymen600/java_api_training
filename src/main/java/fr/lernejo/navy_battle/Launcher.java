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
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.io.BufferedReader;
import java.util.concurrent.TimeUnit;

public class Launcher {
    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(args[0]);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        server.setExecutor(threadPoolExecutor);
        server.createContext("/ping", new PingHandler());
        server.createContext("/api/game/start", new ApiGameStartHandler());
        server.start();
        System.out.println("Server started on port " + port);
    }

    static class PingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Hello";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    static class ApiGameStartHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            //verifier que la méthode de la requête est POST. Si ce n'est pas le cas, renvoyer une réponse "Not Found" avec un statut HTTP 404.
            if (!exchange.getRequestMethod().equals("POST")) {
                sendResponse(exchange, 404, "Not Found");
                return;
            }
            // Récupérer et valider le corps de la requête JSON
            String requestBody = getRequestString(exchange);
            if (requestBody == null) {
                sendResponse(exchange, 400, "Bad Request");
                return;
            }
            // Traiter la requête et générer la réponse JSON et l'envoyer
            String responseJson = processGameStartRequest(requestBody);
            sendResponse(exchange, 202, responseJson);
        }

        //recupere et valide le corps de la requête JSON
        private String getRequestString(HttpExchange exchange) throws IOException {
            try (InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                 BufferedReader br = new BufferedReader(isr)) {
                StringBuilder requestBody = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    requestBody.append(line);
                }
                return requestBody.toString();
            }
        }

        //envoie réponse JSON
        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }

        //représente la logique de traitement de la requête. Dans cet exemple, on utilise une implémentation factice qui renvoie une réponse JSON fixe.
        private String processGameStartRequest(String requestBody) throws IOException {
            // Implémenter la logique pour traiter la requête de démarrage du jeu
            // Générer et renvoyer la réponse JSON correspondante
            // Assurez-vous de respecter le schéma de réponse indiqué dans l'énoncé
            // Consommer l'API tierce
            String apiUrl = "https://api.example.com/start";
            String apiResponse = sendPostRequest(apiUrl, requestBody);

            // Vérifier la réponse de l'API et générer la réponse JSON appropriée
            if (apiResponse != null && apiResponse.startsWith("SUCCESS")) {
                String gameId = apiResponse.substring(apiResponse.indexOf(':') + 1).trim();
                return "{\"id\": \"" + gameId + "\", \"url\": \"http://localhost:9876\", \"message\": \"Game started\"}";
            } else {
                return "{\"error\": \"Failed to start the game\"}";
            }
            //return "{\"id\": \"2aca7611-0ae4-49f3-bf63-75bef4769028\", \"url\": \"http://localhost:9876\", \"message\": \"May the best code win\"}";
        }

        private String sendPostRequest(String apiUrl, String requestBody) throws IOException {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] requestBodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(requestBodyBytes);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString();
                }
            } else {
                return null;
            }
        }

    }
}

