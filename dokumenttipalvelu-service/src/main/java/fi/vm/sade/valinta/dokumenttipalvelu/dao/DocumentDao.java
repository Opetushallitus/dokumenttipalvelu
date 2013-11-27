package fi.vm.sade.valinta.dokumenttipalvelu.dao;

import java.io.InputStream;
import java.util.Collection;

import fi.vm.sade.valinta.dokumenttipalvelu.dto.FileDescription;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.MetaData;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface DocumentDao {

    Collection<MetaData> getAll();

    Collection<MetaData> getAll(Collection<String> tags);

    InputStream get(String documentId);

    MetaData put(FileDescription description, InputStream documentData);

}
