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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.OngoingStubbing;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkException;
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

  class RetryableException extends SdkException {

    protected RetryableException(Builder builder) {
      super(builder);
    }

    @Override
    public boolean retryable() {
      return true;
    }
  }
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

  private void mockSequenceForSave(int nbrOfRecoverablePutFailures, int nbrOfRecoverableGetAttributesFailures) {
    RetryableException exception = new RetryableException(SdkException.builder());

    OngoingStubbing<CompletableFuture<PutObjectResponse>> putCall =
      when(client.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)));
    for (int retryCount = 0; retryCount < nbrOfRecoverablePutFailures; retryCount++) {
      putCall = putCall.thenThrow(exception);
    }
    putCall.thenReturn(completedFuture(PutObjectResponse.builder().build()));

    OngoingStubbing<CompletableFuture<GetObjectAttributesResponse>> getAttributesCall =
      when(client.getObjectAttributes(any(GetObjectAttributesRequest.class)));
    for (int retryCount = 0; retryCount < nbrOfRecoverableGetAttributesFailures; retryCount++) {
      getAttributesCall = getAttributesCall.thenThrow(exception);
    }
    getAttributesCall.thenReturn(completedFuture(GetObjectAttributesResponse.builder().build()));

    when(client.listObjectsV2(any(ListObjectsV2Request.class)))
            .thenReturn(completedFuture(ListObjectsV2Response.builder().isTruncated(false).build()));
  }

  @Test
  public void testSaveGeneratesIdWhenNotProvided() throws IOException {
    mockSequenceForSave(0, 0);
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

  @Test
  public void testSaveSucceedsAfterRetry() throws IOException {
    mockSequenceForSave(2, 2);
    final ObjectMetadata metadata =
      dokumenttipalvelu.save(
              UUID.randomUUID().toString(),
              "testfile.txt",
              new Date(),
              Collections.emptySet(),
              "text/plain",
              Files.newInputStream(Paths.get("src/test/resources/testfile.txt")),
              3
      );
    assertNotNull(metadata);
  }

  @Test
  public void testSaveFailsAfterPutObjectRetries() throws IOException {
    mockSequenceForSave(4, 2);
      try {
        dokumenttipalvelu.save(
                UUID.randomUUID().toString(),
                "testfile.txt",
                new Date(),
                Collections.emptySet(),
                "text/plain",
                Files.newInputStream(Paths.get("src/test/resources/testfile.txt")),
                3
        );
        fail("Expected exception not thrown");
      } catch(CompletionException | RetryableException ignored) {}
  }

  @Test
  public void testSaveFailsAfterGetAttributesRetries() throws IOException {
    mockSequenceForSave(0, 4);
    try {
      dokumenttipalvelu.save(
              UUID.randomUUID().toString(),
              "testfile.txt",
              new Date(),
              Collections.emptySet(),
              "text/plain",
              Files.newInputStream(Paths.get("src/test/resources/testfile.txt")),
              3
      );
      fail("Expected exception not thrown");
    } catch(CompletionException | RetryableException ignored) {}
  }

  @Test
  public void testSaveFailsWhenNonRetryableError() throws IOException {
    when(client.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
      .thenReturn(completedFuture(PutObjectResponse.builder().build()));
    when(client.getObjectAttributes(any(GetObjectAttributesRequest.class)))
      .thenThrow(new RuntimeException());
    when(client.listObjectsV2(any(ListObjectsV2Request.class)))
      .thenReturn(completedFuture(ListObjectsV2Response.builder().isTruncated(false).build()));
    try {
      dokumenttipalvelu.save(
              UUID.randomUUID().toString(),
              "testfile.txt",
              new Date(),
              Collections.emptySet(),
              "text/plain",
              Files.newInputStream(Paths.get("src/test/resources/testfile.txt")),
              3
      );
      fail("Expected exception not thrown");
    } catch(RuntimeException ignored) {
    }
  }
}
