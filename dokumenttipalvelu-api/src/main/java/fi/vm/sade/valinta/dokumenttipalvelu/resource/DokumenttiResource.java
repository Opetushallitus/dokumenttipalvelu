package fi.vm.sade.valinta.dokumenttipalvelu.resource;

import java.io.InputStream;
import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sun.jersey.multipart.FormDataParam;

import fi.vm.sade.valinta.dokumenttipalvelu.dto.FileDescription;
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

    /**
     * 
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("tallenna")
    void tallenna(@FormDataParam("tiedosto") InputStream tiedosto, @FormDataParam("kuvaus") FileDescription kuvaus);
}
