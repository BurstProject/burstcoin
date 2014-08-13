package nxt.peer;

import nxt.Account;
import nxt.Block;
import nxt.Constants;
import nxt.Nxt;
import nxt.Transaction;
import nxt.util.JSON;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;
import nxt.util.ThreadPool;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlets.DoSFilter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

public final class Peers {

    public static enum Event {
        BLACKLIST, UNBLACKLIST, DEACTIVATE, REMOVE,
        DOWNLOADED_VOLUME, UPLOADED_VOLUME, WEIGHT,
        ADDED_ACTIVE_PEER, CHANGED_ACTIVE_PEER,
        NEW_PEER
    }

    static final int LOGGING_MASK_EXCEPTIONS = 1;
    static final int LOGGING_MASK_NON200_RESPONSES = 2;
    static final int LOGGING_MASK_200_RESPONSES = 4;
    static final int communicationLoggingMask;

    static final Set<String> wellKnownPeers;
    static final Set<String> knownBlacklistedPeers;

    static final int connectTimeout;
    static final int readTimeout;
    static final int blacklistingPeriod;

    static final int DEFAULT_PEER_PORT = 8123;
    static final int TESTNET_PEER_PORT = 7123;
    private static final String myPlatform;
    private static final String myAddress;
    private static final int myPeerServerPort;
    private static final String myHallmark;
    private static final boolean shareMyAddress;
    private static final int maxNumberOfConnectedPublicPeers;
    private static final boolean enableHallmarkProtection;
    private static final int pushThreshold;
    private static final int pullThreshold;
    private static final int sendToPeersLimit;
    private static final boolean usePeersDb;
    private static final boolean savePeers;

    static final JSONStreamAware myPeerInfoRequest;
    static final JSONStreamAware myPeerInfoResponse;

    private static final Listeners<Peer,Event> listeners = new Listeners<>();

    private static final ConcurrentMap<String, PeerImpl> peers = new ConcurrentHashMap<>();

    static final Collection<PeerImpl> allPeers = Collections.unmodifiableCollection(peers.values());

    private static final ExecutorService sendToPeersService = Executors.newFixedThreadPool(10);

