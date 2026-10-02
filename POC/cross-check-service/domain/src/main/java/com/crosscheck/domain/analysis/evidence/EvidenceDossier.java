package com.crosscheck.domain.analysis.evidence;

import com.crosscheck.domain.analysis.PublicationTrace;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import static com.crosscheck.domain.analysis.evidence.EvidenceChecks.*;

/** Internal contract. Structural validation does not certify factual or interpretive accuracy. */
public record EvidenceDossier(int schemaVersion, List<String> anchorTexts, List<Target> targets,
                              List<Source> sources, List<Finding> findings) {
    public static final int VERSION = 1;
    public enum Access { CAPTURED, MODEL_EXCERPT_ONLY, INACCESSIBLE }
    public enum DatePrecision { DAY, MONTH, YEAR, UNKNOWN }
    public enum DateKind { PUBLICATION, ENACTMENT, UPDATE, UNKNOWN }
    public enum DateReview { DECLARED, CONFIRMED }
    public enum Method { DESCRIPTIVE, ASSOCIATION, CAUSAL_ESTIMATE, PREDICTION, ATTRIBUTED_ARGUMENT }

    /** Offsets use Java UTF-16 indexing and reference an exact supplied input/context string. */
    public record Target(String id, String proposition, int anchorIndex, int start, int end, String excerpt) {
        public Target {
            id = text(id, "target.id");
            proposition = text(proposition, "target.proposition");
            excerpt = text(excerpt, "target.excerpt");
            require(anchorIndex >= 0 && start >= 0 && end > start, "Invalid anchor offsets");
        }
    }

    /** Created by a trusted acquisition adapter, never deserialized from model output as a capture. */
    public record Capture(URI resolvedUrl, Instant retrievedAt, String documentVersion, String content) {
        public Capture {
            resolvedUrl = url(resolvedUrl);
            retrievedAt = required(retrievedAt, "capture.retrievedAt");
            documentVersion = text(documentVersion, "capture.documentVersion");
            content = text(content, "capture.content");
        }
        public String contentHash() {
            try {
                return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(content.getBytes(StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException impossible) {
                throw new IllegalStateException(impossible);
            }
        }
    }

    public record Fragment(int start, int end, String text, String locator) {
        public Fragment {
            text = EvidenceChecks.text(text, "fragment.text");
            locator = EvidenceChecks.text(locator, "fragment.locator");
            require(start >= 0 && end > start, "Invalid fragment offsets");
        }
        void validate(Capture capture) {
            required(capture, "fragment.capture");
            require(end <= capture.content().length()
                    && capture.content().substring(start, end).equals(text), "Fragment does not match captured content");
        }
    }

    /** CONFIRMED is a trusted review decision, not a claim the model may grant itself. */
    public record PublicationDate(LocalDate value, DatePrecision precision, DateKind kind,
                                  DateReview review, String label, Fragment evidence) {
        public PublicationDate {
            precision = required(precision, "date.precision");
            kind = required(kind, "date.kind");
            review = required(review, "date.review");
            label = text(label, "date.label");
            require((precision == DatePrecision.DAY) == (value != null), "Date precision conflicts with value");
            require(review != DateReview.CONFIRMED || evidence != null, "Confirmed date needs evidence");
        }
        public LocalDate publishedAt() {
            return review == DateReview.CONFIRMED && kind == DateKind.PUBLICATION
                    && precision == DatePrecision.DAY ? value : null;
        }
    }

    public record Source(String id, URI requestedUrl, String title, String publisher, List<String> authors,
                         Access access, Capture capture, PublicationDate publicationDate, String limitation) {
        public Source {
            id = text(id, "source.id");
            requestedUrl = url(requestedUrl);
            title = text(title, "source.title");
            if (publisher != null) text(publisher, "source.publisher");
            authors = list(authors, "source.authors");
            authors.forEach(author -> text(author, "source.author"));
            access = required(access, "source.access");
            publicationDate = required(publicationDate, "source.publicationDate");
            require((access == Access.CAPTURED) == (capture != null), "Capture/access mismatch");
            if (access != Access.CAPTURED) text(limitation, "source.limitation");
            else if (limitation != null) text(limitation, "source.limitation");
            if (publicationDate.evidence() != null) publicationDate.evidence().validate(capture);
        }
    }

    public record Finding(String id, String sourceId, String targetId, Fragment excerpt, String paraphrase,
                          PublicationTrace.Attribution attribution, Method method, PublicationTrace.Scope scope,
                          PublicationTrace.TemporalRelation temporalRelation, PublicationTrace.Relation relation) {
        public Finding {
            id = text(id, "finding.id");
            sourceId = text(sourceId, "finding.sourceId");
            targetId = text(targetId, "finding.targetId");
            excerpt = required(excerpt, "finding.excerpt");
            paraphrase = text(paraphrase, "finding.paraphrase");
            attribution = required(attribution, "finding.attribution");
            method = required(method, "finding.method");
            scope = required(scope, "finding.scope");
            temporalRelation = required(temporalRelation, "finding.temporalRelation");
            relation = required(relation, "finding.relation");
        }
    }

    public EvidenceDossier {
        require(schemaVersion == VERSION, "Unsupported evidence contract version");
        anchorTexts = list(anchorTexts, "anchorTexts");
        anchorTexts.forEach(value -> text(value, "anchorText"));
        require(!anchorTexts.isEmpty(), "Missing input/context");
        targets = list(targets, "targets");
        require(!targets.isEmpty() && targets.size() <= 8, "Invalid target count");
        sources = list(sources, "sources");
        findings = list(findings, "findings");
        var targetIds = new HashSet<String>();
        var excerpts = new HashSet<String>();
        var previousTargets = new ArrayList<Target>();
        for (var target : targets) {
            require(targetIds.add(target.id()), "Duplicate target ID");
            require(excerpts.add(target.excerpt()), "Duplicate input anchor");
            require(target.anchorIndex() < anchorTexts.size(), "Unknown anchor input");
            var input = anchorTexts.get(target.anchorIndex());
            require(target.end() <= input.length()
                    && input.substring(target.start(), target.end()).equals(target.excerpt()), "Non-literal input anchor");
            require(input.indexOf(target.excerpt()) == target.start()
                    && input.indexOf(target.excerpt(), target.start() + 1) < 0, "Ambiguous input anchor");
            for (var previous : previousTargets)
                require(previous.anchorIndex() != target.anchorIndex() || previous.end() <= target.start()
                        || target.end() <= previous.start(), "Overlapping input anchors");
            previousTargets.add(target);
        }
        var sourceMap = new HashMap<String, Source>();
        for (var source : sources) require(sourceMap.put(source.id(), source) == null, "Duplicate source ID");
        var findingIds = new HashSet<String>();
        for (var finding : findings) {
            require(findingIds.add(finding.id()), "Duplicate finding ID");
            require(targetIds.contains(finding.targetId()), "Unknown finding target");
            var source = sourceMap.get(finding.sourceId());
            require(source != null, "Unknown finding source");
            require(source.access() == Access.CAPTURED, "Finding requires independently captured content");
            finding.excerpt().validate(source.capture());
        }
    }
}
