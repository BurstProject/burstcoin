package nxt.util;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

import fr.cryptohash.Shabal256;

public class MiningPlot {
	public static int HASH_SIZE = 32;
	public static int HASHES_PER_SCOOP = 2;
	public static int SCOOP_SIZE = HASHES_PER_SCOOP * HASH_SIZE;
	public static int SCOOPS_PER_PLOT = 4096; // original 1MB/plot = 16384
	public static int PLOT_SIZE = SCOOPS_PER_PLOT * SCOOP_SIZE;
	
	public static int HASH_CAP = 4096;
	
	public byte[] data = new byte[PLOT_SIZE];
	
	public MiningPlot(long addr, long nonce) {
		ByteBuffer base_buffer = ByteBuffer.allocate(16);
		base_buffer.putLong(addr);
		base_buffer.putLong(nonce);
		byte[] base = base_buffer.array();
		Shabal256 md = new Shabal256();
		byte[] gendata = new byte[PLOT_SIZE + base.length];
		System.arraycopy(base, 0, gendata, PLOT_SIZE, base.length);
		for(int i = PLOT_SIZE; i > 0; i -= HASH_SIZE) {
			md.reset();
			int len = PLOT_SIZE + base.length - i;
			if(len > HASH_CAP) {
				len = HASH_CAP;
			}
			md.update(gendata, i, len);
			md.digest(gendata, i - HASH_SIZE, HASH_SIZE);
		}
		md.reset();
		md.update(gendata);
		byte[] finalhash = md.digest();
		for(int i = 0; i < PLOT_SIZE; i++) {
			data[i] = (byte) (gendata[i] ^ finalhash[i % HASH_SIZE]);
		}
	}
	
	public byte[] getScoop(int pos) {
		return Arrays.copyOfRange(data, pos * SCOOP_SIZE, (pos + 1) * SCOOP_SIZE);
	}
	
	public void hashScoop(Shabal256 md, int pos) {
		md.update(data, pos * SCOOP_SIZE, SCOOP_SIZE);
	}
}
