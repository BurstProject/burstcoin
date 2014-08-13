package nxt.user;

import nxt.Block;
import nxt.Constants;
import nxt.Nxt;
import nxt.Transaction;
import nxt.peer.Peer;
import nxt.peer.Peers;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

public final class GetInitialData extends UserServlet.UserRequestHandler {

    static final GetInitialData instance = new GetInitialData();

    private GetInitialData() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req, User user) throws IOException {

        JSONArray unconfirmedTransactions = new JSONArray();
        JSONArray activePeers = new JSONArray(), knownPeers = new JSONArray(), blacklistedPeers = new JSONArray();
        JSONArray recentBlocks = new JSONArray();

        for (Transaction transaction : Nxt.getTransactionProcessor().getAllUnconfirmedTransactions()) {

            JSONObject unconfirmedTransaction = new JSONObject();
            unconfirmedTransaction.put("index", Users.getIndex(transaction));
            unconfirmedTransaction.put("timestamp", transaction.getTimestamp());
            unconfirmedTransaction.put("deadline", transaction.getDeadline());
            unconfirmedTransaction.put("recipient", Convert.toUnsignedLong(transaction.getRecipientId()));
            unconfirmedTransaction.put("amountNQT", transaction.getAmountNQT());
            unconfirmedTransaction.put("feeNQT", transaction.getFeeNQT());
            unconfirmedTransaction.put("sender", Convert.toUnsignedLong(transaction.getSenderId()));
            unconfirmedTransaction.put("id", transaction.getStringId());

            unconfirmedTransactions.add(unconfirmedTransaction);

        }

        for (Peer peer : Peers.getAllPeers()) {

            if (peer.isBlacklisted()) {

                JSONObject blacklistedPeer = new JSONObject();
                blacklistedPeer.put("index", Users.getIndex(peer));
                blacklistedPeer.put("address", peer.getPeerAddress());
                blacklistedPeer.put("announcedAddress", Convert.truncate(peer.getAnnouncedAddress(), "-", 25, true));
                blacklistedPeer.put("software", peer.getSoftware());
                if (peer.isWellKnown()) {
                    blacklistedPeer.put("wellKnown", true);
                }
                blacklistedPeers.add(blacklistedPeer);

            } else if (peer.getState() == Peer.State.NON_CONNECTED) {

                JSONObject knownPeer = new JSONObject();
                knownPeer.put("index", Users.getIndex(peer));
                knownPeer.put("address", peer.getPeerAddress());
                knownPeer.put("announcedAddress", Convert.truncate(peer.getAnnouncedAddress(), "-", 25, true));
                knownPeer.put("software", peer.getSoftware());
                if (peer.isWellKnown()) {
                    knownPeer.put("wellKnown", true);
                }
                knownPeers.add(knownPeer);

            } else {

                JSONObject activePeer = new JSONObject();
                activePeer.put("index", Users.getIndex(peer));
                if (peer.getState() == Peer.State.DISCONNECTED) {
                    activePeer.put("disconnected", true);
                }
                activePeer.put("address", peer.getPeerAddress());
                activePeer.put("announcedAddress", Convert.truncate(peer.getAnnouncedAddress(), "-", 25, true));
                activePeer.put("weight", peer.getWeight());
                activePeer.put("downloaded", peer.getDownloadedVolume());
                activePeer.put("uploaded", peer.getUploadedVolume());
                activePeer.put("software", peer.getSoftware());
                if (peer.isWellKnown()) {
                    activePeer.put("wellKnown", true);
                }
                activePeers.add(activePeer);
            }
        }

        int height = Nxt.getBlockchain().getLastBlock().getHeight();
        List<? extends Block> lastBlocks = Nxt.getBlockchain().getBlocksFromHeight(Math.max(0, height - 59));

        for (int i = lastBlocks.size() - 1; i >=0; i--) {
            Block block = lastBlocks.get(i);
            JSONObject recentBlock = new JSONObject();
            recentBlock.put("index", Users.getIndex(block));
            recentBlock.put("timestamp", block.getTimestamp());
            recentBlock.put("numberOfTransactions", block.getTransactionIds().size());
            recentBlock.put("totalAmountNQT", block.getTotalAmountNQT());
            recentBlock.put("totalFeeNQT", block.getTotalFeeNQT());
            recentBlock.put("payloadLength", block.getPayloadLength());
            recentBlock.put("generator", Convert.toUnsignedLong(block.getGeneratorId()));
            recentBlock.put("height", block.getHeight());
            recentBlock.put("version", block.getVersion());
            recentBlock.put("block", block.getStringId());
            recentBlock.put("baseTarget", BigInteger.valueOf(block.getBaseTarget()).multiply(BigInteger.valueOf(100000))
                    .divide(BigInteger.valueOf(Constants.INITIAL_BASE_TARGET)));

            recentBlocks.add(recentBlock);
        }

        JSONObject response = new JSONObject();
        response.put("response", "processInitialData");
        response.put("version", Nxt.VERSION);
        if (unconfirmedTransactions.size() > 0) {
            response.put("unconfirmedTransactions", unconfirmedTransactions);
        }
        if (activePeers.size() > 0) {
            response.put("activePeers", activePeers);
        }
        if (knownPeers.size() > 0) {
            response.put("knownPeers", knownPeers);
        }
        if (blacklistedPeers.size() > 0) {
            response.put("blacklistedPeers", blacklistedPeers);
        }
        if (recentBlocks.size() > 0) {
            response.put("recentBlocks", recentBlocks);
        }

        return response;
    }
}