    static {

        myPlatform = Nxt.getStringProperty("nxt.myPlatform");
        myAddress = Nxt.getStringProperty("nxt.myAddress");
        if (myAddress != null && myAddress.endsWith(":" + TESTNET_PEER_PORT) && ! Constants.isTestnet) {
            throw new RuntimeException("Port " + TESTNET_PEER_PORT + " should only be used for testnet!!!");
        }
        myPeerServerPort = Nxt.getIntProperty("nxt.peerServerPort");
        if (myPeerServerPort == TESTNET_PEER_PORT && ! Constants.isTestnet) {
            throw new RuntimeException("Port " + TESTNET_PEER_PORT + " should only be used for testnet!!!");
        }
        shareMyAddress = Nxt.getBooleanProperty("nxt.shareMyAddress");
        myHallmark = Nxt.getStringProperty("nxt.myHallmark");
        if (Peers.myHallmark != null && Peers.myHallmark.length() > 0) {
            try {
                Hallmark hallmark = Hallmark.parseHallmark(Peers.myHallmark);
                if (! hallmark.isValid() || myAddress == null) {
                    throw new RuntimeException();
                }
                URI uri = new URI("http://" + myAddress.trim());
                String host = uri.getHost();
                if (! hallmark.getHost().equals(host)) {
                    throw new RuntimeException();
                }
            } catch (RuntimeException|URISyntaxException e) {
                Logger.logMessage("Your hallmark is invalid: " + Peers.myHallmark + " for your address: " + myAddress);
                throw new RuntimeException(e.toString(), e);
            }
        }

        JSONObject json = new JSONObject();
        if (myAddress != null && myAddress.length() > 0) {
            try {
                URI uri = new URI("http://"+myAddress.trim());
                String host = uri.getHost();
                int port = uri.getPort();
                if (!Constants.isTestnet) {
                    if (port >= 0)
                        json.put("announcedAddress", myAddress);
                    else
                        json.put("announcedAddress", host+(myPeerServerPort!=DEFAULT_PEER_PORT ? ":"+myPeerServerPort : ""));
                } else {
                    json.put("announcedAddress", host);
                }
            } catch (URISyntaxException e) {
                Logger.logMessage("Your announce address is invalid: " + myAddress);
                throw new RuntimeException(e.toString(), e);
            }
        }
        if (Peers.myHallmark != null && Peers.myHallmark.length() > 0) {
            json.put("hallmark", Peers.myHallmark);
        }
        json.put("application", "NRS");
        json.put("version", Nxt.VERSION);
        json.put("platform", Peers.myPlatform);
        json.put("shareAddress", Peers.shareMyAddress);
        Logger.logDebugMessage("My peer info:\n" + json.toJSONString());
        myPeerInfoResponse = JSON.prepare(json);
        json.put("requestType", "getInfo");
        myPeerInfoRequest = JSON.prepareRequest(json);

        List<String> wellKnownPeersList = Constants.isTestnet ? Nxt.getStringListProperty("nxt.testnetPeers")
                : Nxt.getStringListProperty("nxt.wellKnownPeers");
        if (wellKnownPeersList.isEmpty()) {
            wellKnownPeers = Collections.emptySet();
        } else {
            wellKnownPeers = Collections.unmodifiableSet(new HashSet<>(wellKnownPeersList));
        }

        List<String> knownBlacklistedPeersList = Nxt.getStringListProperty("nxt.knownBlacklistedPeers");
        if (knownBlacklistedPeersList.isEmpty()) {
            knownBlacklistedPeers = Collections.emptySet();
        } else {
            knownBlacklistedPeers = Collections.unmodifiableSet(new HashSet<>(knownBlacklistedPeersList));
        }

        maxNumberOfConnectedPublicPeers = Nxt.getIntProperty("nxt.maxNumberOfConnectedPublicPeers");
        connectTimeout = Nxt.getIntProperty("nxt.connectTimeout");
        readTimeout = Nxt.getIntProperty("nxt.readTimeout");
        enableHallmarkProtection = Nxt.getBooleanProperty("nxt.enableHallmarkProtection");
        pushThreshold = Nxt.getIntProperty("nxt.pushThreshold");
        pullThreshold = Nxt.getIntProperty("nxt.pullThreshold");

        blacklistingPeriod = Nxt.getIntProperty("nxt.blacklistingPeriod");
        communicationLoggingMask = Nxt.getIntProperty("nxt.communicationLoggingMask");
        sendToPeersLimit = Nxt.getIntProperty("nxt.sendToPeersLimit");

        if (! wellKnownPeers.isEmpty()) {
            StringBuilder buf = new StringBuilder();
            for (String address : wellKnownPeers) {
                Peer peer = Peers.addPeer(address);
                if (peer != null) {
                    buf.append(peer.getPeerAddress()).append("; ");
                } else {
                    Logger.logMessage("Invalid well known peer address: " + address);
                }
            }
            Logger.logDebugMessage("Well known peers: " + buf.toString());
        }

        usePeersDb = Nxt.getBooleanProperty("nxt.usePeersDb");
        if (usePeersDb) {
            Logger.logDebugMessage("Loading known peers from the database...");
            for (String savedPeer : PeerDb.loadPeers()) {
                Peers.addPeer(savedPeer);
            }
        }
        Logger.logDebugMessage("Known peers: " + peers.size());
        savePeers = usePeersDb && Nxt.getBooleanProperty("nxt.savePeers");

    }

    private static class Init {

        private final static Server peerServer;

