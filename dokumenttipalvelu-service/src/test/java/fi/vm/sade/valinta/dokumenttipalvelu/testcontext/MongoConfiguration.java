package fi.vm.sade.valinta.dokumenttipalvelu.testcontext;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.Mongo;

import de.flapdoodle.embed.mongo.tests.MongodForTestsFactory;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Configuration
public class MongoConfiguration {
    public static final String DATABASE_NAME = "fakemongodb";

    // fake mongo db
    @Bean(destroyMethod = "shutdown")
    public MongodForTestsFactory getMongoFactory() throws IOException {
        return new MongodForTestsFactory();// .newMongo();
    }

    @Bean
    public Mongo getMongo(MongodForTestsFactory factory) throws IOException {
        return factory.newMongo();
    }

    @Bean
    public Morphia getMorphia() {
        return new Morphia();
    }

    @Bean
    public Datastore getDatastore(Morphia morphia, Mongo mongo) {
        return morphia.createDatastore(mongo, DATABASE_NAME);
    }
}
