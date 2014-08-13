package nxt.http;

import nxt.Account;
import nxt.Alias;
import nxt.Asset;
import nxt.Generator;
import nxt.Nxt;
import nxt.Order;
import nxt.Poll;
import nxt.Trade;
import nxt.Vote;
import nxt.peer.Peer;
import nxt.peer.Peers;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public final class GetState extends APIServlet.APIRequestHandler {

    static final GetState instance = new GetState();

    private GetState() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        response.put("version", Nxt.VERSION);
        response.put("time", Convert.getEpochTime());
        response.put("lastBlock", Nxt.getBlockchain().getLastBlock().getStringId());
        response.put("cumulativeDifficulty", Nxt.getBlockchain().getLastBlock().getCumulativeDifficulty().toString());

        long totalEffectiveBalance = 0;
        for (Account account : Account.getAllAccounts()) {
            long effectiveBalanceNXT = account.getEffectiveBalanceNXT();
            if (effectiveBalanceNXT > 0) {
                totalEffectiveBalance += effectiveBalanceNXT;
            }
        }
        response.put("totalEffectiveBalanceNXT", totalEffectiveBalance);

        response.put("numberOfBlocks", Nxt.getBlockchain().getHeight() + 1);
        response.put("numberOfTransactions", Nxt.getBlockchain().getTransactionCount());
        response.put("numberOfAccounts", Account.getAllAccounts().size());
        response.put("numberOfAssets", Asset.getAllAssets().size());
        response.put("numberOfOrders", Order.Ask.getAllAskOrders().size() + Order.Bid.getAllBidOrders().size());
        int numberOfTrades = 0;
        for (List<Trade> assetTrades : Trade.getAllTrades()) {
            numberOfTrades += assetTrades.size();
        }
        response.put("numberOfTrades", numberOfTrades);
        response.put("numberOfAliases", Alias.getAllAliases().size());
        response.put("numberOfPolls", Poll.getAllPolls().size());
        response.put("numberOfVotes", Vote.getVotes().size());
        response.put("numberOfPeers", Peers.getAllPeers().size());
        response.put("numberOfUnlockedAccounts", Generator.getAllGenerators().size());
        Peer lastBlockchainFeeder = Nxt.getBlockchainProcessor().getLastBlockchainFeeder();
        response.put("lastBlockchainFeeder", lastBlockchainFeeder == null ? null : lastBlockchainFeeder.getAnnouncedAddress());
        response.put("lastBlockchainFeederHeight", Nxt.getBlockchainProcessor().getLastBlockchainFeederHeight());
        response.put("isScanning", Nxt.getBlockchainProcessor().isScanning());
        response.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        response.put("maxMemory", Runtime.getRuntime().maxMemory());
        response.put("totalMemory", Runtime.getRuntime().totalMemory());
        response.put("freeMemory", Runtime.getRuntime().freeMemory());

        return response;
    }

}
