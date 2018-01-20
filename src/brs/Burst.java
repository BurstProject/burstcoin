package brs;

import brs.http.APIServlet;
import brs.services.AssetAccountService;
import brs.services.AccountService;
import brs.services.AliasService;
import brs.services.AssetService;
import brs.services.AssetTransferService;
import brs.services.DGSGoodsStoreService;
import brs.services.EscrowService;
import brs.services.OrderService;
import brs.services.ParameterService;
import brs.services.TradeService;
import brs.services.impl.AssetAccountServiceImpl;
import brs.services.impl.AccountServiceImpl;
import brs.services.impl.AliasServiceImpl;
import brs.services.impl.AssetServiceImpl;
import brs.services.impl.AssetTransferServiceImpl;
import brs.services.impl.DGSGoodsStoreServiceImpl;
import brs.services.impl.EscrowServiceImpl;
import brs.services.impl.OrderServiceImpl;
import brs.services.impl.ParameterServiceImpl;
import brs.services.impl.TradeServiceImpl;
import brs.db.sql.Db;
import brs.db.store.Dbs;
import brs.db.store.Stores;
import brs.http.API;
import brs.peer.Peers;
import brs.user.Users;
import brs.util.LoggerConfigurator;
import brs.util.ThreadPool;
import brs.util.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public final class Burst {

  public static final String VERSION     = "1.9.1";
  public static final String APPLICATION = "BRS";

  private static final String LOG_UNDEF_NAME_DEFAULT = "{} undefined. Default: {}";
  private static final String DEFAULT_PROPERTIES_NAME = "brs-default.properties";

  private static final Logger logger = LoggerFactory.getLogger(Burst.class);
  private static final Properties defaultProperties = new Properties();
  private static final Properties properties = new Properties(defaultProperties);
  private static volatile Time time = new Time.EpochTime();
  private static Stores stores;
  private static Dbs dbs;
  private static Generator generator = new GeneratorImpl();

  static {
    logger.info("Initializing Burst server version {}", VERSION);
    try (InputStream is = ClassLoader.getSystemResourceAsStream(DEFAULT_PROPERTIES_NAME)) {
      if (is != null) {
        defaultProperties.load(is);
      }
      else {
        String configFile = System.getProperty(DEFAULT_PROPERTIES_NAME);
        if (configFile != null) {
          try (InputStream fis = new FileInputStream(configFile)) {
            defaultProperties.load(fis);
          } catch (IOException e) {
            throw new RuntimeException("Error loading " + DEFAULT_PROPERTIES_NAME + " from " + configFile);
          }
        }
        else {
          throw new RuntimeException(DEFAULT_PROPERTIES_NAME + " not in classpath and system property " + DEFAULT_PROPERTIES_NAME + " not defined either");
        }
      }
    }
    catch (IOException e) {
      throw new RuntimeException("Error loading " + DEFAULT_PROPERTIES_NAME, e);
    }
  }

  static {
    try (InputStream is = ClassLoader.getSystemResourceAsStream("brs.properties")) {
      if (is != null) { // parse if brs.properties was loaded
        properties.load(is);
      }
    }
    catch (IOException e) {
      throw new RuntimeException("Error loading brs.properties", e);
    }
  }

  private Burst() {
  } // never

  // Boolean Properties handling
  public static Boolean getBooleanProperty(String name, boolean assume) {
    String value = properties.getProperty(name);

    if (value != null) {
      if (value.matches("(?i)^1|true|yes|on$")) {
        logger.debug("{} = 'true'", name);
        return true;
      }

      if (value.matches("(?i)^0|false|no|off$")) {
        logger.debug("{} = 'false'", name);
        return false;
      }
    }

    logger.info(LOG_UNDEF_NAME_DEFAULT, name, assume);
    return assume;
  }

  public static Boolean getBooleanProperty(String name) {
    return getBooleanProperty(name, false);
  }

  // Int Properties handling, can accept binary (0b), decimal and hexadecimal (0x) numbers
  public static int getIntProperty(String name, int defaultValue) {
    try {
      String value = properties.getProperty(name);
      int radix    = 10;

      if (value!= null && value.matches("(?i)^0x.+$")) {
        value = value.replaceFirst("^0x", "");
        radix = 16;
      }
      else if (value != null && value.matches("(?i)^0b[01]+$")) {
        value = value.replaceFirst("^0b", "");
        radix = 2;
      }

      int result   = Integer.parseInt(value, radix);
      logger.debug("{} = '{}'", name, result);
      return result;
    }
    catch (NumberFormatException e) {
      logger.info(LOG_UNDEF_NAME_DEFAULT, name, defaultValue);
      return defaultValue;
    }
  }

  // without any default value, we assume 0 and are facade for the generic previous method
  public static int getIntProperty(String name) {
    return getIntProperty(name, 0);
  }

  // String Properties handling
  public static String getStringProperty(String name, String defaultValue) {
    String value = properties.getProperty(name);
    if (value != null && !"".equals(value)) {
      logger.debug(name + " = \"" + value + "\"");
      return value;
    }

    logger.info(LOG_UNDEF_NAME_DEFAULT, name, defaultValue);

    return defaultValue;
  }

  public static String getStringProperty(String name) {
    return getStringProperty(name, null);
  }

  // StringList Properties handling
  public static List<String> getStringListProperty(String name) {
    String value = getStringProperty(name);
    if (value == null || value.length() == 0) {
      return Collections.emptyList();
    }
    List<String> result = new ArrayList<>();
    for (String s : value.split(";")) {
      s = s.trim();
      if (s.length() > 0) {
        result.add(s);
      }
    }
    return result;
  }

  public static Blockchain getBlockchain() {
    return BlockchainImpl.getInstance();
  }

  public static BlockchainProcessor getBlockchainProcessor() {
    return BlockchainProcessorImpl.getInstance();
  }

  public static TransactionProcessor getTransactionProcessor() {
    return TransactionProcessorImpl.getInstance();
  }

  public static Generator getGenerator() {
    return generator;
  }

  public static void setGenerator(Generator newGenerator) {
    generator = newGenerator;
  }

  public static int getEpochTime() {
    return time.getTime();
  }

  static void setTime(Time t) {
    time = t;
  }

  public static void main(String[] args) {
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
        @Override
        public void run() {
          shutdown();
        }
      }));
    init();
  }

  public static void init(Properties customProperties) {
    properties.putAll(customProperties);
    init();
  }

  public static void init() {
    Init.init();

    final DGSGoodsStoreService digitalGoodsStoreService = new DGSGoodsStoreServiceImpl(Burst.getStores().getDigitalGoodsStoreStore());
    final EscrowService escrowService = new EscrowServiceImpl(Burst.getStores().getEscrowStore());
    final TradeService tradeService = new TradeServiceImpl(Burst.getStores().getTradeStore());
    final AssetAccountService assetAccountService = new AssetAccountServiceImpl(stores.getAccountStore());
    final AssetTransferService assetTransferService = new AssetTransferServiceImpl(stores.getAssetTransferStore());
    final AccountService accountService = new AccountServiceImpl(stores.getAccountStore(), stores.getAssetTransferStore());
    final AliasService aliasService = new AliasServiceImpl(stores.getAliasStore());
    final AssetService assetService = new AssetServiceImpl(assetAccountService, tradeService, stores.getAssetStore(), assetTransferService);
    final ParameterService parameterService = new ParameterServiceImpl(accountService, aliasService, assetService,
        digitalGoodsStoreService, getBlockchain(), getBlockchainProcessor(), getTransactionProcessor());
    final OrderService orderService = new OrderServiceImpl(stores.getOrderStore());

    APIServlet.injectServices(getTransactionProcessor(), getBlockchain(), getBlockchainProcessor(), parameterService, accountService,
        aliasService, orderService, assetService, assetTransferService, tradeService, escrowService, digitalGoodsStoreService, assetAccountService);
  }

  public static void shutdown() {
    logger.info("Shutting down...");
    API.shutdown();
    Users.shutdown();
    Peers.shutdown();
    ThreadPool.shutdown();
    Db.shutdown();
    if (BlockchainProcessorImpl.getOclVerify()) {
      OCLPoC.destroy();
    }
    logger.info("Burst server " + VERSION + " stopped.");
    LoggerConfigurator.shutdown();
  }

  public static Stores getStores() {
    return stores;
  }

  public static Dbs getDbs() {
    return dbs;
  }

  private static class Init {

    static {
      try {
        long startTime = System.currentTimeMillis();

        LoggerConfigurator.init();

        dbs = Db.getDbsByDatabaseType();

        stores = new Stores();

        TransactionProcessorImpl.getInstance();
        BlockchainProcessorImpl.getInstance();


        Account.init();
        Alias.init();
        Asset.init();
        DigitalGoodsStore.init();
        Order.init();
        Trade.init();
        AssetTransfer.init();
        Peers.init();
        getGenerator().init();
        API.init();
        Users.init();
        DebugTrace.init();

        int timeMultiplier = (Constants.isTestnet && Constants.isOffline) ? Math.max(getIntProperty("brs.timeMultiplier"), 1) : 1;

        ThreadPool.start(timeMultiplier);
        if (timeMultiplier > 1) {
          setTime(new Time.FasterTime(Math.max(getEpochTime(), getBlockchain().getLastBlock().getTimestamp()), timeMultiplier));
          logger.info("TIME WILL FLOW " + timeMultiplier + " TIMES FASTER!");
        }

        long currentTime = System.currentTimeMillis();
        logger.info("Initialization took " + (currentTime - startTime) + " ms");
        logger.info("Burst server " + VERSION + " started successfully.");

        if (Constants.isTestnet) {
          logger.info("RUNNING ON TESTNET - DO NOT USE REAL ACCOUNTS!");
        }
        if (getBooleanProperty("brs.mockMining")) {
          setGenerator(new GeneratorImpl.MockGeneratorImpl());
        }

        if (BlockchainProcessorImpl.getOclVerify()) {
          try {
            OCLPoC.init();
          }
          catch (OCLPoC.OCLCheckerException e) {
            logger.error("Error initializing OpenCL, disabling ocl verify: " + e.getMessage());
            BlockchainProcessorImpl.setOclVerify(false);
          }
        }
      }
      catch (Exception e) {
        logger.error(e.getMessage(), e);
        System.exit(1);
      }
    }

    //    private Init() {
    //  logger.info("private Init");
    // } // never

    private static void init() {
    }
  }
}
