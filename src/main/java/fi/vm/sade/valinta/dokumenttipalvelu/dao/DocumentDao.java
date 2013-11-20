package fi.vm.sade.valinta.dokumenttipalvelu.dao;

import java.io.InputStream;
import java.util.Collection;

import fi.vm.sade.valinta.dokumenttipalvelu.domain.FileDescription;
import fi.vm.sade.valinta.dokumenttipalvelu.domain.MetaData;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface DocumentDao {

    Collection<MetaData> getAll();

    InputStream get(String documentId);

    MetaData put(FileDescription description, InputStream documentData);

}
