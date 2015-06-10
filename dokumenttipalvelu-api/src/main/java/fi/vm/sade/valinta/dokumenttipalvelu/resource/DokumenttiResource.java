package fi.vm.sade.valinta.dokumenttipalvelu.resource;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fi.vm.sade.valinta.dokumenttipalvelu.dto.Message;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.MetaData;

@Path("dokumentit")
public interface DokumenttiResource {

    /**
     * Hakee listan kaikista dokumenteista. Kayttajatunnus on osa tagia. Eli
     * talla metodilla voi hakea ainoastaan omia dokumentteja.
     *
     * @return json kaikista taltioiduista dokumenteista
     */
    @GET
    @Path("/hae")
    @Produces(MediaType.APPLICATION_JSON)
    Collection<MetaData> hae(@QueryParam("tags") List<String> tags);

    /**
     * Hakee listan kaikista dokumenteista. Ei rajoita kayttajatunnuksella
     * automaattisesti.
     * <p/>
     * hasAnyRole('ROLE_APP_VALINTAPERUSTEET_CRUD_1.2.246.562.10.00000000001')
     *
     * @return json kaikista taltioiduista dokumenteista
     */
    @GET
    @Path("/yllapitohaku")
    @Produces(MediaType.APPLICATION_JSON)
    Collection<MetaData> yllapitohaku(@QueryParam("tags") List<String> tags);

    /**
     * Lataa dokumentin
     *
     * @param documentId esim viestintapalvelu.hyvaksymiskirje.3241234ID
     * @return dokumentti
     */
    @GET
    @Path("/lataa/{documentid}")
    Response lataa(@PathParam("documentid") String documentId);

<<<<<<< HEAD
    /**
     * @param expirationDate [OPTIONAL] DEFAULTS TO 24H
     */
    @PUT
    @Path("/tallenna")
    @Consumes("application/octet-stream")
    void tallenna(@QueryParam("id") String id,
                  @QueryParam("filename") String filename,
                  @QueryParam("expirationDate") Long expirationDate,
                  @QueryParam("tags") List<String> tags,
                  @QueryParam("mimeType") String mimeType, InputStream filedata);
=======
	/**
	 * @param filename
	 * @param expirationDate
	 *            [OPTIONAL] DEFAULTS TO 24H
	 * @param tags
	 *            [OPTIONAL]
	 * @param mimeType
	 *            [OPTIONAL]
	 * @param filedata
	 */
	@PUT
	@Path("/tallenna")
	@Consumes("application/octet-stream")
	void tallenna(@QueryParam("id") String id,
			@QueryParam("filename") String filename,
			@QueryParam("expirationDate") Long expirationDate,
			@QueryParam("tags") List<String> tags,
			@QueryParam("mimeType") String mimeType,
			InputStream filedata);

	@PUT
	@Path("/uudelleennimea/{documentid}")
	@Consumes("text/plain")
	String uudelleennimea(@PathParam("documentid") String documentId,
				  String filename);
>>>>>>> hyväksymiskirjeiden generointi koko haulle

    @PUT
    @Path("/viesti")
    @Consumes(MediaType.APPLICATION_JSON)
    public void viesti(Message message);

    /**
     * Tyhjentaa vanhentuneet dokumentit tietokannasta. Koostepalvelu kutsuu
     * toimintoa. Tarkoitus on etta dokumenttipalvelu on mahdollisimman
     * passiivinen.
     */
    @PUT
    @Path("/tyhjenna")
    public void tyhjenna();

    @GET
    @Path("/osoitetarrat/{hakukohdeOid}")
    public Collection<MetaData> osoitetarrat(@PathParam("hakukohdeOid") String hakukohdeOid);

    @GET
    @Path("/hyvaksymiskirjeet/{hakukohdeOid}")
    public Collection<MetaData> hyvaksymiskirjeet(@PathParam("hakukohdeOid") String hakukohdeOid);

    @GET
    @Path("/sijoitteluntulokset/{hakukohdeOid}")
    public Collection<MetaData> sijoitteluntulokset(@PathParam("hakukohdeOid") String hakukohdeOid);
}
