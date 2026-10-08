package com.campusconnect.events;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.campusconnect.events.model.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public final class CampusConnectApplication {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String EVENTS_PATH = "/api/events";

    private CampusConnectApplication() {}

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(EVENTS_PATH, CampusConnectApplication::handle);
        server.start();
        System.out.println("CampusConnect API listening on port " + port);
    }

    private static void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            try {
                addCorsHeaders(exchange);
                if (exchange.getRequestMethod().equals("OPTIONS")) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                String path = exchange.getRequestURI().getPath();
                String method = exchange.getRequestMethod();
                if (path.equals(EVENTS_PATH) || path.equals(EVENTS_PATH + "/")) {
                    switch (method) {
                        case "GET" -> sendJson(exchange, 200, findAll());
                        case "POST" -> sendJson(exchange, 201, create(readEvent(exchange)));
                        default -> throw new ApiException(405, "Method not allowed.");
                    }
                    return;
                }

                if (!path.startsWith(EVENTS_PATH + "/")) {
                    throw new ApiException(404, "Endpoint not found.");
                }
                String idText = path.substring((EVENTS_PATH + "/").length());
                if (idText.isBlank() || idText.contains("/")) throw new ApiException(404, "Event not found.");
                long id;
                try {
                    id = Long.parseLong(idText);
                } catch (NumberFormatException exception) {
                    throw new ApiException(400, "Event ID must be a number.");
                }

                switch (method) {
                    case "GET" -> sendJson(exchange, 200, findById(id));
                    case "PUT" -> sendJson(exchange, 200, update(id, readEvent(exchange)));
                    case "DELETE" -> {
                        delete(id);
                        exchange.sendResponseHeaders(204, -1);
                    }
                    default -> throw new ApiException(405, "Method not allowed.");
                }
            } catch (ApiException exception) {
                sendJson(exchange, exception.status, Map.of("message", exception.getMessage()));
            } catch (IOException | SQLException | RuntimeException exception) {
                sendJson(exchange, 500, Map.of("message", "Something went wrong on the server. Please try again."));
            }
        }
    }

    private static List<Event> findAll() throws SQLException {
        List<Event> events = new ArrayList<>();
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, title, description, category, venue, start_time FROM events ORDER BY start_time ASC");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) events.add(readRow(rows));
        }
        return events;
    }

    private static Event findById(long id) throws SQLException {
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, title, description, category, venue, start_time FROM events WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) return readRow(rows);
            }
        }
        throw new ApiException(404, "Event not found.");
    }

    private static Event create(Event event) throws SQLException {
        validate(event);
        String sql = "INSERT INTO events (title, description, category, venue, start_time) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setEventFields(statement, event);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) event.setId(keys.getLong(1));
            }
        }
        return event;
    }

    private static Event update(long id, Event event) throws SQLException {
        validate(event);
        String sql = "UPDATE events SET title = ?, description = ?, category = ?, venue = ?, start_time = ? WHERE id = ?";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            setEventFields(statement, event);
            statement.setLong(6, id);
            if (statement.executeUpdate() == 0) throw new ApiException(404, "Event not found.");
        }
        event.setId(id);
        return event;
    }

    private static void delete(long id) throws SQLException {
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM events WHERE id = ?")) {
            statement.setLong(1, id);
            if (statement.executeUpdate() == 0) throw new ApiException(404, "Event not found.");
        }
    }

    private static Event readRow(ResultSet row) throws SQLException {
        Event event = new Event();
        event.setId(row.getLong("id"));
        event.setTitle(row.getString("title"));
        event.setDescription(row.getString("description"));
        event.setCategory(row.getString("category"));
        event.setVenue(row.getString("venue"));
        event.setStartTime(row.getObject("start_time", LocalDateTime.class).toString());
        return event;
    }

    private static void setEventFields(PreparedStatement statement, Event event) throws SQLException {
        statement.setString(1, event.getTitle());
        statement.setString(2, event.getDescription());
        statement.setString(3, event.getCategory());
        statement.setString(4, event.getVenue());
        statement.setObject(5, LocalDateTime.parse(event.getStartTime()));
    }

    private static Event readEvent(HttpExchange exchange) throws IOException {
        try {
            return JSON.readValue(exchange.getRequestBody(), Event.class);
        } catch (IOException exception) {
            throw new ApiException(400, "Please send valid JSON with a valid event date and time.");
        }
    }

    private static void validate(Event event) {
        if (event == null || blank(event.getTitle()) || blank(event.getCategory())
                || blank(event.getVenue()) || blank(event.getStartTime())) {
            throw new ApiException(400, "Title, category, venue, and date/time are required.");
        }
        if (event.getTitle().length() > 120 || event.getVenue().length() > 120
                || event.getCategory().length() > 40
                || (event.getDescription() != null && event.getDescription().length() > 1000)) {
            throw new ApiException(400, "One of the fields is longer than the allowed limit.");
        }
        try {
            if (!LocalDateTime.parse(event.getStartTime()).isAfter(LocalDateTime.now())) {
                throw new ApiException(400, "Event date and time must be in the future.");
            }
        } catch (java.time.format.DateTimeParseException exception) {
            throw new ApiException(400, "Please provide a valid event date and time.");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static Connection connect() throws SQLException {
        String url = System.getenv().getOrDefault("DB_URL",
                "jdbc:mysql://localhost:3306/campus_connect?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        String user = System.getenv().getOrDefault("DB_USERNAME", "campus_user");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "campus_pass");
        return DriverManager.getConnection(url, user, password);
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if ("http://localhost:5173".equals(origin) || "http://127.0.0.1:5173".equals(origin)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
            exchange.getResponseHeaders().set("Vary", "Origin");
        }
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendJson(HttpExchange exchange, int status, Object value) throws IOException {
        byte[] body = JSON.writeValueAsString(value).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }

    private static final class ApiException extends RuntimeException {
        private final int status;

        private ApiException(int status, String message) {
            super(message);
            this.status = status;
        }
    }
}
