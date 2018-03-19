drop database if exists brs_master;
create database brs_master
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
use brs_master;
CREATE TABLE version(
    next_update INT NOT NULL
);
INSERT INTO version (next_update) VALUES (170);

CREATE TABLE alias(
    db_id BIGINT AUTO_INCREMENT,
    id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    alias_name VARCHAR(100) NOT NULL,
    alias_name_LOWER VARCHAR(100) NOT NULL DEFAULT '',
    alias_uri TEXT NOT NULL,
    timestamp INT NOT NULL,
    height INT NOT NULL,
    latest BOOLEAN DEFAULT TRUE NOT NULL,
    PRIMARY KEY (db_id)
);
CREATE TRIGGER lower_alias_name_insert BEFORE INSERT ON alias FOR EACH ROW SET NEW.alias_name_lower = LOWER(NEW.alias_name);
CREATE TRIGGER lower_alias_name_update BEFORE UPDATE ON alias FOR EACH ROW SET NEW.alias_name_lower = LOWER(NEW.alias_name);

CREATE UNIQUE INDEX alias_id_height_idx ON alias(id, height DESC);
CREATE INDEX alias_account_id_idx ON alias(account_id, height DESC);
CREATE INDEX alias_name_lower_idx ON alias(alias_name_lower);


CREATE TABLE account(
    db_id BIGINT AUTO_INCREMENT,
    id BIGINT NOT NULL,
    creation_HEIGHT INT NOT NULL,
    public_key VARBINARY(32),
    key_height INT,
    balance BIGINT NOT NULL,
    unconfirmed_balance BIGINT NOT NULL,
    forged_balance BIGINT NOT NULL,
    name VARCHAR(100),
    description TEXT,
    height INT NOT NULL,
    latest BOOLEAN DEFAULT TRUE NOT NULL,
    PRIMARY KEY (db_id)
);
CREATE UNIQUE INDEX account_id_height_idx ON account(id, height DESC);
CREATE INDEX account_id_balance_height_idx ON account(id, balance, height DESC);

CREATE TABLE alias_offer(
    db_id BIGINT AUTO_INCREMENT,
    id BIGINT NOT NULL,
    price BIGINT NOT NULL,
    buyer_ID BIGINT,
    height INT NOT NULL,
    latest BOOLEAN DEFAULT TRUE NOT NULL,
    PRIMARY KEY (db_id)
);

-- 151 +/- SELECT COUNT(*) FROM ALIAS_OFFER;
CREATE UNIQUE INDEX alias_offer_id_height_idx ON alias_offer(id, height DESC);
CREATE TABLE peer(
    address VARCHAR(100) NOT NULL,
    primary KEY (ADDRESS)
);

-- 1716 +/- SELECT COUNT(*) FROM PEER;
CREATE TABLE transaction(
    db_id BIGINT AUTO_INCREMENT,
    id BIGINT NOT NULL,
    deadline SMALLINT NOT NULL,
    sender_public_key VARBINARY(32) NOT NULL,
    recipient_id BIGINT,
    amount BIGINT NOT NULL,
    fee BIGINT NOT NULL,
    height INT NOT NULL,
    block_id BIGINT NOT NULL,
    signature VARBINARY(64),
    timestamp INT NOT NULL,
    type TINYINT NOT NULL,
    subtype TINYINT NOT NULL,
    sender_id BIGINT NOT NULL,
    block_timestamp INT NOT NULL,
    full_hash VARBINARY(32) NOT NULL,
    referenced_transaction_full_hash VARBINARY(32),
    attachment_bytes BLOB,
    version TINYINT NOT NULL,
    has_message BOOLEAN DEFAULT FALSE NOT NULL,
    has_encrypted_message BOOLEAN DEFAULT FALSE NOT NULL,
    has_public_key_announcement BOOLEAN DEFAULT FALSE NOT NULL,
    ec_block_height INT DEFAULT NULL,
    ec_block_id BIGINT DEFAULT NULL,
    has_encrypttoself_message BOOLEAN DEFAULT FALSE NOT NULL,
    PRIMARY KEY (db_id)
);

