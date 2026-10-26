package com.vbforge.animalpedia.engine;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vbforge.animalpedia.engine.LookupException.Kind;
import com.vbforge.animalpedia.model.Animal;
import com.vbforge.animalpedia.model.DataSource;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Talks to the Wikipedia REST/Action APIs. Parsing is separate from HTTP so it can be tested with fixtures. */
public class WikipediaClient implements WikiSource {

    public static final String DEFAULT_BASE = "https://en.wikipedia.org";
    static final String USER_AGENT = "AnimalPedia/1.0 (educational desktop app)";

    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final HttpClient http;
    private final String baseUrl;

    public WikipediaClient() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build(), DEFAULT_BASE);
    }

    public WikipediaClient(HttpClient http, String baseUrl) {
        this.http = http;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public List<String> search(String query) throws LookupException {
        String url = baseUrl + "/w/api.php?action=opensearch&limit=8&namespace=0&format=json&search=" + encode(query);
        return parseSearch(get(url));
    }

    @Override
    public Animal summary(String title) throws LookupException {
        String url = baseUrl + "/api/rest_v1/page/summary/" + encode(title.replace(' ', '_'));
        return parseSummary(get(url));
    }

    // ---- HTTP ---------------------------------------------------------------------------------

    private String get(String url) throws LookupException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status == 404) {
                throw new LookupException(Kind.NOT_FOUND, "Hmm, I couldn't find that animal. Try a different name!");
            }
            if (status != 200) {
                throw new LookupException(Kind.SERVER, "Wikipedia is having a hiccup right now (code " + status + "). Try again in a bit.");
            }
            return response.body();
        } catch (IOException e) {
            throw new LookupException(Kind.OFFLINE,
                    "I can't reach the internet right now. The built-in animals still work!", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LookupException(Kind.OFFLINE, "The lookup was interrupted.", e);
        }
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    // ---- Parsing ------------------------------------------------------------------------------

    /** opensearch returns {@code [query, [titles], [descriptions], [urls]]}. */
    static List<String> parseSearch(String json) throws LookupException {
        try {
            JsonNode root = JSON.readTree(json);
            List<String> titles = new ArrayList<>();
            if (root != null && root.isArray() && root.size() > 1 && root.get(1).isArray()) {
                for (JsonNode t : root.get(1)) {
                    if (t.isTextual() && !t.asText().isBlank()) titles.add(t.asText());
                }
            }
            return titles;
        } catch (IOException e) {
            throw new LookupException(Kind.SERVER, "Wikipedia sent something I couldn't read.", e);
        }
    }

    static Animal parseSummary(String json) throws LookupException {
        JsonNode root;
        try {
            root = JSON.readTree(json);
        } catch (IOException e) {
            throw new LookupException(Kind.SERVER, "Wikipedia sent something I couldn't read.", e);
        }
        if (root == null || !root.isObject()) {
            throw new LookupException(Kind.SERVER, "Wikipedia sent something I couldn't read.");
        }

        if ("disambiguation".equals(text(root, "type"))) {
            throw new LookupException(Kind.UNUSABLE,
                    "That word can mean many things! Try a more specific animal name.");
        }
        String title = text(root, "title");
        String extract = text(root, "extract");
        if (title == null || extract == null) {
            throw new LookupException(Kind.UNUSABLE, "There's no story for that one yet. Try another animal!");
        }

        String pageUrl = root.path("content_urls").path("desktop").path("page").asText(null);
        return new Animal(
                Animal.slugOf(title), title, null, null, null, null, null, null, null, null, null,
                text(root, "description"),
                ArticleFormatter.paragraphs(extract),
                null, DataSource.WIKIPEDIA, pageUrl);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull() || !v.isTextual()) return null;
        String s = v.asText().trim();
        return s.isEmpty() ? null : s;
    }
}
