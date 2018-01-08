package brs.http;

import static brs.http.JSONResponses.ERROR_INCORRECT_REQUEST;
import static brs.http.JSONResponses.ERROR_NOT_ALLOWED;
import static brs.http.JSONResponses.POST_REQUIRED;

import brs.Blockchain;
import brs.BlockchainProcessor;
import brs.Burst;
import brs.BurstException;
import brs.TransactionProcessor;
import brs.services.AccountService;
import brs.services.ParameterService;
import brs.util.JSON;
import brs.util.Subnet;
import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class APIServlet extends HttpServlet {

  private static final Logger logger = LoggerFactory.getLogger(APIServlet.class);

  public static void injectServices(TransactionProcessor transactionProcessor, Blockchain blockchain, BlockchainProcessor blockchainProcessor, ParameterService parameterService,
      AccountService accountService) {
    final Map<String, APIRequestHandler> map = new HashMap<>();

    map.put("broadcastTransaction", new BroadcastTransaction(transactionProcessor));
    map.put("calculateFullHash", new CalculateFullHash());
    map.put("cancelAskOrder", new CancelAskOrder(parameterService, transactionProcessor));
    map.put("cancelBidOrder", new CancelBidOrder(parameterService, transactionProcessor));
    //map.put("castVote", CastVote.instance);
    //map.put("createPoll", CreatePoll.instance);
    map.put("decryptFrom", new DecryptFrom(parameterService));
    map.put("dgsListing", new DGSListing(parameterService, transactionProcessor));
    map.put("dgsDelisting", new DGSDelisting(parameterService, transactionProcessor));
    map.put("dgsDelivery", new DGSDelivery(parameterService, transactionProcessor));
    map.put("dgsFeedback", new DGSFeedback(parameterService, transactionProcessor));
    map.put("dgsPriceChange", new DGSPriceChange(parameterService, transactionProcessor));
    map.put("dgsPurchase", new DGSPurchase(parameterService, transactionProcessor));
    map.put("dgsQuantityChange", new DGSQuantityChange(parameterService, transactionProcessor));
    map.put("dgsRefund", new DGSRefund(parameterService, transactionProcessor));
    map.put("decodeHallmark", DecodeHallmark.instance);
    map.put("decodeToken", DecodeToken.instance);
    map.put("encryptTo", new EncryptTo(parameterService));
    map.put("generateToken", GenerateToken.instance);
    map.put("getAccount", new GetAccount(parameterService));
    map.put("getAccountBlockIds", new GetAccountBlockIds(parameterService));
    map.put("getAccountBlocks", new GetAccountBlocks(blockchain, parameterService));
    map.put("getAccountId", GetAccountId.instance);
    map.put("getAccountPublicKey", new GetAccountPublicKey(parameterService));
    map.put("getAccountTransactionIds", new GetAccountTransactionIds(parameterService));
    map.put("getAccountTransactions", new GetAccountTransactions(parameterService, blockchain));
    map.put("getAccountLessors", new GetAccountLessors(parameterService));
    map.put("sellAlias", new SellAlias(parameterService, transactionProcessor));
    map.put("buyAlias", new BuyAlias(parameterService, transactionProcessor));
    map.put("getAlias", new GetAlias(parameterService));
    map.put("getAliases", new GetAliases(parameterService));
    map.put("getAllAssets", GetAllAssets.instance);
    map.put("getAsset", new GetAsset(parameterService));
    map.put("getAssets", GetAssets.instance);
    map.put("getAssetIds", GetAssetIds.instance);
    map.put("getAssetsByIssuer", new GetAssetsByIssuer(parameterService));
    map.put("getAssetAccounts", new GetAssetAccounts(parameterService));
    map.put("getBalance", new GetBalance(parameterService));
    map.put("getBlock", GetBlock.instance);
    map.put("getBlockId", GetBlockId.instance);
    map.put("getBlocks", GetBlocks.instance);
    map.put("getBlockchainStatus", GetBlockchainStatus.instance);
    map.put("getConstants", GetConstants.instance);
    map.put("getDGSGoods", GetDGSGoods.instance);
    map.put("getDGSGood", new GetDGSGood(parameterService));
    map.put("getDGSPurchases", GetDGSPurchases.instance);
    map.put("getDGSPurchase", GetDGSPurchase.instance);
    map.put("getDGSPendingPurchases", GetDGSPendingPurchases.instance);
    map.put("getGuaranteedBalance", new GetGuaranteedBalance(parameterService));
    map.put("getECBlock", GetECBlock.instance);
    map.put("getMyInfo", GetMyInfo.instance);
    //map.put("getNextBlockGenerators", GetNextBlockGenerators.instance);
    map.put("getPeer", GetPeer.instance);
    map.put("getPeers", GetPeers.instance);
    //map.put("getPoll", GetPoll.instance);
    //map.put("getPollIds", GetPollIds.instance);
    map.put("getState", GetState.instance);
    map.put("getTime", GetTime.instance);
    map.put("getTrades", new GetTrades(parameterService));
    map.put("getAllTrades", GetAllTrades.instance);
    map.put("getAssetTransfers", new GetAssetTransfers(parameterService));
    map.put("getTransaction", new GetTransaction(transactionProcessor));
    map.put("getTransactionBytes", new GetTransactionBytes(blockchain, transactionProcessor));
    map.put("getUnconfirmedTransactionIds", GetUnconfirmedTransactionIds.instance);
    map.put("getUnconfirmedTransactions", GetUnconfirmedTransactions.instance);
    map.put("getAccountCurrentAskOrderIds", new GetAccountCurrentAskOrderIds(parameterService));
    map.put("getAccountCurrentBidOrderIds", new GetAccountCurrentBidOrderIds(parameterService));
    map.put("getAccountCurrentAskOrders", new GetAccountCurrentAskOrders(parameterService));
    map.put("getAccountCurrentBidOrders", new GetAccountCurrentBidOrders(parameterService));
    map.put("getAllOpenAskOrders", GetAllOpenAskOrders.instance);
    map.put("getAllOpenBidOrders", GetAllOpenBidOrders.instance);
    map.put("getAskOrder", GetAskOrder.instance);
    map.put("getAskOrderIds", new GetAskOrderIds(parameterService));
    map.put("getAskOrders", new GetAskOrders(parameterService));
    map.put("getBidOrder", GetBidOrder.instance);
    map.put("getBidOrderIds", new GetBidOrderIds(parameterService));
    map.put("getBidOrders", new GetBidOrders(parameterService));
    map.put("issueAsset", new IssueAsset(parameterService, transactionProcessor));
    map.put("leaseBalance", new LeaseBalance(parameterService, transactionProcessor));
    map.put("longConvert", LongConvert.instance);
    map.put("markHost", MarkHost.instance);
    map.put("parseTransaction", ParseTransaction.instance);
    map.put("placeAskOrder", new PlaceAskOrder(parameterService, transactionProcessor));
    map.put("placeBidOrder", new PlaceBidOrder(parameterService, transactionProcessor));
    map.put("rsConvert", RSConvert.instance);
    map.put("readMessage", ReadMessage.instance);
    map.put("sendMessage", new SendMessage(parameterService, transactionProcessor));
    map.put("sendMoney", new SendMoney(parameterService, transactionProcessor));
    map.put("setAccountInfo", new SetAccountInfo(parameterService, transactionProcessor));
    map.put("setAlias", new SetAlias(parameterService, transactionProcessor));
    map.put("signTransaction", SignTransaction.instance);
    //map.put("startForging", StartForging.instance);
    //map.put("stopForging", StopForging.instance);
    //map.put("getForging", GetForging.instance);
    map.put("transferAsset", new TransferAsset(parameterService, transactionProcessor));
    map.put("getMiningInfo", GetMiningInfo.instance);
    map.put("submitNonce", new SubmitNonce(accountService, blockchain));
    map.put("getRewardRecipient", new GetRewardRecipient(parameterService));
    map.put("setRewardRecipient", new SetRewardRecipient(parameterService, transactionProcessor));
    map.put("getAccountsWithRewardRecipient", new GetAccountsWithRewardRecipient(parameterService, accountService));
    map.put("sendMoneyEscrow", new SendMoneyEscrow(parameterService, transactionProcessor));
    map.put("escrowSign", new EscrowSign(parameterService, transactionProcessor));
    map.put("getEscrowTransaction", GetEscrowTransaction.instance);
    map.put("getAccountEscrowTransactions", new GetAccountEscrowTransactions(parameterService));
    map.put("sendMoneySubscription", new SendMoneySubscription(parameterService, transactionProcessor));
    map.put("subscriptionCancel", new SubscriptionCancel(parameterService, transactionProcessor));
    map.put("getSubscription", GetSubscription.instance);
    map.put("getAccountSubscriptions", new GetAccountSubscriptions(parameterService));
    map.put("getSubscriptionsToAccount", new GetSubscriptionsToAccount(parameterService));
    map.put("createATProgram", new CreateATProgram(parameterService, transactionProcessor));
    map.put("getAT", GetAT.instance);
    map.put("getATDetails", GetATDetails.instance);
    map.put("getATIds", GetATIds.instance);
    map.put("getATLong", GetATLong.instance);
    map.put("getAccountATs", new GetAccountATs(parameterService));

    if (API.enableDebugAPI) {
      map.put("clearUnconfirmedTransactions", new ClearUnconfirmedTransactions(transactionProcessor));
      map.put("fullReset", new FullReset(blockchainProcessor));
      map.put("popOff", new PopOff(blockchainProcessor));
      map.put("scan", Scan.instance);
    }

    apiRequestHandlers = Collections.unmodifiableMap(map);
  }

  abstract static class APIRequestHandler {

    private final List<String> parameters;
    private final Set<APITag> apiTags;

    APIRequestHandler(APITag[] apiTags, String... parameters) {
      this.parameters = Collections.unmodifiableList(Arrays.asList(parameters));
      this.apiTags = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(apiTags)));
    }

    final List<String> getParameters() {
      return parameters;
    }

    final Set<APITag> getAPITags() {
      return apiTags;
    }

    abstract JSONStreamAware processRequest(HttpServletRequest request) throws BurstException;

    boolean requirePost() {
      return false;
    }

    boolean startDbTransaction() {
      return false;
    }

  }

  private static final boolean enforcePost = Burst.getBooleanProperty("brs.apiServerEnforcePOST");

  static Map<String, APIRequestHandler> apiRequestHandlers;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    process(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    process(req, resp);
  }

  private void process(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
    resp.setHeader("Pragma", "no-cache");
    resp.setDateHeader("Expires", 0);

    JSONStreamAware response = JSON.emptyJSON;

    try {

      long startTime = System.currentTimeMillis();

      if (API.allowedBotHosts != null) {
        InetAddress remoteAddress = InetAddress.getByName(req.getRemoteHost());
        boolean allowed = false;
        for (Subnet allowedSubnet : API.allowedBotHosts) {
          if (allowedSubnet.isInNet(remoteAddress)) {
            allowed = true;
            break;
          }
        }
        if (!allowed) {
          response = ERROR_NOT_ALLOWED;
          return;
        }
      }

      String requestType = req.getParameter("requestType");
      if (requestType == null) {
        response = ERROR_INCORRECT_REQUEST;
        return;
      }

      APIRequestHandler apiRequestHandler = apiRequestHandlers.get(requestType);
      if (apiRequestHandler == null) {
        response = ERROR_INCORRECT_REQUEST;
        return;
      }

      if (enforcePost && apiRequestHandler.requirePost() && !"POST".equals(req.getMethod())) {
        response = POST_REQUIRED;
        return;
      }

      try {
        if (apiRequestHandler.startDbTransaction()) {
          Burst.getStores().beginTransaction();
        }
        response = apiRequestHandler.processRequest(req);
      } catch (ParameterException e) {
        response = e.getErrorResponse();
      } catch (BurstException | RuntimeException e) {
        logger.debug("Error processing API request", e);
        response = ERROR_INCORRECT_REQUEST;
      } finally {
        if (apiRequestHandler.startDbTransaction()) {
          Burst.getStores().endTransaction();
        }
      }

      if (response instanceof JSONObject) {
        ((JSONObject) response).put("requestProcessingTime", System.currentTimeMillis() - startTime);
      }

    } finally {
      resp.setContentType("text/plain; charset=UTF-8");
      try (Writer writer = resp.getWriter()) {
        response.writeJSONString(writer);
      }
    }

  }

}
