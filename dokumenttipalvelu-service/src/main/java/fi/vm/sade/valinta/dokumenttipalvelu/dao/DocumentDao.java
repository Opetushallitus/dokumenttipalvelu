package fi.vm.sade.valinta.dokumenttipalvelu.dao;

import java.io.InputStream;
import java.util.Collection;

import fi.vm.sade.valinta.dokumenttipalvelu.dto.ContentTypeAndEntity;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.FileDescription;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.MetaData;

public interface DocumentDao {
    Collection<MetaData> getAll();

    Collection<MetaData> getAll(Collection<String> tags);

    Collection<MetaData> getMetaDataByName(String filename);

    ContentTypeAndEntity get(String documentId);

    ContentTypeAndEntity getByName(String filename);

<<<<<<< HEAD
    MetaData put(FileDescription description, InputStream documentData);
=======
	void rename(String documentId, String filename);
>>>>>>> hyväksymiskirjeiden generointi koko haulle
}
