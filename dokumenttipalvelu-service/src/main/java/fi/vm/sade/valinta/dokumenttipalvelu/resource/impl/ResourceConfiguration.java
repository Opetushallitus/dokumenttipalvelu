package fi.vm.sade.valinta.dokumenttipalvelu.resource.impl;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ResourceConfiguration extends ResourceConfig {
    //
    public ResourceConfiguration() {
        // packages("fi.vm.sade.valinta.dokumenttipalvelu.resource.impl.DokumenttiResourceImpl");
        // json output and input
        register(JacksonFeature.class);
        // register(MultiPartFeature.class);

        register(DokumenttiResourceImpl.class);

        registerInstances(new com.wordnik.swagger.jaxrs.listing.ResourceListingProvider(),
                new com.wordnik.swagger.jaxrs.listing.ApiDeclarationProvider());
        register(com.wordnik.swagger.jaxrs.listing.ApiListingResourceJSON.class);
    }
}
