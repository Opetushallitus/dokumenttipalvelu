package fi.vm.sade.valinta.dokumenttipalvelu.dto;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class MetaData {

    private String mimeType;
    private String documentId;
    private String filename;
    private Date createdAt;
    private Collection<String> tags; // viestintapalvelu,
                                     // valintalaskentakoostepalvelu,
    // hyvaksymiskirje, jalkiohjauskirje, ...
    private Map<String, ? extends Object> data;
    private long size;
    private String md5;
    private Date expirationDate;

    public MetaData(String documentId, String filename, String mimeType, Date createdAt, Date expirationDate,
            Collection<String> tags, Map data, long size, String md5) {
        this.documentId = documentId;
        this.mimeType = mimeType;
        this.filename = filename;
        this.data = data;
        this.tags = tags;
        this.size = size;
        this.createdAt = createdAt;
        this.expirationDate = expirationDate;
        this.md5 = md5;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public String getMd5() {
        return md5;
    }

    public Map<String, ? extends Object> getData() {
        return data;
    }

    public long getSize() {
        return size;
    }

    public Collection<String> getTags() {
        return tags;
    }

    public String getFilename() {
        return filename;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getMimeType() {
        return mimeType;
    }

}
