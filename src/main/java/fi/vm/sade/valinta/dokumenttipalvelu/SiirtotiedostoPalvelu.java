package fi.vm.sade.valinta.dokumenttipalvelu;

import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectMetadata;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.sts.model.StsException;
import software.amazon.awssdk.utils.StringUtils;

public class SiirtotiedostoPalvelu extends Dokumenttipalvelu {
  private static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("Europe/Helsinki");

  @Override
  public S3AsyncClient getClient() {
    return S3AsyncClient.builder()
        .httpClientBuilder(NettyNioAsyncHttpClient.builder())
        .credentialsProvider(StaticCredentialsProvider.create(sessionCredentials()))
        .defaultsMode(DefaultsMode.IN_REGION)
        .region(Region.of(awsRegion))
        .build();
  }

  protected AwsSessionCredentials sessionCredentials() throws StsException {
    AssumeRoleResponse roleResponse;
    try (StsClient stsClient = StsClient.create()) {
      AssumeRoleRequest roleRequest =
          AssumeRoleRequest.builder()
              .roleArn(this.bucketTargetRoleArn)
              .roleSessionName(roleSessionName())
              .build();
      roleResponse = stsClient.assumeRole(roleRequest);
    }
    Credentials tempRoleCredentials = roleResponse.credentials();
    return AwsSessionCredentials.create(
        tempRoleCredentials.accessKeyId(),
        tempRoleCredentials.secretAccessKey(),
        tempRoleCredentials.sessionToken());
  }

  protected URI endpointOverride() {
    try {
      return new URI("http://localhost:4566");
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  protected String roleSessionName() {
    return "Siirtotiedosto-session-" + UUID.randomUUID();
  }

  private final String bucketTargetRoleArn;

  public SiirtotiedostoPalvelu(String awsRegion, String bucketName, String bucketTargetRoleArn) {
    super(awsRegion, bucketName);
    this.bucketTargetRoleArn = bucketTargetRoleArn;
  }

  private String timeStamp() {
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ssXX");
    return formatter.format(ZonedDateTime.now(DEFAULT_TIMEZONE));
  }

  private Collection<String> tags(String sourceSystem, String subCategory) {
    return StringUtils.isEmpty(subCategory)
        ? Arrays.asList(sourceSystem)
        : Arrays.asList(sourceSystem, subCategory);
  }

  @Override
  public String composeKey(final Collection<String> tags, final String documentId) {
    return String.format("%s/%s", tags.stream().findFirst().orElse("unknown"), documentId);
  }

  private boolean retryable(RuntimeException exp) {
    Throwable checkedException = exp.getCause() != null ? exp.getCause() : exp;
    if (checkedException instanceof SdkException) {
      SdkException sdkException = (SdkException) checkedException;
      return sdkException.retryable();
    }
    return false;
  }

  /**
   * Saves siirtotiedosto document.
   *
   * @param sourceSystem Source system of this siirtotiedosto, e.g. kouta, ataru, etc. Value is
   *     mandatory.
   * @param subCategory More specific description of the contents, e.g. haku, hakukohde, application
   *     etc. Value is mandatory.
   * @param additionalInfo Additional info to be shown in filename. Value is optional.
   * @param executionId Identifies the files created within certain siirtotiedosto -operation in
   *     source system. Useful in cases where several files were created in one operation. Value is
   *     mandatory
   * @param executionSubId Identifies the file among the files created within certain siirtotiedosto
   *     -operation in source system, in case several files were created in one operation.
   * @param data Document's data input stream. Value is mandatory.
   * @param retryCount Number of retries performed in case save operation failed, and failure was
   *     caused by some temporary / recoverable error
   * @return Metadata describing the document.
   */
  public ObjectMetadata saveSiirtotiedosto(
      final String sourceSystem,
      final String subCategory,
      final String additionalInfo,
      final String executionId,
      final int executionSubId,
      final InputStream data,
      final int retryCount) {

    if (StringUtils.isEmpty(sourceSystem)) {
      throw new IllegalArgumentException("Source system cannot be empty");
    }
    if (StringUtils.isEmpty(subCategory)) {
      throw new IllegalArgumentException("Subcategory cannot be empty");
    }
    if (StringUtils.isEmpty(executionId)) {
      throw new IllegalArgumentException("executionId cannot be empty");
    }
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null");
    }
    final String addInfoStr =
        StringUtils.isEmpty(additionalInfo) ? "" : String.format("_%s", additionalInfo);
    final String timestamp = timeStamp();
    final String executionIdStr = String.format("%s_%d", executionId, executionSubId);
    final String documentId =
        String.format(
            "%s_%s__%s_%s_%s.json",
            sourceSystem, subCategory, timestamp, addInfoStr, executionIdStr);
    final Collection<String> tags = tags(sourceSystem, subCategory);
    return saveWithRetry(documentId, tags, data, retryCount);
  }

  private ObjectMetadata saveWithRetry(
      String documentId, Collection<String> tags, InputStream data, int retryCount) {
    int retryNumber = 0;
    String key = composeKey(tags, documentId);
    SaveOperationData operationData = doPutObject(key, documentId, data);
    while (++retryNumber < retryCount && operationData.failed()) {
      operationData = doPutObject(key, documentId, data);
    }

    if (!operationData.failed()) {
      retryNumber = 0;
      operationData = doGetObjectAttributes(key, documentId);
      while (++retryNumber < retryCount && operationData.failed()) {
        operationData = doGetObjectAttributes(key, documentId);
      }
    }

    if (operationData.failed()) {
      throw new RuntimeException(operationData.getException());
    }
    return operationData.getObjectMetadata();
  }

  private SaveOperationData doPutObject(String key, String documentId, InputStream data) {
    try {
      return new SaveOperationData(putObject(key, documentId, "json", data).join());
    } catch (RuntimeException exp) {
      return throwOrReturnRetryableError(exp);
    }
  }

  private SaveOperationData doGetObjectAttributes(String key, String documentId) {
    try {
      return new SaveOperationData(getObjectAttributesASync(documentId, key).join());
    } catch (RuntimeException exp) {
      return throwOrReturnRetryableError(exp);
    }
  }

  private SaveOperationData throwOrReturnRetryableError(RuntimeException exp) {
    if (!retryable(exp)) {
      throw new RuntimeException(exp);
    }
    return new SaveOperationData(exp);
  }

  private static class SaveOperationData {
    private PutObjectResponse putResponse;
    private ObjectMetadata objectMetadata;
    private RuntimeException exception;

    public SaveOperationData(PutObjectResponse putResponse) {
      this.putResponse = putResponse;
      exception = null;
    }

    public SaveOperationData(ObjectMetadata objectMetadata) {
      this.objectMetadata = objectMetadata;
      exception = null;
    }

    public SaveOperationData(RuntimeException exception) {
      this.exception = exception;
    }

    public PutObjectResponse getPutResponse() {
      return putResponse;
    }

    public ObjectMetadata getObjectMetadata() {
      return objectMetadata;
    }

    public RuntimeException getException() {
      return exception;
    }

    public boolean failed() {
      return this.exception != null;
    }
  }
}
