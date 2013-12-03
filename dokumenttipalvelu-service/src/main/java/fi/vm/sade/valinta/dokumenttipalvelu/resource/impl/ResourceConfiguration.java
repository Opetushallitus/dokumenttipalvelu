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
        packages("org.glassfish.jersey.examples.jackson")
        // json output and input
                .register(JacksonFeature.class);
        // register(MultiPartFeature.class);
        register(DokumenttiResourceImpl.class);
    }
}
