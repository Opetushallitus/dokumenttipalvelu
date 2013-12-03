package fi.vm.sade.valinta.dokumenttipalvelu.dto;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class FileDescription {

    // private static final int ONE_DAY = 1;
    private String filename;
    private Collection<String> tags;
    private Date expirationDate;
    private String mimeType;

    // private Map<String, String> metaData = Collections.emptyMap();

    public FileDescription(String filename, Collection<String> tags, Date expirationDate, String mimeType) {
        this.filename = filename;
        this.tags = tags;
        this.expirationDate = expirationDate;
        this.mimeType = mimeType;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public String getMimeType() {
        return mimeType;
    }

    // public Map<String, String> getMetaData() { return metaData;}

    public Collection<String> getTags() {
        return tags;
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public String toString() {
        final String VALI = ", ";
        return new StringBuilder().append(filename).append(VALI).append(Arrays.toString(tags.toArray())).toString();
    }

}
