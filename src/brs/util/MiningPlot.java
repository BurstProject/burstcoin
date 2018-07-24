package brs.util;

import static brs.fluxcapacitor.FeatureToggle.POC2;

import brs.fluxcapacitor.FluxCapacitor;
import java.nio.ByteBuffer;
import java.util.Arrays;

import brs.crypto.hash.Shabal256;

public class MiningPlot {
  public static final int HASH_SIZE = 32;
  public static final int HASHES_PER_SCOOP = 2;
  public static final int SCOOP_SIZE = HASHES_PER_SCOOP * HASH_SIZE;
  public static final int SCOOPS_PER_PLOT = 4096; // original 1MB/plot = 16384
  public static final int PLOT_SIZE = SCOOPS_PER_PLOT * SCOOP_SIZE;

  public static final int HASH_CAP = 4096;

  private byte[] data = new byte[PLOT_SIZE];

  private FluxCapacitor fluxCapacitor;

  public MiningPlot(long addr, long nonce, int blockHeight, FluxCapacitor fluxCapacitor) {
    this.fluxCapacitor = fluxCapacitor;
    ByteBuffer base_buffer = ByteBuffer.allocate(16);
    base_buffer.putLong(addr);
    base_buffer.putLong(nonce);
    byte[] base = base_buffer.array();
    Shabal256 md = new Shabal256();
    byte[] gendata = new byte[PLOT_SIZE + base.length];
    System.arraycopy(base, 0, gendata, PLOT_SIZE, base.length);
    for (int i = PLOT_SIZE; i > 0; i -= HASH_SIZE) {
      md.reset();
      int len = PLOT_SIZE + base.length - i;
      if (len > HASH_CAP) {
        len = HASH_CAP;
      }
      md.update(gendata, i, len);
      md.digest(gendata, i - HASH_SIZE, HASH_SIZE);
    }
    md.reset();
    md.update(gendata);
    byte[] finalhash = md.digest();
    for (int i = 0; i < PLOT_SIZE; i++) {
      data[i] = (byte) (gendata[i] ^ finalhash[i % HASH_SIZE]);
    }
    //PoC2 Rearrangement
    if (fluxCapacitor.isActive(POC2, blockHeight)) {
      byte[] hashBuffer = new byte[HASH_SIZE];
      int revPos = PLOT_SIZE - HASH_SIZE; //Start at second hash in last scoop
      for (int pos = 32; pos < (PLOT_SIZE / 2); pos += 64) { //Start at second hash in first scoop
        System.arraycopy(data, pos, hashBuffer, 0, HASH_SIZE); //Copy low scoop second hash to buffer
        System.arraycopy(data, revPos, data, pos, HASH_SIZE); //Copy high scoop second hash to low scoop second hash
        System.arraycopy(hashBuffer, 0, data, revPos, HASH_SIZE); //Copy buffer to high scoop second hash
        revPos -= 64; //move backwards
      }
    }
  }

  public byte[] getScoop(int pos) {
    return Arrays.copyOfRange(data, pos * SCOOP_SIZE, (pos + 1) * SCOOP_SIZE);
  }

  public void hashScoop(Shabal256 md, int pos) {
    md.update(data, pos * SCOOP_SIZE, SCOOP_SIZE);
  }
}
