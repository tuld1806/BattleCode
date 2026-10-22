package edu.uet.battlecode.db;

import java.util.UUID;

public record CiJobRequest(UUID submissionId, String repoUrl, String commitSha,
                           String language, String token) {}