-- 5808373 +/- SELECT COUNT(*) FROM TRANSACTION;
CREATE INDEX transaction_block_timestamp_idx ON transaction(block_timestamp DESC);
CREATE UNIQUE INDEX transaction_id_idx ON transaction(id);
CREATE INDEX transaction_sender_id_idx ON transaction(sender_ID);
CREATE UNIQUE INDEX transaction_full_hash_idx ON transaction(full_hash);
CREATE INDEX transaction_recipient_id_idx ON transaction(recipient_id);
CREATE INDEX transaction_recipient_id_amount_height_idx ON transaction(recipient_id, amount, height);
CREATE TABLE asset(
    db_id BIGINT AUTO_INCREMENT,
    id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    name VARCHAR(10) NOT NULL,
    description TEXT,
    quantity BIGINT NOT NULL,
    decimals TINYINT NOT NULL,
    height INT NOT NULL,
    PRIMARY KEY (db_id)
);

-- 298 +/- SELECT COUNT(*) FROM ASSET;
CREATE UNIQUE INDEX asset_id_idx ON asset(id);
CREATE INDEX asset_account_id_idx ON asset(account_id);
CREATE TABLE trade(
    db_id BIGINT AUTO_INCREMENT,
    asset_id BIGINT NOT NULL,
    block_id BIGINT NOT NULL,
    ask_order_id BIGINT NOT NULL,
    bid_order_id BIGINT NOT NULL,
    ask_order_height INT NOT NULL,
    bid_order_height INT NOT NULL,
    seller_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    quantity BIGINT NOT NULL,
    price BIGINT NOT NULL,
    timestamp INT NOT NULL,
    height INT NOT NULL,
    PRIMARY KEY (db_id)
);

-- 85455 +/- SELECT COUNT(*) FROM TRADE;
CREATE UNIQUE INDEX trade_ask_bid_idx ON trade(ask_order_id, bid_order_id);
CREATE INDEX trade_asset_id_idx ON trade(asset_id, height DESC);
CREATE INDEX trade_seller_id_idx ON trade(seller_id, height DESC);
CREATE INDEX trade_buyer_id_idx ON trade(buyer_id, height DESC);
CREATE TABLE ask_order(
    db_id BIGINT AUTO_INCREMENT,
    id bigint NOT NULL,
    account_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    price BIGINT NOT NULL,
    quantity BIGINT NOT NULL,
    creation_height INT NOT NULL,
    height INT NOT NULL,
    latest BOOLEAN DEFAULT TRUE NOT NULL,
    PRIMARY KEY (db_id)
);

-- 3532 +/- SELECT COUNT(*) FROM ASK_ORDER;
CREATE UNIQUE INDEX ask_order_id_height_idx ON ask_order(id, height DESC);
CREATE INDEX ask_order_account_id_idx ON ask_order(account_id, height DESC);
CREATE INDEX ask_order_asset_id_price_idx ON ask_order(asset_id, price);
CREATE INDEX ask_order_creation_idx ON ask_order(creation_height DESC);
CREATE TABLE bid_order(
    db_id BIGINT AUTO_INCREMENT,
    id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    price BIGINT NOT NULL,
    quantity BIGINT NOT NULL,
    creation_height INT NOT NULL,
    height INT NOT NULL,
    latest BOOLEAN DEFAULT TRUE NOT NULL,
    PRIMARY KEY (db_id)
);

-- 2015 +/- SELECT COUNT(*) FROM BID_ORDER;
CREATE UNIQUE INDEX bid_order_id_height_idx ON bid_order(id, height DESC);
CREATE INDEX bid_order_account_id_idx ON bid_order(account_id, height DESC);
CREATE INDEX bid_order_asset_id_price_idx ON bid_order(asset_id, price DESC);
CREATE INDEX bid_order_creation_idx ON bid_order(creation_height DESC);
CREATE TABLE goods(
    db_id BIGINT AUTO_INCREMENT,
    ID BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    tags VARCHAR(100),
    timestamp INT NOT NULL,
    quantity INT NOT NULL,
    price BIGINT NOT NULL,
    delisted BOOLEAN NOT NULL,
    height INT NOT NULL,
    latest BOOLEAN DEFAULT TRUE NOT NULL,
    PRIMARY KEY (db_id)
);

