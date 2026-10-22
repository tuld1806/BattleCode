package edu.uet.battlecode.db;

import java.net.URI;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
public final class R2ArtifactStore implements ArtifactStore, AutoCloseable {
    private final S3Presigner presigner;
    private final String bucket;
    private final String publicBase;

    public R2ArtifactStore(@Value("${battlecode.r2-endpoint}") String endpoint,
                           @Value("${battlecode.r2-access-key}") String accessKey,
                           @Value("${battlecode.r2-secret-key}") String secretKey,
                           @Value("${battlecode.r2-bucket}") String bucket,
                           @Value("${battlecode.r2-public-base}") String publicBase) {
        if (!endpoint.startsWith("https://") || !publicBase.startsWith("https://")) {
            throw new IllegalArgumentException("R2 endpoints must use HTTPS");
        }
        this.bucket = bucket;
        this.publicBase = publicBase.replaceAll("/$", "");
        this.presigner = S3Presigner.builder().endpointOverride(URI.create(endpoint))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Override public String presignPut(String key, String sha256) {
        byte[] digest = HexFormat.of().parseHex(sha256);
        PutObjectRequest put = PutObjectRequest.builder().bucket(bucket).key(key)
                .contentType("application/gzip")
                .checksumSHA256(Base64.getEncoder().encodeToString(digest)).build();
        return presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15)).putObjectRequest(put).build()).url().toString();
    }

    @Override public String artifactUrl(String key) { return publicBase + "/" + key; }

    @Override public void close() { presigner.close(); }
}
