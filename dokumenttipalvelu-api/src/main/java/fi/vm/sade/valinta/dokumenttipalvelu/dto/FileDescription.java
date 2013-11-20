package fi.vm.sade.valinta.dokumenttipalvelu.dto;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class FileDescription {

    private static final int ONE_DAY = 1;
    private String filename;
    private String serviceName;
    private String documentType;
    private Date expirationDate;
    private Map<String, String> metaData = Collections.emptyMap();

    public FileDescription(String filename, String serviceName, String documentType, Date expirationDate) {
        this.filename = filename;
        this.serviceName = serviceName;
        this.documentType = documentType;
        this.expirationDate = expirationDate;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public Map<String, String> getMetaData() {
        return metaData;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getFilename() {
        return filename;
    }

}
