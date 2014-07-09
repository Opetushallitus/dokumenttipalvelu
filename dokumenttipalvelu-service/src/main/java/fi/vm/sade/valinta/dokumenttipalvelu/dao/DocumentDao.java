package fi.vm.sade.valinta.dokumenttipalvelu.dao;

import java.io.InputStream;
import java.util.Collection;

import fi.vm.sade.valinta.dokumenttipalvelu.dto.ContentTypeAndEntity;
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

	Collection<MetaData> getMetaDataByName(String filename);

	ContentTypeAndEntity get(String documentId);

	ContentTypeAndEntity getByName(String filename);

	MetaData put(FileDescription description, InputStream documentData);

}
