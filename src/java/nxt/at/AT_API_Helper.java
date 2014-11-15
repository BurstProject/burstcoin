/*
 * Copyright (c) 2014 CIYAM Developers

 Distributed under the MIT/X11 software license, please refer to the file license.txt
 in the root project directory or http://www.opensource.org/licenses/mit-license.php.

 */

package nxt.at;

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
    
    
    
    
}
