package nxt.peer;

import nxt.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class PeerDb {

    static List<String> loadPeers() {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM peer")) {
            List<String> peers = new ArrayList<>();
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                peers.add(rs.getString("address"));
            }
            rs.close();
            return peers;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static void deletePeers(Collection<String> peers) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("DELETE FROM peer WHERE address = ?")) {
            for (String peer : peers) {
                pstmt.setString(1, peer);
                pstmt.executeUpdate();
            }
            con.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static void addPeers(Collection<String> peers) {
        try (Connection con = Db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("INSERT INTO peer (address) values (?)")) {
            for (String peer : peers) {
                pstmt.setString(1, peer);
                pstmt.executeUpdate();
            }
            con.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
