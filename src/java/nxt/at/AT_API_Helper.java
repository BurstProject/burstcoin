/*
 * Copyright (c) 2014 CIYAM Developers

 Distributed under the MIT/X11 software license, please refer to the file license.txt
 in the root project directory or http://www.opensource.org/licenses/mit-license.php.

 */

package nxt.at;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AT_API_Helper {
    //private static ByteBuffer buffer = ByteBuffer.allocate(8);    
    
    public static int longToHeight(long x){
    	ByteBuffer buffer = ByteBuffer.allocate(8);
    	buffer.order(ByteOrder.LITTLE_ENDIAN);
    	buffer.putLong(0,x);
    	return buffer.getInt(4);
    }
    
    public static long getLong(byte[] b){
    	ByteBuffer buffer = ByteBuffer.allocate(8);
    	buffer.order(ByteOrder.LITTLE_ENDIAN);
    	buffer.position(0);
    	buffer.put(b);
    	return buffer.getLong(0);
    }
    
    public static byte[] getByteArray( long l ){
    	ByteBuffer buffer = ByteBuffer.allocate(8);
    	buffer.order(ByteOrder.LITTLE_ENDIAN);
    	buffer.clear();
    	buffer.putLong( l );
    	return buffer.array();
    }
    
    public static int longToNumOfTx(long x){
    	ByteBuffer buffer = ByteBuffer.allocate(8);
    	buffer.order(ByteOrder.LITTLE_ENDIAN);
    	buffer.putLong(0,x);
    	return buffer.getInt(0);
    }
    
    protected static long getLongTimestamp(int height, int numOfTx){
    	ByteBuffer buffer = ByteBuffer.allocate(8);
    	buffer.order(ByteOrder.LITTLE_ENDIAN);
    	buffer.putInt(4, height);
    	buffer.putInt(0,numOfTx);
    	return buffer.getLong(0);
    }
    
    public static BigInteger getBigInteger(byte[] b1, byte[] b2, byte[] b3, byte[] b4) {
    	ByteBuffer buffer = ByteBuffer.allocate(32);
    	buffer.order(ByteOrder.LITTLE_ENDIAN);
    	buffer.put(b1);
    	buffer.put(b2);
    	buffer.put(b3);
    	buffer.put(b4);
    	
    	byte[] bytes = buffer.array();
    	
    	return new BigInteger(new byte[] {
    			bytes[31], bytes[30], bytes[29], bytes[28], bytes[27], bytes[26], bytes[25], bytes[24],
    			bytes[23], bytes[22], bytes[21], bytes[20], bytes[19], bytes[18], bytes[17], bytes[16],
    			bytes[15], bytes[14], bytes[13], bytes[12], bytes[11], bytes[10], bytes[9], bytes[8],
    			bytes[7], bytes[6], bytes[5], bytes[4], bytes[3], bytes[2], bytes[1], bytes[0]
    	});
    }
    
    public static byte[] getByteArray(BigInteger bigInt) {
    	byte[] bigIntBytes = bigInt.toByteArray();
    	ByteBuffer paddedBytesBuffer = ByteBuffer.allocate(32);
    	byte padding = 0;
    	if(bigIntBytes.length < 32) {
    		padding = (byte)(((byte)(bigIntBytes[0] & ((byte)0x80))) >> 7);
    		for(int i = 0; i < 32 - bigIntBytes.length; i++)
    			paddedBytesBuffer.put(padding);
    	}
    	
    	paddedBytesBuffer.put(bigIntBytes, (32 >= bigIntBytes.length) ? 0 : (bigIntBytes.length - 32), (32 > bigIntBytes.length) ? bigIntBytes.length : 32);
    	paddedBytesBuffer.clear();
    	byte[] padded = paddedBytesBuffer.array();
    	return new byte[]{
    			padded[31], padded[30], padded[29], padded[28], padded[27], padded[26], padded[25], padded[24],
    			padded[23], padded[22], padded[21], padded[20], padded[19], padded[18], padded[17], padded[16],
    			padded[15], padded[14], padded[13], padded[12], padded[11], padded[10], padded[9], padded[8],
    			padded[7], padded[6], padded[5], padded[4], padded[3], padded[2], padded[1], padded[0]
    	};
    }
    
    
}
