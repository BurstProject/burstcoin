/*
 * Some portion .. Copyright (c) 2014 CIYAM Developers

 Distributed under the MIT/X11 software license, please refer to the file license.txt
 in the root project directory or http://www.opensource.org/licenses/mit-license.php.

 */

package nxt;


import nxt.at.AT_API_Helper;
import nxt.at.AT_Constants;
import nxt.at.AT_Controller;
import nxt.at.AT_Exception;
import nxt.at.AT_Machine_State;
import nxt.at.AT_Transaction;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.Account;
import nxt.TransactionImpl.BuilderImpl;

import nxt.db.Db;
import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.DbUtils;
import nxt.db.VersionedEntityDbTable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

public final class AT extends AT_Machine_State implements Cloneable  {

	static {
		Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
			@Override
			public void notify(Block block) {
				try {
					if (block.getBlockATs()!=null)
					{
						LinkedHashMap<byte[],byte[]> blockATs = AT_Controller.getATsFromBlock(block.getBlockATs());
						for ( byte[] id : blockATs.keySet()){
							Long atID = AT_API_Helper.getLong( id );
							AT at = AT.getAT( id );
							
							Account senderAccount = Account.getAccount( AT_API_Helper.getLong( at.getId() ) );
							Long fees = at.getMachineState().getSteps() * AT_Constants.getInstance().STEP_FEE( block.getHeight() );
							
							if ( !( senderAccount.getUnconfirmedBalanceNQT() < fees ) )
							{
						        senderAccount.addToUnconfirmedBalanceNQT( -fees );
						        senderAccount.addToBalanceNQT( -fees );
								makeTransactions( at , block );
					        }
							
							at.saveState();
						}
					}
				} catch (AT_Exception e) {
					e.printStackTrace();
				}	

			}

			private void makeTransactions( AT at, Block block )
			{
				try (Connection con = Db.getConnection()) {
					TransactionDb.saveTransactions(con , at , block );
				} catch (SQLException e) {
					throw new RuntimeException(e.toString(), e);
				}
			}

		}, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
	}    

	public static class ATState {
		
		private final long atId;
		private final DbKey dbKey;
		private byte[] state;
		private int nextHeight;
		
		private ATState(long atId, byte[] state, int nextHeight) {
			this.atId = atId;
			this.dbKey = atStateDbKeyFactory.newKey(this.atId);
			this.state = state;
			this.nextHeight = nextHeight;
		}
		
		private ATState(ResultSet rs) throws SQLException {
			this.atId = rs.getLong("id");
			this.dbKey = atStateDbKeyFactory.newKey(this.atId);
			this.state = rs.getBytes("state");
			this.nextHeight = rs.getInt("next_height");
		}
		
		private void save(Connection con) throws SQLException {
			try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO at_state (at_id, "
					+ "state, next_height, height, latest) KEY (at_id) VALUES (?, ?, ?, ?, TRUE)")) {
				int i = 0;
				pstmt.setLong(++i, atId);
				DbUtils.setBytes(pstmt, ++i, state);
				pstmt.setInt(++i, nextHeight);
				pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
				pstmt.executeUpdate();
			}
		}
		
		public long getATId() {
			return atId;
		}
		
		public byte[] getState() {
			return state;
		}
		
		public int getNextHeight() {
			return nextHeight;
		}
		
		public void setState(byte[] newState) {
			state = newState;
		}
		
		public void setNextHeight(int newNextHeight) {
			nextHeight = newNextHeight;
		}
	}
	
	private static final DbKey.LongKeyFactory<AT> atDbKeyFactory = new DbKey.LongKeyFactory<AT>("id") {
		@Override
		public DbKey newKey(AT at) {
			return at.dbKey;
		}
	};
	
	private static final VersionedEntityDbTable<AT> atTable = new VersionedEntityDbTable<AT>("at_info", atDbKeyFactory) {
		@Override
		protected AT load(Connection con, ResultSet rs) throws SQLException {
			return new AT(rs);
		}
		@Override
		protected void save(Connection con, AT at) throws SQLException {
			at.save(con);
		}
		@Override
		protected String defaultSort() {
			return " ORDER BY id ";
		}
	};
	
	private static final DbKey.LongKeyFactory<ATState> atStateDbKeyFactory = new DbKey.LongKeyFactory<AT.ATState>("at_id") {
		@Override
		public DbKey newKey(ATState atState) {
			return atState.dbKey;
		}
	};
	
	private static final VersionedEntityDbTable<ATState> atStateTable = new VersionedEntityDbTable<ATState>("at_state", atStateDbKeyFactory) {
		@Override
		protected ATState load(Connection con, ResultSet rs) throws SQLException {
			return new ATState(rs);
		}
		@Override
		protected void save(Connection con, ATState atState) throws SQLException {
			atState.save(con);
		}
		@Override
		protected String defaultSort() {
			return " ORDER BY height, at_id ";
		}
	};


	public static Collection<AT> getAllATs() {
		DbIterator<AT> ats = atTable.getAll(0, -1);
		List<AT> allATs = new ArrayList<>();
		for(AT at : ats) {
			allATs.add(at);
		}
		return allATs;
	}

	/*protected int getPreviousBlock() {
		return this.previousBlock;
	}*/

	public static AT getAT(byte[] id) {

		return atTable.get(atDbKeyFactory.newKey(AT_API_Helper.getLong(id)));
	}

	public static AT getAT(Long id) {

		return atTable.get(atDbKeyFactory.newKey(id));
	}
	
	public static List<AT> getATsIssuedBy(Long accountId) {
		return getATsIssuedBy(accountId, 0, -1);
	}

	public static List<AT> getATsIssuedBy(Long accountId, int from, int to) {
		DbIterator<AT> ats = atTable.getManyBy(new DbClause.LongClause("creator_id", accountId), from, to);
		List<AT> issuedATs = new ArrayList<>();
		for(AT at : ats) {
			issuedATs.add(at);
		}
		return issuedATs;
	}

	static void addAT(Long atId, Long senderAccountId, String name, String description, byte[] creationBytes , int height) {

		AT at = new AT( atId , senderAccountId , name , description , creationBytes , height );

		AT_Controller.resetMachine(at);
		
		atTable.insert(at);
		at.saveState();

	}
	
	private static byte[] longToByteArray(long in) {
		ByteBuffer bf = ByteBuffer.allocate( 8 );
		bf.order( ByteOrder.LITTLE_ENDIAN );
		bf.putLong(in);
		byte[] out = new byte[ 8 ];
		bf.flip();
		bf.get( out , 0 , 8 );
		return out;
	}

	/*private void setPreviousBlock(int height) {
		this.previousBlock = height;

	}*/
	
	private static DbClause getOrderedATsClause(final int height) {
		return new DbClause(" next_height <= ? ") {
			@Override
			public int set(PreparedStatement pstmt, int index) throws SQLException {
				pstmt.setInt(index++, height);
				return index;
			}
		};
	}

	public static List<AT> getOrderedATs(){
		return getOrderedATs(0);
	}
	
	public static List<AT> getOrderedATs(int max) {
		DbIterator<AT> ats = atTable.getManyBy(getOrderedATsClause(Nxt.getBlockchain().getHeight()),
				0, max - 1, " ORDER BY next_height, id ");
		List<AT> orderedATs = new ArrayList<>();
		for(AT at : ats) {
			orderedATs.add(at);
		}
		return orderedATs;
	}

	static void removeAT(Long atId) {
		AT at = atTable.get(atDbKeyFactory.newKey(atId));
		if(at != null) {
			atTable.delete(at);
		}
		
		ATState atState = atStateTable.get(atStateDbKeyFactory.newKey(atId));
		if(atState != null) {
			atStateTable.delete(atState);
		}
	}

	/*static void clear() {
		AT.ATs.clear();
		AT.accountATs.clear();
		AT.orderedATs.clear();
	}*/

	static boolean isATAccountId(Long id) {
		return atTable.get(atDbKeyFactory.newKey(id)) != null;
	}

	private final long id;
	private final long creatorId;
	private final DbKey dbKey;
	private final String name;    
	private final String description;
	//private int previousBlock;
	private final byte[] creationBytes;
	private final int creationHeight;


	private AT( long id , long creator , String name , String description , byte[] creationBytes , int creationHeight ) {
		super( longToByteArray(id) , longToByteArray(creator) , creationBytes , creationHeight );
		this.id = id;
		this.dbKey = atDbKeyFactory.newKey(this.id);
		this.creatorId = creator;
		this.name = name;
		this.description = description;
		//this.previousBlock = 0;
		this.creationBytes = creationBytes;
		this.creationHeight = creationHeight;
		ATState state = atStateTable.get(atStateDbKeyFactory.newKey(this.id));
		if(state != null) {
			setState(state.getState());
		}
	}
	
	/*public AT( String name , String description , byte[] stateBytes )
	{
		super( stateBytes );
		this.name = name;
		this.description = description;
	}*/
	
	private AT(ResultSet rs) throws SQLException {
		super(longToByteArray(rs.getLong("id")),
				longToByteArray(rs.getLong("creator_id")),
				rs.getBytes("creation_bytes"),
				rs.getByte("creation_height"));
		this.id = rs.getLong("id");
		this.dbKey = atDbKeyFactory.newKey(this.id);
		this.creatorId = rs.getLong("creator_id");
		this.name = rs.getString("name");
		this.description = rs.getString("description");
		this.creationBytes = rs.getBytes("creation_bytes");
		this.creationHeight = rs.getByte("creation_height");
		ATState state = atStateTable.get(atStateDbKeyFactory.newKey(this.id));
		if(state != null) {
			setState(state.getState());
		}
	}
	
	private void save(Connection con) throws SQLException {
		try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO at_info (id, "
				+ "creator_id, name, description, creation_bytes, creation_height, "
				+ "height, latest) KEY (id) VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)")) {
			int i = 0;
			pstmt.setLong(++i, id);
			pstmt.setLong(++i, creatorId);
			DbUtils.setString(pstmt, ++i, name);
			DbUtils.setString(pstmt, ++i, description);
			DbUtils.setBytes(pstmt, ++i, creationBytes);
			pstmt.setInt(++i, creationHeight);
			pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
			pstmt.executeUpdate();
		}
	}
	
	private void saveState() {
		ATState state = atStateTable.get(atStateDbKeyFactory.newKey(id));
		int nextHeight = Nxt.getBlockchain().getHeight() + getWaitForNumberOfBlocks();
		if(state != null) {
			state.setState(getState());
			state.setNextHeight(nextHeight);
		}
		else {
			state = new ATState(id, getState(), nextHeight);
		}
		atStateTable.insert(state);
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public byte[] getApCode() {
		return getAp_code().array();
	}

	public byte[] getApData() {
		return getAp_data().array();
	}

}
