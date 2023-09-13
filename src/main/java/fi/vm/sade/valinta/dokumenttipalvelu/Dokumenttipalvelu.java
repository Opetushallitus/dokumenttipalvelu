package fi.vm.sade.valinta.dokumenttipalvelu;

import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectEntity;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectMetadata;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.internal.async.ByteBuffersAsyncRequestBody;
import software.amazon.awssdk.core.internal.async.InputStreamResponseTransformer;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Api for saving and fetching documents in AWS S3 backed storage.
 *
 * <p>Should be used as a singleton. For example with Spring, define a bean: <code>
 *      &#064;Bean
 *      public Dokumenttipalvelu dokumenttipalvelu(&#064;Value("${aws.region}") final String region,
 *                                                 &#064;Value("${aws.bucket.name}") final String bucketName) {
 *          return new Dokumenttipalvelu(region, bucketName);
 *      }
 * </code>
 */
public class Dokumenttipalvelu {
  private static final Logger LOGGER = LoggerFactory.getLogger(Dokumenttipalvelu.class);
  private static final String TAG_FORMAT = "t-%s";
  private static final String METADATA_FILENAME = "filename";
  private final String awsRegion;
  private final String bucketName;
  public Integer listObjectsMaxKeys = 1000;

  public Dokumenttipalvelu(final String awsRegion, final String bucketName) {
    this.awsRegion = awsRegion;
    this.bucketName = bucketName;
    LOGGER.info(
        "Dokumenttipalvelu initialized: awsRegion={}, bucketName={}", awsRegion, bucketName);
  }

  protected S3AsyncClient getClient() {
    return S3AsyncClient.builder()
        .httpClientBuilder(NettyNioAsyncHttpClient.builder())
        .credentialsProvider(DefaultCredentialsProvider.create())
        .defaultsMode(DefaultsMode.IN_REGION)
        .region(Region.of(awsRegion))
        .build();
  }

  protected S3Presigner getPresigner() {
    return S3Presigner.builder()
        .credentialsProvider(DefaultCredentialsProvider.create())
        .region(Region.of(awsRegion))
        .build();
  }

  private CompletableFuture<Collection<ObjectMetadata>> findRecursive(
      final Collection<String> tags,
      final Collection<ObjectMetadata> previousResults,
      final String nextMarker) {
    final ListObjectsV2Request.Builder requestBuilder =
        ListObjectsV2Request.builder().bucket(bucketName).maxKeys(listObjectsMaxKeys);
    if (nextMarker != null) {
      requestBuilder.continuationToken(nextMarker);
    }
    LOGGER.info(
        "findRecursive: tags={}, previousResults size={}, nextMarker={}",
        tags,
        previousResults.size(),
        nextMarker);
    return getClient()
        .listObjectsV2(requestBuilder.build())
        .thenComposeAsync(
            res -> {
              previousResults.addAll(
                  res.contents().stream()
                      .filter(
                          s3Object -> {
                            if (tags.isEmpty()) {
                              return true;
                            }
                            final Collection<String> objectTags = extractTags(s3Object.key());
                            return tags.stream().map(this::sanitize).allMatch(objectTags::contains);
                          })
                      .map(this::convert)
                      .collect(Collectors.toList()));
              if (res.isTruncated()) {
                return findRecursive(tags, previousResults, res.nextContinuationToken());
              } else {
                return CompletableFuture.completedFuture(previousResults);
              }
            });
  }

  /**
   * Finds documents where all the provided tags match. If tags collection is empty, returns all
   * objects.
   *
   * @param tags Collection of tags
   * @return Collection of metadata objects
   */
  public CompletableFuture<Collection<ObjectMetadata>> findAsync(final Collection<String> tags) {
    return findRecursive(tags, new ArrayList<>(), null);
  }

  /**
   * Finds documents where all the provided tags match. If tags collection is empty, returns all
   * objects.
   *
   * @param tags Collection of tags
   * @return Collection of metadata objects
   */
  public Collection<ObjectMetadata> find(final Collection<String> tags) {
    return findAsync(tags).join();
  }

  /**
   * Fetch (full) document with key.
   *
   * @param key Document's key
   * @return Full document with content and metadata
   * @throws java.util.concurrent.CompletionException with NoSuchKeyException as cause if document
   *     was not found with given key
   */
  public CompletableFuture<ObjectEntity> getAsync(final String key) {
    LOGGER.info("getAsync: key={}", key);
    return getClient()
        .getObject(
            GetObjectRequest.builder().bucket(bucketName).key(key).build(),
            new InputStreamResponseTransformer<>())
        .thenApply(
            inputStream -> {
              final GetObjectResponse response = inputStream.response();
              return new ObjectEntity(
                  inputStream,
                  extractDocumentId(key),
                  response.metadata().get(METADATA_FILENAME),
                  response.contentType(),
                  response.contentLength(),
                  extractTags(key),
                  response.expires());
            });
  }

  /**
   * Fetch (full) document with key.
   *
   * @param key Document's key
   * @return Full document with content and metadata
   * @throws java.util.concurrent.CompletionException with NoSuchKeyException as cause if document
   *     was not found
   */
  public ObjectEntity get(final String key) {
    return getAsync(key).join();
  }

