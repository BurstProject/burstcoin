package brs;

import static brs.schema.Tables.UNCONFIRMED_TRANSACTION;

import brs.AT.HandleATBlockTransactionsListener;
import brs.GeneratorImpl.MockGeneratorImpl;
import brs.assetexchange.AssetExchange;
import brs.assetexchange.AssetExchangeImpl;
import brs.blockchainlistener.DevNullListener;
import brs.feesuggestions.FeeSuggestionCalculator;
import brs.props.Props;
import brs.db.BlockDb;
import brs.db.cache.DBCacheManagerImpl;
import brs.db.sql.Db;

import brs.db.store.BlockchainStore;
import brs.db.store.Dbs;
import brs.db.store.DerivedTableManager;
import brs.db.store.Stores;
import brs.fluxcapacitor.FluxCapacitor;
import brs.fluxcapacitor.FluxCapacitorImpl;
import brs.http.API;
import brs.http.APITransactionManager;
import brs.http.APITransactionManagerImpl;
import brs.peer.Peers;
import brs.services.ATService;
import brs.services.AccountService;
import brs.services.AliasService;
import brs.services.BlockService;
import brs.services.DGSGoodsStoreService;
import brs.services.EscrowService;
import brs.services.ParameterService;
import brs.props.PropertyService;
import brs.services.SubscriptionService;
import brs.services.TimeService;
import brs.services.TransactionService;
import brs.services.impl.ATServiceImpl;
import brs.services.impl.AccountServiceImpl;
import brs.services.impl.AliasServiceImpl;
import brs.services.impl.BlockServiceImpl;
import brs.services.impl.DGSGoodsStoreServiceImpl;
import brs.services.impl.EscrowServiceImpl;
import brs.services.impl.ParameterServiceImpl;
import brs.props.PropertyServiceImpl;
import brs.services.impl.SubscriptionServiceImpl;
import brs.services.impl.TimeServiceImpl;
import brs.services.impl.TransactionServiceImpl;
import brs.statistics.StatisticsManagerImpl;
import brs.util.DownloadCacheImpl;
import brs.util.LoggerConfigurator;
import brs.util.ThreadPool;
import brs.util.Time;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.util.Properties;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Burst {

  public static final String VERSION     = "2.3.0";
  public static final String APPLICATION = "BRS";
  public static final String LEGACY_APP  = "NRS";
  public static final String LEGACY_VER  = "1.2.9";
  
  private static final String DEFAULT_PROPERTIES_NAME = "brs-default.properties";

  private static final Logger logger = LoggerFactory.getLogger(Burst.class);

  private static Stores stores;
  private static Dbs dbs;

  private static ThreadPool threadPool;

  private static BlockchainImpl blockchain;
  private static BlockchainProcessorImpl blockchainProcessor;
  private static TransactionProcessorImpl transactionProcessor;

  private static PropertyService propertyService;
  private static FluxCapacitor fluxCapacitor;

  private static EconomicClustering economicClustering;

  private static DBCacheManagerImpl dbCacheManager;

  private static API api;

  static Properties properties;

  private static PropertyService loadProperties() {
    final Properties defaultProperties = new Properties();

    logger.info("Initializing Burst Reference Software (BRS) version {}", VERSION);
    try (InputStream is = ClassLoader.getSystemResourceAsStream(DEFAULT_PROPERTIES_NAME)) {
      if (is != null) {
        defaultProperties.load(is);
      } else {
        String configFile = System.getProperty(DEFAULT_PROPERTIES_NAME);

        if (configFile != null) {
          try (InputStream fis = new FileInputStream(configFile)) {
            defaultProperties.load(fis);
          } catch (IOException e) {
            throw new RuntimeException("Error loading " + DEFAULT_PROPERTIES_NAME + " from " + configFile);
          }
        } else {
          throw new RuntimeException(DEFAULT_PROPERTIES_NAME + " not in classpath and system property " + DEFAULT_PROPERTIES_NAME + " not defined either");
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Error loading " + DEFAULT_PROPERTIES_NAME, e);
    }

    try (InputStream is = ClassLoader.getSystemResourceAsStream("brs.properties")) {
      properties = new Properties(defaultProperties);
      if (is != null) { // parse if brs.properties was loaded
        properties.load(is);
      }
    } catch (IOException e) {
      throw new RuntimeException("Error loading brs.properties", e);
    }

    return new PropertyServiceImpl(properties);
  }

  private Burst() {
  } // never

  public static BlockchainImpl getBlockchain() {
    return blockchain;
  }

  public static BlockchainProcessorImpl getBlockchainProcessor() {
    return blockchainProcessor;
  }

  public static TransactionProcessorImpl getTransactionProcessor() {
    return transactionProcessor;
  }

  public static Stores getStores() {
    return stores;
  }

  public static Dbs getDbs() {
    return dbs;
  }

  public static void main(String[] args) {
    validateVersionNotDev(VERSION);
    Runtime.getRuntime().addShutdownHook(new Thread(Burst::shutdown));
    init();
  }

  private static void validateVersionNotDev(String version) {
    if(isDevVersion(version) && System.getProperty("dev") == null) {
      logger.error("THIS IS A DEVELOPMENT WALLET, PLEASE DO NOT USE THIS");
      System.exit(0);
    }
  }

  private static boolean isDevVersion(String version) {
    return Integer.parseInt(version.split("\\.")[1]) % 2 != 0;
  }

  public static void init(Properties customProperties) {
    loadWallet(new PropertyServiceImpl(customProperties));
  }

  public static void init() {
    loadWallet(loadProperties());
  }

  private static void loadWallet(PropertyService propertyService) {
    Burst.propertyService = propertyService;

    try {
      long startTime = System.currentTimeMillis();

      final TimeService timeService = new TimeServiceImpl();

      final DerivedTableManager derivedTableManager = new DerivedTableManager();

      final StatisticsManagerImpl statisticsManager = new StatisticsManagerImpl(timeService);
      dbCacheManager = new DBCacheManagerImpl(statisticsManager);

      threadPool = new ThreadPool(propertyService);

      LoggerConfigurator.init();

      Db.init(propertyService, dbCacheManager);
      dbs = Db.getDbsByDatabaseType();

      stores = new Stores(derivedTableManager, dbCacheManager, timeService, propertyService);

      final TransactionDb transactionDb = dbs.getTransactionDb();
      final BlockDb blockDb =  dbs.getBlockDb();
      final BlockchainStore blockchainStore = stores.getBlockchainStore();
      blockchain = new BlockchainImpl(transactionDb, blockDb, blockchainStore);

      final AliasService aliasService = new AliasServiceImpl(stores.getAliasStore());
      fluxCapacitor = new FluxCapacitorImpl(blockchain, propertyService);

      economicClustering = new EconomicClustering(blockchain);

      final Generator generator = propertyService.getBoolean(Props.DEV_MOCK_MINING) ? new MockGeneratorImpl() : new GeneratorImpl(blockchain, timeService, fluxCapacitor);

      final AccountService accountService = new AccountServiceImpl(stores.getAccountStore(), stores.getAssetTransferStore());

      final TransactionService transactionService = new TransactionServiceImpl(accountService, blockchain);

      transactionProcessor = new TransactionProcessorImpl(propertyService, economicClustering, blockchain, stores, timeService, dbs,
          accountService, transactionService, threadPool);

      final ATService atService = new ATServiceImpl(stores.getAtStore());
      final SubscriptionService subscriptionService = new SubscriptionServiceImpl(stores.getSubscriptionStore(), transactionDb, blockchain, aliasService, accountService);
      final DGSGoodsStoreService digitalGoodsStoreService = new DGSGoodsStoreServiceImpl(blockchain, stores.getDigitalGoodsStoreStore(), accountService);
      final EscrowService escrowService = new EscrowServiceImpl(stores.getEscrowStore(), blockchain, aliasService, accountService);

      final AssetExchange assetExchange = new AssetExchangeImpl(accountService, stores.getTradeStore(), stores.getAccountStore(), stores.getAssetTransferStore(), stores.getAssetStore(), stores.getOrderStore());

      final DownloadCacheImpl downloadCache = new DownloadCacheImpl(propertyService, fluxCapacitor, blockchain);

      final BlockService blockService = new BlockServiceImpl(accountService, transactionService, blockchain, downloadCache, generator);
      blockchainProcessor = new BlockchainProcessorImpl(threadPool, blockService, transactionProcessor, blockchain, propertyService, subscriptionService,
          timeService, derivedTableManager,
          blockDb, transactionDb, economicClustering, blockchainStore, stores, escrowService, transactionService, downloadCache, generator, statisticsManager,
          dbCacheManager, accountService);

      final FeeSuggestionCalculator feeSuggestionCalculator = new FeeSuggestionCalculator(blockchainProcessor, blockchainStore, 10);

      generator.generateForBlockchainProcessor(threadPool, blockchainProcessor);

      final ParameterService parameterService = new ParameterServiceImpl(accountService, aliasService, assetExchange,
          digitalGoodsStoreService, blockchain, blockchainProcessor, transactionProcessor, atService);

      addBlockchainListeners(blockchainProcessor, accountService, digitalGoodsStoreService, blockchain, dbs.getTransactionDb());

      final APITransactionManager apiTransactionManager = new APITransactionManagerImpl(parameterService, transactionProcessor, blockchain, accountService, transactionService);

      Peers.init(timeService, accountService, blockchain, transactionProcessor, blockchainProcessor, propertyService, threadPool);

      TransactionType.init(blockchain, fluxCapacitor, accountService, digitalGoodsStoreService, aliasService, assetExchange, subscriptionService, escrowService);

      api = new API(transactionProcessor, blockchain, blockchainProcessor, parameterService,
          accountService, aliasService, assetExchange, escrowService, digitalGoodsStoreService,
          subscriptionService, atService, timeService, economicClustering, propertyService, threadPool,
          transactionService, blockService, generator, apiTransactionManager, feeSuggestionCalculator);

      DebugTrace.init(propertyService, blockchainProcessor, accountService, assetExchange, digitalGoodsStoreService);

      // backward compatibility for those who have some unconfirmed transactions in their db
      try {
        stores.beginTransaction();
        try (DSLContext ctx = Db.getDSLContext()) {
          ResultSet rs = ctx.selectFrom(UNCONFIRMED_TRANSACTION).fetchResultSet();
          while ( rs.next() ) {
            byte[] transactionBytes = rs.getBytes("transaction_bytes");
            Transaction transaction = Transaction.parseTransaction(transactionBytes);
            transaction.setHeight(rs.getInt("transaction_height"));
            transactionService.undoUnconfirmed(transaction);
          }
          ctx.truncate(UNCONFIRMED_TRANSACTION).execute();
        }
        accountService.flushAccountTable();
        stores.commitTransaction();
      } catch (Exception e) {
        logger.error(e.toString(), e);
        stores.rollbackTransaction();
        throw e;
      } finally {
        stores.endTransaction();
      }

      int timeMultiplier = (propertyService.getBoolean(Props.DEV_TESTNET) && propertyService.getBoolean(Props.DEV_OFFLINE)) ? Math.max(propertyService.getInt(Props.DEV_TIMEWARP), 1) : 1;

      threadPool.start(timeMultiplier);
      if (timeMultiplier > 1) {
        timeService.setTime(new Time.FasterTime(Math.max(timeService.getEpochTime(), getBlockchain().getLastBlock().getTimestamp()), timeMultiplier));
        logger.info("TIME WILL FLOW " + timeMultiplier + " TIMES FASTER!");
      }

      long currentTime = System.currentTimeMillis();
      logger.info("Initialization took " + (currentTime - startTime) + " ms");
      logger.info("BRS " + VERSION + " started successfully.");

      if (propertyService.getBoolean(Props.DEV_TESTNET)) {
        logger.info("RUNNING ON TESTNET - DO NOT USE REAL ACCOUNTS!");
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      System.exit(1);
    }

  }

  private static void addBlockchainListeners(BlockchainProcessor blockchainProcessor, AccountService accountService, DGSGoodsStoreService goodsService, Blockchain blockchain,
      TransactionDb transactionDb) {

    final HandleATBlockTransactionsListener handleATBlockTransactionListener = new HandleATBlockTransactionsListener(accountService, blockchain, transactionDb);
    final DevNullListener devNullListener = new DevNullListener(accountService, goodsService);

    blockchainProcessor.addListener(handleATBlockTransactionListener, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    blockchainProcessor.addListener(devNullListener, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
  }

  public static void shutdown() {
    shutdown(false);
  }

  public static void shutdown(boolean ignoreDBShutdown) {
    logger.info("Shutting down...");
    if (api != null)
      api.shutdown();
    Peers.shutdown(threadPool);
    threadPool.shutdown();
    dbCacheManager.close();

    if(! ignoreDBShutdown) {
      Db.shutdown();
    }
    if (blockchainProcessor != null && blockchainProcessor.getOclVerify()) {
      OCLPoC.destroy();
    }
    logger.info("BRS " + VERSION + " stopped.");
    LoggerConfigurator.shutdown();
  }

  public static PropertyService getPropertyService() {
    return propertyService;
  }

  public static FluxCapacitor getFluxCapacitor() { return fluxCapacitor; }

}
