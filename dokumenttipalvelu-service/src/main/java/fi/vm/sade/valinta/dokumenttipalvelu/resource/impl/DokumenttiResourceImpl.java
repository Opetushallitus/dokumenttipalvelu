package fi.vm.sade.valinta.dokumenttipalvelu.resource.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.dokumenttipalvelu.dao.DocumentDao;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.FileDescription;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.Message;
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
    public void tallenna(String filename, Long expirationDate, List<String> tags, String mimeType, InputStream filedata) {
        if (tags == null) {
            tags = Collections.emptyList();
        }
        if (mimeType == null) {
            mimeType = MimeTypeUtil.guessMimeType(filename);
        }
        LOG.info("Filename {}, date {}, tags {} and stream {}",
                new Object[] { filename, expirationDate, Arrays.toString(tags.toArray()), filedata });
        documentDao.put(new FileDescription(filename, tags, new DateTime(expirationDate).toDate(), mimeType), filedata);
    }

    @Override
    public void viesti(Message message) {
        documentDao.put(new FileDescription(message.getMessage(), message.getTags(), message.getExpirationDate(),
                "text/plain"), new ByteArrayInputStream(new byte[] {}));
    }
}
