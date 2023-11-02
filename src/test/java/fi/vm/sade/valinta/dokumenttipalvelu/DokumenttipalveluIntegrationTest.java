package fi.vm.sade.valinta.dokumenttipalvelu;

import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectEntity;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectHead;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectMetadata;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.DockerHealthcheckWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Testcontainers
@SpringBootTest
public class DokumenttipalveluIntegrationTest {
  private static final String BUCKET_NAME = "opintopolku-test-dokumenttipalvelu";

  @Container
  private static final LocalStackContainer LOCAL_STACK =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
          .withServices(S3)
          .waitingFor(new DockerHealthcheckWaitStrategy());

  @BeforeAll
  public static void createBucket() throws IOException, InterruptedException {
    LOCAL_STACK.execInContainer("awslocal", "s3", "mb", "s3://" + BUCKET_NAME);
  }

  @SpringBootConfiguration
  static class TestConfiguration {
    @Bean
    public Dokumenttipalvelu dokumenttipalvelu() {
      final Dokumenttipalvelu dokumenttipalvelu =
          new Dokumenttipalvelu(LOCAL_STACK.getRegion(), BUCKET_NAME) {
            @Override
            public S3AsyncClient getClient() {
              return S3AsyncClient.builder()
                  .endpointOverride(LOCAL_STACK.getEndpointOverride(S3))
                  .credentialsProvider(
                      StaticCredentialsProvider.create(
                          AwsBasicCredentials.create(
                              LOCAL_STACK.getAccessKey(), LOCAL_STACK.getSecretKey())))
                  .region(Region.of(LOCAL_STACK.getRegion()))
                  .httpClientBuilder(
                      NettyNioAsyncHttpClient.builder()
                          .connectionTimeout(Duration.ofSeconds(60))
                          .maxConcurrency(100))
                  .build();
            }

            @Override
            public S3Presigner getPresigner() {
              return S3Presigner.builder()
                  .region(Region.of(LOCAL_STACK.getRegion()))
                  .endpointOverride(LOCAL_STACK.getEndpointOverride(S3))
                  .credentialsProvider(
                      StaticCredentialsProvider.create(
                          AwsBasicCredentials.create(
                              LOCAL_STACK.getAccessKey(), LOCAL_STACK.getSecretKey())))
                  .build();
            }
          };
      dokumenttipalvelu.listObjectsMaxKeys = 5;
      return dokumenttipalvelu;
    }
  }

  @Autowired private Dokumenttipalvelu dokumenttipalvelu;

  @BeforeEach
  public void clearBucketContents() throws IOException, InterruptedException {
    LOCAL_STACK.execInContainer(
        "awslocal", "s3", "rm", "s3://" + BUCKET_NAME, "--recursive", "--include", "'*'");
  }

  @Test
  public void testS3Integration() throws IOException {
    final ObjectMetadata metadata =
        dokumenttipalvelu.save(
            "id-1",
            "testifile.txt",
            Date.from(Instant.now().plus(Duration.of(1, ChronoUnit.DAYS))),
            Arrays.asList("testipalvelu", "&bar-xyz#"),
            "text/plain",
            Files.newInputStream(Paths.get("src/test/resources/testfile.txt")));
    assertEquals("id-1", metadata.documentId);
    assertEquals("t-testipalvelu/t-bar-xyz/id-1", metadata.key);
    assertEquals(Stream.of("testipalvelu", "bar-xyz").collect(Collectors.toSet()), metadata.tags);
    assertNotNull(metadata.lastModified);
    assertNotNull(metadata.eTag);

    final ObjectMetadata tokaMetadata =
        dokumenttipalvelu.save(
            "id-2",
            "toka.txt",
            Date.from(Instant.now().plus(Duration.of(2, ChronoUnit.DAYS))),
            Collections.singletonList("tokapalvelu"),
            "text/plain",
            Files.newInputStream(Paths.get("src/test/resources/testfile.txt")));
    assertEquals("id-2", tokaMetadata.documentId);
    assertEquals("t-tokapalvelu/id-2", tokaMetadata.key);
    assertEquals(metadata.eTag, tokaMetadata.eTag);

    final ObjectEntity objectEntity = dokumenttipalvelu.get(metadata.key);
    assertEquals("text/plain", objectEntity.contentType);
    assertEquals(
        "This is a test file.", IOUtils.toString(objectEntity.entity, StandardCharsets.UTF_8));
    assertEquals(20, objectEntity.contentLength);
    assertEquals("testifile.txt", objectEntity.fileName);
    assertEquals("id-1", objectEntity.documentId);

    final ObjectHead objectHead = dokumenttipalvelu.head(metadata.key);
    assertEquals("text/plain", objectHead.contentType);
    assertEquals(20, objectHead.contentLength);
    assertEquals("testifile.txt", objectHead.fileName);
    assertEquals("id-1", objectHead.documentId);

    final Collection<ObjectMetadata> findWithTagsResults =
        dokumenttipalvelu.find(Collections.singleton("testipalvelu"));
    assertEquals(1, findWithTagsResults.size());
    assertTrue(findWithTagsResults.stream().findFirst().isPresent());
    assertEquals("id-1", findWithTagsResults.stream().findFirst().get().documentId);

    final Collection<ObjectMetadata> findWithTagsAndDocumentIdResults =
        dokumenttipalvelu.find(Arrays.asList("testipalvelu", "id-1"));
    assertEquals(1, findWithTagsAndDocumentIdResults.size());
    assertTrue(findWithTagsAndDocumentIdResults.stream().findFirst().isPresent());
    assertEquals("id-1", findWithTagsAndDocumentIdResults.stream().findFirst().get().documentId);

    dokumenttipalvelu.rename(metadata.key, "file2.txt");
    final ObjectEntity renamedEntity = dokumenttipalvelu.get(metadata.key);
    assertEquals("file2.txt", renamedEntity.fileName);

    final FileDownload fileDownload =
        download(dokumenttipalvelu.getDownloadUrl(metadata.key, Duration.of(1, ChronoUnit.DAYS)));
    assertEquals(
        Collections.singletonList("file2.txt"), fileDownload.headers.get("x-amz-meta-filename"));
    assertEquals("This is a test file.", fileDownload.content);

    dokumenttipalvelu.delete("id-1");
    final CompletionException e =
        assertThrows(CompletionException.class, () -> dokumenttipalvelu.get("id-1"));
    assertInstanceOf(NoSuchKeyException.class, e.getCause());
  }

