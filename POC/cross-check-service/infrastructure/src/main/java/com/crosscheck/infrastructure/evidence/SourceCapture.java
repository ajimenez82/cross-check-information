package com.crosscheck.infrastructure.evidence;

import com.crosscheck.domain.analysis.evidence.EvidenceDossier;
import java.net.URI;

@FunctionalInterface
public interface SourceCapture {
    record Document(EvidenceDossier.Capture capture, EvidenceDossier.PublicationDate date, String title) {}
    Document read(URI uri);
}
