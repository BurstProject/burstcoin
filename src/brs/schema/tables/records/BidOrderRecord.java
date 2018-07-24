/*
 * This file is generated by jOOQ.
*/
package brs.schema.tables.records;


import brs.schema.tables.BidOrder;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record9;
import org.jooq.Row9;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.10.0"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class BidOrderRecord extends UpdatableRecordImpl<BidOrderRecord> implements Record9<Long, Long, Long, Long, Long, Long, Integer, Integer, Boolean> {

    private static final long serialVersionUID = -257247046;

    /**
     * Setter for <code>DB.bid_order.db_id</code>.
     */
    public void setDbId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>DB.bid_order.db_id</code>.
     */
    public Long getDbId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>DB.bid_order.id</code>.
     */
    public void setId(Long value) {
        set(1, value);
    }

    /**
     * Getter for <code>DB.bid_order.id</code>.
     */
    public Long getId() {
        return (Long) get(1);
    }

    /**
     * Setter for <code>DB.bid_order.account_id</code>.
     */
    public void setAccountId(Long value) {
        set(2, value);
    }

    /**
     * Getter for <code>DB.bid_order.account_id</code>.
     */
    public Long getAccountId() {
        return (Long) get(2);
    }

    /**
     * Setter for <code>DB.bid_order.asset_id</code>.
     */
    public void setAssetId(Long value) {
        set(3, value);
    }

    /**
     * Getter for <code>DB.bid_order.asset_id</code>.
     */
    public Long getAssetId() {
        return (Long) get(3);
    }

    /**
     * Setter for <code>DB.bid_order.price</code>.
     */
    public void setPrice(Long value) {
        set(4, value);
    }

    /**
     * Getter for <code>DB.bid_order.price</code>.
     */
    public Long getPrice() {
        return (Long) get(4);
    }

    /**
     * Setter for <code>DB.bid_order.quantity</code>.
     */
    public void setQuantity(Long value) {
        set(5, value);
    }

    /**
     * Getter for <code>DB.bid_order.quantity</code>.
     */
    public Long getQuantity() {
        return (Long) get(5);
    }

    /**
     * Setter for <code>DB.bid_order.creation_height</code>.
     */
    public void setCreationHeight(Integer value) {
        set(6, value);
    }

    /**
     * Getter for <code>DB.bid_order.creation_height</code>.
     */
    public Integer getCreationHeight() {
        return (Integer) get(6);
    }

    /**
     * Setter for <code>DB.bid_order.height</code>.
     */
    public void setHeight(Integer value) {
        set(7, value);
    }

    /**
     * Getter for <code>DB.bid_order.height</code>.
     */
    public Integer getHeight() {
        return (Integer) get(7);
    }

    /**
     * Setter for <code>DB.bid_order.latest</code>.
     */
    public void setLatest(Boolean value) {
        set(8, value);
    }

    /**
     * Getter for <code>DB.bid_order.latest</code>.
     */
    public Boolean getLatest() {
        return (Boolean) get(8);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record9 type implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Row9<Long, Long, Long, Long, Long, Long, Integer, Integer, Boolean> fieldsRow() {
        return (Row9) super.fieldsRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Row9<Long, Long, Long, Long, Long, Long, Integer, Integer, Boolean> valuesRow() {
        return (Row9) super.valuesRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field1() {
        return BidOrder.BID_ORDER.DB_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field2() {
        return BidOrder.BID_ORDER.ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field3() {
        return BidOrder.BID_ORDER.ACCOUNT_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field4() {
        return BidOrder.BID_ORDER.ASSET_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field5() {
        return BidOrder.BID_ORDER.PRICE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field6() {
        return BidOrder.BID_ORDER.QUANTITY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Integer> field7() {
        return BidOrder.BID_ORDER.CREATION_HEIGHT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Integer> field8() {
        return BidOrder.BID_ORDER.HEIGHT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Boolean> field9() {
        return BidOrder.BID_ORDER.LATEST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component1() {
        return getDbId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component2() {
        return getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component3() {
        return getAccountId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component4() {
        return getAssetId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component5() {
        return getPrice();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component6() {
        return getQuantity();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer component7() {
        return getCreationHeight();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer component8() {
        return getHeight();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean component9() {
        return getLatest();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value1() {
        return getDbId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value2() {
        return getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value3() {
        return getAccountId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value4() {
        return getAssetId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value5() {
        return getPrice();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value6() {
        return getQuantity();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer value7() {
        return getCreationHeight();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer value8() {
        return getHeight();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean value9() {
        return getLatest();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BidOrderRecord value1(Long value) {
        setDbId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BidOrderRecord value2(Long value) {
        setId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BidOrderRecord value3(Long value) {
        setAccountId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BidOrderRecord value4(Long value) {
        setAssetId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BidOrderRecord value5(Long value) {
        setPrice(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BidOrderRecord value6(Long value) {
        setQuantity(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BidOrderRecord value7(Integer value) {
        setCreationHeight(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BidOrderRecord value8(Integer value) {
        setHeight(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BidOrderRecord value9(Boolean value) {
        setLatest(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BidOrderRecord values(Long value1, Long value2, Long value3, Long value4, Long value5, Long value6, Integer value7, Integer value8, Boolean value9) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached BidOrderRecord
     */
    public BidOrderRecord() {
        super(BidOrder.BID_ORDER);
    }

    /**
     * Create a detached, initialised BidOrderRecord
     */
    public BidOrderRecord(Long dbId, Long id, Long accountId, Long assetId, Long price, Long quantity, Integer creationHeight, Integer height, Boolean latest) {
        super(BidOrder.BID_ORDER);

        set(0, dbId);
        set(1, id);
        set(2, accountId);
        set(3, assetId);
        set(4, price);
        set(5, quantity);
        set(6, creationHeight);
        set(7, height);
        set(8, latest);
    }
}
