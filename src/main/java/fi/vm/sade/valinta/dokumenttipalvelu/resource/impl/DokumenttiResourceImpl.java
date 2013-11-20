package fi.vm.sade.valinta.dokumenttipalvelu.resource.impl;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.dokumenttipalvelu.dao.DocumentDao;
import fi.vm.sade.valinta.dokumenttipalvelu.domain.MetaData;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;

@Component
public class DokumenttiResourceImpl implements DokumenttiResource {

    @Autowired
    DocumentDao documentDao;

    @Override
    public Collection<MetaData> hae() {
        // MetaData metaData =
        // MetaData.newBuilder().setFilename("application.pdf").setDocumentType("osoitetarrat")
        // .setServiceName("viestintapalvelu").build();
        try {
            // documentDao.put(metaData, new
            // ClassPathResource("addresslabels.pdf").getInputStream());
            documentDao.put(null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    @Override
    public Collection<MetaData> hae(String serviceName, String documentType) {

        return Collections.emptyList();
    }

    @Override
    public InputStream lataa(String documentId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void tallenna(InputStream tiedosto) { // MetaData metaData,
        // TODO Auto-generated method stub

    }
}
