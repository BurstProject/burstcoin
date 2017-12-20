package brs.db.sql;

import brs.db.PeerDb;
import brs.schema.tables.records.PeerRecord;
import org.jooq.DSLContext;
import org.jooq.Insert;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static brs.schema.Tables.PEER;

public abstract class SqlPeerDb implements PeerDb {

    @Override public List<String> loadPeers() {
        try (DSLContext ctx = Db.getDSLContext()) {
            return ctx.selectFrom(PEER).fetch(PEER.ADDRESS, String.class);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override public void deletePeers(Collection<String> peers) {
        try (DSLContext ctx = Db.getDSLContext()) {
            for (String peer : peers) {
                ctx.deleteFrom(PEER).where(PEER.ADDRESS.eq(peer));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override public void addPeers(Collection<String> peers) {
        try (DSLContext ctx = Db.getDSLContext()) {
            List<Insert<PeerRecord>> inserts = peers.stream().map(peer -> ctx.insertInto(PEER).set(PEER.ADDRESS, peer)).collect(Collectors.toList());
            ctx.batch(inserts).execute();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
