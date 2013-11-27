package fi.vm.sade.valinta.dokumenttipalvelu.resource.impl;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

public class ResourceConfiguration extends ResourceConfig {
    //
    public ResourceConfiguration() {
        packages("org.glassfish.jersey.examples.jackson").register(JacksonFeature.class);
        register(MultiPartFeature.class);
        register(DokumenttiResourceImpl.class);
    }
}
