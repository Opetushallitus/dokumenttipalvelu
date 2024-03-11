package fi.vm.sade.valinta.dokumenttipalvelu;

import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectMetadata;
import java.io.InputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.*;
import software.amazon.awssdk.utils.StringUtils;

public class SiirtotiedostoPalvelu extends Dokumenttipalvelu {
  private final static ZoneId DEFAULT_TIMEZONE = ZoneId.of("Europe/Helsinki");

  final DateTimeFormatter dateTimeFormatter =
      new DateTimeFormatterBuilder()
          .appendPattern("[yyyy-MM-dd'T'HH:mm:ss]")
          .appendPattern("[dd.MM.yyyy HH:mm]")
          .appendPattern("[yyyy-MM-dd HH:mm:ss.SSSSSS XXX]")
          .toFormatter().withZone(DEFAULT_TIMEZONE);

  public SiirtotiedostoPalvelu(String awsRegion, String bucketName) {
    super(awsRegion, bucketName);
  }

  private String timeStamp(
          final Optional<ZonedDateTime> timeStampVal) {
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy_HH.mm.ssXXX");
    return timeStampVal.map(formatter::format).orElseGet(() -> formatter.format(ZonedDateTime.now(DEFAULT_TIMEZONE)));
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

  /**
   * Saves siirtotiedosto document.
   *
   * @param timestamp Point of time when data contained by this siirtotiedosto is obtained,
   *     allowed formats: '2024-01-24T10:15:30', '24.01.2024 10:15', '2024-01-24 10:15:20.123456 +00:00'.
   *     Value is optional, can be null or empty. If null or empty, timestamp is set to current time.
   * @param sourceSystem Source system of this siirtotiedosto, e.g. kouta, ataru, etc. Value is
   *     mandatory.
   * @param subCategory More specific description of the contents, e.g. haku, hakukohde, application etc. Value is
   *     mandatory.
   * @param additionalInfo Additional info to be shown in filename. Value is optional.
   * @param data Document's data input stream. Value is mandatory.
   * @param retryCount Number of retries performed in case save operation failed, and failure was
   *     caused by some temporary / recoverable error
   * @return Metadata describing the document.
   */
  public ObjectMetadata saveSiirtotiedosto(
      final String timestamp,
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
    Optional<ZonedDateTime> timestampVal =
        StringUtils.isEmpty(timestamp)
            ? Optional.empty()
            : Optional.of(ZonedDateTime.parse(timestamp, dateTimeFormatter));
    //System.out.println("!!!!!!!!!!!!!!!!!!!!!! " + timestampVal.get().getZone());
    final String addInfoStr = StringUtils.isEmpty(additionalInfo) ? "" : String.format("_%s", additionalInfo);
    final String documentId =
        String.format(
            "%s-%s__%s_%s_%s.json",
            sourceSystem, subCategory, timeStamp(timestampVal), addInfoStr, UUID.randomUUID());
    final Collection<String> tags = tags(sourceSystem, subCategory);
    // TODO Poista expirationDate siinä vaiheessa kun se poistuu save -metodista
    final Date expirationDate = Date.from(Instant.now().plus(Duration.of(5, ChronoUnit.DAYS)));
    return save(documentId, documentId, expirationDate, tags, "json", data, retryCount);
  }

  public ObjectMetadata saveSiirtotiedosto(
      final String timeRangeStart,
      final String timeRangeEnd,
      final String sourceSystem,
      final String subCategory,
      final InputStream data) {
    return saveSiirtotiedosto(timeRangeStart, timeRangeEnd, sourceSystem, subCategory, data, 0);
  }
}
