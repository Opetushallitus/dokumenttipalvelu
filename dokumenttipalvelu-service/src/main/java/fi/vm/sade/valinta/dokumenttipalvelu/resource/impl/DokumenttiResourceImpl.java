package fi.vm.sade.valinta.dokumenttipalvelu.resource.impl;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.dokumenttipalvelu.dao.DocumentDao;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.MetaData;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;

@Component
public class DokumenttiResourceImpl implements DokumenttiResource {

    private static final Logger LOG = LoggerFactory.getLogger(DokumenttiResourceImpl.class);

    @Autowired
    DocumentDao documentDao;

    @Override
    public Collection<MetaData> hae(List<String> tags) {
        if (tags == null || tags.size() == 0) {
            return documentDao.getAll();
        } else {
            return documentDao.getAll(tags);
        }
    }

    @Override
    public InputStream lataa(String documentId) {
        return documentDao.get(documentId);
    }

    @Override
    public void tallenna(String filename, Long expirationDate, List<String> tags, InputStream filedata) {
        if (tags == null) {
            tags = Collections.emptyList();
        }
        LOG.info("Filename {}, date {}, tags {} and stream {}",
                new Object[] { filename, expirationDate, Arrays.toString(tags.toArray()), filedata });
    }

}
