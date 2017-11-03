package brs.http;

import brs.*;
import brs.db.BurstIterator;
import brs.peer.Peer;
import brs.peer.Peers;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetState extends APIServlet.APIRequestHandler {

    static final GetState instance = new GetState();

    private GetState() {
        super(new APITag[] {APITag.INFO}, "includeCounts");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();

        response.put("application", Burst.APPLICATION);
        response.put("version", Burst.VERSION);
        response.put("time", Burst.getEpochTime());
        response.put("lastBlock", Burst.getBlockchain().getLastBlock().getStringId());
        response.put("cumulativeDifficulty", Burst.getBlockchain().getLastBlock().getCumulativeDifficulty().toString());

        
        long totalEffectiveBalance = 0;
        try (BurstIterator<Account> accounts = Account.getAllAccounts(0, -1)) {
            for (Account account : accounts) {
                long effectiveBalanceNXT = account.getBalanceNQT();
                if (effectiveBalanceNXT > 0) {
                    totalEffectiveBalance += effectiveBalanceNXT;
                }
            }
        }
        try(BurstIterator<Escrow> escrows = Escrow.getAllEscrowTransactions()) {
        	for(Escrow escrow : escrows) {
        		totalEffectiveBalance += escrow.getAmountNQT();
        	}
        }
        response.put("totalEffectiveBalanceNXT", totalEffectiveBalance / Constants.ONE_NXT);
        

        if (!"false".equalsIgnoreCase(req.getParameter("includeCounts"))) {
            response.put("numberOfBlocks", Burst.getBlockchain().getHeight() + 1);
            response.put("numberOfTransactions", Burst.getBlockchain().getTransactionCount());
            response.put("numberOfAccounts", Account.getCount());
            response.put("numberOfAssets", Asset.getCount());
            int askCount = Order.Ask.getCount();
            int bidCount = Order.Bid.getCount();
            response.put("numberOfOrders", askCount + bidCount);
            response.put("numberOfAskOrders", askCount);
            response.put("numberOfBidOrders", bidCount);
            response.put("numberOfTrades", Trade.getCount());
            response.put("numberOfTransfers", AssetTransfer.getCount());
            response.put("numberOfAliases", Alias.getCount());
            //response.put("numberOfPolls", Poll.getCount());
            //response.put("numberOfVotes", Vote.getCount());
        }
        response.put("numberOfPeers", Peers.getAllPeers().size());
        response.put("numberOfUnlockedAccounts", Burst.getGenerator().getAllGenerators().size());
        Peer lastBlockchainFeeder = Burst.getBlockchainProcessor().getLastBlockchainFeeder();
        response.put("lastBlockchainFeeder", lastBlockchainFeeder == null ? null : lastBlockchainFeeder.getAnnouncedAddress());
        response.put("lastBlockchainFeederHeight", Burst.getBlockchainProcessor().getLastBlockchainFeederHeight());
        response.put("isScanning", Burst.getBlockchainProcessor().isScanning());
        response.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        response.put("maxMemory", Runtime.getRuntime().maxMemory());
        response.put("totalMemory", Runtime.getRuntime().totalMemory());
        response.put("freeMemory", Runtime.getRuntime().freeMemory());

        return response;
    }

}
