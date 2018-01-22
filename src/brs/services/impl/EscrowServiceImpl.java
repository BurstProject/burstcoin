package brs.services.impl;

import brs.Escrow;
import brs.db.BurstIterator;
import brs.db.BurstKey.LongKeyFactory;
import brs.db.VersionedEntityTable;
import brs.db.store.EscrowStore;
import brs.services.EscrowService;

public class EscrowServiceImpl implements EscrowService {

  private final VersionedEntityTable<Escrow> escrowTable;
  private final LongKeyFactory<Escrow> escrowDbKeyFactory;

  public EscrowServiceImpl(EscrowStore escrowStore) {
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

}
