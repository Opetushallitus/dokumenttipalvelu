package fi.vm.sade.valinta.dokumenttipalvelu.dto;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;

public class ObjectMetadata {
  public final String key;

  public final String documentId;

  public final Instant lastModified;

  // viestintapalvelu, valintalaskentakoostepalvelu, hyvaksymiskirje, jalkiohjauskirje, ...
  public final Collection<String> tags;

  public final Long contentLength;

  public final String eTag;

  public ObjectMetadata(
      final String key,
      final String documentId,
      final Collection<String> tags,
      final Instant lastModified,
      final Long contentLength,
      final String eTag) {
    this.key = key;
    this.documentId = documentId;
    this.tags = tags;
    this.lastModified = lastModified;
    this.contentLength = contentLength;
    this.eTag = eTag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ObjectMetadata that = (ObjectMetadata) o;
    return Objects.equals(key, that.key)
        && Objects.equals(documentId, that.documentId)
        && Objects.equals(lastModified, that.lastModified)
        && Objects.equals(tags, that.tags)
        && Objects.equals(contentLength, that.contentLength)
        && Objects.equals(eTag, that.eTag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, documentId, lastModified, tags, contentLength, eTag);
  }

  @Override
  public String toString() {
    return "ObjectMetadata{"
        + "key='"
        + key
        + '\''
        + ", documentId='"
        + documentId
        + '\''
        + ", lastModified="
        + lastModified
        + ", tags="
        + tags
        + ", contentLength="
        + contentLength
        + ", eTag='"
        + eTag
        + '\''
        + '}';
  }
}
