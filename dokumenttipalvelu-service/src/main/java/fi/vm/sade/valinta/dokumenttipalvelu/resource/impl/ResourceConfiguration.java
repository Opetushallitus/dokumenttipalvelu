package fi.vm.sade.valinta.dokumenttipalvelu.resource.impl;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

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
		register(new ContainerResponseFilter() {

			@Override
			public void filter(ContainerRequestContext requestContext,
					ContainerResponseContext responseContext)
					throws IOException {
				responseContext.getHeaders().add("Access-Control-Allow-Origin",
						"*");
			}
		});

		register(JacksonFeature.class);
		// register(MultiPartFeature.class);

		register(DokumenttiResourceImpl.class);
		/**
		 * CORS Filter
		 */

		registerInstances(
				new com.wordnik.swagger.jaxrs.listing.ResourceListingProvider(),
				new com.wordnik.swagger.jaxrs.listing.ApiDeclarationProvider());
		register(com.wordnik.swagger.jaxrs.listing.ApiListingResourceJSON.class);
	}
}