-- 4698 +/- SELECT COUNT(*) FROM GOODS;
CREATE UNIQUE INDEX goods_id_height_idx ON goods(id, height DESC);
CREATE INDEX goods_seller_id_name_idx ON goods(seller_id, NAME);
CREATE index goods_timestamp_idx ON goods(timestamp DESC, height DESC);
CREATE TABLE purchase(
    db_id BIGINT AUTO_INCREMENT,
    id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    goods_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price BIGINT NOT NULL,
    deadline INT NOT NULL,
    note BLOB,
    nonce VARBINARY(32),
    timestamp INT NOT NULL,
    pending BOOLEAN NOT NULL,
    goods BLOB,
    goods_nonce VARBINARY(32),
    refund_note BLOB,
    refund_nonce VARBINARY(32),
    has_feedback_notes BOOLEAN DEFAULT FALSE NOT NULL,
    has_public_feedbacks BOOLEAN DEFAULT FALSE NOT NULL,
    discount BIGINT NOT NULL,
    refund BIGINT NOT NULL,
    height INT NOT NULL,
    latest BOOLEAN DEFAULT TRUE NOT NULL,
    PRIMARY KEY (db_id)
);

-- 120 +/- SELECT COUNT(*) FROM PURCHASE;
CREATE UNIQUE INDEX purchase_id_height_idx ON purchase(id, height DESC);
CREATE INDEX purchase_buyer_id_height_idx ON purchase(buyer_id, height DESC);
CREATE INDEX purchase_seller_id_height_idx ON purchase(seller_id, height DESC);
CREATE INDEX purchase_deadline_idx ON purchase(deadline DESC, height DESC);
CREATE INDEX purchase_timestamp_idx ON purchase(timestamp DESC, id);
CREATE TABLE account_asset(
    db_id BIGINT AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    quantity BIGINT NOT NULL,
    unconfirmed_quantity BIGINT NOT NULL,
    height INT NOT NULL,
    latest BOOLEAN DEFAULT TRUE NOT NULL,
    PRIMARY KEY (db_id)
);

-- 24744 +/- SELECT COUNT(*) FROM ACCOUNT_ASSET;
CREATE UNIQUE INDEX account_asset_id_height_idx ON account_asset(account_id, asset_id, height DESC);
CREATE INDEX account_asset_quantity_idx ON account_asset(quantity DESC);
CREATE TABLE purchase_feedback(
    db_id BIGINT AUTO_INCREMENT,
    id BIGINT NOT NULL,
    feedback_data BLOB NOT NULL,
    feedback_nonce VARBINARY(32) NOT NULL,
    height INT NOT NULL,
    latest BOOLEAN DEFAULT TRUE NOT NULL,
    PRIMARY KEY (db_id)
);

-- 0 +/- SELECT COUNT(*) FROM PURCHASE_FEEDBACK;
CREATE INDEX purchase_feedback_id_height_idx ON purchase_feedback(id, height DESC);
CREATE TABLE purchase_public_feedback(
    db_id BIGINT AUTO_INCREMENT,
    id BIGINT NOT NULL,
    public_feedback TEXT NOT NULL,
    height INT NOT NULL,
    latest BOOLEAN DEFAULT TRUE NOT NULL,
    PRIMARY KEY (db_id)
);

-- 0 +/- SELECT COUNT(*) FROM PURCHASE_PUBLIC_FEEDBACK;
CREATE INDEX purchase_public_feedback_id_height_idx ON purchase_public_feedback(id, height DESC);
CREATE TABLE unconfirmed_transaction(
    db_id BIGINT AUTO_INCREMENT,
    id BIGINT NOT NULL,
    expiration INT NOT NULL,
    transaction_height INT NOT NULL,
    fee_per_byte BIGINT NOT NULL,
    timestamp INT NOT NULL,
    transaction_bytes BLOB NOT NULL,
    height INT NOT NULL,
    PRIMARY KEY (db_id)
);

