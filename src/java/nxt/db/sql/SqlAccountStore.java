package nxt.db.sql;

import nxt.Account;
import nxt.Nxt;
import nxt.db.VersionedBatchEntityTable;
import nxt.db.store.AccountStore;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Created by jens on 10.08.2017.
 */
public class SqlAccountStore extends AccountStore {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SqlAccountStore.class);

    protected static final DbKey.LongKeyFactory<Account> accountDbKeyFactory = new DbKey.LongKeyFactory<Account>("id") {

        @Override
        public DbKey newKey(Account account) {
            return account.dbKey;
        }
    };

    protected class SqlAccount extends Account {
        SqlAccount(Long id) {
            super(id);
        }

        SqlAccount(ResultSet rs) throws SQLException {
            super(rs.getLong("id"), accountDbKeyFactory.newKey(rs.getLong("id")),
                    rs.getInt("creation_height"));
            this.publicKey = rs.getBytes("public_key");
            this.keyHeight = rs.getInt("key_height");
            this.balanceNQT = rs.getLong("balance");
            this.unconfirmedBalanceNQT = rs.getLong("unconfirmed_balance");
            this.forgedBalanceNQT = rs.getLong("forged_balance");
            this.name = rs.getString("name");
            this.description = rs.getString("description");
        }
    }

    protected void doBatch(PreparedStatement pstmt, Account account) throws SQLException {
        int i = 0;
        pstmt.setLong(++i, account.getId());
        pstmt.setInt(++i, account.getCreationHeight());
        DbUtils.setBytes(pstmt, ++i, account.getPublicKey());
        pstmt.setInt(++i, account.getKeyHeight());
        pstmt.setLong(++i, account.getBalanceNQT());
        pstmt.setLong(++i, account.getUnconfirmedBalanceNQT());
        pstmt.setLong(++i, account.getForgedBalanceNQT());
        DbUtils.setString(pstmt, ++i, account.getName());
        DbUtils.setString(pstmt, ++i, account.getDescription());
        pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
        pstmt.addBatch();
    }


    public void saveAccountAsset(Account.AccountAsset accountAsset, Connection con) {
        try (PreparedStatement pstmt = con.prepareStatement("REPLACE INTO account_asset "
                + "(account_id, asset_id, quantity, unconfirmed_quantity, height, latest) "
                + "VALUES (?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, accountAsset.accountId);
            pstmt.setLong(++i, accountAsset.assetId);
            pstmt.setLong(++i, accountAsset.quantityQNT);
            pstmt.setLong(++i, accountAsset.unconfirmedQuantityQNT);
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        } catch (SQLException e)
        {
            logger.error("Error saving account asset", e);
        }
    }


    @Override
    public VersionedBatchEntityTable<Account> getAccountTable() {


        return new VersionedBatchEntitySqlTable<Account>("account", accountDbKeyFactory) {
            @Override
            protected Account load(Connection con, ResultSet rs) throws SQLException {
                return new SqlAccount(rs);
            }

        /*@Override
        protected void save(Connection con, Account account) throws SQLException {
            account.save(con);
        }*/

            @Override
            protected String updateQuery() {
                return "REPLACE INTO account (id, creation_height, public_key, key_height, balance, unconfirmed_balance, " +
                        "forged_balance, name, description, height, latest) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)";
            }

            @Override
            protected void batch(PreparedStatement pstmt, Account account) throws SQLException {
                doBatch(pstmt, account);
            }

        };
    }
}
