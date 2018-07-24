/*
 * This file is generated by jOOQ.
*/
package brs.schema.tables;


import brs.schema.Db;
import brs.schema.Indexes;
import brs.schema.Keys;
import brs.schema.tables.records.PeerRecord;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;


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
public class Peer extends TableImpl<PeerRecord> {

    private static final long serialVersionUID = -1229535114;

    /**
     * The reference instance of <code>DB.peer</code>
     */
    public static final Peer PEER = new Peer();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<PeerRecord> getRecordType() {
        return PeerRecord.class;
    }

    /**
     * The column <code>DB.peer.address</code>.
     */
    public final TableField<PeerRecord, String> ADDRESS = createField("address", org.jooq.impl.SQLDataType.VARCHAR(100).nullable(false), this, "");

    /**
     * Create a <code>DB.peer</code> table reference
     */
    public Peer() {
        this(DSL.name("peer"), null);
    }

    /**
     * Create an aliased <code>DB.peer</code> table reference
     */
    public Peer(String alias) {
        this(DSL.name(alias), PEER);
    }

    /**
     * Create an aliased <code>DB.peer</code> table reference
     */
    public Peer(Name alias) {
        this(alias, PEER);
    }

    private Peer(Name alias, Table<PeerRecord> aliased) {
        this(alias, aliased, null);
    }

    private Peer(Name alias, Table<PeerRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return Db.DB;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.PEER_PRIMARY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<PeerRecord> getPrimaryKey() {
        return Keys.KEY_PEER_PRIMARY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<PeerRecord>> getKeys() {
        return Arrays.<UniqueKey<PeerRecord>>asList(Keys.KEY_PEER_PRIMARY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Peer as(String alias) {
        return new Peer(DSL.name(alias), this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Peer as(Name alias) {
        return new Peer(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Peer rename(String name) {
        return new Peer(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Peer rename(Name name) {
        return new Peer(name, null);
    }
}
