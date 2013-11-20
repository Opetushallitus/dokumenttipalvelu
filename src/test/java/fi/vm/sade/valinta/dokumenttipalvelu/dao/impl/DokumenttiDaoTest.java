package fi.vm.sade.valinta.dokumenttipalvelu.dao.impl;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.code.morphia.Datastore;

import fi.vm.sade.valinta.dokumenttipalvelu.dao.DocumentDao;
import fi.vm.sade.valinta.dokumenttipalvelu.domain.FileDescription;
import fi.vm.sade.valinta.dokumenttipalvelu.domain.MetaData;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@ContextConfiguration(classes = DokumenttiDaoTest.class)
@Configuration
@Import(MongoConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class DokumenttiDaoTest {

    private static final String TEST_PDF = "addresslabels.pdf";
    private static final String VIESTINTAPALVELU = "viestintapalvelu";
    private static final String OSOITETARRAT = "osoitetarrat";

    // testattava dao kerros
    @Bean
    public DocumentDaoImpl getDokumenttiDaoImpl(Datastore datastore) {
        return new DocumentDaoImpl(datastore);
    }

    @Autowired
    private DocumentDao dokumenttiDao;

    @Test
    public void testDao() throws IOException, InterruptedException {
        MetaData metaData = dokumenttiDao.put(new FileDescription(TEST_PDF, VIESTINTAPALVELU, OSOITETARRAT),
                new ClassPathResource(TEST_PDF).getInputStream());
        Assert.assertTrue(IOUtils.contentEquals(new ClassPathResource(TEST_PDF).getInputStream(),
                dokumenttiDao.get(metaData.getDocumentId())));

        Assert.assertEquals(Arrays.asList(VIESTINTAPALVELU, OSOITETARRAT), metaData.getTags());
        dokumenttiDao.getAll();
    }
}