-- 0 +/- SELECT COUNT(*) FROM UNCONFIRMED_TRANSACTION;
CREATE UNIQUE INDEX unconfirmed_transaction_id_idx ON unconfirmed_transaction(id);
CREATE INDEX unconfirmed_transaction_height_fee_timestamp_idx ON unconfirmed_transaction(transaction_height, fee_per_byte DESC, timestamp);
CREATE TABLE asset_transfer(
    db_id BIGINT AUTO_INCREMENT,
    id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    quantity BIGINT NOT NULL,
    timestamp INT NOT NULL,
    height INT NOT NULL,
    PRIMARY KEY (db_id)
);

-- 36109 +/- SELECT COUNT(*) FROM ASSET_TRANSFER;
CREATE UNIQUE INDEX asset_transfer_id_idx ON asset_transfer(id);
CREATE INDEX asset_transfer_asset_id_idx ON asset_transfer(asset_id, height DESC);
CREATE INDEX asset_transfer_sender_id_idx ON asset_transfer(sender_id, height DESC);
CREATE INDEX asset_transfer_recipient_id_idx ON asset_transfer(recipient_id, height DESC);
CREATE TABLE reward_recip_assign(
    db_id BIGINT AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    prev_recip_id BIGINT NOT NULL,
    recip_id BIGINT NOT NULL,
    from_height INT NOT NULL,
    height INT NOT NULL,
    latest BOOLEAN DEFAULT TRUE NOT NULL,
    PRIMARY KEY (db_id)
);

-- 35767 +/- SELECT COUNT(*) FROM REWARD_RECIP_ASSIGN;
CREATE UNIQUE INDEX reward_recip_assign_account_id_height_idx ON reward_recip_assign(account_id, height DESC);
CREATE INDEX reward_recip_assign_recip_id_height_idx ON reward_recip_assign(recip_id, height DESC);
CREATE TABLE escrow(
    db_id BIGINT AUTO_INCREMENT,
    id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    required_signers INT,
    deadline INT NOT NULL,
    deadline_action INT NOT NULL,
    height INT NOT NULL,
    latest BOOLEAN DEFAULT TRUE NOT NULL,
    PRIMARY KEY (db_id)
);

-- 0 +/- SELECT COUNT(*) FROM ESCROW;
CREATE UNIQUE INDEX escrow_id_height_idx ON escrow(id, height DESC);
CREATE INDEX escrow_sender_id_height_idx ON escrow(sender_id, height DESC);
CREATE INDEX escrow_recipient_id_height_idx ON escrow(recipient_id, height DESC);
CREATE INDEX escrow_deadline_height_idx ON escrow(deadline, height DESC);
CREATE TABLE escrow_decision(
    db_id BIGINT AUTO_INCREMENT,
    escrow_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    decision int NOT NULL,
    height INT NOT NULL,
    latest BOOLEAN DEFAULT TRUE NOT NULL,
    PRIMARY KEY (db_id)
);

-- 0 +/- SELECT COUNT(*) FROM ESCROW_DECISION;
CREATE UNIQUE INDEX escrow_decision_escrow_id_account_id_height_idx ON escrow_decision(escrow_id, account_id, height DESC);
CREATE INDEX escrow_decision_escrow_id_height_idx ON escrow_decision(escrow_id, height DESC);
CREATE INDEX escrow_decision_account_id_height_idx ON escrow_decision(account_id, height DESC);
CREATE TABLE subscription(
    db_id BIGINT AUTO_INCREMENT,
    id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    frequency INT NOT NULL,
    time_next INT NOT NULL,
    height INT NOT NULL,
    latest BOOLEAN DEFAULT TRUE NOT NULL,
    PRIMARY KEY (db_id)
);

