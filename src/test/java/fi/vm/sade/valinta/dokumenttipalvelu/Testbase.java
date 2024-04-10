package fi.vm.sade.valinta.dokumenttipalvelu;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

public abstract class Testbase {
  final S3AsyncClient client = mock(S3AsyncClient.class);
  final S3Presigner presigner = mock(S3Presigner.class);
  final String bucketName = "test-bucket";

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

  class MockSiirtotiedostoPalvelu extends SiirtotiedostoPalvelu {

    public MockSiirtotiedostoPalvelu(String awsRegion, String bucketName) {
      super(awsRegion, bucketName, "");
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

  final Dokumenttipalvelu dokumenttipalvelu = new MockDokumenttipalvelu("eu-west-1", bucketName);

  final SiirtotiedostoPalvelu siirtotiedostoPalvelu =
      new MockSiirtotiedostoPalvelu("eu-west-1", bucketName);

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
}
