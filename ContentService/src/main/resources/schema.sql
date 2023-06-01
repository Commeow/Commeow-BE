CREATE TABLE channel
(
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    streamer         VARCHAR(255) NOT NULL,
    chatting_address VARCHAR(255) NOT NULL,
    on_air           BOOLEAN      NOT NULL
);

CREATE TABLE member
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    nickname   VARCHAR(255) NOT NULL,
    stream_key VARCHAR(255) NOT NULL,
    role       VARCHAR(50)  NOT NULL
);
