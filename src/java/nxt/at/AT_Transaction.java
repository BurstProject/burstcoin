/*
 * Copyright (c) 2014 CIYAM Developers

 Distributed under the MIT/X11 software license, please refer to the file license.txt
 in the root project directory or http://www.opensource.org/licenses/mit-license.php.

 */

package nxt.at;

import java.util.SortedMap;
import java.util.TreeMap;


public class AT_Transaction{
	
	private static SortedMap<Long,SortedMap<Long,AT_Transaction>> all_AT_Txs = new TreeMap<>();
	
	private byte[] recipientId = new byte[ AT_Constants.AT_ID_SIZE ];
	private long amount;
	
	AT_Transaction( byte[] recipientId , long amount ){
		this.recipientId = recipientId.clone();
		this.amount = amount;
	}
	
	public Long getAmount(){
		return amount;
	}
	
	public byte[] getRecipientId(){
		return recipientId;
	}

	public void addTransaction( long atId , Long height) {
		
		
		if (all_AT_Txs.containsKey(atId)){
			all_AT_Txs.get(atId).put(height, this);
		}
		else
		{
			SortedMap< Long , AT_Transaction > temp = new TreeMap<>();
			temp.put( (Long) height , this );
			all_AT_Txs.put( atId , temp );
		}
		
	}
	
	public static AT_Transaction getATTransaction(Long atId, Long height){
		if (all_AT_Txs.containsKey(atId)){
			return all_AT_Txs.get(atId).get(height);
		}
		return null;
	}
	
}