        static {
            if (Peers.shareMyAddress) {
                peerServer = new Server();
                ServerConnector connector = new ServerConnector(peerServer);
                final int port = Constants.isTestnet ? TESTNET_PEER_PORT : Peers.myPeerServerPort;
                connector.setPort(port);
                final String host = Nxt.getStringProperty("nxt.peerServerHost");
                connector.setHost(host);
                connector.setIdleTimeout(Nxt.getIntProperty("nxt.peerServerIdleTimeout"));
                peerServer.addConnector(connector);

                ServletHandler peerHandler = new ServletHandler();
                peerHandler.addServletWithMapping(PeerServlet.class, "/*");
                if (Nxt.getBooleanProperty("nxt.enablePeerServerDoSFilter")) {
                    FilterHolder filterHolder = peerHandler.addFilterWithMapping(DoSFilter.class, "/*", FilterMapping.DEFAULT);
                    filterHolder.setInitParameter("maxRequestsPerSec", Nxt.getStringProperty("nxt.peerServerDoSFilter.maxRequestsPerSec"));
                    filterHolder.setInitParameter("delayMs", Nxt.getStringProperty("nxt.peerServerDoSFilter.delayMs"));
                    filterHolder.setInitParameter("maxRequestMs", Nxt.getStringProperty("nxt.peerServerDoSFilter.maxRequestMs"));
                    filterHolder.setInitParameter("trackSessions", "false");
                    filterHolder.setAsyncSupported(true);
                }

                peerServer.setHandler(peerHandler);
                peerServer.setStopAtShutdown(true);
                ThreadPool.runBeforeStart(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            peerServer.start();
                            Logger.logMessage("Started peer networking server at " + host + ":" + port);
                        } catch (Exception e) {
                            Logger.logDebugMessage("Failed to start peer networking server", e);
                            throw new RuntimeException(e.toString(), e);
                        }
                    }
                });
            } else {
                peerServer = null;
                Logger.logMessage("shareMyAddress is disabled, will not start peer networking server");
            }
        }

        private static void init() {}

        private Init() {}

    }

    private static final Runnable peerUnBlacklistingThread = new Runnable() {

        @Override
        public void run() {

            try {
                try {

                    long curTime = System.currentTimeMillis();
                    for (PeerImpl peer : peers.values()) {
                        peer.updateBlacklistedStatus(curTime);
                    }

                } catch (Exception e) {
                    Logger.logDebugMessage("Error un-blacklisting peer", e);
                }
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

    };

    private static final Runnable peerConnectingThread = new Runnable() {

        @Override
        public void run() {

            try {
                try {

                    if (getNumberOfConnectedPublicPeers() < Peers.maxNumberOfConnectedPublicPeers) {
                        PeerImpl peer = (PeerImpl)getAnyPeer(ThreadLocalRandom.current().nextInt(2) == 0 ? Peer.State.NON_CONNECTED : Peer.State.DISCONNECTED, false);
                        if (peer != null) {
                            peer.connect();
                        }
                    }

                } catch (Exception e) {
                    Logger.logDebugMessage("Error connecting to peer", e);
                }
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

    };

    private static final Runnable getMorePeersThread = new Runnable() {

        private final JSONStreamAware getPeersRequest;
        {
            JSONObject request = new JSONObject();
            request.put("requestType", "getPeers");
            getPeersRequest = JSON.prepareRequest(request);
        }

        @Override
        public void run() {

            try {
                try {

                    Peer peer = getAnyPeer(Peer.State.CONNECTED, true);
                    if (peer == null) {
                        return;
                    }
                    JSONObject response = peer.send(getPeersRequest);
                    if (response == null) {
                        return;
                    }
                    JSONArray peers = (JSONArray)response.get("peers");
                    if (peers == null) {
                        return;
                    }
                    for (Object announcedAddress : peers) {
                        addPeer((String) announcedAddress);
                    }
                    if (savePeers) {
                        updateSavedPeers();
                    }

                } catch (Exception e) {
                    Logger.logDebugMessage("Error requesting peers from a peer", e);
                }
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

        private void updateSavedPeers() {
            Set<String> oldPeers = new HashSet<>(PeerDb.loadPeers());
            Set<String> currentPeers = new HashSet<>();
            for (Peer peer : Peers.peers.values()) {
                if (peer.getAnnouncedAddress() != null && ! peer.isBlacklisted()) {
                    currentPeers.add(peer.getAnnouncedAddress());
                }
            }
            Set<String> toDelete = new HashSet<>(oldPeers);
            toDelete.removeAll(currentPeers);
            PeerDb.deletePeers(toDelete);
            currentPeers.removeAll(oldPeers);
            PeerDb.addPeers(currentPeers);
        }

    };

    static {
        Account.addListener(new Listener<Account>() {
            @Override
            public void notify(Account account) {
                for (PeerImpl peer : Peers.peers.values()) {
                    if (peer.getHallmark() != null && peer.getHallmark().getAccountId().equals(account.getId())) {
                        Peers.listeners.notify(peer, Peers.Event.WEIGHT);
                    }
                }
            }
        }, Account.Event.BALANCE);
    }

    static {
        ThreadPool.scheduleThread(Peers.peerConnectingThread, 5);
        ThreadPool.scheduleThread(Peers.peerUnBlacklistingThread, 1);
        ThreadPool.scheduleThread(Peers.getMorePeersThread, 5);
    }

    public static void init() {
        Init.init();
    }

    public static void shutdown() {
        if (Init.peerServer != null) {
            try {
                Init.peerServer.stop();
            } catch (Exception e) {
                Logger.logDebugMessage("Failed to stop peer server", e);
            }
        }
        String dumpPeersVersion = Nxt.getStringProperty("nxt.dumpPeersVersion");
        if (dumpPeersVersion != null) {
            StringBuilder buf = new StringBuilder();
            for (Peer peer : new HashSet<>(peers.values())) {
                if (peer.getAnnouncedAddress() != null && peer.shareAddress() && !peer.isBlacklisted()
                        && peer.getVersion() != null && peer.getVersion().startsWith(dumpPeersVersion)) {
                    buf.append("('").append(peer.getAnnouncedAddress()).append("'), ");
                }
            }
            Logger.logDebugMessage(buf.toString());
        }
        ThreadPool.shutdownExecutor(sendToPeersService);

    }

    public static boolean addListener(Listener<Peer> listener, Event eventType) {
        return Peers.listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Peer> listener, Event eventType) {
        return Peers.listeners.removeListener(listener, eventType);
    }

    static void notifyListeners(Peer peer, Event eventType) {
        Peers.listeners.notify(peer, eventType);
    }

    public static Collection<? extends Peer> getAllPeers() {
        return allPeers;
    }

    public static Peer getPeer(String peerAddress) {
        return peers.get(peerAddress);
    }

    public static Peer addPeer(String announcedAddress) {
        if (announcedAddress == null) {
            return null;
        }
        try {
            URI uri = new URI("http://" + announcedAddress.trim());
            String host = uri.getHost();
            InetAddress inetAddress = InetAddress.getByName(host);
            return addPeer(inetAddress.getHostAddress(), announcedAddress);
        } catch (URISyntaxException | UnknownHostException e) {
            //Logger.logDebugMessage("Invalid peer address: " + announcedAddress + ", " + e.toString());
            return null;
        }
    }

    static PeerImpl addPeer(final String address, final String announcedAddress) {

        String peerAddress = normalizeHostAndPort(address);
        if (peerAddress == null) {
            return null;
        }

        String announcedPeerAddress = normalizeHostAndPort(announcedAddress);

        if (Peers.myAddress != null && Peers.myAddress.length() > 0 && Peers.myAddress.equalsIgnoreCase(announcedPeerAddress)) {
            return null;
        }

        PeerImpl peer = peers.get(peerAddress);
        if (peer == null) {
            peer = new PeerImpl(peerAddress, announcedPeerAddress);
            if (Constants.isTestnet && peer.getPort() > 0 && peer.getPort() != TESTNET_PEER_PORT) {
                Logger.logDebugMessage("Peer " + peerAddress + " on testnet is not using port " + TESTNET_PEER_PORT + ", ignoring");
                return null;
            }
            peers.put(peerAddress, peer);
            listeners.notify(peer, Event.NEW_PEER);
        }

        return peer;
    }

    static PeerImpl removePeer(PeerImpl peer) {
        return peers.remove(peer.getPeerAddress());
    }

    public static void sendToSomePeers(Block block) {
        JSONObject request = block.getJSONObject();
        request.put("requestType", "processBlock");
        sendToSomePeers(request);
    }

    public static void sendToSomePeers(List<Transaction> transactions) {
        JSONObject request = new JSONObject();
        JSONArray transactionsData = new JSONArray();
        for (Transaction transaction : transactions) {
            transactionsData.add(transaction.getJSONObject());
        }
        request.put("requestType", "processTransactions");
        request.put("transactions", transactionsData);
        sendToSomePeers(request);
    }

    private static void sendToSomePeers(final JSONObject request) {

        final JSONStreamAware jsonRequest = JSON.prepareRequest(request);

        int successful = 0;
        List<Future<JSONObject>> expectedResponses = new ArrayList<>();
        for (final Peer peer : peers.values()) {

            if (Peers.enableHallmarkProtection && peer.getWeight() < Peers.pushThreshold) {
                continue;
            }

            if (! peer.isBlacklisted() && peer.getState() == Peer.State.CONNECTED && peer.getAnnouncedAddress() != null) {
                Future<JSONObject> futureResponse = sendToPeersService.submit(new Callable<JSONObject>() {
                    @Override
                    public JSONObject call() {
                        return peer.send(jsonRequest);
                    }
                });
                expectedResponses.add(futureResponse);
            }
            if (expectedResponses.size() >= Peers.sendToPeersLimit - successful) {
                for (Future<JSONObject> future : expectedResponses) {
                    try {
                        JSONObject response = future.get();
                        if (response != null && response.get("error") == null) {
                            successful += 1;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        Logger.logDebugMessage("Error in sendToSomePeers", e);
                    }

                }
                expectedResponses.clear();
            }
            if (successful >= Peers.sendToPeersLimit) {
                return;
            }

        }

    }

    public static Peer getAnyPeer(Peer.State state, boolean applyPullThreshold) {

        List<Peer> selectedPeers = new ArrayList<>();
        for (Peer peer : peers.values()) {
            if (! peer.isBlacklisted() && peer.getState() == state && peer.shareAddress()
                    && (!applyPullThreshold || ! Peers.enableHallmarkProtection || peer.getWeight() >= Peers.pullThreshold)) {
                selectedPeers.add(peer);
            }
        }

        if (selectedPeers.size() > 0) {
            long totalWeight = 0;
            for (Peer peer : selectedPeers) {
                long weight = peer.getWeight();
                if (weight == 0) {
                    weight = 1;
                }
                totalWeight += weight;
            }

            long hit = ThreadLocalRandom.current().nextLong(totalWeight);
            for (Peer peer : selectedPeers) {
                long weight = peer.getWeight();
                if (weight == 0) {
                    weight = 1;
                }
                if ((hit -= weight) < 0) {
                    return peer;
                }
            }
        }
        return null;
    }

    static String normalizeHostAndPort(String address) {
        try {
            if (address == null) {
                return null;
            }
            URI uri = new URI("http://" + address.trim());
            String host = uri.getHost();
            if (host == null || host.equals("") || host.equals("localhost") ||
                                host.equals("127.0.0.1") || host.equals("[0:0:0:0:0:0:0:1]")) {
                return null;
            }
            InetAddress inetAddress = InetAddress.getByName(host);
            if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress() ||
                                                   inetAddress.isLinkLocalAddress()) {
                return null;
            }
            int port = uri.getPort();
            return port == -1 ? host : host + ':' + port;
        } catch (URISyntaxException |UnknownHostException e) {
            return null;
        }
    }

    private static int getNumberOfConnectedPublicPeers() {
        int numberOfConnectedPeers = 0;
        for (Peer peer : peers.values()) {
            if (peer.getState() == Peer.State.CONNECTED && peer.getAnnouncedAddress() != null) {
                numberOfConnectedPeers++;
            }
        }
        return numberOfConnectedPeers;
    }

    private Peers() {} // never

}
