package brs.db.sql;

import brs.Order;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.VersionedEntityTable;
import brs.db.store.OrderStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jooq.impl.TableImpl;
import org.jooq.DSLContext;
import org.jooq.SelectQuery;
import brs.schema.Tables.*;

public class SqlOrderStore implements OrderStore {
  protected DbKey.LongKeyFactory<Order.Ask> askOrderDbKeyFactory = new DbKey.LongKeyFactory<Order.Ask>("id") {

      @Override
      public BurstKey newKey(Order.Ask ask) {
        return ask.dbKey;
      }

    };
  protected VersionedEntityTable<Order.Ask> askOrderTable = new VersionedEntitySqlTable<Order.Ask>("ask_order", brs.schema.Tables.ASK_ORDER, askOrderDbKeyFactory) {
      @Override
      protected Order.Ask load(DSLContext ctx, ResultSet rs) throws SQLException {
        return new SqlAsk(rs);
      }

      @Override
      protected void save(DSLContext ctx, Order.Ask ask) throws SQLException {
        saveAsk(ctx, brs.schema.Tables.ASK_ORDER, ask);
      }

      @Override
      protected String defaultSort() {
        return " ORDER BY creation_height DESC ";
      }

    };
  private DbKey.LongKeyFactory<Order.Bid> bidOrderDbKeyFactory = new DbKey.LongKeyFactory<Order.Bid>("id") {

      @Override
      public BurstKey newKey(Order.Bid bid) {
        return bid.dbKey;
      }

    };
  protected VersionedEntityTable<Order.Bid> bidOrderTable = new VersionedEntitySqlTable<Order.Bid>("bid_order", brs.schema.Tables.BID_ORDER, bidOrderDbKeyFactory) {

      @Override
      protected Order.Bid load(DSLContext ctx, ResultSet rs) throws SQLException {
        return new SqlBid(rs);
      }

      @Override
      protected void save(DSLContext ctx, Order.Bid bid) throws SQLException {
        saveBid(ctx, brs.schema.Tables.BID_ORDER, bid);
      }

      @Override
      protected String defaultSort() {
        return " ORDER BY creation_height DESC ";
      }

    };

  @Override
  public VersionedEntityTable<Order.Bid> getBidOrderTable() {
    return bidOrderTable;
  }

  @Override
  public BurstIterator<Order.Ask> getAskOrdersByAccountAsset(final long accountId, final long assetId, int from, int to) {
    return askOrderTable.getManyBy(
      brs.schema.Tables.ASK_ORDER.ACCOUNT_ID.eq(accountId).and(
        brs.schema.Tables.ASK_ORDER.ASSET_ID.eq(assetId)
      ),
      from,
      to
    );
  }

  @Override
  public BurstIterator<Order.Ask> getSortedAsks(long assetId, int from, int to) {
    return askOrderTable.getManyBy(brs.schema.Tables.ASK_ORDER.ASSET_ID.eq(assetId), from, to,
                                   " ORDER BY price ASC, creation_height ASC, id ASC ");
  }

