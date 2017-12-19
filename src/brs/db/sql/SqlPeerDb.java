package brs.db.sql;

import brs.db.PeerDb;
import org.jooq.DSLContext;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

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
            ctx.insertInto(PEER, PEER.ADDRESS).values(peers);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
