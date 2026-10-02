package com.crosscheck.infrastructure.evidence;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PublicSourceCaptureTest {
    final URI url = URI.create("https://example.org/report");
    final Instant now = Instant.parse("2026-09-30T00:00:00Z");
    final String body = "A sufficiently long independent source document describing rental listings and their limitations. ".repeat(3);
    SourceCapture.Document html(String metadata) throws Exception {
        return PublicSourceCapture.parse(url, "text/html", ("<html><head><title>Source</title>" + metadata
                + "</head><body><main>" + body + "</main></body></html>").getBytes(StandardCharsets.UTF_8), now);
    }
    @Test void extractsOnlyFullEditorialPublicationDate() throws Exception {
        var result = html("<meta property='article:published_time' content='2024-03-15T10:00:00Z'>");
        assertEquals(LocalDate.of(2024, 3, 15), result.date().publishedAt());
        assertTrue(result.capture().content().contains(body.strip()));
        assertEquals(now, result.capture().retrievedAt());
    }
    @Test void partialConflictingOrUpdatedDatesRemainUnknown() throws Exception {
        for (var metadata : new String[]{"<meta property='article:published_time' content='2024-03'>",
                "<meta property='article:modified_time' content='2024-03-15'>",
                "<meta property='article:published_time' content='2024-03-15'><meta name='citation_publication_date' content='2024-03-16'>",
                "<meta property='article:published_time' content='2024-02-31'>"})
            assertNull(html(metadata).date().publishedAt());
    }
    @Test void rejectsPrivateAndSpecialDestinations() throws Exception {
        for (var value : new String[]{"127.0.0.1", "10.1.2.3", "172.16.0.1", "192.168.1.1", "169.254.169.254",
                "100.64.0.1", "198.18.0.1", "192.0.2.1", "203.0.113.1", "::1", "fe80::1", "fc00::1", "2001:db8::1", "2002:7f00:1::"})
            assertFalse(PublicSourceCapture.publicAddress(InetAddress.getByName(value)), value);
        assertTrue(PublicSourceCapture.publicAddress(InetAddress.getByName("8.8.8.8")));
    }
    @Test void rejectsUnsafeUrlsBeforeNetworkAccess() {
        for (var value : new String[]{"file:///tmp/source", "http://localhost/report", "http://127.0.0.1/",
                "https://example.org:8443/", "https://user:pass@example.org/", "https://example.org/#fragment"})
            assertThrows(IllegalArgumentException.class, () -> PublicSourceCapture.validateUri(URI.create(value)), value);
        PublicSourceCapture.validateUri(url);
    }
    @Test void stripsExecutableMarkupAndDoesNotUseCopyrightYearAsDate() throws Exception {
        var result = html("<script>window.secret='not evidence';</script>");
        assertFalse(result.capture().content().contains("window.secret"));
        assertNull(result.date().publishedAt());
    }
    @Test void unsupportedBinaryIsRejected() {
        assertThrows(java.io.IOException.class,
                () -> PublicSourceCapture.parse(url, "application/octet-stream", new byte[100], now));
    }
}
