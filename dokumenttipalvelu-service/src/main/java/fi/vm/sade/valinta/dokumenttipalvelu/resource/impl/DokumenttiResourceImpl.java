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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import fi.vm.sade.valinta.dokumenttipalvelu.dao.DocumentDao;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.FileDescription;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.Message;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.MetaData;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;

@PreAuthorize("isAuthenticated()")
@Component
public class DokumenttiResourceImpl implements DokumenttiResource {

    private static final Logger LOG = LoggerFactory.getLogger(DokumenttiResourceImpl.class);

    @Autowired
    DocumentDao documentDao;

    @Override
    public Collection<MetaData> hae(List<String> tags) {
        return documentDao.getAll(addUserAsTag(tags));
    }

    @Override
    @PreAuthorize("hasAnyRole('ROLE_APP_VALINTAPERUSTEET_CRUD_1.2.246.562.10.00000000001')")
    public Collection<MetaData> yllapitohaku(List<String> tags) {
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
        tags = addUserAsTag(tags);
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

    private List<String> addUserAsTag(Collection<String> tags) {
        List<String> s = Lists.newArrayList();
        if (tags != null) {
            s.addAll(tags);
        }
        s.add(getUsername());
        return s;
    }

    private String getUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (NullPointerException e) {

        }
        // this is for unit tests
        return "<< not authenticated >>"; // <- is not possible in production
                                          // <prop
                                          // key="spring_security_default_access">isAuthenticated()</prop>
    }

    @Override
    public void viesti(Message message) {
        documentDao.put(
                new FileDescription(message.getMessage(), addUserAsTag(message.getTags()), message.getExpirationDate(),
                        "text/plain"), new ByteArrayInputStream(new byte[] {}));
    }
}
