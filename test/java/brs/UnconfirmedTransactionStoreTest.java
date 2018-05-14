package brs;

import static brs.Attachment.ORDINARY_PAYMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import brs.BurstException.NotValidException;
import brs.common.Props;
import brs.common.TestConstants;
import brs.fluxcapacitor.FeatureToggle;
import brs.fluxcapacitor.FluxCapacitor;
import brs.services.PropertyService;
import brs.services.TimeService;
import brs.services.impl.TimeServiceImpl;
import java.util.ArrayList;
import java.util.List;
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

  private TimeService timeService = new TimeServiceImpl();

  private UnconfirmedTransactionStore t;

  @Before
  public void setUp() {
    mockStatic(Burst.class);

    final PropertyService mockPropertyService = mock(PropertyService.class);
    when(mockPropertyService.getInt(Props.DB_MAX_ROLLBACK)).thenReturn(1440);
    when(Burst.getPropertyService()).thenReturn(mockPropertyService);

    mockBlockChain = mock(BlockchainImpl.class);
    when(Burst.getBlockchain()).thenReturn(mockBlockChain);

    FluxCapacitor mockFluxCapacitor = mock(FluxCapacitor.class);
    when(mockFluxCapacitor.isActive(FeatureToggle.PRE_DYMAXION)).thenReturn(true);

    TransactionType.init(mockBlockChain, mockFluxCapacitor, null, null, null, null, null, null);

    t = new UnconfirmedTransactionStore(timeService);
  }

  @DisplayName("The amount of unconfirmed transactions exceeds 8192, when adding another the cache size stays the same")
  @Test
  public void numberOfUnconfirmedTransactionsExceeds8192AddAnotherThenCacheSizeStays8192() throws NotValidException {

    when(mockBlockChain.getHeight()).thenReturn(20);

    for (int i = 1; i <= 8192; i++) {
      Transaction transaction = new Transaction.Builder((byte) 1, TestConstants.TEST_PUBLIC_KEY_BYTES, i, 735000, timeService.getEpochTime() + 50000, (short) 500, ORDINARY_PAYMENT)
        .id(i).build();
      transaction.sign(TestConstants.TEST_SECRET_PHRASE);
      t.put(transaction);
    }

    assertEquals(8192, t.getAll().size());
    assertNotNull(t.get(1L));

    final Transaction oneTransactionTooMany =
        new Transaction.Builder((byte) 1, TestConstants.TEST_PUBLIC_KEY_BYTES, 9999, 735000, timeService.getEpochTime() + 50000, (short) 500, ORDINARY_PAYMENT)
            .id(8193L).build();
    oneTransactionTooMany.sign(TestConstants.TEST_SECRET_PHRASE);
    t.put(oneTransactionTooMany);

    assertEquals(8192, t.getAll().size());
    assertNull(t.get(1L));
  }

  @DisplayName("The amount of unconfirmed transactions exceeds 8192, when adding a group of others the cache size stays the same")
  @Test
  public void numberOfUnconfirmedTransactionsExceeds8192AddAGroupOfOthersThenCacheSizeStays8192() throws NotValidException {

    when(mockBlockChain.getHeight()).thenReturn(20);

    for (int i = 1; i <= 8192; i++) {
      Transaction transaction = new Transaction.Builder((byte) 1, TestConstants.TEST_PUBLIC_KEY_BYTES, i, 735000, timeService.getEpochTime() + 50000, (short) 500, ORDINARY_PAYMENT)
          .id(i).build();
      transaction.sign(TestConstants.TEST_SECRET_PHRASE);
      t.put(transaction);
    }

    assertEquals(8192, t.getAll().size());
    assertNotNull(t.get(1L));
    assertNotNull(t.get(2L));
    assertNotNull(t.get(3L));

    final List<Transaction> groupOfExtras = new ArrayList<>();
    for(int i = 0; i < 3; i++) {
      final Transaction extraTransaction =
          new Transaction.Builder((byte) 1, TestConstants.TEST_PUBLIC_KEY_BYTES, 9999, 735000, timeService.getEpochTime() + 50000, (short) 500, ORDINARY_PAYMENT)
              .id(8193 + i).build();
      extraTransaction.sign(TestConstants.TEST_SECRET_PHRASE);
      groupOfExtras.add(extraTransaction);
    }

    t.put(groupOfExtras);

    assertEquals(8192, t.getAll().size());
    assertNull(t.get(1L));
    assertNull(t.get(2L));
    assertNull(t.get(3L));

    assertNotNull(t.get(8123L));
    assertNotNull(t.get(8124L));
    assertNotNull(t.get(8125L));
  }

  @DisplayName("Old transactions get removed from the cache when they are expired")
  @Test
  public void transactionGetsRemovedWhenExpired() throws NotValidException, InterruptedException {
    final int deadlineWithin2Seconds = timeService.getEpochTime() - 29998;
    final Transaction transaction = new Transaction.Builder((byte) 1, TestConstants.TEST_PUBLIC_KEY_BYTES, 500, 735000, deadlineWithin2Seconds, (short) 500, ORDINARY_PAYMENT)
        .id(1).build();

    transaction.sign(TestConstants.TEST_SECRET_PHRASE);

    t.put(transaction);

    assertNotNull(t.get(1L));

    Thread.sleep(3000);

    assertNull(t.get(1L));
  }

}