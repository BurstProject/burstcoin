package nxt;

import nxt.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
                stmt.executeUpdate("UPDATE version SET next_update = next_update + 1");
                con.commit();
            } catch (Exception e) {
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
                apply(null);
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
                apply(null);
            case 29:
                apply(null);
            case 30:
                apply(null);
            case 31:
                apply(null);
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
                            "('54.84.4.195'), ('139.30.62.180'), ('bitsy05.vps.nxtcrypto.org'), ('128.199.151.235'), ('199.233.245.105'), " +
                            "('54.201.228.27'), ('209.126.70.159'), ('77.123.25.170'), ('wallet.nxtty.com'), ('198.27.64.207'), " +
                            "('188.226.245.226'), ('79.215.198.253'), ('88.12.55.125'), ('nxt.sx'), ('24.149.8.238'), ('abctc.vps.nxtcrypto.org'), " +
                            "('162.243.198.24'), ('178.32.221.221'), ('nxt3.grit.tk:37874'), ('54.187.153.45'), ('24.161.110.115'), " +
                            "('nxtnode.hopto.org'), ('89.12.92.15'), ('191.238.101.73'), ('nxt.homer.ru'), ('nxtcoint119a.no-ip.org'), " +
                            "('84.46.53.162'), ('178.15.99.67'), ('107.17.66.127'), ('77.179.112.19'), ('195.154.136.165'), ('210.188.36.5'), " +
                            "('212.18.225.173'), ('109.254.63.44'), ('90.146.62.91'), ('nxt01.now.im'), ('node10.mynxt.info'), ('198.98.119.85'), " +
                            "('zobue.com'), ('89.250.240.56'), ('bitsy08.vps.nxtcrypto.org'), ('beor.homeip.net'), ('vps12.nxtcrypto.org'), " +
                            "('188.35.156.10'), ('54.88.54.58'), ('77.249.237.229'), ('mycrypto.no-ip.biz'), ('213.46.57.77'), ('nxtpi.zapto.org'), " +
                            "('nxt4.grit.tk'), ('80.82.209.82'), ('ns1.anameserver.de'), ('46.28.111.249'), ('178.194.110.193'), ('106.187.95.232'), " +
                            "('nebula-gw.hopto.org'), ('vps3.nxtcrypto.org'), ('198.57.198.33'), ('168.63.232.16'), ('77.179.97.27'), " +
                            "('178.33.203.157'), ('79.24.191.97'), ('bitsy04.vps.nxtcrypto.org'), ('humanoide.thican.net'), " +
                            "('bitsy09.vps.nxtcrypto.org'), ('bitsy07.vps.nxtcrypto.org'), ('173.17.82.130'), ('vps2.nxtcrypto.org'), " +
                            "('80.86.92.139'), ('180.157.101.215'), ('54.214.250.209'), ('87.230.14.1'), ('zdani.szn.dk'), ('nxt5.webice.ru'), " +
                            "('188.226.206.41'), ('nxt7.webice.ru'), ('nxt1.grit.tk:17874'), ('node13.mynxt.info'), ('90.153.116.122'), " +
                            "('66.30.204.105'), ('nxt1.webice.ru'), ('63.165.243.112'), ('oldnbold.vps.nxtcrypto.org'), ('sluni.szn.dk'), " +
                            "('105.229.177.106'), ('88.163.78.131'), ('80.153.101.190'), ('188.138.88.154'), ('85.25.198.120'), ('89.250.243.200'), " +
                            "('nxtx.ru'), ('87.139.122.48'), ('188.226.179.119'), ('vps5.nxtcrypto.org'), ('dreschel2.dyndns.org'), " +
                            "('67.212.71.173'), ('88.153.5.179'), ('rigel1.ddns.net'), ('77.58.253.73'), ('miasik.no-ip.org'), ('217.26.24.27'), " +
                            "('cyborg.thican.net'), ('cryptkeeper.vps.nxtcrypto.org'), ('cryonet.de'), ('nxt2.webice.ru'), ('43.252.230.48'), " +
                            "('91.202.253.240'), ('nxtforgersr.ddns.net'), ('silvanoip.dhcp.biz'), ('85.214.222.82'), ('87.139.122.157'), " +
                            "('54.164.174.175'), ('24.230.136.187'), ('128.199.189.226'), ('101.164.96.109'), ('allbits.vps.nxtcrypto.org'), " +
                            "('211.149.213.86'), ('node7.mynxt.info'), ('107.170.189.27'), ('89.12.202.115'), ('49.245.169.94'), ('91.121.41.192'), " +
                            "('xeqtorcreed.vps.nxtcrypto.org'), ('knattereule.sytes.net'), ('jefdiesel.vps.nxtcrypto.org'), ('node1.mynxt.info'), " +
                            "('111.199.191.99'), ('nxt1107.no-ip.biz'), ('95.68.48.56'), ('87.138.143.21'), ('mycoinmine.org'), ('46.173.9.98'), " +
                            "('ct.flipflop.mooo.com'), ('83.172.25.92'), ('xyzzyx.vps.nxtcrypto.org'), ('31.15.211.201'), ('84.128.162.88'), " +
                            "('80.86.92.70'), ('108.61.57.76'), ('samson.vps.nxtcrypto.org'), ('screenname.vps.nxtcrypto.org'), ('nxt.hofhom.nl'), " +
                            "('111.194.210.159'), ('58.95.145.117'), ('95.68.86.118'), ('54.245.255.250'), ('bitsy06.vps.nxtcrypto.org'), " +
                            "('vps4.nxtcrypto.org'), ('54.88.120.117'), ('54.164.98.250'), ('46.165.208.108'), ('xeqtorcreed2.vps.nxtcrypto.org'), " +
                            "('vps1.nxtcrypto.org'), ('nxt8.webice.ru'), ('162.201.61.133'), ('54.191.138.175'), ('54.85.132.143'), " +
                            "('80.86.92.66'), ('84.242.91.139'), ('82.211.30.183'), ('bitsy03.vps.nxtcrypto.org'), ('vh44.ddns.net:7873'), " +
                            "('node0.forgenxt.com'), ('185.4.72.115'), ('108.170.9.170'), ('54.200.164.31'), ('bitsy01.vps.nxtcrypto.org'), " +
                            "('178.150.207.53'), ('71.90.207.16'), ('107.170.3.62'), ('54.86.132.52'), ('85.214.199.215'), ('cubie-solar.mjke.de'), " +
                            "('nxt2.grit.tk:27874'), ('108.231.13.250'), ('104.131.254.22'), ('cerub.ddns.net'), ('216.36.3.42'), " +
                            "('37.120.168.131'), ('54.77.63.53'), ('93.103.20.35'), ('54.213.222.141'), ('162.243.213.190'), ('89.250.240.60'), " +
                            "('67.255.7.120'), ('87.254.182.122'), ('95.143.216.60'), ('79.164.108.1'), ('67.212.71.171'), " +
                            "('node15.mynxt.info'), ('192.51.0.12'), ('76.164.201.91'), ('88.198.74.99'), ('bitsy10.vps.nxtcrypto.org'), " +
                            "('node2.mynxt.info'), ('vps9.nxtcrypto.org'), ('46.165.209.144'), ('46.4.212.230'), ('185.12.44.108'), " +
                            "('162.243.87.10'), ('76.250.84.156'), ('91.69.121.229'), ('178.24.158.31'), ('209.126.70.170'), ('93.129.172.14'), " +
                            "('93.219.140.96'), ('serras.homenet.org'), ('89.72.57.246'), ('37.187.21.28'), ('lyynx.vps.nxtcrypto.org'), " +
                            "('105.224.254.145'), ('195.154.174.124'), ('184.57.30.220'), ('54.83.4.11'), ('nxt.phukhew.com'), ('nxt5.grit.tk'), " +
                            "('nxt4.webice.ru'), ('162.243.145.83'), ('89.133.34.109'), ('73.36.141.199'), ('90.153.30.215'), " +
                            "('209.126.70.156'), ('bitsy02.vps.nxtcrypto.org'), ('pakisnxt.no-ip.org'), ('188.167.90.118'), ('107.170.164.129'), " +
                            "('23.102.0.45'), ('46.194.8.79')");
                } else {
                    apply("INSERT INTO peer (address) VALUES " +
                            "('178.150.207.53'), ('192.241.223.132'), ('node9.mynxtcoin.org'), ('node10.mynxtcoin.org'), " +
                            "('node3.mynxtcoin.org'), ('109.87.169.253'), ('nxtnet.fr'), ('50.112.241.97'), " +
                            "('2.84.142.149'), ('bug.airdns.org'), ('83.212.103.14'), ('62.210.131.30'), ('104.131.254.22'), " +
                            "('46.28.111.249'), ('94.79.54.205')");
                }*/
            	apply(null);
            case 38:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS full_hash BINARY(32)");
            case 39:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS referenced_transaction_full_hash BINARY(32)");
            case 40:
                apply(null);
            case 41:
                apply("ALTER TABLE transaction ALTER COLUMN full_hash SET NOT NULL");
            case 42:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS transaction_full_hash_idx ON transaction (full_hash)");
            case 43:
                apply(null);
            case 44:
                apply(null);
            case 45:
                apply(null);
            case 46:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS attachment_bytes VARBINARY");
            case 47:
                BlockDb.deleteAll();
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
                if (Constants.isTestnet) {
                    BlockchainProcessorImpl.getInstance().validateAtNextScan();
                }
                apply(null);
            case 53:
                apply("DROP INDEX transaction_recipient_id_idx");
            case 54:
                apply("ALTER TABLE transaction ALTER COLUMN recipient_id SET NULL");
            case 55:
                try (Connection con = Db.getConnection();
                     Statement stmt = con.createStatement();
                     PreparedStatement pstmt = con.prepareStatement("UPDATE transaction SET recipient_id = null WHERE type = ? AND subtype = ?")) {
                    try {
                        for (byte type = 0; type <= 4; type++) {
                            for (byte subtype = 0; subtype <= 8; subtype++) {
                                TransactionType transactionType = TransactionType.findTransactionType(type, subtype);
                                if (transactionType == null) {
                                    continue;
                                }
                                if (!transactionType.hasRecipient()) {
                                    pstmt.setByte(1, type);
                                    pstmt.setByte(2, subtype);
                                    pstmt.executeUpdate();
                                }
                            }
                        }
                        stmt.executeUpdate("UPDATE version SET next_update = next_update + 1");
                        con.commit();
                    } catch (SQLException e) {
                        con.rollback();
                        throw e;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            case 56:
                apply("CREATE INDEX IF NOT EXISTS transaction_recipient_id_idx ON transaction (recipient_id)");
            case 57:
                apply("DROP INDEX transaction_timestamp_idx");
            case 58:
                apply("CREATE INDEX IF NOT EXISTS transaction_timestamp_idx ON transaction (timestamp DESC)");
            case 59:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS version TINYINT");
            case 60:
                apply("UPDATE transaction SET version = 0");
            case 61:
                apply("ALTER TABLE transaction ALTER COLUMN version SET NOT NULL");
            case 62:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS has_message BOOLEAN NOT NULL DEFAULT FALSE");
            case 63:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS has_encrypted_message BOOLEAN NOT NULL DEFAULT FALSE");
            case 64:
                apply("UPDATE transaction SET has_message = TRUE WHERE type = 1 AND subtype = 0");
            case 65:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS has_public_key_announcement BOOLEAN NOT NULL DEFAULT FALSE");
            case 66:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS ec_block_height INT DEFAULT NULL");
            case 67:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS ec_block_id BIGINT DEFAULT NULL");
            case 68:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS has_encrypttoself_message BOOLEAN NOT NULL DEFAULT FALSE");
            case 69:
                apply("CREATE INDEX IF NOT EXISTS transaction_block_timestamp_idx ON transaction (block_timestamp DESC)");
            case 70:
                apply("DROP INDEX transaction_timestamp_idx");
            case 71:
            	apply("ALTER TABLE transaction ALTER COLUMN signature SET NULL");
            case 72:
                return;
            default:
                throw new RuntimeException("Database inconsistent with code, probably trying to run older code on newer database");
        }
    }

    private DbVersion() {} //never
}