-- 33 +/- SELECT COUNT(*) FROM SUBSCRIPTION;
CREATE UNIQUE INDEX subscription_id_height_idx ON subscription(id, height DESC);
CREATE INDEX subscription_sender_id_height_idx ON subscription(sender_id, height DESC);
CREATE INDEX subscription_recipient_id_height_idx ON subscription(recipient_id, height DESC);
CREATE TABLE at(
    db_id BIGINT AUTO_INCREMENT,
    id BIGINT NOT NULL,
    creator_id BIGINT NOT NULL,
    name VARCHAR(30),
    description TEXT,
    version SMALLINT NOT NULL,
    csize INT NOT NULL,
    dsize INT NOT NULL,
    c_user_stack_bytes INT NOT NULL,
    c_call_stack_bytes INT NOT NULL,
    creation_height INT NOT NULL,
    ap_code BLOB NOT NULL,
    height INT NOT NULL,
    latest BOOLEAN DEFAULT TRUE NOT NULL,
    PRIMARY KEY (db_id)
);

-- 392 +/- SELECT COUNT(*) FROM AT;
CREATE UNIQUE INDEX at_id_height_idx ON at(id, height DESC);
CREATE INDEX at_creator_id_height_idx ON at(creator_id, height DESC);
CREATE TABLE at_state(
    db_id BIGINT AUTO_INCREMENT,
    at_id BIGINT NOT NULL,
    state BLOB NOT NULL,
    prev_height INT NOT NULL,
    next_height INT NOT NULL,
    sleep_between INT NOT NULL,
    prev_balance BIGINT NOT NULL,
    freeze_when_same_balance BOOLEAN NOT NULL,
    min_activate_amount BIGINT NOT NULL,
    height INT NOT NULL,
    latest BOOLEAN DEFAULT TRUE NOT NULL,
    PRIMARY KEY (db_id)
);

-- 398 +/- SELECT COUNT(*) FROM AT_STATE;
CREATE UNIQUE INDEX at_state_at_id_height_idx ON at_state(at_id, height DESC);
CREATE INDEX at_state_id_next_height_height_idx ON at_state(at_id, next_height, height DESC);
CREATE TABLE block(
    db_id BIGINT AUTO_INCREMENT,
    id BIGINT NOT NULL,
    version INT NOT NULL,
    timestamp INT NOT NULL,
    previous_block_id BIGINT,
    total_amount BIGINT NOT NULL,
    total_fee BIGINT NOT NULL,
    payload_length INT NOT NULL,
    generator_public_key VARBINARY(32) NOT NULL,
    previous_block_hash VARBINARY(32),
    cumulative_difficulty BLOB NOT NULL,
    base_target BIGINT NOT NULL,
    next_block_id BIGINT,
    height INT NOT NULL,
    generation_signature VARBINARY(64) NOT NULL,
    block_signature VARBINARY(64) NOT NULL,
    payload_hash VARBINARY(32) NOT NULL,
    generator_id BIGINT NOT NULL,
    nonce BIGINT NOT NULL,
    /* ac0v: HACK!!!! -> should be binary*/
    ats BLOB,
    PRIMARY KEY (db_id)
);

-- 384388 +/- SELECT COUNT(*) FROM BLOCK;
CREATE UNIQUE INDEX block_id_idx ON block(id);
CREATE UNIQUE INDEX block_height_idx ON block(height);
CREATE INDEX block_generator_id_idx ON block(generator_id);
CREATE UNIQUE INDEX block_timestamp_idx ON block(timestamp DESC);
ALTER TABLE transaction ADD CONSTRAINT constraint_ff FOREIGN KEY(block_id) REFERENCES block(id) ON DELETE CASCADE;
ALTER TABLE block ADD CONSTRAINT constraint_3c5 FOREIGN KEY(next_block_id) REFERENCES block(id) ON DELETE SET NULL;
ALTER TABLE block ADD CONSTRAINT constraint_3c FOREIGN KEY(previous_block_id) REFERENCES block(id) ON DELETE CASCADE;
