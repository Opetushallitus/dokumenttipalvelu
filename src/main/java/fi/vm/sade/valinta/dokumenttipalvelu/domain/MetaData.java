package fi.vm.sade.valinta.dokumenttipalvelu.domain;

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
    private Date createdAt;
    private List<String> tags; // viestintapalvelu, koostepalvelu,
                               // hyvaksymiskirje, jalkiohjauskirje, ...
    private Map data;
    private long size;
    private String md5;

    public MetaData(String documentId, String filename, String mimeType, Date createdAt, List<String> tags, Map data,
            long size, String md5) {
        this.documentId = documentId;
        this.mimeType = mimeType;
        this.filename = filename;
        this.data = data;
        this.tags = tags;
        this.size = size;
        this.createdAt = createdAt;
        this.md5 = md5;
    }

    public String getMd5() {
        return md5;
    }

    public Map getData() {
        return data;
    }

    public long getSize() {
        return size;
    }

    public List<String> getTags() {
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
