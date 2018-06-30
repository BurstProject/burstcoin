package brs.unconfirmedtransactions;

import static brs.Attachment.ORDINARY_PAYMENT;
import static brs.Constants.FEE_QUANT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import brs.Account;
import brs.BlockchainImpl;
import brs.Burst;
import brs.BurstException.NotCurrentlyValidException;
import brs.BurstException.ValidationException;
import brs.Constants;
import brs.Transaction;
import brs.Transaction.Builder;
import brs.TransactionType;
import brs.common.TestConstants;
import brs.db.BurstKey;
import brs.db.BurstKey.LongKeyFactory;
import brs.db.VersionedBatchEntityTable;
import brs.db.store.AccountStore;
import brs.fluxcapacitor.FeatureToggle;
import brs.fluxcapacitor.FluxCapacitor;
import brs.props.PropertyService;
import brs.props.Props;
import brs.services.TimeService;
import brs.services.impl.TimeServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Burst.class)
public class UnconfirmedTransactionStoreTest {

  private BlockchainImpl mockBlockChain;

  private AccountStore accountStoreMock;
  private VersionedBatchEntityTable<Account> accountTableMock;
  private LongKeyFactory<Account> accountBurstKeyFactoryMock;

  private TimeService timeService = new TimeServiceImpl();
  private UnconfirmedTransactionStore t;

  @Before
  public void setUp() {
    mockStatic(Burst.class);

    final PropertyService mockPropertyService = mock(PropertyService.class);
    when(mockPropertyService.getInt(eq(Props.DB_MAX_ROLLBACK))).thenReturn(1440);
    when(Burst.getPropertyService()).thenReturn(mockPropertyService);
    when(mockPropertyService.getInt(eq(Props.P2P_MAX_UNCONFIRMED_TRANSACTIONS))).thenReturn(8192);
    when(mockPropertyService.getInt(eq(Props.P2P_MAX_PERCENTAGE_UNCONFIRMED_TRANSACTIONS_FULL_HASH_REFERENCE))).thenReturn(5);

    mockBlockChain = mock(BlockchainImpl.class);
    when(Burst.getBlockchain()).thenReturn(mockBlockChain);

    accountStoreMock = mock(AccountStore.class);
    accountTableMock = mock(VersionedBatchEntityTable.class);
    accountBurstKeyFactoryMock = mock(LongKeyFactory.class);
    when(accountStoreMock.getAccountTable()).thenReturn(accountTableMock);
    when(accountStoreMock.getAccountKeyFactory()).thenReturn(accountBurstKeyFactoryMock);

    final Account mockAccount = mock(Account.class);
    final BurstKey mockAccountKey = mock(BurstKey.class);
    when(accountBurstKeyFactoryMock.newKey(eq(123L))).thenReturn(mockAccountKey);
    when(accountTableMock.get(eq(mockAccountKey))).thenReturn(mockAccount);
    when(mockAccount.getUnconfirmedBalanceNQT()).thenReturn(Constants.MAX_BALANCE_NQT);

    FluxCapacitor mockFluxCapacitor = mock(FluxCapacitor.class);
    when(mockFluxCapacitor.isActive(eq(FeatureToggle.PRE_DYMAXION))).thenReturn(true);
    when(mockFluxCapacitor.isActive(eq(FeatureToggle.PRE_DYMAXION), anyInt())).thenReturn(true);

    TransactionType.init(mockBlockChain, mockFluxCapacitor, null, null, null, null, null, null);

    t = new UnconfirmedTransactionStoreImpl(timeService, mockPropertyService, accountStoreMock);
  }

