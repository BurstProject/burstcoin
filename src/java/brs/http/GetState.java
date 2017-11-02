package brs.http;

import brs.*;
import brs.db.NxtIterator;
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

        response.put("application", Nxt.APPLICATION);
        response.put("version", Nxt.VERSION);
        response.put("time", Nxt.getEpochTime());
        response.put("lastBlock", Nxt.getBlockchain().getLastBlock().getStringId());
        response.put("cumulativeDifficulty", Nxt.getBlockchain().getLastBlock().getCumulativeDifficulty().toString());

        
        long totalEffectiveBalance = 0;
        try (NxtIterator<Account> accounts = Account.getAllAccounts(0, -1)) {
            for (Account account : accounts) {
                long effectiveBalanceNXT = account.getBalanceNQT();
                if (effectiveBalanceNXT > 0) {
                    totalEffectiveBalance += effectiveBalanceNXT;
                }
            }
        }
        try(NxtIterator<Escrow> escrows = Escrow.getAllEscrowTransactions()) {
        	for(Escrow escrow : escrows) {
        		totalEffectiveBalance += escrow.getAmountNQT();
        	}
        }
        response.put("totalEffectiveBalanceNXT", totalEffectiveBalance / Constants.ONE_NXT);
        

        if (!"false".equalsIgnoreCase(req.getParameter("includeCounts"))) {
            response.put("numberOfBlocks", Nxt.getBlockchain().getHeight() + 1);
            response.put("numberOfTransactions", Nxt.getBlockchain().getTransactionCount());
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
        response.put("numberOfUnlockedAccounts", Nxt.getGenerator().getAllGenerators().size());
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
