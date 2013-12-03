package fi.vm.sade.valinta.dokumenttipalvelu.dto;

import java.util.Collection;
import java.util.Date;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class Message {

    private String message;
    private Collection<String> tags;
    private Date expirationDate;

    public Message() {
        this.message = null;
        this.expirationDate = null;
        this.tags = null;
    }

    public Message(String message, Collection<String> tags, Date expirationDate) {
        this.message = message;
        this.expirationDate = expirationDate;
        this.tags = tags;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public String getMessage() {
        return message;
    }

    public Collection<String> getTags() {
        return tags;
    }

}
