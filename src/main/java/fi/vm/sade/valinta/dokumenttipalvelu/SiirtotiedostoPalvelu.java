package fi.vm.sade.valinta.dokumenttipalvelu;

import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectMetadata;
import software.amazon.awssdk.utils.StringUtils;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;


public class SiirtotiedostoPalvelu extends Dokumenttipalvelu {
    final DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
            .appendPattern("[yyyy-MM-dd'T'HH:mm:ss]")
            .appendPattern("[dd.MM.yyyy HH:mm]")
            .appendPattern("[yyyy-MM-dd HH:mm:ss.SSSSSS XXX]")
            .toFormatter();

    public SiirtotiedostoPalvelu(String awsRegion, String bucketName) {
        super(awsRegion, bucketName);
    }

    private String timeRangeString(
            final Optional<LocalDateTime> timeRangeStart, final Optional<LocalDateTime> timeRangeEnd) {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy_HH.mm.ss");
        if (timeRangeStart.isPresent() && timeRangeEnd.isPresent()) {
            return String.format(
                    "%s-%s_", formatter.format(timeRangeStart.get()), formatter.format(timeRangeEnd.get()));
        }
        if (timeRangeStart.isPresent()) {
            return String.format("%s-_", formatter.format(timeRangeStart.get()));
        }
        if (timeRangeEnd.isPresent()) {
            return String.format("-%s_", formatter.format(timeRangeEnd.get()));
        }
        return "";
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
     * @param timeRangeStart Startpoint of the data timerange contained by this siirtotiedosto,
     *                       allowed formats: '2024-01-24T10:15:30', '24.01.2024 10.15.30'.
     *                       Value is optional, can be null or empty.
     * @param timeRangeEnd   Endpoint of the data timerange contained by this siirtotiedosto
     *                       allowed formats: '2024-01-24T10:15:30', '24.01.2024 10.15.30'.
     *                       Value is optional, can be null or empty.
     * @param sourceSystem   Source system of this siirtotiedosto, e.g. kouta, ataru, etc. Value is mandatory.
     * @param subCategory    More detailed description of the contents
     *                       Value is optional, can be null or empty.
     * @param data           Document's data input stream. Value is mandatory.
     * @return Metadata describing the document.
     */
    public ObjectMetadata saveSiirtotiedosto(
            final String timeRangeStart,
            final String timeRangeEnd,
            final String sourceSystem,
            final String subCategory,
            final InputStream data) {

        if (StringUtils.isEmpty(sourceSystem)) {
            throw new IllegalArgumentException("Source system cannot be empty");
        }
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        Optional<LocalDateTime> timeRangeStartVal = StringUtils.isEmpty(timeRangeStart) ?
                Optional.empty() : Optional.of(LocalDateTime.parse(timeRangeStart, dateTimeFormatter));
        Optional<LocalDateTime> timeRangeEndVal = StringUtils.isEmpty(timeRangeEnd) ?
                Optional.empty() : Optional.of(LocalDateTime.parse(timeRangeEnd, dateTimeFormatter));
        final String categoryStr =
                StringUtils.isEmpty(subCategory) ? "" : String.format("%s_%s_", sourceSystem, subCategory);
        final String documentId =
                String.format(
                        "%s%s%s.json",
                        categoryStr, timeRangeString(timeRangeStartVal, timeRangeEndVal), UUID.randomUUID());
        final Collection<String> tags = tags(sourceSystem, subCategory);
        // TODO Poista expirationDate siinä vaiheessa kun se poistuu save -metodista
        final Date expirationDate = Date.from(Instant.now().plus(Duration.of(5, ChronoUnit.DAYS)));
        return save(documentId, documentId, expirationDate, tags, "json", data);
    }
}
