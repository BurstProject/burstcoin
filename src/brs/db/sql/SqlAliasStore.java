package brs.db.sql;

import brs.Alias;
import brs.Burst;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.VersionedEntityTable;
import brs.db.store.AliasStore;
import org.jooq.DSLContext;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import static brs.schema.Tables.ALIAS;
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

  protected void saveOffer(Alias.Offer offer) throws SQLException {
    try (DSLContext ctx = Db.getDSLContext()) {
      ctx.insertInto(
              ALIAS_OFFER,
              ALIAS_OFFER.ID, ALIAS_OFFER.PRICE, ALIAS_OFFER.BUYER_ID, ALIAS_OFFER.HEIGHT
      ).values(
              offer.getId(), offer.getPriceNQT(), DbUtils.longZeroToNull(offer.getBuyerId()), Burst.getBlockchain().getHeight()
      ).execute();
    }
  }

  private final VersionedEntityTable<Alias.Offer> offerTable = new VersionedEntitySqlTable<Alias.Offer>("alias_offer", ALIAS_OFFER, offerDbKeyFactory) {
      @Override
      protected Alias.Offer load(Connection con, ResultSet rs) throws SQLException {
        return new SqlOffer(rs);
      }

      @Override
      protected void save(Connection con, Alias.Offer offer) throws SQLException {
        saveOffer(offer);
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
    try (DSLContext ctx = Db.getDSLContext()) {
      ctx.insertInto(ALIAS).
              set(ALIAS.ID, alias.getId()).
              set(ALIAS.ACCOUNT_ID, alias.getAccountId()).
              set(ALIAS.ALIAS_NAME, alias.getAliasName()).
              set(ALIAS.ALIAS_URI, alias.getAliasURI()).
              set(ALIAS.TIMESTAMP, alias.getTimestamp()).
              set(ALIAS.HEIGHT, Burst.getBlockchain().getHeight()).execute();
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
