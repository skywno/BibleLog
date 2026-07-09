CREATE KEYSPACE IF NOT EXISTS biblelog
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

USE biblelog;

CREATE TABLE IF NOT EXISTS notes_by_author (
    author_id    TEXT,
    created_at   TIMESTAMP,
    note_id      TEXT,
    content      TEXT,
    prayer_topic TEXT,
    emotion      TEXT,
    visibility   TEXT,
    church_id    TEXT,
    group_ids    SET<TEXT>,
    reference    TEXT,
    updated_at   TIMESTAMP,
    deleted_at   TIMESTAMP,
    PRIMARY KEY ((author_id), created_at, note_id)
) WITH CLUSTERING ORDER BY (created_at DESC, note_id DESC);

CREATE TABLE IF NOT EXISTS notes_by_id (
    note_id      TEXT PRIMARY KEY,
    author_id    TEXT,
    created_at   TIMESTAMP,
    content      TEXT,
    prayer_topic TEXT,
    emotion      TEXT,
    visibility   TEXT,
    church_id    TEXT,
    group_ids    SET<TEXT>,
    reference    TEXT,
    updated_at   TIMESTAMP,
    deleted_at   TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notes_by_visibility (
    visibility   TEXT,
    bucket       TEXT,
    created_at   TIMESTAMP,
    note_id      TEXT,
    author_id    TEXT,
    PRIMARY KEY ((visibility, bucket), created_at, note_id)
) WITH CLUSTERING ORDER BY (created_at DESC, note_id DESC);

CREATE TABLE IF NOT EXISTS reactions_by_note (
    note_id       TEXT,
    user_id       TEXT,
    reaction_type TEXT,
    created_at    TIMESTAMP,
    PRIMARY KEY (note_id, user_id)
);

CREATE TABLE IF NOT EXISTS comments_by_note (
    note_id       TEXT,
    created_at    TIMESTAMP,
    comment_id    TEXT,
    author_id     TEXT,
    content       TEXT,
    PRIMARY KEY (note_id, created_at, comment_id)
) WITH CLUSTERING ORDER BY (created_at ASC, comment_id ASC);

CREATE TABLE IF NOT EXISTS reading_records_by_user (
    user_id       TEXT,
    date          DATE,
    record_id     TEXT,
    book_id       SMALLINT,
    start_chapter SMALLINT,
    start_verse   SMALLINT,
    end_chapter   SMALLINT,
    end_verse     SMALLINT,
    minutes_read  INT,
    created_at    TIMESTAMP,
    reference_json TEXT,
    PRIMARY KEY ((user_id), date, record_id)
) WITH CLUSTERING ORDER BY (date DESC, record_id DESC);
