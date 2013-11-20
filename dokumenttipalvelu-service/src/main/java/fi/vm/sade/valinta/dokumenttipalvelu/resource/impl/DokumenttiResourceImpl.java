package fi.vm.sade.valinta.dokumenttipalvelu.resource.impl;

import java.io.InputStream;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.dokumenttipalvelu.dao.DocumentDao;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.FileDescription;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.MetaData;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;

@Component
public class DokumenttiResourceImpl implements DokumenttiResource {

    private static final Logger LOG = LoggerFactory.getLogger(DokumenttiResourceImpl.class);

    @Autowired
    DocumentDao documentDao;

    @Override
    public Collection<MetaData> hae() {
        return documentDao.getAll();
    }

    @Override
    public Collection<MetaData> hae(String serviceName, String documentType) {
        return documentDao.getAll(serviceName, documentType);
    }

    @Override
    public InputStream lataa(String documentId) {
        return documentDao.get(documentId);
    }

    @Override
    public void tallenna(InputStream tiedosto, FileDescription kuvaus) {
        LOG.info("Filename {}", kuvaus.getFilename());
    }
}
