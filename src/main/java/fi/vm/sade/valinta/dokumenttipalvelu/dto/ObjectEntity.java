package fi.vm.sade.valinta.dokumenttipalvelu.dto;

import java.io.InputStream;
import java.time.Instant;
import java.util.Collection;

public class ObjectEntity {
  public final InputStream entity;
  public final String documentId;
  public final String fileName;
  public final String contentType;
  public final Long contentLength;
  public final Collection<String> tags;
  public final Instant expires;

  public ObjectEntity(
      final InputStream entity,
      final String documentId,
      final String fileName,
      final String contentType,
      final Long contentLength,
      Collection<String> tags,
      Instant expires) {
    this.entity = entity;
    this.fileName = fileName;
    this.contentType = contentType;
    this.documentId = documentId;
    this.contentLength = contentLength;
    this.tags = tags;
    this.expires = expires;
  }
}
