package brs.user;

import com.codahale.metrics.jetty9.InstrumentedHandler;
import brs.*;
import brs.peer.Peer;
import brs.peer.Peers;
import brs.util.Convert;
import brs.util.Listener;
import brs.util.Subnet;
import brs.util.ThreadPool;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class Users {

  private static final Logger logger = LoggerFactory.getLogger(Users.class);

  private static final int TESTNET_UI_PORT=6875;

  private static final ConcurrentMap<String, User> users = new ConcurrentHashMap<>();
  private static final Collection<User> allUsers = Collections.unmodifiableCollection(users.values());

  private static final AtomicInteger peerCounter = new AtomicInteger();
  private static final ConcurrentMap<String, Integer> peerIndexMap = new ConcurrentHashMap<>();
  private static final ConcurrentMap<Integer, String> peerAddressMap = new ConcurrentHashMap<>();

  private static final AtomicInteger blockCounter = new AtomicInteger();
  private static final ConcurrentMap<Long, Integer> blockIndexMap = new ConcurrentHashMap<>();

  private static final AtomicInteger transactionCounter = new AtomicInteger();
  private static final ConcurrentMap<Long, Integer> transactionIndexMap = new ConcurrentHashMap<>();

  static final Set<Subnet> allowedUserHosts;

  static {

    List<String> allowedUserHostsList = Burst.getStringListProperty("brs.allowedUserHosts");
    if (! allowedUserHostsList.contains("*")) {

      // Temp hashset to store allowed subnets
      Set<Subnet> allowedSubnets = new HashSet<>();

      for (String allowedHost : allowedUserHostsList) {
        try {
          allowedSubnets.add(Subnet.createInstance(allowedHost));
        }
        catch (UnknownHostException e) {
          logger.error("Error adding allowed user host '" + allowedHost + "'", e);
        }
      }

      allowedUserHosts = Collections.unmodifiableSet(allowedSubnets);

    }
    else {
      allowedUserHosts = null;
    }
  }

  static Collection<User> getAllUsers() {
    return allUsers;
  }

  static User getUser(String userId) {
    User user = users.get(userId);
    if (user == null) {
      user = new User(userId);
      User oldUser = users.putIfAbsent(userId, user);
      if (oldUser != null) {
        user = oldUser;
        user.setInactive(false);
      }
    }
    else {
      user.setInactive(false);
    }
    return user;
  }

  static User remove(User user) {
    return users.remove(user.getUserId());
  }

  private static void sendNewDataToAll(JSONObject response) {
    response.put("response", "processNewData");
    sendToAll(response);
  }

  private static void sendToAll(JSONStreamAware response) {
    for (User user : users.values()) {
      user.send(response);
    }
  }

  static int getIndex(Peer peer) {
    Integer index = peerIndexMap.get(peer.getPeerAddress());
    if (index == null) {
      index = peerCounter.incrementAndGet();
      peerIndexMap.put(peer.getPeerAddress(), index);
      peerAddressMap.put(index, peer.getPeerAddress());
    }
    return index;
  }

  static Peer getPeer(int index) {
    String peerAddress = peerAddressMap.get(index);
    if (peerAddress == null) {
      return null;
    }
    return Peers.getPeer(peerAddress);
  }

  static int getIndex(Block block) {
    Integer index = blockIndexMap.get(block.getId());
    if (index == null) {
      index = blockCounter.incrementAndGet();
      blockIndexMap.put(block.getId(), index);
    }
    return index;
  }

  static int getIndex(Transaction transaction) {
    Integer index = transactionIndexMap.get(transaction.getId());
    if (index == null) {
      index = transactionCounter.incrementAndGet();
      transactionIndexMap.put(transaction.getId(), index);
    }
    return index;
  }

  public static void init() {}

  public static void shutdown() {
  }

  private Users() {} // never

}
