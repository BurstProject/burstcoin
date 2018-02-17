package brs.services;

import brs.Block;
import brs.Escrow;
import brs.db.BurstIterator;
import java.util.Collection;

public interface EscrowService {

  BurstIterator<Escrow> getAllEscrowTransactions();

  Escrow getEscrowTransaction(Long id);

  Collection<Escrow> getEscrowTransactionsByParticipant(Long accountId);

  boolean isEnabled();

  void removeEscrowTransaction(Long id);

  void updateOnBlock(Block block, int blockchainHeight);
}
