package com.crosscheck.domain.analysis;

import java.time.LocalDate;

public record PublicationPeriod(LocalDate from, LocalDate to) {
    public PublicationPeriod {
        from = ReportChecks.required(from, "period.from");
        to = ReportChecks.required(to, "period.to");
        ReportChecks.require(!from.isAfter(to), "El periodo está invertido");
    }
}