  @DisplayName("When The amount of unconfirmed transactions exceeds max size, and adding another then the cache size stays the same")
  @Test
  public void numberOfUnconfirmedTransactionsOfSameSlotExceedsMaxSizeAddAnotherThenCacheSizeStaysMaxSize() throws ValidationException {

    when(mockBlockChain.getHeight()).thenReturn(20);

    for (int i = 1; i <= 8192; i++) {
      Transaction transaction = new Transaction.Builder((byte) 1, TestConstants.TEST_PUBLIC_KEY_BYTES, i, FEE_QUANT * 100, timeService.getEpochTime() + 50000, (short) 500, ORDINARY_PAYMENT)
          .id(i).senderId(123L).build();
      transaction.sign(TestConstants.TEST_SECRET_PHRASE);
      t.put(transaction);
    }

    assertEquals(8192, t.getAll().getTransactions().size());
    assertNotNull(t.get(1L));

    final Transaction oneTransactionTooMany =
        new Transaction.Builder((byte) 1, TestConstants.TEST_PUBLIC_KEY_BYTES, 9999, FEE_QUANT * 100, timeService.getEpochTime() + 50000, (short) 500, ORDINARY_PAYMENT)
            .id(8193L).senderId(123L).build();
    oneTransactionTooMany.sign(TestConstants.TEST_SECRET_PHRASE);
    t.put(oneTransactionTooMany);

    assertEquals(8192, t.getAll().getTransactions().size());
    assertNull(t.get(1L));
  }

  @DisplayName("When the amount of unconfirmed transactions exceeds max size, and adding another of a higher slot, the cache size stays the same, and a lower slot transaction gets removed")
  @Test
  public void numberOfUnconfirmedTransactionsOfSameSlotExceedsMaxSizeAddAnotherThenCacheSizeStaysMaxSizeAndLowerSlotTransactionGetsRemoved() throws ValidationException {

    when(mockBlockChain.getHeight()).thenReturn(20);

    for (int i = 1; i <= 8192; i++) {
      Transaction transaction = new Transaction.Builder((byte) 1, TestConstants.TEST_PUBLIC_KEY_BYTES, i, FEE_QUANT * 100, timeService.getEpochTime() + 50000, (short) 500, ORDINARY_PAYMENT)
          .id(i).senderId(123L).build();
      transaction.sign(TestConstants.TEST_SECRET_PHRASE);
      t.put(transaction);
    }

    assertEquals(8192, t.getAll().getTransactions().size());
    assertEquals(8192, t.getAll().getTransactions().stream().filter(t -> t.getFeeNQT() == FEE_QUANT * 100).count());
    assertNotNull(t.get(1L));

    final Transaction oneTransactionTooMany =
        new Transaction.Builder((byte) 1, TestConstants.TEST_PUBLIC_KEY_BYTES, 9999, FEE_QUANT * 200, timeService.getEpochTime() + 50000, (short) 500, ORDINARY_PAYMENT)
            .id(8193L).senderId(123L).build();
    oneTransactionTooMany.sign(TestConstants.TEST_SECRET_PHRASE);
    t.put(oneTransactionTooMany);

    assertEquals(8192, t.getAll().getTransactions().size());
    assertEquals(8192 - 1, t.getAll().getTransactions().stream().filter(t -> t.getFeeNQT() == FEE_QUANT * 100).count());
    assertEquals(1, t.getAll().getTransactions().stream().filter(t -> t.getFeeNQT() == FEE_QUANT * 200).count());
  }

  @DisplayName("The unconfirmed transaction gets denied in case the account is unknown")
  @Test(expected = NotCurrentlyValidException.class)
  public void unconfirmedTransactionGetsDeniedForUnknownAccount() throws ValidationException {
    when(mockBlockChain.getHeight()).thenReturn(20);

    Transaction transaction = new Transaction.Builder((byte) 1, TestConstants.TEST_PUBLIC_KEY_BYTES, 1, 735000, timeService.getEpochTime() + 50000, (short) 500, ORDINARY_PAYMENT)
        .id(1).senderId(124L).build();
    transaction.sign(TestConstants.TEST_SECRET_PHRASE);
    t.put(transaction);
  }

