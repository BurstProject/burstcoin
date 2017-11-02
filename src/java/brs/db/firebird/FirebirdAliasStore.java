package brs.db.firebird;

import brs.Alias;
import brs.Nxt;
import brs.db.sql.SqlAliasStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class FirebirdAliasStore extends SqlAliasStore {
    @Override
    protected void saveAlias(Alias alias, Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO alias (id, account_id, alias_name, "
                + "alias_uri, \"timestamp\", height) "
                + "VALUES (?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, alias.getId());
            pstmt.setLong(++i, alias.getAccountId());
            pstmt.setString(++i, alias.getAliasName());
            pstmt.setString(++i, alias.getAliasURI());
            pstmt.setInt(++i, alias.getTimestamp());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }
}
