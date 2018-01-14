package brs.services.impl;

import brs.Escrow;
import brs.db.BurstIterator;
import brs.db.VersionedEntityTable;
import brs.db.store.EscrowStore;
import brs.services.EscrowService;

public class EscrowServiceImpl implements EscrowService {

  private final VersionedEntityTable<Escrow> escrowTable;

  public EscrowServiceImpl(EscrowStore escrowStore) {
    this.escrowTable = escrowStore.getEscrowTable();
  }

  @Override
  public BurstIterator<Escrow> getAllEscrowTransactions() {
    return escrowTable.getAll(0, -1);
  }
}
