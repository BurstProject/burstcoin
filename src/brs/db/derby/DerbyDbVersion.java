package brs.db.derby;

import brs.db.sql.Db;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class DerbyDbVersion {

  private static final Logger logger = LoggerFactory.getLogger(DerbyDbVersion.class);

  static void init() {
    try (Connection con = Db.beginTransaction(); Statement stmt = con.createStatement()) {
      int nextUpdate = 1;
      try ( ResultSet rs = stmt.executeQuery("SELECT \"next_update\" FROM \"version\"") ) {
        if (! rs.next()) {
          throw new RuntimeException("Invalid version table");
        }
        nextUpdate = rs.getInt("next_update");
        if (rs.next()) {
          throw new RuntimeException("Invalid version table");
        }
        logger.info("Database update may take a while if needed, current db version " + (nextUpdate - 1) + "...");
      } catch (SQLException e) {
        logger.info("Initializing an empty database");
        stmt.executeUpdate("CREATE TABLE \"version\" (\"next_update\" INT NOT NULL)");
        Db.commitTransaction();
        stmt.executeUpdate("INSERT INTO \"version\" VALUES (1)");
        Db.commitTransaction();
      }
      update(nextUpdate);
    } catch (SQLException e) {
      Db.rollbackTransaction();
      throw new RuntimeException(e.toString(), e);
    } finally {
      Db.endTransaction();
    }

  }

  private static void apply(String sql) {
    try (Connection con = Db.getConnection(); Statement stmt = con.createStatement()) {
      try {
        if (sql != null) {
          logger.debug("Will apply sql:\n" + sql);
          stmt.executeUpdate(sql);
        }
        stmt.executeUpdate("UPDATE \"version\" SET \"next_update\" = \"next_update\" + 1");
        Db.commitTransaction();
      } catch (SQLException e) {
        Db.rollbackTransaction();
        throw e;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Database error executing " + sql, e);
    }
  }

  public static String maybeToShortIdentifier( String identifier) {
    return identifier;
    //    return "\"" + identifier.replaceAll("(.)[^_]+_", "$1" + "_") + "\"";
  }

  private static void update(int nextUpdate) {
    logger.debug("Next update is "+nextUpdate);
    switch (nextUpdate) {
      case 1:
        apply("CREATE TABLE \"alias\"("
              + "    \"db_id\"            BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
              + "    \"id\"               BIGINT NOT NULL,"
              + "    \"account_id\"       BIGINT NOT NULL,"
              + "    \"alias_name\"       VARCHAR(100) NOT NULL,"
              + "    \"alias_name_lower\" VARCHAR(100) NOT NULL,"
              + "    \"alias_uri\"        CLOB NOT NULL,"
              + "    \"timestamp\"        INT NOT NULL,"
              + "    \"height\"           INT NOT NULL,"
              + "    \"latest\"           BOOLEAN DEFAULT true NOT NULL,"
              + "    CONSTRAINT \"alias_db_id\" PRIMARY KEY (\"db_id\"),"
              + "    UNIQUE(\"id\", \"height\")"
              + ")");
      case 2:
        apply("UPDATE \"version\" SET \"next_update\" = 3");
      case 3:
        apply("CREATE UNIQUE INDEX \"alias_id_height_idx\" ON \"alias\"(\"id\" DESC, \"height\" DESC)");
      case 4:
        apply("CREATE INDEX \"alias_account_id_idx\" ON \"alias\"(\"account_id\" DESC, \"height\" DESC)");
      case 5:
        apply("CREATE INDEX \"alias_name_lower_idx\" ON \"alias\"(\"alias_name_lower\")");
      case 6:
        apply("CREATE TABLE \"account\"("
              + "    \"db_id\"               BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
              + "    \"id\"                  BIGINT NOT NULL,"
              + "    \"creation_height\"     INT NOT NULL,"
              + "    \"public_key\"          BLOB,"
              + "    \"key_height\"          INT,"
              + "    \"balance\"             BIGINT NOT NULL,"
              + "    \"unconfirmed_balance\" BIGINT NOT NULL,"
              + "    \"forged_balance\"      BIGINT NOT NULL,"
              + "    \"name\"                VARCHAR(100),"
              + "    \"description\"         CLOB,"
              + "    \"height\"              INT NOT NULL,"
              + "    \"latest\"              BOOLEAN DEFAULT true NOT NULL,"
              + "    CONSTRAINT \"account_db_id\" PRIMARY KEY (\"db_id\"),"
              + "    UNIQUE(\"id\", \"height\")"
              + ")");
      case 7:
        apply("CREATE UNIQUE INDEX \"account_id_height_idx\" ON \"account\"(\"id\" DESC, \"height\" DESC)");
      case 8:
        apply("CREATE INDEX \"account_id_balance_height_idx\" ON \"account\"(\"id\" DESC, \"balance\" DESC, \"height\" DESC)");
      case 9:
        apply("CREATE TABLE \"alias_offer\"("
              + "    \"db_id\"    BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
              + "    \"id\"       BIGINT NOT NULL,"
              + "    \"price\"    BIGINT NOT NULL,"
              + "    \"buyer_id\" BIGINT,"
              + "    \"height\"   INT NOT NULL,"
              + "    \"latest\"   BOOLEAN DEFAULT true NOT NULL,"
              + "    CONSTRAINT \"alias_offer_db_id\" PRIMARY KEY (\"db_id\"),"
              + "    UNIQUE(\"id\", \"height\")"
              + ")");
      case 10:
        apply("CREATE UNIQUE INDEX \"alias_offer_id_height_idx\" ON \"alias_offer\"(\"id\" DESC, \"height\" DESC)");
      case 11:
        apply("CREATE TABLE \"peer\"("
              + "    \"address\" VARCHAR(100) PRIMARY KEY NOT NULL"
              + ")");
      case 12:
        apply("CREATE TABLE \"transaction\"("
              + "    \"db_id\"                           BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
              + "    \"id\"                              BIGINT NOT NULL,"
              + "    \"deadline\"                        SMALLINT NOT NULL,"
              + "    \"sender_public_key\"               CHAR(32) FOR BIT DATA NOT NULL,"
              + "    \"recipient_id\"                    BIGINT,"
              + "    \"amount\"                          BIGINT NOT NULL,"
              + "    \"fee\"                             BIGINT NOT NULL,"
              + "    \"height\"                          INT NOT NULL,"
              + "    \"block_id\"                        BIGINT NOT NULL,"
              + "    \"signature\"                       CHAR(64) FOR BIT DATA,"
              + "    \"timestamp\"                       INT NOT NULL,"
              + "    \"type\"                            SMALLINT NOT NULL,"
              + "    \"subtype\"                         SMALLINT NOT NULL,"
              + "    \"sender_id\"                       BIGINT NOT NULL,"
              + "    \"block_timestamp\"                 INT NOT NULL,"
              + "    \"full_hash\"                       CHAR(32) FOR BIT DATA NOT NULL,"
              + "    \"referenced_transaction_fullhash\" CHAR(32) FOR BIT DATA,"
              + "    \"attachment_bytes\"                LONG VARCHAR FOR BIT DATA,"
              + "    \"version\"                         SMALLINT NOT NULL,"
              + "    \"has_message\"                     BOOLEAN DEFAULT false NOT NULL,"
              + "    \"has_encrypted_message\"           BOOLEAN DEFAULT false NOT NULL,"
              + "    \"has_public_key_announcement\"     BOOLEAN DEFAULT false NOT NULL,"
              + "    \"ec_block_height\"                 INT DEFAULT NULL,"
              + "    \"ec_block_id\"                     BIGINT DEFAULT NULL,"
              + "    \"has_encrypttoself_message\"       BOOLEAN DEFAULT false NOT NULL,"
              + "    CONSTRAINT \"transaction_db_id\" PRIMARY KEY (\"db_id\"),"
              + "    UNIQUE(\"id\")"
              + ")");
      case 13:
        apply("CREATE INDEX \"transaction_block_timestamp_idx\" ON \"transaction\"(\"block_timestamp\" DESC)");
      case 14:
        apply("CREATE UNIQUE INDEX \"transaction_id_idx\" ON \"transaction\"(\"id\")");
      case 15:
        apply("CREATE INDEX \"transaction_sender_id_idx\" ON \"transaction\"(\"sender_id\")");
      case 16:
        apply("UPDATE \"version\" SET \"next_update\" = 16");
      case 17:
        apply("CREATE INDEX \"transaction_recipient_id_idx\" ON \"transaction\"(\"recipient_id\")");
      case 18:
        apply("CREATE INDEX " + maybeToShortIdentifier("transaction_recipient_id_amount_height_idx") + " ON \"transaction\"(\"recipient_id\", \"amount\", \"height\")");
      case 19:
        apply("CREATE TABLE \"asset\"("
              + "    \"db_id\"       BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
              + "    \"id\"          BIGINT NOT NULL,"
              + "    \"account_id\"  BIGINT NOT NULL,"
              + "    \"name\"        VARCHAR(10) NOT NULL,"
              + "    \"description\" CLOB,"
              + "    \"quantity\"    BIGINT NOT NULL,"
              + "    \"decimals\"    SMALLINT NOT NULL,"
              + "    \"height\"      INT NOT NULL,"
              + "    CONSTRAINT \"asset_db_id\" PRIMARY KEY (\"db_id\"),"
              + "    UNIQUE(\"id\")"
              + ")");
      case 20:
        apply("CREATE UNIQUE INDEX \"asset_id_idx\" ON \"asset\"(\"id\")");
      case 21:
        apply("CREATE INDEX \"asset_account_id_idx\" ON \"asset\"(\"account_id\")");
      case 22:
        apply("CREATE TABLE \"trade\"("
              + "    \"db_id\"            BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
              + "    \"asset_id\"         BIGINT NOT NULL,"
              + "    \"block_id\"         BIGINT NOT NULL,"
              + "    \"ask_order_id\"     BIGINT NOT NULL,"
              + "    \"bid_order_id\"     BIGINT NOT NULL,"
              + "    \"ask_order_height\" INT NOT NULL,"
              + "    \"bid_order_height\" INT NOT NULL,"
              + "    \"seller_id\"        BIGINT NOT NULL,"
              + "    \"buyer_id\"         BIGINT NOT NULL,"
              + "    \"quantity\"         BIGINT NOT NULL,"
              + "    \"price\"            BIGINT NOT NULL,"
              + "    \"timestamp\"        INT NOT NULL,"
              + "    \"height\"           INT NOT NULL,"
              + "    CONSTRAINT \"trade_db_id\" PRIMARY KEY (\"db_id\"),"
              + "    UNIQUE(\"ask_order_id\", \"bid_order_id\")"
              + ")");
      case 23:
        apply("CREATE UNIQUE INDEX \"trade_ask_bid_idx\" ON \"trade\"(\"ask_order_id\", \"bid_order_id\")");
      case 24:
        apply("CREATE INDEX \"trade_asset_id_idx\" ON \"trade\"(\"asset_id\" DESC, \"height\" DESC)");
      case 25:
        apply("CREATE INDEX \"trade_seller_id_idx\" ON \"trade\"(\"seller_id\" DESC, \"height\" DESC)");
      case 26:
        apply("CREATE INDEX \"trade_buyer_id_idx\" ON \"trade\"(\"buyer_id\" DESC, \"height\" DESC)");
      case 27:
        apply("CREATE TABLE \"ask_order\"("
              + "    \"db_id\"           BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
              + "    \"id\"              BIGINT NOT NULL,"
              + "    \"account_id\"      BIGINT NOT NULL,"
              + "    \"asset_id\"        BIGINT NOT NULL,"
              + "    \"price\"           BIGINT NOT NULL,"
              + "    \"quantity\"        BIGINT NOT NULL,"
              + "    \"creation_height\" INT NOT NULL,"
              + "    \"height\"          INT NOT NULL,"
              + "    \"latest\"          BOOLEAN DEFAULT true NOT NULL,"
              + "    CONSTRAINT \"ask_order_db_id\" PRIMARY KEY (\"db_id\"),"
              + "    UNIQUE(\"id\", \"height\")"
              + ")");
      case 28:
        apply("CREATE UNIQUE INDEX \"ask_order_id_height_idx\" ON \"ask_order\"(\"id\" DESC, \"height\" DESC)");
      case 29:
        apply("CREATE INDEX \"ask_order_account_id_idx\" ON \"ask_order\"(\"account_id\" DESC, \"height\" DESC)");
      case 30:
        apply("CREATE INDEX \"ask_order_asset_id_price_idx\" ON \"ask_order\"(\"asset_id\", \"price\")");
      case 31:
        apply("CREATE INDEX \"ask_order_creation_idx\" ON \"ask_order\"(\"creation_height\" DESC)");
      case 32:
        apply("CREATE TABLE \"bid_order\"("
              + "    \"db_id\"           BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
              + "    \"id\"              BIGINT NOT NULL,"
              + "    \"account_id\"      BIGINT NOT NULL,"
              + "    \"asset_id\"        BIGINT NOT NULL,"
              + "    \"price\"           BIGINT NOT NULL,"
              + "    \"quantity\"        BIGINT NOT NULL,"
              + "    \"creation_height\" INT NOT NULL,"
              + "    \"height\"          INT NOT NULL,"
              + "    \"latest\"          BOOLEAN DEFAULT true NOT NULL,"
              + "    CONSTRAINT \"bid_order_db_id\" PRIMARY KEY (\"db_id\"),"
              + "    UNIQUE(\"id\", \"height\")"
              + ")");
      case 33:
        apply("CREATE UNIQUE INDEX \"bid_order_id_height_idx\" ON \"bid_order\"(\"id\" DESC, \"height\" DESC)");
      case 34:
        apply("CREATE INDEX \"bid_order_account_id_idx\" ON \"bid_order\"(\"account_id\" DESC, \"height\" DESC)");
      case 35:
        apply("CREATE INDEX \"bid_order_asset_id_price_idx\" ON \"bid_order\"(\"asset_id\" DESC, \"price\" DESC)");
      case 36:
        apply("CREATE INDEX \"bid_order_creation_idx\" ON \"bid_order\"(\"creation_height\" DESC)");
      case 37:
        apply("CREATE TABLE \"goods\"("
              + "    \"db_id\"       BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
              + "    \"id\"          BIGINT NOT NULL,"
              + "    \"seller_id\"   BIGINT NOT NULL,"
              + "    \"name\"        VARCHAR(100) NOT NULL,"
              + "    \"description\" CLOB,"
              + "    \"tags\"        VARCHAR(100),"
              + "    \"timestamp\"   INT NOT NULL,"
              + "    \"quantity\"    INT NOT NULL,"
              + "    \"price\"       BIGINT NOT NULL,"
              + "    \"delisted\"    BOOLEAN NOT NULL,"
              + "    \"height\"      INT NOT NULL,"
              + "    \"latest\"      BOOLEAN DEFAULT true NOT NULL,"
              + "    CONSTRAINT \"goods_db_id\" PRIMARY KEY (\"db_id\"),"
              + "    UNIQUE(\"id\", \"height\")"
              + ")");
      case 38:
        apply("CREATE UNIQUE INDEX \"goods_id_height_idx\" ON \"goods\"(\"id\" DESC, \"height\" DESC)");
      case 39:
        apply("CREATE INDEX \"goods_seller_id_name_idx\" ON \"goods\"(\"seller_id\", \"name\")");
      case 40:
        apply("CREATE INDEX \"goods_timestamp_idx\" ON \"goods\"(\"timestamp\" DESC, \"height\" DESC)");
      case 41:
        apply("CREATE TABLE \"purchase\"("
              + "    \"db_id\"                BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
              + "    \"id\"                   BIGINT NOT NULL,"
              + "    \"buyer_id\"             BIGINT NOT NULL,"
              + "    \"goods_id\"             BIGINT NOT NULL,"
              + "    \"seller_id\"            BIGINT NOT NULL,"
              + "    \"quantity\"             INT NOT NULL,"
              + "    \"price\"                BIGINT NOT NULL,"
              + "    \"deadline\"             INT NOT NULL,"
              + "    \"note\"                 BLOB,"
              + "    \"nonce\"                BLOB,"
              + "    \"timestamp\"            INT NOT NULL,"
              + "    \"pending\"              BOOLEAN NOT NULL,"
              + "    \"goods\"                BLOB,"
              + "    \"goods_nonce\"          BLOB,"
              + "    \"refund_note\"          BLOB,"
              + "    \"refund_nonce\"         BLOB,"
              + "    \"has_feedback_notes\"   BOOLEAN DEFAULT false NOT NULL,"
              + "    \"has_public_feedbacks\" BOOLEAN DEFAULT false NOT NULL,"
              + "    \"discount\"             BIGINT NOT NULL,"
              + "    \"refund\"               BIGINT NOT NULL,"
              + "    \"height\"               INT NOT NULL,"
              + "    \"latest\"               BOOLEAN DEFAULT true NOT NULL,"
              + "    CONSTRAINT \"purchase_db_id\" PRIMARY KEY (\"db_id\"),"
              + "    UNIQUE(\"id\", \"height\")"
              + ")");
      case 42:
        apply("CREATE UNIQUE INDEX \"purchase_id_height_idx\" ON \"purchase\"(\"id\" DESC, \"height\" DESC)");
      case 43:
        apply("CREATE INDEX \"purchase_buyer_id_height_idx\" ON \"purchase\"(\"buyer_id\" DESC, \"height\" DESC)");
      case 44:
        apply("CREATE INDEX \"purchase_seller_id_height_idx\" ON \"purchase\"(\"seller_id\" DESC, \"height\" DESC)");
      case 45:
        apply("CREATE INDEX \"purchase_deadline_idx\" ON \"purchase\"(\"deadline\" DESC, \"height\" DESC)");
      case 46:
        apply("CREATE INDEX \"purchase_timestamp_idx\" ON \"purchase\"(\"timestamp\" DESC, \"id\" DESC)");
      case 47:
        apply("CREATE TABLE \"account_asset\"("
              + "    \"db_id\"                BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
              + "    \"account_id\"           BIGINT NOT NULL,"
              + "    \"asset_id\"             BIGINT NOT NULL,"
              + "    \"quantity\"             BIGINT NOT NULL,"
              + "    \"unconfirmed_quantity\" BIGINT NOT NULL,"
              + "    \"height\"               INT NOT NULL,"
              + "    \"latest\"               BOOLEAN DEFAULT true NOT NULL,"
              + "    CONSTRAINT \"account_asset_db_id\" PRIMARY KEY (\"db_id\"),"
              + "    UNIQUE(\"account_id\", \"asset_id\", \"height\")"
              + ")");
      case 48:
        apply("CREATE UNIQUE INDEX \"account_asset_id_height_idx\" ON \"account_asset\"(\"account_id\" DESC, \"asset_id\" DESC, \"height\" DESC)");
      case 49:
        apply("CREATE INDEX \"account_asset_quantity_idx\" ON \"account_asset\"(\"quantity\" DESC)");
      case 50:
        apply("CREATE TABLE \"purchase_feedback\"("
              + "    \"db_id\"          BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
              + "    \"id\"             BIGINT NOT NULL,"
              + "    \"feedback_data\"  BLOB NOT NULL,"
              + "    \"feedback_nonce\" BLOB NOT NULL,"
              + "    \"height\"         INT NOT NULL,"
              + "    \"latest\"         BOOLEAN DEFAULT true NOT NULL,"
              + "    CONSTRAINT \"purchase_feedback_db_id\" PRIMARY KEY (\"db_id\")"
              + ")");
      case 51:
        apply("CREATE INDEX " +  maybeToShortIdentifier("purchase_feedback_id_height_idx") + " ON \"purchase_feedback\"(\"id\" DESC, \"height\" DESC)");
      case 52:
        apply("CREATE TABLE \"purchase_public_feedback\"("
              + "    \"db_id\"           BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
              + "    \"id\"              BIGINT NOT NULL,"
              + "    \"public_feedback\" CLOB NOT NULL,"
              + "    \"height\"          INT NOT NULL,"
              + "    \"latest\"          BOOLEAN DEFAULT true NOT NULL,"
              + "    CONSTRAINT \"db_id\" PRIMARY KEY (\"db_id\")"
              + ")");
      case 53:
        apply("CREATE INDEX " + maybeToShortIdentifier("purchase_public_feedback_id_height_idx") + " ON \"purchase_public_feedback\"(\"id\" DESC, \"height\" DESC)");
      case 54:
        apply("CREATE TABLE \"unconfirmed_transaction\"("
              + "    \"db_id\"               BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
              + "    \"id\"                 BIGINT NOT NULL,"
              + "    \"expiration\"         INT NOT NULL,"
              + "    \"transaction_height\" INT NOT NULL,"
              + "    \"fee_per_byte\"       BIGINT NOT NULL,"
              + "    \"timestamp\"          INT NOT NULL,"
              + "    \"transaction_bytes\"  BLOB NOT NULL,"
              + "    \"height\"             INT NOT NULL,"
              + "    CONSTRAINT \"unconfirmed_transaction_db_id\" PRIMARY KEY (\"db_id\"),"
              + "    UNIQUE(\"id\")"
              + ")");
      case 55:
        apply("CREATE UNIQUE INDEX " + maybeToShortIdentifier("unconfirmed_transaction_id_idx") + " ON \"unconfirmed_transaction\"(\"id\")");
      case 56:
        apply("CREATE INDEX " + maybeToShortIdentifier("unconfirmed_transaction_fee_timestamp_idx") + " ON \"unconfirmed_transaction\"(\"transaction_height\" DESC, \"fee_per_byte\" DESC, \"timestamp\" DESC)");
      case 57:
        apply("CREATE TABLE \"asset_transfer\"("
              + "    \"db_id\"        BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
              + "    \"id\"           BIGINT NOT NULL,"
              + "    \"asset_id\"     BIGINT NOT NULL,"
              + "    \"sender_id\"    BIGINT NOT NULL,"
              + "    \"recipient_id\" BIGINT NOT NULL,"
              + "    \"quantity\"     BIGINT NOT NULL,"
              + "    \"timestamp\"    INT NOT NULL,"
              + "    \"height\"       INT NOT NULL,"
              + "    CONSTRAINT \"asset_transfer_db_id\" PRIMARY KEY (\"db_id\"),"
              + "    UNIQUE(\"id\")"
              + ")");
      case 58:
        apply("CREATE UNIQUE INDEX \"asset_transfer_id_idx\" ON \"asset_transfer\"(\"id\")");
      case 59:
        apply("CREATE INDEX \"asset_transfer_asset_id_idx\" ON \"asset_transfer\"(\"asset_id\" DESC, \"height\" DESC)");
      case 60:
        apply("CREATE INDEX \"asset_transfer_sender_id_idx\" ON \"asset_transfer\"(\"sender_id\" DESC, \"height\" DESC)");
      case 61:
        apply("CREATE INDEX \"asset_transfer_recipient_id_idx\" ON \"asset_transfer\"(\"recipient_id\" DESC, \"height\" DESC)");
      case 62:
        apply("CREATE TABLE \"reward_recip_assign\"("
              + "    \"db_id\"         BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
              + "    \"account_id\"    BIGINT NOT NULL,"
              + "    \"prev_recip_id\" BIGINT NOT NULL,"
              + "    \"recip_id\"      BIGINT NOT NULL,"
              + "    \"from_height\"   INT NOT NULL,"
              + "    \"height\"        INT NOT NULL,"
              + "    \"latest\"        BOOLEAN DEFAULT true NOT NULL,"
              + "    CONSTRAINT \"reward_recip_assign_db_id\" PRIMARY KEY (\"db_id\"),"
              + "    UNIQUE(\"account_id\", \"height\")"
              + ")");
      case 63:
        apply("CREATE UNIQUE INDEX " + maybeToShortIdentifier("reward_recip_assign_account_id_height_idx") + " ON \"reward_recip_assign\"(\"account_id\" DESC, \"height\" DESC)");
      case 64:
        apply("CREATE INDEX " + maybeToShortIdentifier("reward_recip_assign_recip_id_height_idx") + " ON \"reward_recip_assign\"(\"recip_id\" DESC, \"height\" DESC)");
      case 65:
        apply("CREATE TABLE \"escrow\"("
              + "    \"db_id\"            BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
              + "    \"id\"               BIGINT NOT NULL,"
              + "    \"sender_id\"        BIGINT NOT NULL,"
              + "    \"recipient_id\"     BIGINT NOT NULL,"
              + "    \"amount\"           BIGINT NOT NULL,"
              + "    \"required_signers\" INT,"
              + "    \"deadline\"         INT NOT NULL,"
              + "    \"deadline_action\"  INT NOT NULL,"
              + "    \"height\"           INT NOT NULL,"
              + "    \"latest\"           BOOLEAN DEFAULT true NOT NULL,"
              + "    CONSTRAINT \"escrow_db_id\" PRIMARY KEY (\"db_id\"),"
              + "    UNIQUE(\"id\", \"height\")"
              + ")");
      case 66:
        apply("CREATE UNIQUE INDEX \"escrow_id_height_idx\" ON \"escrow\"(\"id\" DESC, \"height\" DESC)");
      case 67:
        apply("CREATE INDEX \"escrow_sender_id_height_idx\" ON \"escrow\"(\"sender_id\" DESC, \"height\" DESC)");
      case 68:
        apply("CREATE INDEX \"escrow_recipient_id_height_idx\" ON \"escrow\"(\"recipient_id\" DESC, \"height\" DESC)");
      case 69:
        apply("CREATE INDEX \"escrow_deadline_height_idx\" ON \"escrow\"(\"deadline\" DESC, \"height\" DESC)");
      case 70:
        apply("CREATE TABLE \"escrow_decision\"("
              + "    \"db_id\"      BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
              + "    \"escrow_id\"  BIGINT NOT NULL,"
              + "    \"account_id\" BIGINT NOT NULL,"
              + "    \"decision\"   INT NOT NULL,"
              + "    \"height\"     INT NOT NULL,"
              + "    \"latest\"     BOOLEAN DEFAULT true NOT NULL,"
              + "    CONSTRAINT \"escrow_decision_db_id\" PRIMARY KEY (\"db_id\"),"
              + "    UNIQUE(\"escrow_id\", \"account_id\", \"height\")"
              + ")");
      case 71:
        apply("CREATE UNIQUE INDEX " + maybeToShortIdentifier("escrow_decision_escrow_id_account_id_height_idx") + " ON \"escrow_decision\"(\"escrow_id\" DESC, \"account_id\" DESC, \"height\" DESC)");
      case 72:
        apply("CREATE INDEX " + maybeToShortIdentifier("escrow_decision_escrow_id_height_idx") + " ON \"escrow_decision\"(\"escrow_id\" DESC, \"height\" DESC)");
      case 73:
        apply("CREATE INDEX " + maybeToShortIdentifier("escrow_decision_account_id_height_idx") + " ON \"escrow_decision\"(\"account_id\" DESC, \"height\" DESC)");
      case 74:
        apply("CREATE TABLE \"subscription\"("
              + "    \"db_id\"        BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
              + "    \"id\"           BIGINT NOT NULL,"
              + "    \"sender_id\"    BIGINT NOT NULL,"
              + "    \"recipient_id\" BIGINT NOT NULL,"
              + "    \"amount\"       BIGINT NOT NULL,"
              + "    \"frequency\"    INT NOT NULL,"
              + "    \"time_next\"    INT NOT NULL,"
              + "    \"height\"       INT NOT NULL,"
              + "    \"latest\"       BOOLEAN DEFAULT true NOT NULL,"
              + "    CONSTRAINT \"subscription_db_id\" PRIMARY KEY (\"db_id\"),"
              + "    UNIQUE(\"id\", \"height\")"
              + ")");
      case 75:
        apply("CREATE UNIQUE INDEX \"subscription_id_height_idx\" ON \"subscription\"(\"id\", \"height\")");
      case 76:
        apply("CREATE INDEX " + maybeToShortIdentifier("subscription_sender_id_height_idx") + " ON \"subscription\"(\"sender_id\" DESC, \"height\" DESC)");
      case 77:
        apply("CREATE INDEX " + maybeToShortIdentifier("subscription_recipient_id_height_idx") + " ON \"subscription\"(\"recipient_id\" DESC, \"height\" DESC)");
      case 78:
        apply("CREATE TABLE \"at\"("
              + "    \"db_id\"              BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
              + "    \"id\"                 BIGINT NOT NULL,"
              + "    \"creator_id\"         BIGINT NOT NULL,"
              + "    \"name\"               VARCHAR(30),"
              + "    \"description\"        CLOB,"
              + "    \"version\"            SMALLINT NOT NULL,"
              + "    \"csize\"              INT NOT NULL,"
              + "    \"dsize\"              INT NOT NULL,"
              + "    \"c_user_stack_bytes\" INT NOT NULL,"
              + "    \"c_call_stack_bytes\" INT NOT NULL,"
              + "    \"creation_height\"    INT NOT NULL,"
              + "    \"ap_code\"            BLOB NOT NULL,"
              + "    \"height\"             INT NOT NULL,"
              + "    \"latest\"             BOOLEAN DEFAULT true NOT NULL,"
              + "    CONSTRAINT \"at_db_id\" PRIMARY KEY (\"db_id\"),"
              + "    UNIQUE(\"id\", \"height\")"
              + ")");
      case 79:
        apply("CREATE UNIQUE INDEX \"at_id_height_idx\" ON \"at\"(\"id\" DESC, \"height\" DESC)");
      case 80:
        apply("CREATE INDEX \"at_creator_id_height_idx\" ON \"at\"(\"creator_id\" DESC, \"height\" DESC)");
      case 81:
        apply("CREATE TABLE \"at_state\"("
              + "    \"db_id\"                    BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
              + "    \"at_id\"                    BIGINT NOT NULL,"
              + "    \"state\"                    BLOB NOT NULL,"
              + "    \"prev_height\"              INT NOT NULL,"
              + "    \"next_height\"              INT NOT NULL,"
              + "    \"sleep_between\"            INT NOT NULL,"
              + "    \"prev_balance\"             BIGINT NOT NULL,"
              + "    \"freeze_when_same_balance\" BOOLEAN NOT NULL,"
              + "    \"min_activate_amount\"      BIGINT NOT NULL,"
              + "    \"height\"                   INT NOT NULL,"
              + "    \"latest\"                   BOOLEAN DEFAULT true NOT NULL,"
              + "    CONSTRAINT \"at_state_db_id\" PRIMARY KEY (\"db_id\"),"
              + "    UNIQUE(\"at_id\", \"height\")"
              + ")");
      case 82:
        apply("CREATE UNIQUE INDEX \"at_state_at_id_height_idx\" ON \"at_state\"(\"at_id\" DESC, \"height\" DESC)");
      case 83:
        apply("CREATE INDEX " + maybeToShortIdentifier("at_state_id_next_height_height_idx") + " ON \"at_state\"(\"at_id\" DESC, \"next_height\" DESC, \"height\" DESC)");
      case 84:
        apply("CREATE TABLE \"block\"("
              + "    \"db_id\"                 BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
              + "    \"id\"                    BIGINT NOT NULL,"
              + "    \"version\"               INT NOT NULL,"
              + "    \"timestamp\"             INT NOT NULL,"
              + "    \"previous_block_id\"     BIGINT,"
              + "    \"total_amount\"          BIGINT NOT NULL,"
              + "    \"total_fee\"             BIGINT NOT NULL,"
              + "    \"payload_length\"        INT NOT NULL,"
              + "    \"generator_public_key\"  BLOB NOT NULL,"
              + "    \"previous_block_hash\"   BLOB,"
              + "    \"cumulative_difficulty\" BLOB NOT NULL,"
              + "    \"base_target\"           BIGINT NOT NULL,"
              + "    \"next_block_id\"         BIGINT,"
              + "    \"height\"                INT NOT NULL,"
              + "    \"generation_signature\"  BLOB NOT NULL,"
              + "    \"block_signature\"       BLOB NOT NULL,"
              + "    \"payload_hash\"          BLOB NOT NULL,"
              + "    \"generator_id\"          BIGINT NOT NULL,"
              + "    \"nonce\"                 BIGINT NOT NULL,"
              + "    \"ats\"                   BLOB,"
              + "    CONSTRAINT \"block_db_id\" PRIMARY KEY (\"db_id\"),"
              + "    UNIQUE(\"id\"),"
              + "    UNIQUE(\"height\"),"
              + "    UNIQUE(\"timestamp\")"
              + ")");
      case 85:
        apply("CREATE UNIQUE INDEX \"block_id_idx\" ON \"block\"(\"id\")");
      case 86:
        apply("CREATE UNIQUE INDEX \"block_height_idx\" ON \"block\"(\"height\")");
      case 87:
        apply("CREATE INDEX \"block_generator_id_idx\" ON \"block\"(\"generator_id\")");
      case 88:
        apply("CREATE UNIQUE INDEX \"block_timestamp_idx\" ON \"block\"(\"timestamp\" DESC)");
      case 89:
        apply("ALTER TABLE \"transaction\" ADD CONSTRAINT \"constraint_ff\" FOREIGN KEY(\"block_id\") REFERENCES \"block\"(\"id\") ON DELETE CASCADE");
      case 90:
        apply("ALTER TABLE \"block\" ADD CONSTRAINT \"constraint_3c5\" FOREIGN KEY(\"next_block_id\") REFERENCES \"block\"(\"id\") ON DELETE SET NULL");
      case 91:
        apply("ALTER TABLE \"alias\" ALTER COLUMN \"alias_name_lower\" SET DEFAULT ''");
      case 92:
        apply("UPDATE \"version\" SET \"next_update\" = 174");
      case 175:
        apply("CREATE INDEX \"account_id_latest_idx\" ON \"account\"(\"id\", \"latest\")");
      case 176:
        return;
      default:
        throw new RuntimeException("Database inconsistent with code, probably trying to run older code on newer database");


        /**
         *  IMPORTANT: IF YOU ADD FOREIGN KEYS OR OTHER CONTRAINTS HERE MAKE SURE TO ADD THEM TO DerbyDbs.java as well!
         *  */

    }
  }

  private DerbyDbVersion() {} //never
}
