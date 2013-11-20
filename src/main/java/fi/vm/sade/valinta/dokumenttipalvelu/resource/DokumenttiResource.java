package fi.vm.sade.valinta.dokumenttipalvelu.resource;

import java.io.InputStream;
import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.web.bind.annotation.RequestBody;

import fi.vm.sade.valinta.dokumenttipalvelu.domain.MetaData;

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
    Collection<MetaData> hae();

    /**
     * Hakee listan dokumenteista palvelunnimella ja dokumentin tyypilla.
     * Erottelu siksi etta tulevaisuudessa voidaan tehda yksilolliset
     * kaytto-oikeusvaatimukset palveluille.
     * 
     * @param serviceName
     *            viestintapalvelu
     * @param documentType
     *            hyvaksymiskirje
     * @return json kaikista taltioiduista dokumenteista
     */
    @GET
    @Path("hae/{servicename}/{documenttype}")
    @Produces(MediaType.APPLICATION_JSON)
    Collection<MetaData> hae(@PathParam("servicename") String serviceName,
            @PathParam("documenttype") String documentType);

    // @QueryParam("") MetaData metaData,
    @PUT
    @Path("tallenna")
    // @Consumes(MediaType.MEDIA_TYPE_WILDCARD)
    void tallenna(@RequestBody InputStream tiedosto); // @RequestBody
}
