package nxt;

import nxt.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

final class DbVersion {

    static void init() {
        try (Connection con = Db.getConnection(); Statement stmt = con.createStatement()) {
            int nextUpdate = 1;
            try {
                ResultSet rs = stmt.executeQuery("SELECT next_update FROM version");
                if (! rs.next()) {
                    throw new RuntimeException("Invalid version table");
                }
                nextUpdate = rs.getInt("next_update");
                if (! rs.isLast()) {
                    throw new RuntimeException("Invalid version table");
                }
                rs.close();
                Logger.logMessage("Database update may take a while if needed, current db version " + (nextUpdate - 1) + "...");
            } catch (SQLException e) {
                Logger.logMessage("Initializing an empty database");
                stmt.executeUpdate("CREATE TABLE version (next_update INT NOT NULL)");
                stmt.executeUpdate("INSERT INTO version VALUES (1)");
                con.commit();
            }
            update(nextUpdate);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }

    }

    private static void apply(String sql) {
        try (Connection con = Db.getConnection(); Statement stmt = con.createStatement()) {
            try {
                if (sql != null) {
                    Logger.logDebugMessage("Will apply sql:\n" + sql);
                    stmt.executeUpdate(sql);
                }
                stmt.executeUpdate("UPDATE version SET next_update = (SELECT next_update + 1 FROM version)");
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error executing " + sql, e);
        }
    }

    private static void update(int nextUpdate) {
        switch (nextUpdate) {
            case 1:
                apply("CREATE TABLE IF NOT EXISTS block (db_id INT IDENTITY, id BIGINT NOT NULL, version INT NOT NULL, "
                        + "timestamp INT NOT NULL, previous_block_id BIGINT, "
                        + "FOREIGN KEY (previous_block_id) REFERENCES block (id) ON DELETE CASCADE, total_amount INT NOT NULL, "
                        + "total_fee INT NOT NULL, payload_length INT NOT NULL, generator_public_key BINARY(32) NOT NULL, "
                        + "previous_block_hash BINARY(32), cumulative_difficulty VARBINARY NOT NULL, base_target BIGINT NOT NULL, "
                        + "next_block_id BIGINT, FOREIGN KEY (next_block_id) REFERENCES block (id) ON DELETE SET NULL, "
                        + "index INT NOT NULL, height INT NOT NULL, generation_signature BINARY(64) NOT NULL, "
                        + "block_signature BINARY(64) NOT NULL, payload_hash BINARY(32) NOT NULL, generator_account_id BIGINT NOT NULL, nonce BIGINT NOT NULL)");
            case 2:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS block_id_idx ON block (id)");
            case 3:
                apply("CREATE TABLE IF NOT EXISTS transaction (db_id INT IDENTITY, id BIGINT NOT NULL, "
                        + "deadline SMALLINT NOT NULL, sender_public_key BINARY(32) NOT NULL, recipient_id BIGINT NOT NULL, "
                        + "amount INT NOT NULL, fee INT NOT NULL, referenced_transaction_id BIGINT, index INT NOT NULL, "
                        + "height INT NOT NULL, block_id BIGINT NOT NULL, FOREIGN KEY (block_id) REFERENCES block (id) ON DELETE CASCADE, "
                        + "signature BINARY(64) NOT NULL, timestamp INT NOT NULL, type TINYINT NOT NULL, subtype TINYINT NOT NULL, "
                        + "sender_account_id BIGINT NOT NULL, attachment OTHER)");
            case 4:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS transaction_id_idx ON transaction (id)");
            case 5:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS block_height_idx ON block (height)");
            case 6:
                apply("CREATE INDEX IF NOT EXISTS transaction_timestamp_idx ON transaction (timestamp)");
            case 7:
                apply("CREATE INDEX IF NOT EXISTS block_generator_account_id_idx ON block (generator_account_id)");
            case 8:
                apply("CREATE INDEX IF NOT EXISTS transaction_sender_account_id_idx ON transaction (sender_account_id)");
            case 9:
                apply("CREATE INDEX IF NOT EXISTS transaction_recipient_id_idx ON transaction (recipient_id)");
            case 10:
                apply("ALTER TABLE block ALTER COLUMN generator_account_id RENAME TO generator_id");
            case 11:
                apply("ALTER TABLE transaction ALTER COLUMN sender_account_id RENAME TO sender_id");
            case 12:
                apply("ALTER INDEX block_generator_account_id_idx RENAME TO block_generator_id_idx");
            case 13:
                apply("ALTER INDEX transaction_sender_account_id_idx RENAME TO transaction_sender_id_idx");
            case 14:
                apply("ALTER TABLE block DROP COLUMN IF EXISTS index");
            case 15:
                apply("ALTER TABLE transaction DROP COLUMN IF EXISTS index");
            case 16:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS block_timestamp INT");
            case 17:
                apply("UPDATE transaction SET block_timestamp = (SELECT timestamp FROM block WHERE block.id = transaction.block_id)");
            case 18:
                apply("ALTER TABLE transaction ALTER COLUMN block_timestamp SET NOT NULL");
            case 19:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS hash BINARY(32)");
            case 20:
                apply(null);
            case 21:
                apply(null);
            case 22:
                apply("CREATE INDEX IF NOT EXISTS transaction_hash_idx ON transaction (hash)");
            case 23:
                apply(null);
            case 24:
                apply("ALTER TABLE block ALTER COLUMN total_amount BIGINT");
            case 25:
                apply("ALTER TABLE block ALTER COLUMN total_fee BIGINT");
            case 26:
                apply("ALTER TABLE transaction ALTER COLUMN amount BIGINT");
            case 27:
                apply("ALTER TABLE transaction ALTER COLUMN fee BIGINT");
            case 28:
                apply("UPDATE block SET total_amount = total_amount * " + Constants.ONE_NXT + " WHERE height <= " + Constants.NQT_BLOCK);
            case 29:
                apply("UPDATE block SET total_fee = total_fee * " + Constants.ONE_NXT + " WHERE height <= " + Constants.NQT_BLOCK);
            case 30:
                apply("UPDATE transaction SET amount = amount * " + Constants.ONE_NXT + " WHERE height <= " + Constants.NQT_BLOCK);
            case 31:
                apply("UPDATE transaction SET fee = fee * " + Constants.ONE_NXT + " WHERE height <= " + Constants.NQT_BLOCK);
            case 32:
                apply(null);
            case 33:
                apply(null);
            case 34:
                apply(null);
            case 35:
                apply(null);
            case 36:
                apply("CREATE TABLE IF NOT EXISTS peer (address VARCHAR PRIMARY KEY)");
            case 37:
                /*if (!Constants.isTestnet) {
                    apply("INSERT INTO peer (address) VALUES " +
                            "('samson.vps.nxtcrypto.org'), ('210.70.137.216:80'), ('178.15.99.67'), ('114.215.130.79'), " +
                            "('85.25.198.120'), ('82.192.39.33'), ('84.242.91.139'), ('178.33.203.157'), ('bitsy08.vps.nxtcrypto.org'), " +
                            "('oldnbold.vps.nxtcrypto.org'), ('46.28.111.249'), ('vps8.nxtcrypto.org'), ('95.24.81.240'), " +
                            "('85.214.222.82'), ('188.226.179.119'), ('54.187.153.45'), ('89.176.190.43'), ('nxtpi.zapto.org'), " +
                            "('89.70.254.145'), ('wallet.nxtty.com'), ('95.85.24.151'), ('95.188.237.51'), ('nrs02.nxtsolaris.info'), " +
                            "('fsom.no-ip.org'), ('nrs01.nxtsolaris.info'), ('allbits.vps.nxtcrypto.org'), ('mycrypto.no-ip.biz'), " +
                            "('72.14.177.42'), ('bitsy03.vps.nxtcrypto.org'), ('199.195.148.27'), ('bitsy06.vps.nxtcrypto.org'), " +
                            "('188.226.139.71'), ('enricoip.no-ip.biz'), ('54.200.196.116'), ('24.161.110.115'), ('88.163.78.131'), " +
                            "('vps12.nxtcrypto.org'), ('vps10.nxtcrypto.org'), ('bitsy09.vps.nxtcrypto.org'), ('nxtnode.noip.me'), " +
                            "('49.245.183.103'), ('xyzzyx.vps.nxtcrypto.org'), ('nxt.ravensbloodrealms.com'), ('nxtio.org'), " +
                            "('67.212.71.173'), ('xeqtorcreed2.vps.nxtcrypto.org'), ('195.154.127.172'), ('vps11.nxtcrypto.org'), " +
                            "('184.57.30.220'), ('213.46.57.77'), ('162.243.159.190'), ('188.138.88.154'), ('178.150.207.53'), " +
                            "('54.76.203.25'), ('146.185.168.129'), ('107.23.118.157'), ('bitsy04.vps.nxtcrypto.org'), " +
                            "('nxt.alkeron.com'), ('23.88.229.194'), ('23.88.59.40'), ('77.179.121.91'), ('58.95.145.117'), " +
                            "('188.35.156.10'), ('166.111.77.95'), ('pakisnxt.no-ip.org'), ('81.4.107.191'), ('192.241.190.156'), " +
                            "('69.141.139.8'), ('nxs2.hanza.co.id'), ('bitsy01.vps.nxtcrypto.org'), ('209.126.73.158'), " +
                            "('nxt.phukhew.com'), ('89.250.240.63'), ('cryptkeeper.vps.nxtcrypto.org'), ('54.213.122.21'), " +
                            "('zobue.com'), ('91.69.121.229'), ('vps6.nxtcrypto.org'), ('54.187.49.62'), ('vps4.nxtcrypto.org'), " +
                            "('80.86.92.139'), ('109.254.63.44'), ('nxtportal.org'), ('89.250.243.200'), ('nxt.olxp.in'), " +
                            "('46.194.184.161'), ('178.63.69.203'), ('nxt.sx'), ('185.4.72.115'), ('178.198.145.191'), " +
                            "('panzetti.vps.nxtcrypto.org'), ('miasik.no-ip.org'), ('screenname.vps.nxtcrypto.org'), ('87.230.14.1'), " +
                            "('nacho.damnserver.com'), ('87.229.77.126'), ('bitsy05.vps.nxtcrypto.org'), ('lyynx.vps.nxtcrypto.org'), " +
                            "('209.126.73.156'), ('62.57.115.23'), ('66.30.204.105'), ('vps1.nxtcrypto.org'), " +
                            "('cubie-solar.mjke.de:7873'), ('192.99.212.121'), ('109.90.16.208')");
                } else {
                    apply("INSERT INTO peer (address) VALUES " +
                            "('178.150.207.53'), ('192.241.223.132'), ('node9.mynxtcoin.org'), ('node10.mynxtcoin.org'), " +
                            "('node3.mynxtcoin.org'), ('109.87.169.253'), ('nxtnet.fr'), ('50.112.241.97'), " +
                            "('2.84.142.149'), ('bug.airdns.org'), ('83.212.103.14'), ('62.210.131.30')");
                }*/
            	apply(null);
            case 38:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS full_hash BINARY(32)");
            case 39:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS referenced_transaction_full_hash BINARY(32)");
            case 40:
                BlockDb.deleteAll();
                apply(null);
            case 41:
                apply("ALTER TABLE transaction ALTER COLUMN full_hash SET NOT NULL");
            case 42:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS transaction_full_hash_idx ON transaction (full_hash)");
            case 43:
                apply("UPDATE transaction a SET a.referenced_transaction_full_hash = "
                        + "(SELECT full_hash FROM transaction b WHERE b.id = a.referenced_transaction_id)");
            case 44:
                apply(null);
            case 45:
                BlockchainProcessorImpl.getInstance().validateAtNextScan();
                apply(null);
            case 46:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS attachment_bytes VARBINARY");
            case 47:
                try (Connection con = Db.getConnection();
                     PreparedStatement pstmt = con.prepareStatement("UPDATE transaction SET attachment_bytes = ? where db_id = ?");
                     Statement stmt = con.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SELECT * FROM transaction");
                    while (rs.next()) {
                        long dbId = rs.getLong("db_id");
                        Attachment attachment = (Attachment)rs.getObject("attachment");
                        if (attachment != null) {
                            pstmt.setBytes(1, attachment.getBytes());
                        } else {
                            pstmt.setNull(1, Types.VARBINARY);
                        }
                        pstmt.setLong(2, dbId);
                        pstmt.executeUpdate();
                    }
                    con.commit();
                } catch (SQLException e) {
                    throw new RuntimeException(e.toString(), e);
                }
                apply(null);
            case 48:
                apply("ALTER TABLE transaction DROP COLUMN attachment");
            case 49:
                apply("UPDATE transaction a SET a.referenced_transaction_full_hash = "
                        + "(SELECT full_hash FROM transaction b WHERE b.id = a.referenced_transaction_id) "
                        + "WHERE a.referenced_transaction_full_hash IS NULL");
            case 50:
                apply("ALTER TABLE transaction DROP COLUMN referenced_transaction_id");
            case 51:
                apply("ALTER TABLE transaction DROP COLUMN hash");
            case 52:
                return;
            default:
                throw new RuntimeException("Database inconsistent with code, probably trying to run older code on newer database");
        }
    }

    private DbVersion() {} //never
}
