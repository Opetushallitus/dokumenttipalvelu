package fi.vm.sade.valinta.dokumenttipalvelu.resource;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import fi.vm.sade.valinta.dokumenttipalvelu.dto.Message;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.MetaData;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Path("dokumentit")
public interface DokumenttiResource {
    /**
     * Lataa dokumentin
     * 
     * @param documentId
     *            esim viestintapalvelu.hyvaksymiskirje.3241234ID
     * @return dokumentti
     */
    @GET
    @Path("lataa/{documentid}")
    InputStream lataa(@PathParam("documentid") String documentId);

    /**
     * Hakee listan kaikista dokumenteista
     * 
     * @return json kaikista taltioiduista dokumenteista
     */
    @GET
    @Path("hae")
    @Produces(MediaType.APPLICATION_JSON)
    Collection<MetaData> hae(@QueryParam("tags") List<String> tags);

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
    @Path("tallenna")
    @Consumes("application/octet-stream")
    public void tallenna(@QueryParam("filename") String filename, @QueryParam("expirationDate") Long expirationDate,
            @QueryParam("tags") List<String> tags, @QueryParam("mimeType") String mimeType, InputStream filedata);

    @PUT
    @Path("viesti")
    @Consumes(MediaType.APPLICATION_JSON)
    public void viesti(Message message);
}
