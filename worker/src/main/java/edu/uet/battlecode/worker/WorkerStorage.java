package edu.uet.battlecode.worker;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public final class WorkerStorage implements AutoCloseable {
    private static final int MAX_ARTIFACT = 500 * 1024;
    private static final int MAX_REPLAY = 1024 * 1024;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private final S3Client s3;
    private final String bucket;
    private final String publicBase;

    public WorkerStorage(String endpoint, String accessKey, String secretKey, String bucket, String publicBase) {
        if (!endpoint.startsWith("https://") || !publicBase.startsWith("https://")) {
            throw new IllegalArgumentException("R2 endpoints must use HTTPS");
        }
        this.bucket = bucket;
        this.publicBase = publicBase.replaceAll("/$", "");
        this.s3 = S3Client.builder().endpointOverride(URI.create(endpoint)).region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build()).build();
    }

    public byte[] downloadArtifact(String url) throws IOException, InterruptedException {
        if (url == null || !url.startsWith(publicBase + "/artifacts/")) {
            throw new IllegalArgumentException("Artifact URL is outside the configured R2 bucket");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build();
        HttpResponse<java.io.InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (var input = response.body()) {
            if (response.statusCode() != 200) throw new IOException("Artifact download HTTP " + response.statusCode());
            byte[] bytes = input.readNBytes(MAX_ARTIFACT + 1);
            if (bytes.length > MAX_ARTIFACT) throw new IOException("Artifact exceeds 500 KiB");
            return bytes;
        }
    }

    public String uploadReplay(UUID matchId, byte[] replay) {
        if (replay.length > MAX_REPLAY) throw new IllegalArgumentException("Replay exceeds 1 MiB");
        String key = "replays/" + matchId + ".json.gz";
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key)
                .contentType("application/gzip").build(), RequestBody.fromBytes(replay));
        return publicBase + "/" + key;
    }

    @Override public void close() { s3.close(); }
}
