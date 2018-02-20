package brs.db.sql;

import brs.Order;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.VersionedEntityTable;
import brs.db.store.DerivedTableManager;
import brs.db.store.OrderStore;
import java.util.ArrayList;
import java.util.List;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.jooq.impl.TableImpl;
import org.jooq.DSLContext;
import org.jooq.SortField;
import org.jooq.SelectQuery;
import org.jooq.Field;

public class SqlOrderStore implements OrderStore {
  protected DbKey.LongKeyFactory<Order.Ask> askOrderDbKeyFactory = new DbKey.LongKeyFactory<Order.Ask>("id") {

      @Override
      public BurstKey newKey(Order.Ask ask) {
        return ask.dbKey;
      }

    };
  protected VersionedEntityTable<Order.Ask> askOrderTable;

  public SqlOrderStore(DerivedTableManager derivedTableManager) {
    askOrderTable = new VersionedEntitySqlTable<Order.Ask>("ask_order", brs.schema.Tables.ASK_ORDER, askOrderDbKeyFactory, derivedTableManager) {
      @Override
      protected Order.Ask load(DSLContext ctx, ResultSet rs) throws SQLException {
        return new SqlAsk(rs);
      }

      @Override
      protected void save(DSLContext ctx, Order.Ask ask) throws SQLException {
        saveAsk(ctx, brs.schema.Tables.ASK_ORDER, ask);
      }

      @Override
      protected List<SortField> defaultSort() {
        List<SortField> sort = new ArrayList<>();
        sort.add(tableClass.field("creation_height", Integer.class).desc());
        return sort;
      }
    };

    bidOrderTable = new VersionedEntitySqlTable<Order.Bid>("bid_order", brs.schema.Tables.BID_ORDER, bidOrderDbKeyFactory, derivedTableManager) {

      @Override
      protected Order.Bid load(DSLContext ctx, ResultSet rs) throws SQLException {
        return new SqlBid(rs);
      }

      @Override
      protected void save(DSLContext ctx, Order.Bid bid) throws SQLException {
        saveBid(ctx, brs.schema.Tables.BID_ORDER, bid);
      }

      @Override
      protected List<SortField> defaultSort() {
        List<SortField> sort = new ArrayList<>();
        sort.add(tableClass.field("creation_height", Integer.class).desc());
        return sort;
      }

    };

  }

  private DbKey.LongKeyFactory<Order.Bid> bidOrderDbKeyFactory = new DbKey.LongKeyFactory<Order.Bid>("id") {

      @Override
      public BurstKey newKey(Order.Bid bid) {
        return bid.dbKey;
      }

    };
  protected VersionedEntityTable<Order.Bid> bidOrderTable;

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
    List<SortField> sort = new ArrayList<>();
    sort.add(brs.schema.Tables.ASK_ORDER.field("price", Long.class).asc());
    sort.add(brs.schema.Tables.ASK_ORDER.field("creation_height", Integer.class).asc());
    sort.add(brs.schema.Tables.ASK_ORDER.field("id", Long.class).asc());
    return askOrderTable.getManyBy(brs.schema.Tables.ASK_ORDER.ASSET_ID.eq(assetId), from, to, sort);
  }

  @Override
  public Order.Ask getNextOrder(long assetId) {
    DSLContext ctx = Db.getDSLContext();
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

  private void saveAsk(DSLContext ctx, TableImpl table, Order.Ask ask) throws SQLException {
    brs.schema.tables.records.AskOrderRecord askOrderRecord = ctx.newRecord(brs.schema.Tables.ASK_ORDER);
    askOrderRecord.setId(ask.getId());
    askOrderRecord.setAccountId(ask.getAccountId());
    askOrderRecord.setAssetId(ask.getAssetId());
    askOrderRecord.setPrice(ask.getPriceNQT());
    askOrderRecord.setQuantity(ask.getQuantityQNT());
    askOrderRecord.setCreationHeight(ask.getHeight());
    askOrderRecord.setHeight(brs.Burst.getBlockchain().getHeight());
    askOrderRecord.setLatest(true);
    DbUtils.mergeInto(
      ctx, askOrderRecord, table,
      ( new Field[] { askOrderRecord.field("id"), askOrderRecord.field("height") } )
    );
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
    List<SortField> sort = new ArrayList<>();
    sort.add(brs.schema.Tables.BID_ORDER.field("price", Long.class).desc());
    sort.add(brs.schema.Tables.BID_ORDER.field("creation_height", Integer.class).asc());
    sort.add(brs.schema.Tables.BID_ORDER.field("id", Long.class).asc());
    return bidOrderTable.getManyBy(brs.schema.Tables.BID_ORDER.ASSET_ID.eq(assetId), from, to, sort);
  }

  @Override
  public Order.Bid getNextBid(long assetId) {
    DSLContext ctx = Db.getDSLContext();
    SelectQuery query = ctx.selectFrom(brs.schema.Tables.BID_ORDER).where(
      brs.schema.Tables.BID_ORDER.ASSET_ID.eq(assetId).and(brs.schema.Tables.BID_ORDER.LATEST.isTrue())
    ).orderBy(
      brs.schema.Tables.BID_ORDER.PRICE.desc(),
      brs.schema.Tables.BID_ORDER.CREATION_HEIGHT.asc(),
      brs.schema.Tables.BID_ORDER.ID.asc()
    ).limit(1).getQuery();
    try (BurstIterator<Order.Bid> bidOrders = bidOrderTable.getManyBy(ctx, query, true)) {
      return bidOrders.hasNext() ? bidOrders.next() : null;
    }
  }

  private void saveBid(DSLContext ctx, TableImpl table, Order.Bid bid) throws SQLException {
    brs.schema.tables.records.BidOrderRecord bidOrderRecord = ctx.newRecord(brs.schema.Tables.BID_ORDER);
    bidOrderRecord.setId(bid.getId());
    bidOrderRecord.setAccountId(bid.getAccountId());
    bidOrderRecord.setAssetId(bid.getAssetId());
    bidOrderRecord.setPrice(bid.getPriceNQT());
    bidOrderRecord.setQuantity(bid.getQuantityQNT());
    bidOrderRecord.setCreationHeight(bid.getHeight());
    bidOrderRecord.setHeight(brs.Burst.getBlockchain().getHeight());
    bidOrderRecord.setLatest(true);
    DbUtils.mergeInto(
      ctx, bidOrderRecord, table,
      ( new Field[] { bidOrderRecord.field("id"), bidOrderRecord.field("height") } )
    );
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
