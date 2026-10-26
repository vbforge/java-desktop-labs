package com.vbforge.animalpedia.engine;

import com.sun.net.httpserver.HttpServer;
import com.vbforge.animalpedia.model.Animal;
import com.vbforge.animalpedia.model.DataSource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Parsing is tested with recorded JSON fixtures; HTTP details with a tiny local server. No internet needed. */
class WikipediaClientTest {

    private static String fixture(String name) throws IOException {
        try (InputStream in = WikipediaClientTest.class.getResourceAsStream("/fixtures/wikipedia/" + name)) {
            assertNotNull(in, "missing fixture " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // ---- parsing ------------------------------------------------------------------------------

    @Test
    void parsesALionSummary() throws Exception {
        Animal a = WikipediaClient.parseSummary(fixture("summary-lion.json"));
        assertEquals("Lion", a.commonName());
        assertEquals("lion", a.slug());
        assertEquals("Large cat of the genus Panthera", a.summary());
        assertEquals(DataSource.WIKIPEDIA, a.source());
        assertEquals("https://en.wikipedia.org/wiki/Lion", a.url());
        assertTrue(a.paragraphs().size() >= 1);
        assertTrue(a.paragraphs().get(0).startsWith("The lion (Panthera leo)"));
        assertNull(a.taxonomy());
        assertNull(a.status());
    }

    @Test
    void rejectsDisambiguationPagesWithAFriendlyMessage() throws Exception {
        String json = fixture("summary-disambiguation.json");
        LookupException e = assertThrows(LookupException.class, () -> WikipediaClient.parseSummary(json));
        assertEquals(LookupException.Kind.UNUSABLE, e.kind());
        assertTrue(e.getMessage().contains("more specific"));
    }

    @Test
    void rejectsPagesWithoutText() throws Exception {
        String json = fixture("summary-no-extract.json");
        LookupException e = assertThrows(LookupException.class, () -> WikipediaClient.parseSummary(json));
        assertEquals(LookupException.Kind.UNUSABLE, e.kind());
    }

    @Test
    void handlesUnicodeAndApostrophesAndKeepsParagraphs() throws Exception {
        Animal a = WikipediaClient.parseSummary(fixture("summary-unicode.json"));
        assertEquals("Père David's deer", a.commonName());
        assertEquals("père-david-s-deer", a.slug());
        assertEquals(2, a.paragraphs().size());
    }

    @Test
    void garbageIsReportedNotThrownRaw() {
        assertThrows(LookupException.class, () -> WikipediaClient.parseSummary("<html>nope</html>"));
        assertThrows(LookupException.class, () -> WikipediaClient.parseSummary("[1,2,3]"));
    }

    @Test
    void parsesOpenSearchTitles() throws Exception {
        List<String> titles = WikipediaClient.parseSearch(fixture("opensearch-monkey.json"));
        assertEquals(List.of("Monkey", "Monkey King", "Monkeypox", "Monkey Island"), titles);
    }

    @Test
    void unexpectedSearchShapeGivesNoResults() throws Exception {
        assertTrue(WikipediaClient.parseSearch("[]").isEmpty());
        assertTrue(WikipediaClient.parseSearch("{\"a\":1}").isEmpty());
    }

    // ---- HTTP against a local server ----------------------------------------------------------

    private static HttpServer server(int status, String body, AtomicReference<String> seenUa, AtomicReference<String> seenUri)
            throws IOException {
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext("/", ex -> {
            seenUa.set(ex.getRequestHeaders().getFirst("User-Agent"));
            seenUri.set(ex.getRequestURI().getRawPath() + (ex.getRequestURI().getRawQuery() == null ? "" : "?" + ex.getRequestURI().getRawQuery()));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            ex.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) ex.getResponseBody().write(bytes);
            ex.close();
        });
        s.start();
        return s;
    }

    private static WikipediaClient clientFor(HttpServer s) {
        return new WikipediaClient(HttpClient.newHttpClient(), "http://127.0.0.1:" + s.getAddress().getPort());
    }

    @Test
    void summaryRequestIsEncodedAndIdentifiesTheApp() throws Exception {
        AtomicReference<String> ua = new AtomicReference<>();
        AtomicReference<String> uri = new AtomicReference<>();
        HttpServer s = server(200, fixture("summary-unicode.json"), ua, uri);
        try {
            Animal a = clientFor(s).summary("Père David's deer");
            assertEquals("Père David's deer", a.commonName());
            assertTrue(ua.get().startsWith("AnimalPedia/1.0"), ua.get());
            assertEquals("/api/rest_v1/page/summary/P%C3%A8re_David%27s_deer", uri.get());
        } finally {
            s.stop(0);
        }
    }

    @Test
    void searchRequestCarriesTheQuery() throws Exception {
        AtomicReference<String> ua = new AtomicReference<>();
        AtomicReference<String> uri = new AtomicReference<>();
        HttpServer s = server(200, fixture("opensearch-monkey.json"), ua, uri);
        try {
            List<String> titles = clientFor(s).search("red panda & friends");
            assertEquals(4, titles.size());
            assertTrue(uri.get().startsWith("/w/api.php?"), uri.get());
            assertTrue(uri.get().contains("action=opensearch"), uri.get());
            assertTrue(uri.get().contains("search=red%20panda%20%26%20friends"), uri.get());
        } finally {
            s.stop(0);
        }
    }

    @Test
    void notFoundBecomesAFriendlyNotFound() throws Exception {
        HttpServer s = server(404, "{}", new AtomicReference<>(), new AtomicReference<>());
        try {
            LookupException e = assertThrows(LookupException.class, () -> clientFor(s).summary("Nope"));
            assertEquals(LookupException.Kind.NOT_FOUND, e.kind());
        } finally {
            s.stop(0);
        }
    }

    @Test
    void serverErrorsBecomeServerProblems() throws Exception {
        HttpServer s = server(503, "{}", new AtomicReference<>(), new AtomicReference<>());
        try {
            LookupException e = assertThrows(LookupException.class, () -> clientFor(s).search("lion"));
            assertEquals(LookupException.Kind.SERVER, e.kind());
            assertTrue(e.getMessage().contains("503"));
        } finally {
            s.stop(0);
        }
    }

    @Test
    void unreachableServerMeansOffline() throws Exception {
        HttpServer s = server(200, "{}", new AtomicReference<>(), new AtomicReference<>());
        WikipediaClient client = clientFor(s);
        s.stop(0); // nothing is listening on that port any more
        LookupException e = assertThrows(LookupException.class, () -> client.summary("Lion"));
        assertEquals(LookupException.Kind.OFFLINE, e.kind());
    }
}
