package com.crosscheck.application.features.analysis.common.dto;

import java.time.Instant;
import java.time.LocalDate;

public record SourceResult(String id, String title, String url, String publisher,
                           LocalDate publishedAt, Instant consultedAt, String contribution, String type) {}
