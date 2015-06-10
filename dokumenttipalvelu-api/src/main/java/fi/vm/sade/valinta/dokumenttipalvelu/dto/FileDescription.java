package fi.vm.sade.valinta.dokumenttipalvelu.dto;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

public class FileDescription {
    private String filename;
    private Collection<String> tags;
    private Date expirationDate;
    private String mimeType;
    private String id;

    public FileDescription(String filename, Collection<String> tags, Date expirationDate, String mimeType) {
        this.filename = filename;
        this.tags = tags;
        this.expirationDate = expirationDate;
        this.mimeType = mimeType;
        this.id = null;
    }

    public FileDescription(String id, String filename, Collection<String> tags, Date expirationDate, String mimeType) {
        this.filename = filename;
        this.tags = tags;
        this.expirationDate = expirationDate;
        this.mimeType = mimeType;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Collection<String> getTags() {
        return tags;
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(filename).append(", ").append(Arrays.toString(tags.toArray())).toString();
    }
}
