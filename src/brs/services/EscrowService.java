package brs.services;

import brs.Escrow;
import brs.db.BurstIterator;
import java.util.Collection;

public interface EscrowService {

  BurstIterator<Escrow> getAllEscrowTransactions();

  Escrow getEscrowTransaction(Long id);

  Collection<Escrow> getEscrowTransactionsByParticipent(Long accountId);
}
