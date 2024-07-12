package fi.vm.sade.valinta.dokumenttipalvelu;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectMetadata;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.OngoingStubbing;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.model.*;

public class SiirtotiedostoPalveluTest extends Testbase {
  private void mockSequenceForSave(
      int nbrOfRecoverablePutFailures, int nbrOfRecoverableGetAttributesFailures) {
    Testbase.RetryableException exception = new Testbase.RetryableException(SdkException.builder());

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
  public void testDocumentIdWithAdditionalInfo() throws IOException {
    mockSequenceForSave(0, 0);
    final ObjectMetadata metadata =
        siirtotiedostoPalvelu.saveSiirtotiedosto(
            "source",
            "sub",
            "someInfo",
            UUID.randomUUID().toString(),
            0,
            Files.newInputStream(Paths.get("src/test/resources/testfile.txt")),
            0);
    assertNotNull(metadata);
    assertTrue(metadata.documentId.startsWith("source_sub__"));
    assertTrue(metadata.documentId.contains("_someInfo_"));
  }

  @Test
  public void testSaveSucceedsAfterRetry() throws IOException {
    mockSequenceForSave(2, 2);
    final ObjectMetadata metadata =
        siirtotiedostoPalvelu.saveSiirtotiedosto(
            "source",
            "sub",
            "",
            UUID.randomUUID().toString(),
            0,
            Files.newInputStream(Paths.get("src/test/resources/testfile.txt")),
            3);
    assertNotNull(metadata);
    assertTrue(metadata.documentId.startsWith("source_sub__"));
  }

  @Test
  public void testSaveFailsAfterPutObjectRetries() throws IOException {
    mockSequenceForSave(3, 0);
    try {
      siirtotiedostoPalvelu.saveSiirtotiedosto(
          "source",
          "sub",
          "",
          UUID.randomUUID().toString(),
          0,
          Files.newInputStream(Paths.get("src/test/resources/testfile.txt")),
          2);
      fail("Expected exception not thrown");
    } catch (RuntimeException ignored) {
    }
  }

  @Test
  public void testSaveFailsAfterGetAttributesRetries() throws IOException {
    mockSequenceForSave(0, 4);
    try {
      siirtotiedostoPalvelu.saveSiirtotiedosto(
          "source",
          "sub",
          "",
          UUID.randomUUID().toString(),
          0,
          Files.newInputStream(Paths.get("src/test/resources/testfile.txt")),
          3);
      fail("Expected exception not thrown");
    } catch (RuntimeException ignored) {
    }
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
      siirtotiedostoPalvelu.saveSiirtotiedosto(
          "source",
          "sub",
          "",
          UUID.randomUUID().toString(),
          0,
          Files.newInputStream(Paths.get("src/test/resources/testfile.txt")),
          2);
      fail("Expected exception not thrown");
    } catch (RuntimeException ignored) {
    }
  }
}
