package com.crosscheck.infrastructure.evidence;

import static com.crosscheck.domain.analysis.evidence.EvidenceDossier.*;
import java.net.*;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;

/** Bounded public-document retrieval. Credentials, cookies, proxies and automatic redirects are disabled. */
public final class PublicSourceCapture implements SourceCapture {
    static final int MAX_BYTES = 2_000_000;
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        var thread = new Thread(r, "source-capture-timeout"); thread.setDaemon(true); return thread;
    });
    private final Clock clock;
    public PublicSourceCapture(Clock clock) { this.clock = clock; }

    public static String normalize(String text) {
        return text.replace('\u00a0', ' ').replaceAll("\\s+", " ").strip();
    }

    static boolean publicAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) return false;
        var bytes = address.getAddress();
        if (bytes.length == 4) {
            int a = Byte.toUnsignedInt(bytes[0]), b = Byte.toUnsignedInt(bytes[1]), c = Byte.toUnsignedInt(bytes[2]);
            return a != 0 && a != 10 && a != 127 && a < 224 && !(a == 100 && b >= 64 && b <= 127)
                    && !(a == 169 && b == 254) && !(a == 172 && b >= 16 && b <= 31)
                    && !(a == 192 && (b == 168 || b == 0 || b == 2))
                    && !(a == 198 && (b == 18 || b == 19 || b == 51 && c == 100))
                    && !(a == 203 && b == 0 && c == 113);
        }
        // Restrict IPv6 to global unicast, excluding transition, documentation and special-purpose ranges.
        return (bytes[0] & 0xe0) == 0x20 && !(bytes[0] == 0x20 && bytes[1] == 0x02)
                && !(bytes[0] == 0x20 && bytes[1] == 0x01
                && (Byte.toUnsignedInt(bytes[2]) < 2 || bytes[2] == 0x0d && bytes[3] == (byte) 0xb8));
    }

    static final class PublicDns implements DnsResolver {
        public InetAddress[] resolve(String host) throws UnknownHostException {
            var addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0 || Arrays.stream(addresses).anyMatch(value -> !publicAddress(value)))
                throw new UnknownHostException("Non-public source destination");
            return addresses;
        }
        public String resolveCanonicalHostname(String host) { return host; }
    }

    static void validateUri(URI uri) {
        if (uri == null || !Set.of("https", "http").contains(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getFragment() != null || uri.toString().length() > 2048
                || uri.getPort() != -1 && uri.getPort() != (uri.getScheme().equals("https") ? 443 : 80))
            throw new IllegalArgumentException("Invalid public source URL");
        String host = uri.getHost().replace("[", "").replace("]", "");
        if (host.equalsIgnoreCase("localhost") || host.endsWith(".localhost") || !host.contains("."))
            throw new IllegalArgumentException("Non-public source host");
        if (host.matches("[0-9.]+") || host.contains(":")) {
            try { if (!publicAddress(InetAddress.getByName(host))) throw new IllegalArgumentException("Non-public source address"); }
            catch (UnknownHostException invalid) { throw new IllegalArgumentException("Invalid source address"); }
        }
    }

    private record Download(int status, String location, String contentType, byte[] bytes) {}

    @Override public Document read(URI initial) {
        long deadline = System.nanoTime() + Duration.ofSeconds(12).toNanos();
        var manager = PoolingHttpClientConnectionManagerBuilder.create().setDnsResolver(new PublicDns())
                .setDefaultConnectionConfig(ConnectionConfig.custom().setConnectTimeout(Timeout.ofSeconds(3))
                        .setSocketTimeout(Timeout.ofSeconds(4)).build()).build();
        try (var client = HttpClients.custom().setConnectionManager(manager).disableRedirectHandling()
                .disableAutomaticRetries().disableCookieManagement().disableContentCompression()
                .setDefaultRequestConfig(RequestConfig.custom().setResponseTimeout(Timeout.ofSeconds(4)).build()).build()) {
            URI uri = initial;
            for (int redirects = 0; redirects <= 3; redirects++) {
                validateUri(uri);
                var request = new HttpGet(uri);
                request.setHeader("User-Agent", "CrossCheck/0.1 public-source-review");
                request.setHeader("Accept", "text/html,application/pdf,text/plain");
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) throw new IOException("Capture deadline");
                var timeout = TIMER.schedule(request::cancel, remaining, TimeUnit.NANOSECONDS);
                Download download;
                try {
                    download = client.execute(request, response -> {
                        int status = response.getCode();
                        if (status >= 300 && status < 400) return new Download(status,
                                response.getFirstHeader("Location") == null ? null : response.getFirstHeader("Location").getValue(), "", new byte[0]);
                        if (status != 200 || response.getEntity() == null) throw new IOException("Source unavailable");
                        if (response.getEntity().getContentLength() > MAX_BYTES) throw new IOException("Source too large");
                        try (var input = response.getEntity().getContent()) {
                            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
                            if (bytes.length > MAX_BYTES) throw new IOException("Source too large");
                            return new Download(status, null, Objects.toString(response.getEntity().getContentType(), ""), bytes);
                        }
                    });
                } finally { timeout.cancel(false); }
                if (download.status() >= 300 && download.status() < 400) {
                    if (download.location() == null) throw new IOException("Missing redirect target");
                    var next = uri.resolve(download.location());
                    if (uri.getScheme().equals("https") && !next.getScheme().equals("https")) throw new IOException("Insecure redirect");
                    uri = next; continue;
                }
                if (System.nanoTime() >= deadline) throw new IOException("Capture deadline");
                return parse(uri, download.contentType(), download.bytes(), clock.instant());
            }
            throw new IOException("Too many redirects");
        } catch (IOException invalid) { throw new IllegalArgumentException("Source content could not be independently captured"); }
    }

    static Document parse(URI uri, String contentType, byte[] bytes, Instant retrievedAt) throws IOException {
        String title = null, content;
        var dates = new LinkedHashSet<LocalDate>();
        if (contentType.toLowerCase(Locale.ROOT).contains("pdf")) {
            try (var pdf = Loader.loadPDF(bytes)) {
                if (pdf.isEncrypted() || pdf.getNumberOfPages() > 120) throw new IOException("Unsupported PDF");
                var text = new StringBuilder();
                long parseDeadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
                new PDFTextStripper().writeText(pdf, new Writer() {
                    public void write(char[] buffer, int offset, int length) throws IOException {
                        if (text.length() + length > 300_000 || System.nanoTime() >= parseDeadline) throw new IOException("PDF text budget exceeded");
                        text.append(buffer, offset, length);
                    }
                    public void flush() {}
                    public void close() {}
                });
                content = normalize(text.toString());
                title = pdf.getDocumentInformation().getTitle();
            }
        } else if (contentType.toLowerCase(Locale.ROOT).contains("html")) {
            var html = Jsoup.parse(new ByteArrayInputStream(bytes), null, uri.toString());
            title = html.title();
            for (var meta : html.select("meta[property=article:published_time], meta[name=citation_publication_date], meta[name=DC.date.issued]")) {
                String value = meta.attr("content").strip();
                if (Pattern.matches("\\d{4}-\\d{2}-\\d{2}(?:T.*)?", value)) {
                    try { dates.add(LocalDate.parse(value.substring(0, 10))); } catch (RuntimeException ignored) { }
                }
            }
            html.select("script,style,noscript,nav,footer,header").remove();
            content = normalize(html.body().text());
        } else if (contentType.toLowerCase(Locale.ROOT).startsWith("text/plain")) {
            content = normalize(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        } else throw new IOException("Unsupported document format");
        if (content.length() < 80 || content.length() > 300_000) throw new IOException("Unsupported document size");
        var date = new PublicationDate(null, DatePrecision.UNKNOWN, DateKind.UNKNOWN, DateReview.DECLARED,
                dates.size() > 1 ? "Metadatos de publicación contradictorios" : "Fecha exacta no verificada", null);
        if (dates.size() == 1) {
            var value = dates.iterator().next();
            String metadata = "publicationDate=" + value;
            content = metadata + "\n" + content;
            date = new PublicationDate(value, DatePrecision.DAY, DateKind.PUBLICATION, DateReview.CONFIRMED,
                    value.toString(), new Fragment(0, metadata.length(), metadata, "Metadatos editoriales del HTML capturado"));
        }
        return new Document(new Capture(uri, retrievedAt, "captured-text-v1", content), date,
                title == null || title.isBlank() ? null : title);
    }
}
