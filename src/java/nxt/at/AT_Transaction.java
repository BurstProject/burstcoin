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
	
	private byte[] senderId = new byte[ AT_Constants.AT_ID_SIZE ];
	private byte[] recipientId = new byte[ AT_Constants.AT_ID_SIZE ];
	private long amount;
	private byte[] message;
	
	AT_Transaction( byte[] senderId , byte[] recipientId , long amount , byte[] message ){
		this.senderId = senderId.clone();
		this.recipientId = recipientId.clone();
		this.amount = amount;
		this.message = (message != null) ? message.clone() : null;
	}
	
	public Long getAmount(){
		return amount;
	}
	
	public byte[] getSenderId(){
		return senderId;
	}
	
	public byte[] getRecipientId(){
		return recipientId;
	}
	
	public byte[] getMessage() {
		return message;
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
