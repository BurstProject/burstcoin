package brs.services;

import brs.Escrow;
import brs.db.BurstIterator;

public interface EscrowService {

  BurstIterator<Escrow> getAllEscrowTransactions();
}
