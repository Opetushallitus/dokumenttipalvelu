# Dokumenttipalvelu

## Käyttöönotto

Dokumenttipalvelu on Java-kirjasto, joka käyttää AWS:n S3-palvelua dokumenttien tietovarastona. Kirjasto otetaan
käyttöön lisäämällä se riippuvuudeksi sovellukseen:

```xml
<dependencies>
    <dependency>
        <groupId>fi.vm.sade.dokumenttipalvelu</groupId>
        <artifactId>dokumenttipalvelu</artifactId>
        <version>[viimeisin versio]</version>
    </dependency>
</dependencies>
```

Dokumenttipalvelu määritetään Spring-sovellukseen Beanina:

```java
import fi.vm.sade.valinta.dokumenttipalvelu.Dokumenttipalvelu;

@Configuration
public class DokumenttipalveluConfig {
    @Bean
    public Dokumenttipalvelu dokumenttipalvelu(@Value("${aws.region}") final String region,
                                               @Value("${aws.bucket.name}") final String bucketName) {
        return new Dokumenttipalvelu(region, bucketName);
    }
}
```

Yllä olevan lisäksi tarvitaan kaksi (ympäristökohtaista) asetusta. Ne voi asettaa esimerkiksi propertieseihin tai
vaihtoehtoisesti Springin yaml-konfiguraatioon

```properties
aws.region={{aws_region}}
aws.bucket.name={{dokumenttipalvelu_aws_bucket_name}}
```

```yaml
aws:
  region: {{aws_region}}
  bucket:
    name: {{dokumenttipalvelu_aws_bucket_name}}
```

Dokumenttipalvelua käyttävälle sovellukselle on konfiguroitava tarvittavat kirjoitusoikeudet dokumenttipalvelun S3
bucketiin. Oikeusmäärittelyt tehdään cloud-base repositoryyn,
[esim viestintäpalvelun oikeudet](https://github.com/Opetushallitus/cloud-base/blob/master/aws/templates/services/task_role.py#L184).

## Käyttö

Käyttöesimerkit löytyy
[DokumenttipalveluIntegrationTest](src/test/java/fi/vm/sade/valinta/dokumenttipalvelu/DokumenttipalveluIntegrationTest.java):
stä.

### Dokumentin tallennus

```java
final ObjectMetadata metadata=dokumenttipalvelu.save(
        "my-id",
        "file.pdf",
        Date.from(Instant.now().plus(Duration.of(1,ChronoUnit.DAYS))),
        Arrays.asList("viestintapalvelu","categoria-A"),
        "application/pdf",
        Files.newInputStream(Paths.get("file.pdf")));
// metadata.key: t-viestintapalvelu/t-categoria-A/my-id
```

`ObjectMetadata` sisältää `key`-kentän, joka toimii dokumentin avaimena.

### Dokumentin noutaminen

```java
final ObjectEntity document=dokumenttipalvelu.get("t-viestintapalvelu/t-categoria-A/my-id");
```

### Dokumenttien etsiminen

```java
final Collection<ObjectMetadata> documents=dokumenttipalvelu.find(Arrays.asList("categoria-A"));
```

### Dokumentin uudelleennimeäminen

```java
dokumenttipalvelu.rename("t-viestintapalvelu/t-categoria-A/my-id","filename.pdf");
```

### Dokumentin poistaminen

```java
dokumenttipalvelu.delete("t-viestintapalvelu/t-categoria-A/my-id");
```

## Testit

Testit ajetaan komennolla `mvn test`. Integraatiotestit odottavat, että Docker daemon on käynnissä.
