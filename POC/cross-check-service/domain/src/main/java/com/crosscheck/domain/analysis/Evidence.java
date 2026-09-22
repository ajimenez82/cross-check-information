package com.crosscheck.domain.analysis;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;

public record Evidence(String id, String title, URI url, String publisher,
                       LocalDate publishedAt, Instant consultedAt, String contribution, String type) {
    public Evidence {
        id = ReportChecks.text(id, "source.id");
        title = ReportChecks.text(title, "source.title");
        url = ReportChecks.url(url);
        publisher = ReportChecks.optionalText(publisher, "source.publisher");
        consultedAt = ReportChecks.required(consultedAt, "source.consultedAt");
        contribution = ReportChecks.text(contribution, "source.contribution");
        type = ReportChecks.optionalText(type, "source.type");
    }
}
