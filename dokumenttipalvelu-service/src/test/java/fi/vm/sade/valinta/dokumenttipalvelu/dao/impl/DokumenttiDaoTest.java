package fi.vm.sade.valinta.dokumenttipalvelu.dao.impl;

import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;

import fi.vm.sade.valinta.dokumenttipalvelu.dao.DocumentDao;
import fi.vm.sade.valinta.dokumenttipalvelu.dao.FlushDao;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.FileDescription;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.MetaData;
import fi.vm.sade.valinta.dokumenttipalvelu.testcontext.DokumenttiConfiguration;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@ContextConfiguration(classes = DokumenttiConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class DokumenttiDaoTest {

    private static final String TEST_PDF = "addresslabels.pdf";
    private static final String TEST_PDF2 = "hyvaksymiskirje.pdf";
    private static final String VIESTINTAPALVELU = "viestintapalvelu";
    private static final String OSOITETARRAT = "osoitetarrat";
    private static final String HYVAKSYMISKIRJE = "hyvaksymiskirje";

    @Autowired
    private DocumentDao dokumenttiDao;

    @Autowired
    private FlushDao flushDao;

    private MetaData metaOsoitekirje;
    private MetaData metaHyvaksymiskirje;

    @Before
    public void testPut() throws IOException {
        // expires now
        metaOsoitekirje = dokumenttiDao.put(new FileDescription(TEST_PDF, VIESTINTAPALVELU, OSOITETARRAT, now()
                .toDate()), new ClassPathResource(TEST_PDF).getInputStream());
        // expires not during this test
        metaHyvaksymiskirje = dokumenttiDao.put(new FileDescription(TEST_PDF2, VIESTINTAPALVELU, HYVAKSYMISKIRJE, now()
                .plusYears(1).toDate()), new ClassPathResource(TEST_PDF).getInputStream());
    }

    @Test
    public void testGetMetaData() throws IOException {
        assertTrue(IOUtils.contentEquals(new ClassPathResource(TEST_PDF).getInputStream(),
                dokumenttiDao.get(metaOsoitekirje.getDocumentId())));
        assertEquals(Arrays.asList(VIESTINTAPALVELU, OSOITETARRAT), metaOsoitekirje.getTags());
    }

    @Test
    public void testGetAll() throws IOException {
        Set<?> ids = Sets.newHashSet(Collections2.transform(dokumenttiDao.getAll(), new Function<MetaData, String>() {
            public String apply(MetaData data) {
                return data.getDocumentId();
            }
        }));
        assertTrue(ids.containsAll(Arrays.asList(metaOsoitekirje.getDocumentId(), metaHyvaksymiskirje.getDocumentId())));
    }

    @Test
    public void testGetAllWithQuery() throws IOException {
        Collection<MetaData> m0 = dokumenttiDao.getAll(VIESTINTAPALVELU, OSOITETARRAT);
        Collection<MetaData> m1 = dokumenttiDao.getAll(VIESTINTAPALVELU);
        assertFalse(m0.isEmpty());
        assertFalse(m1.isEmpty());
        assertTrue(m0.size() < m1.size());
    }

    @After
    public void testRemovingExpiredDocuments() {
        Collection<MetaData> m0 = dokumenttiDao.getAll();
        flushDao.flush();
        Collection<MetaData> m1 = dokumenttiDao.getAll();
        assertTrue(m0.size() > m1.size());
    }
}
