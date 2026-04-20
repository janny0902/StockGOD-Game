\connect cldc_ai_collector;

-- Core user session for anonymous gameplay data collection.
CREATE TABLE IF NOT EXISTS game_sessions (
    session_id BIGSERIAL PRIMARY KEY,
    nickname_base VARCHAR(40) NOT NULL,
    nickname_tag INTEGER NOT NULL,
    nickname_display VARCHAR(64) NOT NULL,
    language_code VARCHAR(8) NOT NULL DEFAULT 'ko',
    country_code CHAR(2) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ended_at TIMESTAMP,
    initial_capital NUMERIC(18,2) NOT NULL DEFAULT 10000000,
    final_capital NUMERIC(18,2),
    total_return_rate NUMERIC(9,4),
    solved_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT uq_session_nickname_display UNIQUE (nickname_display)
);

CREATE INDEX IF NOT EXISTS idx_game_sessions_started_at ON game_sessions(started_at DESC);
CREATE INDEX IF NOT EXISTS idx_game_sessions_country_code ON game_sessions(country_code);

-- Problem instances selected from historical candles (older than one month).
CREATE TABLE IF NOT EXISTS game_problems (
    problem_id BIGSERIAL PRIMARY KEY,
    stock_code VARCHAR(12) NOT NULL,
    problem_date DATE NOT NULL,
    reveal_max_date DATE NOT NULL,
    chart_timeframe VARCHAR(8) NOT NULL DEFAULT '1d',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_problem_stock_date_tf UNIQUE (stock_code, problem_date, chart_timeframe)
);

CREATE INDEX IF NOT EXISTS idx_game_problems_date ON game_problems(problem_date);

-- One session has up to 15 ordered attempts.
CREATE TABLE IF NOT EXISTS session_problem_attempts (
    attempt_id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES game_sessions(session_id) ON DELETE CASCADE,
    problem_id BIGINT NOT NULL REFERENCES game_problems(problem_id) ON DELETE RESTRICT,
    sequence_no INTEGER NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    submitted_at TIMESTAMP,
    timed_out BOOLEAN NOT NULL DEFAULT FALSE,
    answer_horizon_days INTEGER NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    action_ratio NUMERIC(5,2) NOT NULL,
    reason_text TEXT,
    reason_card_used BOOLEAN NOT NULL DEFAULT FALSE,
    capital_before NUMERIC(18,2) NOT NULL,
    capital_after NUMERIC(18,2),
    return_rate NUMERIC(9,4),
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    CONSTRAINT uq_attempt_session_seq UNIQUE (session_id, sequence_no)
);

CREATE INDEX IF NOT EXISTS idx_attempts_session ON session_problem_attempts(session_id);
CREATE INDEX IF NOT EXISTS idx_attempts_status ON session_problem_attempts(status);

-- Selected reason cards for explainability labels.
CREATE TABLE IF NOT EXISTS attempt_reason_cards (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT NOT NULL REFERENCES session_problem_attempts(attempt_id) ON DELETE CASCADE,
    card_key VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_attempt_reason_card UNIQUE (attempt_id, card_key)
);

-- Indicator usage and context values selected by users.
CREATE TABLE IF NOT EXISTS attempt_indicator_usage (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT NOT NULL REFERENCES session_problem_attempts(attempt_id) ON DELETE CASCADE,
    indicator_key VARCHAR(64) NOT NULL,
    usage_type VARCHAR(40) NOT NULL,
    indicator_value NUMERIC(18,6),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_attempt_indicator UNIQUE (attempt_id, indicator_key, usage_type)
);

-- Minutely leaderboard snapshots for fast serving.
CREATE TABLE IF NOT EXISTS leaderboard_snapshots (
    snapshot_id BIGSERIAL PRIMARY KEY,
    snapshot_time TIMESTAMP NOT NULL,
    rank_no INTEGER NOT NULL,
    session_id BIGINT NOT NULL REFERENCES game_sessions(session_id) ON DELETE CASCADE,
    nickname_display VARCHAR(64) NOT NULL,
    country_code CHAR(2) NOT NULL,
    final_capital NUMERIC(18,2) NOT NULL,
    total_return_rate NUMERIC(9,4) NOT NULL,
    percentile NUMERIC(6,3),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_leaderboard_snapshot UNIQUE (snapshot_time, rank_no)
);

CREATE INDEX IF NOT EXISTS idx_leaderboard_snapshot_time ON leaderboard_snapshots(snapshot_time DESC);
CREATE INDEX IF NOT EXISTS idx_leaderboard_session ON leaderboard_snapshots(session_id, snapshot_time DESC);

-- Local candle snapshot used by game problem generation.
CREATE TABLE IF NOT EXISTS daily_candles (
    candle_id BIGSERIAL PRIMARY KEY,
    stock_code VARCHAR(12) NOT NULL,
    base_date DATE NOT NULL,
    open_price NUMERIC(18,4),
    high_price NUMERIC(18,4),
    low_price NUMERIC(18,4),
    close_price NUMERIC(18,4),
    acc_trade_vol BIGINT,
    acc_trade_val BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_daily_candle_game UNIQUE (stock_code, base_date)
);

CREATE INDEX IF NOT EXISTS idx_game_daily_candles_code_date ON daily_candles(stock_code, base_date DESC);

-- Prototype problem bundles (5 sets x 15 questions).
CREATE TABLE IF NOT EXISTS problem_sets (
    problem_set_id BIGSERIAL PRIMARY KEY,
    set_name VARCHAR(40) NOT NULL,
    total_questions INTEGER NOT NULL DEFAULT 15,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_problem_set_name UNIQUE (set_name)
);

CREATE TABLE IF NOT EXISTS problem_set_items (
    id BIGSERIAL PRIMARY KEY,
    problem_set_id BIGINT NOT NULL REFERENCES problem_sets(problem_set_id) ON DELETE CASCADE,
    sequence_no INTEGER NOT NULL,
    problem_id BIGINT NOT NULL REFERENCES game_problems(problem_id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_problem_set_sequence UNIQUE (problem_set_id, sequence_no),
    CONSTRAINT uq_problem_set_problem UNIQUE (problem_set_id, problem_id)
);

ALTER TABLE game_sessions
    ADD COLUMN IF NOT EXISTS problem_set_id BIGINT REFERENCES problem_sets(problem_set_id);
