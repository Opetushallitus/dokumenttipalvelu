package fi.vm.sade.valinta.dokumenttipalvelu.dto;

import java.io.InputStream;
import java.time.Instant;
import java.util.Collection;

public class ObjectEntity extends ObjectHead {
  public final InputStream entity;

  public ObjectEntity(
      final InputStream entity,
      final String documentId,
      final String fileName,
      final String contentType,
      final Long contentLength,
      Collection<String> tags,
      Instant expires) {
    super(documentId, fileName, contentType, contentLength, tags, expires);
    this.entity = entity;
  }
}
