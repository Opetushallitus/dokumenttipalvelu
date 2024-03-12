package fi.vm.sade.valinta.dokumenttipalvelu;

import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectMetadata;
import java.io.InputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.*;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.utils.StringUtils;

public class SiirtotiedostoPalvelu extends Dokumenttipalvelu {
  private static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("Europe/Helsinki");

  final DateTimeFormatter dateTimeFormatter =
      new DateTimeFormatterBuilder()
          .appendPattern("[yyyy-MM-dd'T'HH:mm:ss]")
          .appendPattern("[dd.MM.yyyy HH:mm]")
          .appendPattern("[yyyy-MM-dd HH:mm:ss.SSSSSS XXX]")
          .toFormatter()
          .withZone(DEFAULT_TIMEZONE);

  public SiirtotiedostoPalvelu(String awsRegion, String bucketName) {
    super(awsRegion, bucketName);
  }

  private String timeStamp() {
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ssXX");
    return formatter.format(ZonedDateTime.now(DEFAULT_TIMEZONE));
  }

  private Collection<String> tags(String sourceSystem, String subCategory) {
    return StringUtils.isEmpty(subCategory)
        ? Arrays.asList(sourceSystem)
        : Arrays.asList(sourceSystem, subCategory);
  }

  @Override
  String composeKey(final Collection<String> tags, final String documentId) {
    return String.format("%s/%s", tags.stream().findFirst().orElse("unknown"), documentId);
  }

  private boolean retryable(RuntimeException exp) {
    Throwable checkedException = exp.getCause() != null ? exp.getCause() : exp;
    if (checkedException instanceof SdkException) {
      SdkException sdkException = (SdkException) checkedException;
      return sdkException.retryable();
    }
    return false;
  }

  /**
   * Saves siirtotiedosto document.
   *
   * @param sourceSystem Source system of this siirtotiedosto, e.g. kouta, ataru, etc. Value is
   *     mandatory.
   * @param subCategory More specific description of the contents, e.g. haku, hakukohde, application
   *     etc. Value is mandatory.
   * @param additionalInfo Additional info to be shown in filename. Value is optional.
   * @param data Document's data input stream. Value is mandatory.
   * @param retryCount Number of retries performed in case save operation failed, and failure was
   *     caused by some temporary / recoverable error
   * @return Metadata describing the document.
   */
  public ObjectMetadata saveSiirtotiedosto(
      final String sourceSystem,
      final String subCategory,
      final String additionalInfo,
      final InputStream data,
      final int retryCount) {

    if (StringUtils.isEmpty(sourceSystem)) {
      throw new IllegalArgumentException("Source system cannot be empty");
    }
    if (StringUtils.isEmpty(subCategory)) {
      throw new IllegalArgumentException("Subcategory cannot be empty");
    }
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null");
    }
    final String addInfoStr =
        StringUtils.isEmpty(additionalInfo) ? "" : String.format("_%s", additionalInfo);
    final String timestamp = timeStamp();
    final String documentId =
        String.format(
            "%s_%s__%s_%s_%s.json",
            sourceSystem, subCategory, timestamp, addInfoStr, UUID.randomUUID());
    final Collection<String> tags = tags(sourceSystem, subCategory);
    // TODO Poista expirationDate siinä vaiheessa kun se poistuu save -metodista
    final Date expirationDate = Date.from(Instant.now().plus(Duration.of(5, ChronoUnit.DAYS)));
    return saveWithRetry(documentId, expirationDate, tags, data, retryCount);
  }

  private ObjectMetadata saveWithRetry(
      String documentId,
      Date expirationDate,
      Collection<String> tags,
      InputStream data,
      int retryCount) {
    int retryNumber = 0;
    PutObjectResponse saveResponse = null;
    ObjectMetadata metadata = null;
    String key = composeKey(tags, documentId);

    while (retryNumber++ <= retryCount) {
      try {
        if (saveResponse == null) {
          saveResponse = putObject(key, documentId, expirationDate, "json", data).join();
          retryNumber = 1;
        }
        metadata = getObjectAttributesASync(documentId, key).join();
        break;
      } catch (RuntimeException exp) {
        if (!retryable(exp) || retryNumber > retryCount) {
          throw exp;
        }
      }
    }
    return metadata;
  }
}
