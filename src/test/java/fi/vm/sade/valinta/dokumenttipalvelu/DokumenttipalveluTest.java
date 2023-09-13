package fi.vm.sade.valinta.dokumenttipalvelu;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectMetadata;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

public class DokumenttipalveluTest {
  private final S3AsyncClient client = mock(S3AsyncClient.class);
  private final S3Presigner presigner = mock(S3Presigner.class);
  private final String bucketName = "test-bucket";

  class MockDokumenttipalvelu extends Dokumenttipalvelu {
    public MockDokumenttipalvelu(final String awsRegion, final String bucketName) {
      super(awsRegion, bucketName);
    }

    @Override
    public S3AsyncClient getClient() {
      return client;
    }

    @Override
    public S3Presigner getPresigner() {
      return presigner;
    }
  }

  private final Dokumenttipalvelu dokumenttipalvelu =
      new MockDokumenttipalvelu("eu-west-1", bucketName);

  @BeforeEach
  public void beforeEach() {
    reset(client);
    reset(presigner);
  }

  @Test
  public void testComposeKeyValidatesAndSanitizes() {
    assertEquals(
        "DocumentId is null",
        assertThrows(
                IllegalArgumentException.class,
                () -> dokumenttipalvelu.composeKey(Collections.EMPTY_LIST, null))
            .getMessage());

    assertEquals(
        "Tags is null",
        assertThrows(
                IllegalArgumentException.class, () -> dokumenttipalvelu.composeKey(null, "foo"))
            .getMessage());

    final Set<String> tooLongTags =
        IntStream.range(0, 1001)
            .boxed()
            .map(i -> UUID.randomUUID().toString())
            .collect(Collectors.toSet());
    assertEquals(
        "Key too long",
        assertThrows(
                IllegalArgumentException.class,
                () -> dokumenttipalvelu.composeKey(tooLongTags, "id-1"))
            .getMessage());

    assertEquals(
        "t-testipalvelu/t-category-1/id-1",
        dokumenttipalvelu.composeKey(Arrays.asList("testipalvelu", "category-1"), "id-1"));

    assertEquals(
        "t-testipalvelu-A/ID_1.a",
        dokumenttipalvelu.composeKey(
            Collections.singletonList("testipalveluäöå-A€"), "ID_1(%€).a'"));
  }

  @Test
  public void testExtractDocumentId() {
    assertEquals("foo.txt", dokumenttipalvelu.extractDocumentId("t-testipalvelu/foo.txt"));

    assertEquals("foo.txt", dokumenttipalvelu.extractDocumentId("foo.txt"));

    assertEquals(
        "Invalid key",
        assertThrows(IllegalArgumentException.class, () -> dokumenttipalvelu.extractDocumentId("/"))
            .getMessage());

    assertEquals(
        "Key is null",
        assertThrows(
                IllegalArgumentException.class, () -> dokumenttipalvelu.extractDocumentId(null))
            .getMessage());
  }

  @Test
  public void testExtractTags() {
    assertEquals(
        "Key is null",
        assertThrows(IllegalArgumentException.class, () -> dokumenttipalvelu.extractTags(null))
            .getMessage());
    assertEquals(Collections.EMPTY_SET, dokumenttipalvelu.extractTags(""));
    assertEquals(Collections.EMPTY_SET, dokumenttipalvelu.extractTags("/"));
    assertEquals(
        Arrays.stream(new String[] {"eka", "toka"}).collect(Collectors.toSet()),
        dokumenttipalvelu.extractTags("t-eka/t-toka/id-1.txt"));
  }

  @Test
  public void testSanitize() {
    assertEquals("foo_a-1.txt", dokumenttipalvelu.sanitize("foo_a-1.txt'öäå€!"));
    assertEquals(
        "Value is null",
        assertThrows(IllegalArgumentException.class, () -> dokumenttipalvelu.sanitize(null))
            .getMessage());
  }

  @Test
  public void testConvert() {
    final Instant now = Instant.now();
    assertEquals(
        new ObjectMetadata(
            "t-testipalvelu/key.txt",
            "key.txt",
            Collections.singleton("testipalvelu"),
            now,
            1L,
            "abc123"),
        dokumenttipalvelu.convert(
            S3Object.builder()
                .key("t-testipalvelu/key.txt")
                .lastModified(now)
                .size(1L)
                .eTag("abc123")
                .build()));
  }

  @Test
  public void testSaveGeneratesIdWhenNotProvided() throws IOException {
    when(client.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
        .thenReturn(completedFuture(PutObjectResponse.builder().build()));
    when(client.getObjectAttributes(any(GetObjectAttributesRequest.class)))
        .thenReturn(completedFuture(GetObjectAttributesResponse.builder().build()));
    final ObjectMetadata metadata =
        dokumenttipalvelu.save(
            null,
            "testfile.txt",
            new Date(),
            Collections.emptySet(),
            "text/plain",
            Files.newInputStream(Paths.get("src/test/resources/testfile.txt")));
    assertNotNull(UUID.fromString(metadata.documentId));
  }
}
