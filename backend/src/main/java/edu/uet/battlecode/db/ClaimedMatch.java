package edu.uet.battlecode.db;

import java.time.Instant;
import java.util.UUID;

public record ClaimedMatch(UUID id, String workerId, int attemptCount, Instant leaseExpiresAt) {}
