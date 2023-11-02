package fi.vm.sade.valinta.dokumenttipalvelu.dto;

import java.time.Instant;
import java.util.Collection;

public class ObjectHead {
  public final String documentId;
  public final String fileName;
  public final String contentType;
  public final Long contentLength;
  public final Collection<String> tags;
  public final Instant expires;

  public ObjectHead(
      final String documentId,
      final String fileName,
      final String contentType,
      final Long contentLength,
      Collection<String> tags,
      Instant expires) {
    this.fileName = fileName;
    this.contentType = contentType;
    this.documentId = documentId;
    this.contentLength = contentLength;
    this.tags = tags;
    this.expires = expires;
  }
}
