package fi.vm.sade.valinta.dokumenttipalvelu.testcontext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import org.mongodb.morphia.Datastore;

import fi.vm.sade.valinta.dokumenttipalvelu.dao.impl.DocumentDaoImpl;

@Configuration
@Import(MongoConfiguration.class)
public class DokumenttiConfiguration {

    @Bean
    public DocumentDaoImpl getDokumenttiDaoImpl(Datastore datastore) {
        return new DocumentDaoImpl(datastore);
    }

}