  @Test
  public void testThrowsExceptionWhenSavingWithExistingDocumentId() throws IOException {
    dokumenttipalvelu.save(
        "id-1",
        "testifile.txt",
        Date.from(Instant.now().plus(Duration.of(1, ChronoUnit.DAYS))),
        Arrays.asList("testipalvelu", "&bar-xyz#"),
        "text/plain",
        Files.newInputStream(Paths.get("src/test/resources/testfile.txt")));
    final CompletionException e =
        assertThrows(
            CompletionException.class,
            () ->
                dokumenttipalvelu.save(
                    "id-1",
                    "tokafile.txt",
                    Date.from(Instant.now().plus(Duration.of(1, ChronoUnit.DAYS))),
                    Collections.singletonList("tokapalvelu"),
                    "text/plain",
                    Files.newInputStream(Paths.get("src/test/resources/testfile.txt"))));
    assertInstanceOf(DocumentIdAlreadyExistsException.class, e.getCause());
  }

  @Test
  public void testFindReturnsAllObjects() {
    range(0, 6)
        .forEach(
            i -> {
              try {
                dokumenttipalvelu.save(
                    "foo" + i,
                    "testifile.txt",
                    Date.from(Instant.now().plus(Duration.of(1, ChronoUnit.DAYS))),
                    Collections.singletonList("testipalvelu"),
                    "text/plain",
                    Files.newInputStream(Paths.get("src/test/resources/testfile.txt")));
              } catch (final IOException e) {
                throw new RuntimeException(e);
              }
            });
    final Collection<ObjectMetadata> objects = dokumenttipalvelu.find(Collections.emptySet());
    assertEquals(6, objects.size());
  }

  private static class FileDownload {
    public final String content;
    public final Map<String, List<String>> headers;

    public FileDownload(final String content, final Map<String, List<String>> headers) {
      this.content = content;
      this.headers = headers;
    }
  }

  @Test
  public void testGetNotFound() {
    final CompletionException e =
        assertThrows(CompletionException.class, () -> dokumenttipalvelu.get("not_existing"));
    assertInstanceOf(NoSuchKeyException.class, e.getCause());

    final CompletionException ex =
        assertThrows(CompletionException.class, () -> dokumenttipalvelu.head("not_existing"));
    assertInstanceOf(NoSuchKeyException.class, ex.getCause());
  }

  private FileDownload download(final URL url) throws IOException {
    final HttpURLConnection con = (HttpURLConnection) url.openConnection();
    con.setRequestMethod("GET");
    final Map<String, List<String>> headers = con.getHeaderFields();
    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
    String inputLine;
    final StringBuilder content = new StringBuilder();
    while ((inputLine = in.readLine()) != null) {
      content.append(inputLine);
    }
    in.close();
    con.disconnect();
    return new FileDownload(content.toString(), headers);
  }
}
