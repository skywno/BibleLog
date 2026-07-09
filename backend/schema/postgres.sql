CREATE TABLE IF NOT EXISTS users (
    id         TEXT PRIMARY KEY,
    nickname   TEXT NOT NULL,
    bio        TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    token      TEXT PRIMARY KEY,
    user_id    TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS friendships (
    user_id    TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    friend_id  TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, friend_id)
);

CREATE TABLE IF NOT EXISTS user_memberships (
    user_id    TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    church_id  TEXT,
    group_ids  TEXT[] NOT NULL DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS oauth_states (
    state         TEXT PRIMARY KEY,
    redirect_uri  TEXT NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ai_conversations (
    id         TEXT PRIMARY KEY,
    user_id    TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    mode       TEXT NOT NULL DEFAULT 'chat',
    title      TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS ai_messages (
    id                   TEXT PRIMARY KEY,
    conversation_id      TEXT NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    content              TEXT NOT NULL,
    is_from_user         BOOLEAN NOT NULL,
    timestamp            TIMESTAMPTZ NOT NULL,
    suggested_reference  JSONB
);

CREATE INDEX IF NOT EXISTS idx_ai_conversations_user ON ai_conversations(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_messages_conversation ON ai_messages(conversation_id);
