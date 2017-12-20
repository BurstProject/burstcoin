package brs.db.sql;

import brs.Alias;
import brs.Burst;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.VersionedEntityTable;
import brs.db.store.AliasStore;
import org.jooq.DSLContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static brs.schema.Tables.ALIAS_OFFER;

public abstract class SqlAliasStore implements AliasStore {

  private static final DbKey.LongKeyFactory<Alias.Offer> offerDbKeyFactory = new DbKey.LongKeyFactory<Alias.Offer>("id") {
      @Override
      public BurstKey newKey(Alias.Offer offer) {
        return offer.dbKey;
      }
    };

  @Override
  public BurstKey.LongKeyFactory<Alias.Offer> getOfferDbKeyFactory() {
    return offerDbKeyFactory;
  }

  private static final BurstKey.LongKeyFactory<Alias> aliasDbKeyFactory = new DbKey.LongKeyFactory<Alias>("id") {

      @Override
      public BurstKey newKey(Alias alias) {
        return alias.dbKey;
      }
    };

  @Override
  public BurstKey.LongKeyFactory<Alias> getAliasDbKeyFactory() {
    return aliasDbKeyFactory;
  }

  @Override
  public VersionedEntityTable<Alias> getAliasTable() {
    return aliasTable;
  }

  private class SqlOffer extends Alias.Offer {
    private SqlOffer(ResultSet rs) throws SQLException {
      super(rs.getLong("id"), rs.getLong("price"), rs.getLong("buyer_id"), offerDbKeyFactory.newKey(rs.getLong("id")));
    }
  }

  protected void saveOffer(Alias.Offer offer, Connection con) throws SQLException {
    try ( DSLContext ctx = Db.getDSLContext() ) {
      ctx.insertInto(
              ALIAS_OFFER,
              ALIAS_OFFER.ID, ALIAS_OFFER.PRICE, ALIAS_OFFER.BUYER_ID, ALIAS_OFFER.HEIGHT
      ).values(
              offer.getId(), offer.getPriceNQT(), DbUtils.longZeroToNull(offer.getBuyerId()), Burst.getBlockchain().getHeight()
      ).execute();
    }

    try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO alias_offer (id, price, buyer_id, "
                                                        + "height) VALUES (?, ?, ?, ?)")) {
      int i = 0;
      pstmt.setLong(++i, offer.getId());
      pstmt.setLong(++i, offer.getPriceNQT());
      DbUtils.setLongZeroToNull(pstmt, ++i, offer.getBuyerId());
      pstmt.setInt(++i, Burst.getBlockchain().getHeight());
      pstmt.executeUpdate();
    }
  }

  private final VersionedEntityTable<Alias.Offer> offerTable = new VersionedEntitySqlTable<Alias.Offer>("alias_offer", ALIAS_OFFER, offerDbKeyFactory) {
      @Override
      protected Alias.Offer load(Connection con, ResultSet rs) throws SQLException {
        return new SqlOffer(rs);
      }

      @Override
      protected void save(Connection con, Alias.Offer offer) throws SQLException {
        saveOffer(offer, con);
      }
    };

  @Override
  public VersionedEntityTable<Alias.Offer> getOfferTable() {
    return offerTable;
  }

  private class SqlAlias extends Alias {
    private SqlAlias(ResultSet rs) throws SQLException {
      super(
            rs.getLong("id"),
            rs.getLong("account_id"),
            rs.getString("alias_name"),
            rs.getString("alias_uri"),
            rs.getInt("timestamp"),
            aliasDbKeyFactory.newKey(rs.getLong("id"))
            );
    }
  }

  protected void saveAlias(Alias alias, Connection con) throws SQLException {
    try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO alias (id, account_id, alias_name, "
                                                        + "alias_uri, timestamp, height) "
                                                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
      int i = 0;
      pstmt.setLong(++i, alias.getId());
      pstmt.setLong(++i, alias.getAccountId());
      pstmt.setString(++i, alias.getAliasName());
      pstmt.setString(++i, alias.getAliasURI());
      pstmt.setInt(++i, alias.getTimestamp());
      pstmt.setInt(++i, Burst.getBlockchain().getHeight());
      pstmt.executeUpdate();
    }
  }

  private final VersionedEntityTable<Alias> aliasTable = new VersionedEntitySqlTable<Alias>("alias", brs.schema.Tables.ALIAS, aliasDbKeyFactory) {
      @Override
      protected Alias load(Connection con, ResultSet rs) throws SQLException {
        return new SqlAlias(rs);
      }

      @Override
      protected void save(Connection con, Alias alias) throws SQLException {
        saveAlias(alias, con);
      }

      @Override
      protected String defaultSort() {
        return " ORDER BY alias_name_lower ";
      }
    };

  @Override
  public BurstIterator<Alias> getAliasesByOwner(long accountId, int from, int to) {
    return aliasTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
  }

  @Override
  public Alias getAlias(String aliasName) {
    return aliasTable.getBy(new DbClause.StringClause("alias_name_lower", aliasName.toLowerCase()));
  }

}
