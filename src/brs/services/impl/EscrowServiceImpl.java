package brs.services.impl;

import brs.Burst;
import brs.Escrow;
import brs.db.BurstIterator;
import brs.db.BurstKey.LongKeyFactory;
import brs.db.VersionedEntityTable;
import brs.db.store.EscrowStore;
import brs.services.EscrowService;
import java.util.Collection;

public class EscrowServiceImpl implements EscrowService {

  private final VersionedEntityTable<Escrow> escrowTable;
  private final LongKeyFactory<Escrow> escrowDbKeyFactory;
  private final EscrowStore escrowStore;

  public EscrowServiceImpl(EscrowStore escrowStore) {
    this.escrowStore = escrowStore;
    this.escrowTable = escrowStore.getEscrowTable();
    this.escrowDbKeyFactory = escrowStore.getEscrowDbKeyFactory();
  }

  @Override
  public BurstIterator<Escrow> getAllEscrowTransactions() {
    return escrowTable.getAll(0, -1);
  }

  @Override
  public Escrow getEscrowTransaction(Long id) {
    return escrowTable.get(escrowDbKeyFactory.newKey(id));
  }

  @Override
  public Collection<Escrow> getEscrowTransactionsByParticipent(Long accountId) {
    return escrowStore.getEscrowTransactionsByParticipent(accountId);
  }

}
