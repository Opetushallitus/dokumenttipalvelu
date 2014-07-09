package fi.vm.sade.valinta.dokumenttipalvelu.resource.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.dokumenttipalvelu.dao.DocumentDao;
import fi.vm.sade.valinta.dokumenttipalvelu.dao.FlushDao;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.ContentTypeAndEntity;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.FileDescription;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.Message;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.MetaData;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;

@Api(value = "/dokumentit", description = "Dokumenttipalvelun rajapinta")
@Component
public class DokumenttiResourceImpl implements DokumenttiResource {

	private static final Logger LOG = LoggerFactory
			.getLogger(DokumenttiResourceImpl.class);

	@Autowired
	private DocumentDao documentDao;
	@Autowired
	private FlushDao flushDao;

	@PreAuthorize("isAuthenticated()")
	@ApiOperation(value = "Dokumenttien haku käyttäjätunnuksella", response = Collection.class)
	@GET
	@Path("/hae")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Collection<MetaData> hae(@QueryParam("tags") List<String> tags) {
		return documentDao.getAll(addUserAsTag(tags));
	}

	@PreAuthorize("hasAnyRole('ROLE_APP_VALINTAPERUSTEET_CRUD_1.2.246.562.10.00000000001')")
	@ApiOperation(value = "Suojattu operaatio kaikkien dokumenttien hakuun", response = Collection.class)
	@GET
	@Path("/yllapitohaku")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	public Collection<MetaData> yllapitohaku(
			@PathParam("documentid") List<String> tags) {
		if (tags == null || tags.size() == 0) {
			return documentDao.getAll();
		} else {
			return documentDao.getAll(tags);
		}
	}

	@ApiOperation(value = "Dokumentin lataus tunnisteella", response = InputStream.class)
	@GET
	// @Produces("application/pdf")
	@Path("/lataa/{documentid}")
	@Override
	public Response lataa(@PathParam("documentid") String documentId) {
		final ContentTypeAndEntity c = documentDao.get(documentId);
		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws WebApplicationException,
					IOException {
				// Writer writer = new BufferedWriter(new
				// OutputStreamWriter(os));
				// writer.write("test");
				// writer.flush();
				IOUtils.copy(c.getEntity(), os);
			}
		};
		return Response
				.ok(stream)
				.header("Content-Length", "" + c.getLength())
				.header("Content-Type", c.getContentType())
				.header("Content-Disposition",
						"attachment; filename=\"" + c.getFilename() + "\"")
				.build();
	}

	@ApiOperation(value = "Dokumentin tallennus")
	@PUT
	@Path("/tallenna")
	@Consumes("application/octet-stream")
	@Override
	public void tallenna(@QueryParam("id") String id,
			@QueryParam("filename") String filename,
			@QueryParam("expirationDate") Long expirationDate,
			@QueryParam("tags") List<String> tags,
			@QueryParam("mimeType") String mimeType, InputStream filedata) {
		try {
			tags = addUserAsTag(tags);
			if (tags == null) {
				tags = Collections.emptyList();
			}
			if (mimeType == null) {
				mimeType = MimeTypeUtil.guessMimeType(filename);
			}
			LOG.info(
					"Id {}, Filename {}, date {}, tags {} and stream {}",
					new Object[] { id, filename, expirationDate,
							Arrays.toString(tags.toArray()), filedata });
			documentDao.put(new FileDescription(id, filename, tags,
					new DateTime(expirationDate).toDate(), mimeType), filedata);

		} catch (Exception e) {
			LOG.error(
					"Onkohan MongoDB-yhteys konfiguroitu oikein? Tallennus epäonnistui: {} {} {}",
					e.getMessage(), e.getCause(),
					Arrays.toString(e.getStackTrace()));
			throw new RuntimeException(e);
		}
	}

	@ApiOperation(value = "Viestin tallentaminen käyttäjätunnuksella")
	@PUT
	@Path("/viesti")
	@Consumes(MediaType.APPLICATION_JSON)
	@Override
	public void viesti(Message message) {
		documentDao.put(new FileDescription(message.getMessage(),
				addUserAsTag(message.getTags()), message.getExpirationDate(),
				"text/plain"), new ByteArrayInputStream(new byte[] {}));
	}

	@ApiOperation(value = "Tyhjentaa vanhentuneet dokumentit tietokannasta. Koostepalvelu kutsuu toimintoa. "
			+ "Tarkoitus on etta dokumenttipalvelu on mahdollisimman passiivinen.")
	@PUT
	@Path("/tyhjenna")
	public void tyhjenna() {
		flushDao.flush();
	}

	@Override
	@PreAuthorize("hasAnyRole('ROLE_APP_SIJOITTELU_READ','ROLE_APP_SIJOITTELU_READ_UPDATE','ROLE_APP_SIJOITTELU_CRUD')")
	@GET
	@Path("/osoitetarrat/{hakukohdeOid}")
	public Collection<MetaData> osoitetarrat(
			@PathParam("hakukohdeOid") String hakukohdeOid) {
		final String name = "osoitetarrat_" + hakukohdeOid + ".pdf";
		return documentDao.getMetaDataByName(name);
	}

	@Override
	@PreAuthorize("hasAnyRole('ROLE_APP_SIJOITTELU_READ','ROLE_APP_SIJOITTELU_READ_UPDATE','ROLE_APP_SIJOITTELU_CRUD')")
	@GET
	@Path("/hyvaksymiskirjeet/{hakukohdeOid}")
	public Collection<MetaData> hyvaksymiskirjeet(
			@PathParam("hakukohdeOid") String hakukohdeOid) {
		final String name = "hyvaksymiskirje_" + hakukohdeOid + ".pdf";
		return documentDao.getMetaDataByName(name);
	}

	@Override
	@PreAuthorize("hasAnyRole('ROLE_APP_SIJOITTELU_READ','ROLE_APP_SIJOITTELU_READ_UPDATE','ROLE_APP_SIJOITTELU_CRUD')")
	@GET
	@Path("/sijoitteluntulokset/{hakukohdeOid}")
	public Collection<MetaData> sijoitteluntulokset(
			@PathParam("hakukohdeOid") String hakukohdeOid) {
		final String name = "sijoitteluntulos_" + hakukohdeOid + ".xls";
		return documentDao.getMetaDataByName(name);
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
			return SecurityContextHolder.getContext().getAuthentication()
					.getName();
		} catch (NullPointerException e) {

		}
		// this is for unit tests
		return "<< not authenticated >>"; // <- is not possible in production
											// <prop
											// key="spring_security_default_access">isAuthenticated()</prop>
	}

}
