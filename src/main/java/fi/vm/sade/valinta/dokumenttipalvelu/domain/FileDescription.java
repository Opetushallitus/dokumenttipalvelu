package fi.vm.sade.valinta.dokumenttipalvelu.domain;

import java.util.Collections;
import java.util.Map;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class FileDescription {

    private String filename;
    private String serviceName;
    private String documentType;
    private Map<String, String> metaData = Collections.emptyMap();

    public FileDescription(String filename, String serviceName, String documentType) {
        this.filename = filename;
        this.serviceName = serviceName;
        this.documentType = documentType;
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
