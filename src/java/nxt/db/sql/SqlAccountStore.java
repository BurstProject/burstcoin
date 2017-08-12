package nxt.db.sql;

import nxt.Account;
import nxt.Nxt;
import nxt.db.store.AccountStore;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by jens on 10.08.2017.
 */
public class SqlAccountStore extends AccountStore
{
    private class SqlAccount extends Account
    {
        SqlAccount(Long id)
        {
            super(id);
        }

        SqlAccount(ResultSet rs) throws SQLException
        {
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

    public void batch(PreparedStatement pstmt, Account account) throws SQLException
    {
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

    @Override
    public void saveAccountAsset(Account.AccountAsset accountAsset)
    {

    }
}