  @Override
  public Order.Ask getNextOrder(long assetId) {
    try ( DSLContext ctx = Db.getDSLContext() ) {
      SelectQuery query = ctx.selectFrom(brs.schema.Tables.ASK_ORDER).where(
        brs.schema.Tables.ASK_ORDER.ASSET_ID.eq(assetId).and(brs.schema.Tables.ASK_ORDER.LATEST.isTrue())
      ).orderBy(
        brs.schema.Tables.ASK_ORDER.PRICE.asc(),
        brs.schema.Tables.ASK_ORDER.CREATION_HEIGHT.asc(),
        brs.schema.Tables.ASK_ORDER.ID.asc()
      ).limit(1).getQuery();
      try (BurstIterator<Order.Ask> askOrders = askOrderTable.getManyBy(ctx, query, true)) {
        return askOrders.hasNext() ? askOrders.next() : null;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  @Override
  public BurstIterator<Order.Ask> getAll(int from, int to) {
    return askOrderTable.getAll(from, to);
  }

  @Override
  public BurstIterator<Order.Ask> getAskOrdersByAccount(long accountId, int from, int to) {
    return askOrderTable.getManyBy(brs.schema.Tables.ASK_ORDER.ACCOUNT_ID.eq(accountId), from, to);
  }

  @Override
  public BurstIterator<Order.Ask> getAskOrdersByAsset(long assetId, int from, int to) {
    return askOrderTable.getManyBy(brs.schema.Tables.ASK_ORDER.ASSET_ID.eq(assetId), from, to);
  }

  protected void saveOrder(DSLContext ctx, TableImpl table, Order order) throws SQLException {
    ctx.mergeInto(
      table,
      table.field("ID"), table.field("ACCOUNT_ID"), table.field("ASSET_ID"), table.field("PRICE"),
      table.field("QUANTITY"), table.field("CREATION_HEIGHT"), table.field("HEIGHT"), table.field("LATEST")
    )
    .key(table.field("ID"), table.field("HEIGHT"))
    .values(
      order.getId(), order.getAccountId(), order.getAssetId(), order.getPriceNQT(),
      order.getQuantityQNT(), order.getHeight(), brs.Burst.getBlockchain().getHeight(), true
    )
    .execute();
  }

  private void saveAsk(DSLContext ctx, TableImpl table, Order.Ask ask) throws SQLException {
    saveOrder(ctx, table, ask);
  }

  @Override
  public DbKey.LongKeyFactory<Order.Ask> getAskOrderDbKeyFactory() {
    return askOrderDbKeyFactory;
  }

  @Override
  public VersionedEntityTable<Order.Ask> getAskOrderTable() {
    return askOrderTable;
  }

  @Override
  public DbKey.LongKeyFactory<Order.Bid> getBidOrderDbKeyFactory() {
    return bidOrderDbKeyFactory;
  }

  @Override
  public BurstIterator<Order.Bid> getBidOrdersByAccount(long accountId, int from, int to) {
    return bidOrderTable.getManyBy(brs.schema.Tables.BID_ORDER.ACCOUNT_ID.eq(accountId), from, to);
  }

  @Override
  public BurstIterator<Order.Bid> getBidOrdersByAsset(long assetId, int from, int to) {
    return bidOrderTable.getManyBy(brs.schema.Tables.BID_ORDER.ASSET_ID.eq(assetId), from, to);
  }

  @Override
  public BurstIterator<Order.Bid> getBidOrdersByAccountAsset(final long accountId, final long assetId, int from, int to) {
    return bidOrderTable.getManyBy(
      brs.schema.Tables.BID_ORDER.ACCOUNT_ID.eq(accountId).and(
        brs.schema.Tables.BID_ORDER.ASSET_ID.eq(assetId)
      ),
      from,
      to
    );
  }

  @Override
  public BurstIterator<Order.Bid> getSortedBids(long assetId, int from, int to) {

    return bidOrderTable.getManyBy(brs.schema.Tables.BID_ORDER.ASSET_ID.eq(assetId), from, to,
                                   " ORDER BY price DESC, creation_height ASC, id ASC ");
  }

  @Override
  public Order.Bid getNextBid(long assetId) {
    try (DSLContext ctx = Db.getDSLContext() ) {
      SelectQuery query = ctx.selectFrom(brs.schema.Tables.BID_ORDER).where(
        brs.schema.Tables.BID_ORDER.LATEST.isTrue()
      ).orderBy(
        brs.schema.Tables.BID_ORDER.PRICE.desc(),
        brs.schema.Tables.BID_ORDER.CREATION_HEIGHT.asc(),
        brs.schema.Tables.BID_ORDER.ID.asc()
      ).limit(1).getQuery();
      try (BurstIterator<Order.Bid> bidOrders = bidOrderTable.getManyBy(ctx, query, true)) {
        return bidOrders.hasNext() ? bidOrders.next() : null;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  private void saveBid(DSLContext ctx, TableImpl table, Order.Bid bid) throws SQLException {
    saveOrder(ctx, table, bid);
  }

  protected class SqlAsk extends Order.Ask {
    private SqlAsk(ResultSet rs) throws SQLException {
      super(
            rs.getLong("id"),
            rs.getLong("account_id"),
            rs.getLong("asset_id"),
            rs.getLong("price"),
            rs.getInt("creation_height"),
            rs.getLong("quantity"),
            askOrderDbKeyFactory.newKey(rs.getLong("id"))
            );
    }
  }

  protected class SqlBid extends Order.Bid {
    private SqlBid(ResultSet rs) throws SQLException {
      super(
            rs.getLong("id"),
            rs.getLong("account_id"),
            rs.getLong("asset_id"),
            rs.getLong("price"),
            rs.getInt("creation_height"),
            rs.getLong("quantity"),
            bidOrderDbKeyFactory.newKey(rs.getLong("id"))
            );
    }


  }

}
