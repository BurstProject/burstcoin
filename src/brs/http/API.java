package brs.http;

import brs.Blockchain;
import brs.BlockchainProcessor;
import brs.Constants;
import brs.EconomicClustering;
import brs.TransactionProcessor;
import brs.services.ATService;
import brs.services.AccountService;
import brs.services.AliasService;
import brs.services.AssetAccountService;
import brs.services.AssetService;
import brs.services.AssetTransferService;
import brs.services.DGSGoodsStoreService;
import brs.services.EscrowService;
import brs.services.OrderService;
import brs.services.ParameterService;
import brs.services.PropertyService;
import brs.services.SubscriptionService;
import brs.services.TimeService;
import brs.services.TradeService;
import brs.util.Subnet;
import brs.util.ThreadPool;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.servlets.DoSFilter;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class API {

  static Set<Subnet> allowedBotHosts;
  static boolean enableDebugAPI;
  private static final Logger logger = LoggerFactory.getLogger(API.class);
  private static final int TESTNET_API_PORT = 6876;
  private static Server apiServer;

  public API(TransactionProcessor transactionProcessor, Blockchain blockchain, BlockchainProcessor blockchainProcessor, ParameterService parameterService,
      AccountService accountService, AliasService aliasService, OrderService orderService, AssetService assetService, AssetTransferService assetTransferService,
      TradeService tradeService, EscrowService escrowService, DGSGoodsStoreService digitalGoodsStoreService, AssetAccountService assetAccountService,
      SubscriptionService subscriptionService, ATService atService, TimeService timeService, EconomicClustering economicClustering, PropertyService propertyService,
      ThreadPool threadPool) {
    enableDebugAPI = propertyService.getBooleanProperty("API.Debug");
    List<String> allowedBotHostsList = propertyService.getStringListProperty("brs.allowedBotHosts");
    if (!allowedBotHostsList.contains("*")) {
      // Temp hashset to store allowed subnets
      Set<Subnet> allowedSubnets = new HashSet<>();

      for (String allowedHost : allowedBotHostsList) {
        try {
          allowedSubnets.add(Subnet.createInstance(allowedHost));
        } catch (UnknownHostException e) {
          logger.error("Error adding allowed bot host '" + allowedHost + "'", e);
        }
      }
      allowedBotHosts = Collections.unmodifiableSet(allowedSubnets);
    } else {
      allowedBotHosts = null;
    }

    boolean enableAPIServer = propertyService.getBooleanProperty("API.Server");
    if (enableAPIServer) {
      final int port = Constants.isTestnet ? TESTNET_API_PORT : propertyService.getIntProperty("API.ServerPort");
      final String host = propertyService.getStringProperty("API.ServerHost");
      apiServer = new Server();
      ServerConnector connector;

      boolean enableSSL = propertyService.getBooleanProperty("API.SSL");
      if (enableSSL) {
        logger.info("Using SSL (https) for the API server");
        HttpConfiguration https_config = new HttpConfiguration();
        https_config.setSecureScheme("https");
        https_config.setSecurePort(port);
        https_config.addCustomizer(new SecureRequestCustomizer());
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(propertyService.getStringProperty("API.SSL_keyStorePath"));
        sslContextFactory.setKeyStorePassword(propertyService.getStringProperty("API.SSL_keyStorePassword"));
        sslContextFactory.setExcludeCipherSuites("SSL_RSA_WITH_DES_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                                                 "SSL_DHE_DSS_WITH_DES_CBC_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5", "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                                                 "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");
        sslContextFactory.setExcludeProtocols("SSLv3");
        connector = new ServerConnector(apiServer, new SslConnectionFactory(sslContextFactory, "http/1.1"),
                                        new HttpConnectionFactory(https_config));
      } else {
        connector = new ServerConnector(apiServer);
      }

      connector.setPort(port);
      connector.setHost(host);
      connector.setIdleTimeout(propertyService.getIntProperty("API.ServerIdleTimeout"));
      connector.setReuseAddress(true);
      apiServer.addConnector(connector);

      HandlerList apiHandlers = new HandlerList();

      ServletContextHandler apiHandler = new ServletContextHandler();
      String apiResourceBase = propertyService.getStringProperty("API.UI_Dir");
      if (apiResourceBase != null) {
        ServletHolder defaultServletHolder = new ServletHolder(new DefaultServlet());
        defaultServletHolder.setInitParameter("dirAllowed", "false");
        defaultServletHolder.setInitParameter("resourceBase", apiResourceBase);
        defaultServletHolder.setInitParameter("welcomeServlets", "true");
        defaultServletHolder.setInitParameter("redirectWelcome", "true");
        defaultServletHolder.setInitParameter("gzip", "true");
        apiHandler.addServlet(defaultServletHolder, "/*");
        apiHandler.setWelcomeFiles(new String[]{"index.html"});
      }

      String javadocResourceBase = propertyService.getStringProperty("API.Doc_Dir");
      if (javadocResourceBase != null) {
        ContextHandler contextHandler  = new ContextHandler("/doc");
        ResourceHandler docFileHandler = new ResourceHandler();
        docFileHandler.setDirectoriesListed(false);
        docFileHandler.setWelcomeFiles(new String[]{"index.html"});
        docFileHandler.setResourceBase(javadocResourceBase);
        contextHandler.setHandler(docFileHandler);
        apiHandlers.addHandler(contextHandler);
      }

      ServletHolder peerServletHolder = new ServletHolder(new APIServlet(transactionProcessor, blockchain, blockchainProcessor, parameterService,
          accountService, aliasService, orderService, assetService, assetTransferService,
          tradeService, escrowService, digitalGoodsStoreService, assetAccountService,
          subscriptionService, atService, timeService, economicClustering));
      apiHandler.addServlet(peerServletHolder, "/burst");

      if (propertyService.getBooleanProperty("JETTY.API.GzipFilter")) {
        FilterHolder gzipFilterHolder = apiHandler.addFilter(GzipFilter.class, "/burst", null);
        gzipFilterHolder.setInitParameter("methods",     propertyService.getStringProperty("JETTY.API.GZIPFilter.methods"));
        gzipFilterHolder.setInitParameter("bufferSize",  propertyService.getStringProperty("JETTY.API.GZIPFilter.bufferSize"));
        gzipFilterHolder.setInitParameter("minGzipSize", propertyService.getStringProperty("JETTY.API.GZIPFilter.minGzipSize"));
        gzipFilterHolder.setAsyncSupported(true);
      }
      
      if (propertyService.getBooleanProperty("JETTY.API.DoSFilter")) {
        FilterHolder dosFilterHolder = apiHandler.addFilter(DoSFilter.class, "/burst", null);
        dosFilterHolder.setInitParameter("maxRequestsPerSec", propertyService.getStringProperty("JETTY.API.DoSFilter.maxRequestsPerSec"));
        dosFilterHolder.setInitParameter("throttledRequests", propertyService.getStringProperty("JETTY.API.DoSFilter.throttledRequests"));
        dosFilterHolder.setInitParameter("delayMs",           propertyService.getStringProperty("JETTY.API.DoSFilter.delayMs"));
        dosFilterHolder.setInitParameter("maxWaitMs",         propertyService.getStringProperty("JETTY.API.DoSFilter.maxWaitMs"));
        dosFilterHolder.setInitParameter("maxRequestMs",      propertyService.getStringProperty("JETTY.API.DoSFilter.maxRequestMs"));
        dosFilterHolder.setInitParameter("maxthrottleMs",     propertyService.getStringProperty("JETTY.API.DoSFilter.throttleMs"));
        dosFilterHolder.setInitParameter("maxIdleTrackerMs",  propertyService.getStringProperty("JETTY.API.DoSFilter.maxIdleTrackerMs"));
        dosFilterHolder.setInitParameter("trackSessions",     propertyService.getStringProperty("JETTY.API.DoSFilter.trackSessions"));
        dosFilterHolder.setInitParameter("insertHeaders",     propertyService.getStringProperty("JETTY.API.DoSFilter.insertHeaders"));
        dosFilterHolder.setInitParameter("remotePort",        propertyService.getStringProperty("JETTY.API.DoSFilter.remotePort"));
        dosFilterHolder.setInitParameter("ipWhitelist",       propertyService.getStringProperty("JETTY.API.DoSFilter.ipWhitelist"));
        dosFilterHolder.setInitParameter("managedAttr",       propertyService.getStringProperty("JETTY.API.DoSFilter.managedAttr"));
        dosFilterHolder.setAsyncSupported(true);
      }

      apiHandler.addServlet(APITestServlet.class, "/test");

      if (propertyService.getBooleanProperty("API.CrossOriginFilter")) {
        FilterHolder filterHolder = apiHandler.addFilter(CrossOriginFilter.class, "/*", null);
        filterHolder.setInitParameter("allowedHeaders", "*");
        filterHolder.setAsyncSupported(true);
      }

      apiHandlers.addHandler(apiHandler);
      apiHandlers.addHandler(new DefaultHandler());

      apiServer.setHandler(apiHandlers);
      apiServer.setStopAtShutdown(true);

      threadPool.runBeforeStart(new Runnable() {
          @Override
          public void run() {
            try {
              apiServer.start();
              logger.info("Started API server at " + host + ":" + port);
            } catch (Exception e) {
              logger.error("Failed to start API server", e);
              throw new RuntimeException(e.toString(), e);
            }

          }
        }, true);

    } else {
      apiServer = null;
      logger.info("API server not enabled");
    }

  }

  public void shutdown() {
    if (apiServer != null) {
      try {
        apiServer.stop();
      } catch (Exception e) {
        logger.info("Failed to stop API server", e);
      }
    }
  }

}
