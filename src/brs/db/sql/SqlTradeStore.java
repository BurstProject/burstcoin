package brs.db.sql;

import brs.Trade;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.store.TradeStore;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.jooq.DSLContext;

import static brs.schema.Tables.TRADE;

public class SqlTradeStore implements TradeStore {
  private final DbKey.LinkKeyFactory<Trade> tradeDbKeyFactory = new DbKey.LinkKeyFactory<Trade>("ask_order_id", "bid_order_id") {

      @Override
      public BurstKey newKey(Trade trade) {
        return trade.dbKey;
      }

    };

  private final EntitySqlTable<Trade> tradeTable = new EntitySqlTable<Trade>("trade", TRADE, tradeDbKeyFactory) {

      @Override
      protected Trade load(DSLContext ctx, ResultSet rs) throws SQLException {
        return new SqlTrade(rs);
      }

      @Override
      protected void save(DSLContext ctx, Trade trade) throws SQLException {
        saveTrade(ctx, trade);
      }

    };

  @Override
  public BurstIterator<Trade> getAllTrades(int from, int to) {
    return tradeTable.getAll(from, to);
  }

  @Override
  public BurstIterator<Trade> getAssetTrades(long assetId, int from, int to) {
    return tradeTable.getManyBy(TRADE.ASSET_ID.eq(assetId), from, to);
  }

  @Override
  public BurstIterator<Trade> getAccountTrades(long accountId, int from, int to) {
    try ( DSLContext ctx = Db.getDSLContext() ) {
      return tradeTable.getManyBy(
        ctx,
        ctx
          .selectFrom(TRADE).where(
            TRADE.SELLER_ID.eq(accountId)
          )
          .unionAll(
            ctx.selectFrom(TRADE).where(
              TRADE.BUYER_ID.eq(accountId).and(
                TRADE.SELLER_ID.ne(accountId)
              )
            )
          )
          .orderBy(TRADE.HEIGHT.desc()).limit(from, to)
          .getQuery(),
        false
      );
    }
    catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  @Override
  public BurstIterator<Trade> getAccountAssetTrades(long accountId, long assetId, int from, int to) {
    try ( DSLContext ctx = Db.getDSLContext() ) {
      return tradeTable.getManyBy(
        ctx,
        ctx
          .selectFrom(TRADE).where(
            TRADE.SELLER_ID.eq(accountId).and(TRADE.ASSET_ID.eq(assetId))
          )
          .unionAll(
            ctx.selectFrom(TRADE).where(
              TRADE.BUYER_ID.eq(accountId)).and(
                TRADE.SELLER_ID.ne(accountId)
              ).and(TRADE.ASSET_ID.eq(assetId))
          )
          .orderBy(TRADE.HEIGHT.desc()).limit(from, to)
          .getQuery(),
        false
      );
    }
    catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  @Override
  public int getTradeCount(long assetId) {
    try (DSLContext ctx = Db.getDSLContext()) {
      return ctx.fetchCount(ctx.selectFrom(TRADE).where(TRADE.ASSET_ID.eq(assetId)));
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  protected void saveTrade(DSLContext ctx, Trade trade) {
    ctx.insertInto(
      TRADE,
      TRADE.ASSET_ID, TRADE.BLOCK_ID, TRADE.ASK_ORDER_ID, TRADE.BID_ORDER_ID, TRADE.ASK_ORDER_HEIGHT,
      TRADE.BID_ORDER_HEIGHT, TRADE.SELLER_ID, TRADE.BUYER_ID, TRADE.QUANTITY, TRADE.PRICE,
      TRADE.TIMESTAMP, TRADE.HEIGHT
    ).values(
      trade.getAssetId(), trade.getBlockId(), trade.getAskOrderId(), trade.getBidOrderId(), trade.getAskOrderHeight(),
      trade.getBidOrderHeight(), trade.getSellerId(), trade.getBuyerId(), trade.getQuantityQNT(), trade.getPriceNQT(),
      trade.getTimestamp(), trade.getHeight()
    ).execute();
  }

  @Override
  public DbKey.LinkKeyFactory<Trade> getTradeDbKeyFactory() {
    return tradeDbKeyFactory;
  }

  @Override
  public EntitySqlTable<Trade> getTradeTable() {
    return tradeTable;
  }

  private class SqlTrade extends Trade {

    private SqlTrade(ResultSet rs) throws SQLException {
      super(
            rs.getInt("timestamp"),
            rs.getLong("asset_id"),
            rs.getLong("block_id"),
            rs.getInt("height"),
            rs.getLong("ask_order_id"),
            rs.getLong("bid_order_id"),
            rs.getInt("ask_order_height"),
            rs.getInt("bid_order_height"),
            rs.getLong("seller_id"),
            rs.getLong("buyer_id"),
            tradeDbKeyFactory.newKey(rs.getLong("ask_order_id"), rs.getLong("bid_order_id")),
            rs.getLong("quantity"),
            rs.getLong("price")
            );
    }
  }
}
