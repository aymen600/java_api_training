// Launcher.java
package fr.lernejo.navy_battle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.io.BufferedReader;

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

            // Traiter la requête et générer la réponse JSON
            String responseJson = processGameStartRequest(requestBody);

            // Envoyer la réponse avec le statut Accepted (202)
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
        private String processGameStartRequest(String requestBody) {
            // Implémenter la logique pour traiter la requête de démarrage du jeu
            // Générer et renvoyer la réponse JSON correspondante
            // Assurez-vous de respecter le schéma de réponse indiqué dans l'énoncé
            return "{\"id\": \"2aca7611-0ae4-49f3-bf63-75bef4769028\", \"url\": \"http://localhost:9876\", \"message\": \"May the best code win\"}";
        }
    }
}
