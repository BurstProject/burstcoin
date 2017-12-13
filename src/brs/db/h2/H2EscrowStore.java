package brs.db.h2;

import brs.Escrow;
import brs.Burst;
import brs.db.sql.SqlEscrowStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class H2EscrowStore extends SqlEscrowStore {
  @Override
  protected void saveDecision(Connection con, Escrow.Decision decision) throws SQLException {
    try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO escrow_decision (escrow_id, "
                                                        + "account_id, decision, height, latest) KEY (escrow_id, account_id, height) VALUES (?, ?, ?, ?, TRUE)")) {
      int i = 0;
      pstmt.setLong(++i, decision.escrowId);
      pstmt.setLong(++i, decision.accountId);
      pstmt.setInt(++i, Escrow.decisionToByte(decision.getDecision()));
      pstmt.setInt(++i, Burst.getBlockchain().getHeight());
      pstmt.executeUpdate();
    }
  }

  @Override
  protected void saveEscrow(Connection con, Escrow escrow) throws SQLException {
    try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO escrow (id, sender_id, "
                                                        + "recipient_id, amount, required_signers, deadline, deadline_action, height, latest) "
                                                        + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
      int i = 0;
      pstmt.setLong(++i, escrow.id);
      pstmt.setLong(++i, escrow.senderId);
      pstmt.setLong(++i, escrow.recipientId);
      pstmt.setLong(++i, escrow.amountNQT);
      pstmt.setInt(++i, escrow.requiredSigners);
      pstmt.setInt(++i, escrow.deadline);
      pstmt.setInt(++i, Escrow.decisionToByte(escrow.deadlineAction));
      pstmt.setInt(++i, Burst.getBlockchain().getHeight());
      pstmt.executeUpdate();
    }
  }
}
