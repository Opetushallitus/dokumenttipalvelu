package fi.vm.sade.valinta.dokumenttipalvelu;

import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectMetadata;
import software.amazon.awssdk.utils.StringUtils;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class SiirtotiedostoPalvelu extends Dokumenttipalvelu {
    public SiirtotiedostoPalvelu(String awsRegion, String bucketName) {
        super(awsRegion, bucketName);
    }

    private String timeRangeString(
            final Optional<Date> timeRangeStart, final Optional<Date> timeRangeEnd) {
        final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy_HH.mm.ss");
        if (timeRangeStart.isPresent() && timeRangeEnd.isPresent()) {
            return String.format(
                    "%s-%s_", dateFormat.format(timeRangeStart.get()), dateFormat.format(timeRangeEnd.get()));
        }
        if (timeRangeStart.isPresent()) {
            return String.format("%s-_", dateFormat.format(timeRangeStart.get()));
        }
        if (timeRangeEnd.isPresent()) {
            return String.format("-%s_", dateFormat.format(timeRangeEnd.get()));
        }
        return "";
    }

    private Collection<String> tags(String sourceSystem, Optional<String> subCategory) {
        return subCategory.isPresent()
                ? Arrays.asList(sourceSystem, subCategory.get())
                : Arrays.asList(sourceSystem);
    }

    @Override
    String composeKey(final Collection<String> tags, final String documentId) {
        return String.format("%s/%s", tags.stream().findFirst().orElse("unknown"), documentId);
    }

    /**
     * Saves siirtotiedosto document.
     *
     * @param timeRangeStart Startpoint of the data timerange contained by this siirtotiedosto
     * @param timeRangeEnd   Endpoint of the data timerange contained by this siirtotiedosto
     * @param sourceSystem   Source system of this siirtotiedosto, e.g. kouta, ataru, etc
     * @param subCategory    More detailed description of the contents
     * @param data           Document's data input stream
     * @return Metadata describing the document.
     */
    public ObjectMetadata saveSiirtotiedosto(
            final Optional<Date> timeRangeStart,
            final Optional<Date> timeRangeEnd,
            final String sourceSystem,
            final Optional<String> subCategory,
            final InputStream data) {

        if (StringUtils.isEmpty(sourceSystem)) {
            throw new IllegalArgumentException("Source system cannot be empty");
        }
        final String categoryStr =
                subCategory.isPresent() ? String.format("%s_%s_", sourceSystem, subCategory.get()) : "";
        final String documentId =
                String.format(
                        "%s%s%s.json",
                        categoryStr, timeRangeString(timeRangeStart, timeRangeEnd), UUID.randomUUID());
        final Collection<String> tags = tags(sourceSystem, subCategory);
        // TODO Poista expirationDate siinä vaiheessa kun se poistuu save -metodista
        final Date expirationDate = Date.from(Instant.now().plus(Duration.of(5, ChronoUnit.DAYS)));
        return save(documentId, documentId, expirationDate, tags, "json", data);
    }
}
