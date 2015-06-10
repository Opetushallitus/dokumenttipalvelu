package fi.vm.sade.valinta.dokumenttipalvelu.resource.impl;

import org.apache.commons.lang.StringUtils;

public class MimeTypeUtil {
    private MimeTypeUtil() {
    }

    public static String guessMimeType(String filename) {
        String mimeType = StringUtils.EMPTY;
        String extension = getExtension(filename);
        if ("pdf".equalsIgnoreCase(extension)) {
            mimeType = "application/pdf";
        } else if ("xls".equalsIgnoreCase(extension)) {
            mimeType = "application/vnd.ms-excel";
        }
        return mimeType;
    }

    private static String getExtension(String filename) {
        try {
            String dotSeparated[] = filename.split("\\.");
            if (dotSeparated == null || dotSeparated.length == 0) {
                return StringUtils.EMPTY;
            }
            return dotSeparated[dotSeparated.length - 1];
        } catch (Exception e) {
            // LOG.warn("Couldn't get extension for filename {}", filename);
        }
        return StringUtils.EMPTY;
    }
}
