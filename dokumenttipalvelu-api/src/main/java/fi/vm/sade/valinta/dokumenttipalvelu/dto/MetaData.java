package fi.vm.sade.valinta.dokumenttipalvelu.dto;

import java.util.Date;
import java.util.List;
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
    private String createdAt;
    private List<String> tags; // viestintapalvelu,
                               // valintalaskentakoostepalvelu,
    // hyvaksymiskirje, jalkiohjauskirje, ...
    // private Map<String, ? extends Object> data;
    private String size;
    private String md5;
    private Date expirationDate;

    public MetaData() {

    }

    public MetaData(String documentId, String filename, String mimeType, String createdAt, Date expirationDate,
            List<String> tags, Map data, String size, String md5) {
        this.documentId = documentId;
        this.mimeType = mimeType;
        this.filename = filename;
        // this.data = data;
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

    // public Map<String, ? extends Object> getData() { return data; }

    public String getSize() {
        return size;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getFilename() {
        return filename;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getMimeType() {
        return mimeType;
    }

}