  @DisplayName("The unconfirmed transaction gets denied in case the account does not have enough unconfirmed balance")
  @Test(expected = NotCurrentlyValidException.class)
  public void unconfirmedTransactionGetsDeniedForNotEnoughUnconfirmedBalance() throws ValidationException {
    when(mockBlockChain.getHeight()).thenReturn(20);

    Transaction transaction = new Transaction.Builder((byte) 1, TestConstants.TEST_PUBLIC_KEY_BYTES, 1, Constants.MAX_BALANCE_NQT, timeService.getEpochTime() + 50000, (short) 500, ORDINARY_PAYMENT)
        .id(1).senderId(123L).build();
    transaction.sign(TestConstants.TEST_SECRET_PHRASE);
    t.put(transaction);
  }

  @DisplayName("When adding the same unconfirmed transaction, nothing changes")
  @Test
  public void addingNewUnconfirmedTransactionWithSameIDResultsInNothingChanging() throws ValidationException {
    when(mockBlockChain.getHeight()).thenReturn(20);

    Builder transactionBuilder = new Builder((byte) 1, TestConstants.TEST_PUBLIC_KEY_BYTES, 1, Constants.MAX_BALANCE_NQT - 100000, timeService.getEpochTime() + 50000,
        (short) 500, ORDINARY_PAYMENT)
        .id(1).senderId(123L);

    Transaction transaction1 = transactionBuilder.build();
    transaction1.sign(TestConstants.TEST_SECRET_PHRASE);
    t.put(transaction1);

    Transaction transaction2 = transactionBuilder.build();
    transaction2.sign(TestConstants.TEST_SECRET_PHRASE);

    t.put(transaction2);

    assertEquals(1, t.getAll().getTransactions().size());
  }

  @DisplayName("When the maximum number of transactions with full hash reference is reached, following ones are ignored")
  @Test
  public void whenMaximumNumberOfTransactionsWithFullHashReferenceIsReachedFollowingOnesAreIgnored() throws ValidationException {

    when(mockBlockChain.getHeight()).thenReturn(20);

    for (int i = 1; i <= 500; i++) {
      Transaction transaction = new Transaction.Builder((byte) 1, TestConstants.TEST_PUBLIC_KEY_BYTES, i, FEE_QUANT * 2, timeService.getEpochTime() + 50000, (short) 500, ORDINARY_PAYMENT)
          .id(i).senderId(123L).referencedTransactionFullHash("b33f").build();
      transaction.sign(TestConstants.TEST_SECRET_PHRASE);
      t.put(transaction);
    }

    assertEquals(409, t.getAll().getTransactions().size());
  }

  @DisplayName("When the maximum number of transactions for a slot size is reached, following ones are ignored")
  @Test
  public void whenMaximumNumberOfTransactionsForSlotSizeIsReachedFollowingOnesAreIgnored() throws ValidationException {

    when(mockBlockChain.getHeight()).thenReturn(20);

    for (int i = 1; i <= 500; i++) {
      Transaction transaction = new Transaction.Builder((byte) 1, TestConstants.TEST_PUBLIC_KEY_BYTES, i, FEE_QUANT, timeService.getEpochTime() + 50000, (short) 500, ORDINARY_PAYMENT)
          .id(i).senderId(123L).build();
      transaction.sign(TestConstants.TEST_SECRET_PHRASE);
      t.put(transaction);
    }

    assertEquals(360, t.getAll().getTransactions().size());

    for (int i = 1; i <= 800; i++) {
      Transaction transaction = new Transaction.Builder((byte) 1, TestConstants.TEST_PUBLIC_KEY_BYTES, i, FEE_QUANT * 2, timeService.getEpochTime() + 50000, (short) 500, ORDINARY_PAYMENT)
          .id(i).senderId(123L).build();
      transaction.sign(TestConstants.TEST_SECRET_PHRASE);
      t.put(transaction);
    }

    assertEquals(1080, t.getAll().getTransactions().size());
  }

}