  /**
   * Generates a download link, that works until the expiration date is reached.
   *
   * @param key Object key
   * @param expirationDuration How long the link should be active
   * @return Unauthenticated download URL
   */
  public URL getDownloadUrl(final String key, final Duration expirationDuration) {
    LOGGER.info("getDownloadUrl: key={}, expirationDuration={}", key, expirationDuration);
    return getPresigner()
        .presignGetObject(
            GetObjectPresignRequest.builder()
                .signatureDuration(expirationDuration)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucketName).key(key).build())
                .build())
        .url();
  }

  /**
   * Saves document.
   *
   * @param documentId Document's id, can be left null, then it will be generated as a new UUID
   * @param fileName File name, will be saved as part of document's metadata
   * @param expirationDate Date when the document will be removed
   * @param tags Collection of tags that the document can be searched with
   * @param contentType Document's content type
   * @param data Document's data input stream
   * @return Metadata describing the document
   */
  public CompletableFuture<ObjectMetadata> saveAsync(
      final String documentId,
      final String fileName,
      final Date expirationDate,
      final Collection<String> tags,
      final String contentType,
      final InputStream data) {
    final String id = documentId != null ? documentId : UUID.randomUUID().toString();
    final String key = composeKey(tags, id);
    LOGGER.info(
        "saveAsync: documentId={}, id={}, key={}, fileName={}, expirationDate={}, tags={}, contentType={}",
        documentId,
        id,
        key,
        fileName,
        expirationDate,
        tags,
        contentType);
    AsyncRequestBody body;
    try {
      body = ByteBuffersAsyncRequestBody.from(IoUtils.toByteArray(data));
    } catch (final IOException e) {
      throw new RuntimeException("Error reading input data", e);
    }
    return getClient()
        .putObject(
            PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .expires(expirationDate.toInstant())
                .contentType(contentType)
                .metadata(Collections.singletonMap(METADATA_FILENAME, fileName))
                .build(),
            body)
        .thenCompose(
            putObjectResponse ->
                getClient()
                    .getObjectAttributes(
                        GetObjectAttributesRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .objectAttributes(ObjectAttributes.E_TAG, ObjectAttributes.OBJECT_SIZE)
                            .build())
                    .thenApply(
                        attributesResponse ->
                            new ObjectMetadata(
                                key,
                                id,
                                extractTags(key),
                                attributesResponse.lastModified(),
                                attributesResponse.objectSize(),
                                attributesResponse.eTag())));
  }

  /**
   * Saves document.
   *
   * @param documentId Document's id, can be left null, then it will be generated as a new UUID
   * @param fileName File name, will be saved as part of document's metadata
   * @param expirationDate Date when the document will be removed
   * @param tags Collection of tags that the document can be searched with
   * @param contentType Document's content type
   * @param data Document's data input stream
   * @return Metadata describing the document
   */
  public ObjectMetadata save(
      final String documentId,
      final String fileName,
      final Date expirationDate,
      final Collection<String> tags,
      final String contentType,
      final InputStream data) {
    return saveAsync(documentId, fileName, expirationDate, tags, contentType, data).join();
  }

  /**
   * Renames an object to a new file name.
   *
   * @param key Existing object key
   * @param fileName New file name
   */
  public CompletableFuture<Void> renameAsync(final String key, final String fileName) {
    LOGGER.info("renameAsync: key={}, fileName={}", key, fileName);
    return getClient()
        .copyObject(
            CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .destinationBucket(bucketName)
                .sourceKey(key)
                .destinationKey(key)
                .metadataDirective(MetadataDirective.REPLACE)
                .metadata(Collections.singletonMap(METADATA_FILENAME, fileName))
                .build())
        .thenApply(response -> null);
  }

  /**
   * Renames an object to a new file name.
   *
   * @param key Existing object key
   * @param fileName New file name
   */
  public void rename(final String key, final String fileName) {
    renameAsync(key, fileName).join();
  }

  /**
   * Deletes object
   *
   * @param key Existing object key
   */
  public CompletableFuture<Void> deleteAsync(final String key) {
    LOGGER.info("deleteAsync: key={}", key);
    return getClient()
        .deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build())
        .thenApply(response -> null);
  }

  /**
   * Deletes object
   *
   * @param key Existing object key
   */
  public void delete(final String key) {
    deleteAsync(key).join();
  }

  ObjectMetadata convert(final S3Object object) {
    if (object == null) {
      throw new IllegalArgumentException("Object is null");
    }
    final String key = object.key();
    return new ObjectMetadata(
        key,
        extractDocumentId(key),
        extractTags(key),
        object.lastModified(),
        object.size(),
        object.eTag());
  }

  String sanitize(final String value) {
    if (value == null) {
      throw new IllegalArgumentException("Value is null");
    }
    return value.replaceAll("[^a-zA-Z0-9_.-]", "");
  }

  String composeKey(final Collection<String> tags, final String documentId) {
    if (tags == null) {
      throw new IllegalArgumentException("Tags is null");
    }
    if (documentId == null) {
      throw new IllegalArgumentException("DocumentId is null");
    }
    final Collection<String> keyParts =
        tags.stream()
            .filter(Objects::nonNull)
            .map(t -> String.format(TAG_FORMAT, t))
            .collect(Collectors.toList());
    keyParts.add(documentId);
    final String key = keyParts.stream().map(this::sanitize).collect(Collectors.joining("/"));
    if (key.length() > 1024) {
      throw new IllegalArgumentException("Key too long");
    }
    return key;
  }

  Collection<String> extractTags(final String key) {
    if (key == null) {
      throw new IllegalArgumentException("Key is null");
    }
    return Arrays.stream(key.split("/"))
        .filter(keyPart -> keyPart.startsWith("t-"))
        .map(tag -> tag.substring(2))
        .collect(Collectors.toSet());
  }

  String extractDocumentId(final String key) {
    if (key == null) {
      throw new IllegalArgumentException("Key is null");
    }
    final String[] keyParts = key.split("/");
    if (keyParts.length == 0) {
      throw new IllegalArgumentException("Invalid key");
    }
    return keyParts[keyParts.length - 1];
  }
}
