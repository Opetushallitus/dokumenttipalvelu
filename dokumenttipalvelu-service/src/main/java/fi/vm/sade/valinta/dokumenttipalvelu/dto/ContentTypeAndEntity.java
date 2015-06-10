package fi.vm.sade.valinta.dokumenttipalvelu.dto;

import java.io.InputStream;

public class ContentTypeAndEntity {
    private final InputStream entity;
    private final String contentType;
    private final String filename;
    private final Long length;

    public ContentTypeAndEntity(InputStream entity, String contentType, String filename, Long length) {
        this.entity = entity;
        this.contentType = contentType;
        this.filename = filename;
        this.length = length;
    }

    public Long getLength() {
        return length;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public InputStream getEntity() {
        return entity;
    }
}
