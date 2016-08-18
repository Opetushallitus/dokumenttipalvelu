package fi.vm.sade.valinta.dokumenttipalvelu.resource.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DokumenttipalveluJaxRsConfiguration {
    @Bean(name="dokumenttipalveluServiceJsonProvider")
    public JacksonJsonProvider getJacksonJsonProvider() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new GuavaModule());
        return new JacksonJsonProvider(objectMapper);
    }
}
