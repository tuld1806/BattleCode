CREATE TABLE profiles (
    id UUID PRIMARY KEY,
    display_name VARCHAR(120) NOT NULL DEFAULT '',
    role VARCHAR(16) NOT NULL DEFAULT 'STUDENT' CHECK (role IN ('STUDENT','ADMIN')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE tournaments (
    id UUID PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'REGISTRATION_OPEN'
        CHECK (status IN ('REGISTRATION_OPEN','SUBMISSION_FROZEN','RUNNING_MATCHES','COMPLETED','PUBLISHED')),
    map_size INTEGER NOT NULL DEFAULT 16 CHECK (map_size = 16),
    max_ticks INTEGER NOT NULL DEFAULT 200 CHECK (max_ticks BETWEEN 1 AND 2000),
    seed BIGINT NOT NULL,
    registration_deadline TIMESTAMPTZ,
    submission_deadline TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE teams (
    id UUID PRIMARY KEY,
    tournament_id UUID NOT NULL REFERENCES tournaments(id),
    name VARCHAR(100) NOT NULL,
    captain_id UUID NOT NULL REFERENCES profiles(id),
    seed INTEGER CHECK (seed BETWEEN 1 AND 16),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (id, tournament_id),
    UNIQUE (tournament_id, name),
    UNIQUE (tournament_id, seed)
);

CREATE TABLE team_members (
    team_id UUID NOT NULL,
    tournament_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES profiles(id),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (team_id, user_id),
    UNIQUE (tournament_id, user_id),
    FOREIGN KEY (team_id, tournament_id) REFERENCES teams(id, tournament_id)
);

CREATE TABLE submissions (
    id UUID PRIMARY KEY,
    tournament_id UUID NOT NULL,
    team_id UUID NOT NULL,
    submitted_by UUID NOT NULL REFERENCES profiles(id),
    repo_url VARCHAR(500) NOT NULL,
    commit_sha CHAR(40) NOT NULL CHECK (commit_sha ~ '^[0-9a-f]{40}$'),
    language VARCHAR(8) NOT NULL CHECK (language IN ('python','cpp','java')),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','PASSED','FAILED')),
    artifact_url VARCHAR(1000),
    artifact_sha256 CHAR(64),
    validation_log_url VARCHAR(1000),
    compile_log_url VARCHAR(1000),
    error_summary VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    FOREIGN KEY (team_id, tournament_id) REFERENCES teams(id, tournament_id),
    UNIQUE (team_id, commit_sha)
);

CREATE FUNCTION prevent_submission_identity_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.commit_sha <> OLD.commit_sha OR NEW.repo_url <> OLD.repo_url OR
       NEW.team_id <> OLD.team_id OR NEW.language <> OLD.language THEN
        RAISE EXCEPTION 'submission identity is immutable';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER submission_identity_immutable BEFORE UPDATE ON submissions
    FOR EACH ROW EXECUTE FUNCTION prevent_submission_identity_change();

CREATE TABLE ci_jobs (
    submission_id UUID PRIMARY KEY REFERENCES submissions(id),
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    artifact_key VARCHAR(500) NOT NULL,
    artifact_sha256 CHAR(64)
);

CREATE TABLE tournament_entries (
    tournament_id UUID NOT NULL REFERENCES tournaments(id),
    team_id UUID NOT NULL,
    submission_id UUID NOT NULL UNIQUE REFERENCES submissions(id),
    seed INTEGER NOT NULL CHECK (seed BETWEEN 1 AND 16),
    PRIMARY KEY (tournament_id, team_id),
    UNIQUE (tournament_id, seed),
    FOREIGN KEY (team_id, tournament_id) REFERENCES teams(id, tournament_id)
);

CREATE TABLE rounds (
    id UUID PRIMARY KEY,
    tournament_id UUID NOT NULL REFERENCES tournaments(id),
    round_number INTEGER NOT NULL CHECK (round_number BETWEEN 1 AND 4),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','RUNNING','COMPLETED')),
    UNIQUE (tournament_id, round_number)
);

CREATE TABLE series (
    id UUID PRIMARY KEY,
    round_id UUID NOT NULL REFERENCES rounds(id),
    position INTEGER NOT NULL,
    team_one_id UUID REFERENCES teams(id),
    team_two_id UUID REFERENCES teams(id),
    winner_team_id UUID REFERENCES teams(id),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','RUNNING','COMPLETED')),
    UNIQUE (round_id, position),
    CHECK (team_one_id IS NULL OR team_one_id <> team_two_id)
);

CREATE TABLE matches (
    id UUID PRIMARY KEY,
    series_id UUID REFERENCES series(id),
    game_number INTEGER NOT NULL CHECK (game_number BETWEEN 1 AND 3),
    team_one_id UUID REFERENCES teams(id),
    team_two_id UUID REFERENCES teams(id),
    submission_one_id UUID REFERENCES submissions(id),
    submission_two_id UUID REFERENCES submissions(id),
    random_seed BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'QUEUED'
        CHECK (status IN ('QUEUED','RUNNING','COMPLETED','SYSTEM_ERROR')),
    worker_id VARCHAR(100),
    lease_expires_at TIMESTAMPTZ,
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count BETWEEN 0 AND 3),
    winner_slot INTEGER CHECK (winner_slot IN (1,2)),
    total_ticks INTEGER,
    replay_data_url VARCHAR(1000),
    log_url VARCHAR(1000),
    error_summary VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    UNIQUE (series_id, game_number)
);

CREATE INDEX matches_claim_idx ON matches(status, lease_expires_at, created_at)
    WHERE status IN ('QUEUED','RUNNING');
CREATE INDEX submissions_team_latest_idx ON submissions(team_id, created_at DESC);
CREATE INDEX matches_series_idx ON matches(series_id, game_number);
