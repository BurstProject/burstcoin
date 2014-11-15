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
import nxt.db.Db;
import nxt.db.DbUtils;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.Account;
import nxt.TransactionImpl.BuilderImpl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
							int prevHeight = at.getPreviousBlock();
							//at.setRunBlock(block.getHeight());
							
							Account senderAccount = Account.getAccount( AT_API_Helper.getLong( at.getId() ) );
							Long fees = at.getMachineState().getSteps() * AT_Constants.getInstance().STEP_FEE( block.getHeight() );
							
							if ( !( senderAccount.getUnconfirmedBalanceNQT() < fees ) )
							{
						        senderAccount.addToUnconfirmedBalanceNQT( -fees );
						        senderAccount.addToBalanceNQT( -fees );
								makeTransactions( at , block );
					        }
							
							if (orderedATs.containsKey(prevHeight)){
								if (!orderedATs.get(prevHeight).isEmpty()) {
									orderedATs.get(prevHeight).remove(atID);
								}

								if (orderedATs.get(prevHeight).isEmpty())
								{
									orderedATs.remove(prevHeight);
								}								
							}
							if (!orderedATs.containsKey(block.getHeight()))
							{
								List<Long> newAdds = new ArrayList<>();
								newAdds.add(atID);
								orderedATs.put(block.getHeight(),newAdds);
							}
							else
							{
								orderedATs.get(block.getHeight()).add(atID);
							}
							at.previousBlock = block.getHeight();
						}
					}
				} catch (AT_Exception e) {
					e.printStackTrace();
				}	

			}

			private void makeTransactions( AT at, Block block )
			{
				try (Connection con = Db.getConnection()) {
					try {
						TransactionDb.saveTransactions(con , at , block );
						con.commit();
					} catch (SQLException e) {
						con.rollback();
						throw e;
					}
				} catch (SQLException e) {
					throw new RuntimeException(e.toString(), e);
				}
			}

		}, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
	}    


	private static ConcurrentMap< Long , AT> ATs = new ConcurrentHashMap<>();
	private static ConcurrentMap<Long, List<AT>> accountATs = new ConcurrentHashMap<>();
	private static Collection<AT> allATs = Collections.unmodifiableCollection(ATs.values());

	private static ConcurrentSkipListMap< Integer , List< Long > > orderedATs = new ConcurrentSkipListMap<>();


	public static Collection<AT> getAllATs() {
		return allATs;
	}

	protected int getPreviousBlock() {
		return this.previousBlock;
	}

	public static AT getAT(byte[] id) {

		return ATs.get(AT_API_Helper.getLong(id));
	}

	public static AT getAT(Long id) {

		return ATs.get(id);
	}

	public static List<AT> getATsIssuedBy(Long accountId) {
		List<AT> ATs = accountATs.get(accountId);
		if (ATs == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(ATs);
	}

	static void addAT(Long atId, Long senderAccountId, String name, String description, byte[] creationBytes , int height) {

		ByteBuffer bf = ByteBuffer.allocate( 8 + 8 );
		bf.order( ByteOrder.LITTLE_ENDIAN );

		bf.putLong( atId );

		byte[] id = new byte[ 8 ];

		bf.putLong( 8 , senderAccountId );

		byte[] creator = new byte[ 8 ];
		bf.clear();
		bf.get( id , 0 , 8 );
		bf.get( creator , 0 , 8);
		
		AT at = new AT( id , creator , name , description , creationBytes , height );

		AT_Controller.resetMachine(at);
		
		
		saveAT( at );

	}
	
	private static void saveAT( AT at )
	{
		try ( PreparedStatement pstmt = Db.getConnection().prepareStatement( "INSERT INTO at " 
				+ "(atId , creator , name , description , version , g_balance , p_balance , "
				+ "stateBytes , csize , dsize , c_user_stack_bytes , c_call_stack_bytes , "
				+ "minimumFee , creationBlockHeight , waitForNumberOfBlocks , freezeWhenSameBalance , "
				+ "ap_data , ap_code , next_height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" ) )
				{
					int i = 0;
					pstmt.setLong( ++i , AT_API_Helper.getLong( at.getId() ) );
					pstmt.setLong( ++i, AT_API_Helper.getLong( at.getCreator() ) );
					DbUtils.setString( pstmt , ++i , at.getName() );
					DbUtils.setString( pstmt , ++i , at.getDescription() );
					pstmt.setShort( ++i , at.getVersion() );
					pstmt.setLong( ++i , at.getG_balance() );
					pstmt.setLong( ++i , at.getP_balance() );
					DbUtils.setBytes( pstmt , ++i ,  at.getState() );
					pstmt.setInt( ++i , at.getCsize() );
					pstmt.setInt( ++i , at.getDsize() );
					pstmt.setInt( ++i , at.getC_user_stack_bytes() );
					pstmt.setInt( ++i , at.getC_call_stack_bytes() );
					pstmt.setLong( ++i , at.getMinimumFee() );
					pstmt.setInt( ++i, at.getCreationBlockHeight() );
					pstmt.setInt( ++i , at.getWaitForNumberOfBlocks() );
					pstmt.setBoolean( ++i , at.freezeOnSameBalance() );
					DbUtils.setBytes( pstmt , ++i , at.getApData() );
					DbUtils.setBytes( pstmt , ++i , at.getApCode() );
					pstmt.setInt( ++i , Nxt.getBlockchain().getHeight() + at.getWaitForNumberOfBlocks() );
					
					pstmt.executeUpdate();
				}
		catch (SQLException e) {
			throw new RuntimeException(e.toString(), e);
		}
		
	}
	
	private static void updateAT( AT at )
	{
		try ( PreparedStatement pstmt = Db.getConnection().prepareStatement( "UPDATE at " 
				+ "SET g_balance = ? , p_balance = ?, stateBytes = ? , waitForNumberOfBlocks = ?, "
				+ "ap_data = ?  , next_height = ? where atId = " + AT_API_Helper.getLong( at.getId() ) ) )
				{
					int i = 0;
					pstmt.setLong( ++i , at.getG_balance() );
					pstmt.setLong( ++i , at.getP_balance() );
					DbUtils.setBytes( pstmt , ++i ,  at.getState() );
					pstmt.setInt( ++i , at.getWaitForNumberOfBlocks() );
					DbUtils.setBytes( pstmt , ++i , at.getApData() );
					pstmt.setInt( ++i , Nxt.getBlockchain().getHeight() + at.getWaitForNumberOfBlocks() );
					
					pstmt.executeUpdate();
				}
		catch (SQLException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}
	
	private static void deleteAT( AT at )
	{
		try ( PreparedStatement pstmt = Db.getConnection().prepareStatement( "DELETE FROM at where atId = " + AT_API_Helper.getLong( at.getId() ) ) )
				{
					pstmt.executeUpdate();
				}
		catch (SQLException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}
	
	private static void deleteAT( Long id )
	{
		try ( PreparedStatement pstmt = Db.getConnection().prepareStatement( "DELETE FROM at where atId = " + id  ) )
				{
					pstmt.executeUpdate();
				}
		catch (SQLException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}
	
	private static void saveStates()
	{
		try ( PreparedStatement pstmt = Db.getConnection().prepareStatement( "DELETE FROM at_state " ) )
				{
					pstmt.executeUpdate();
				}
		catch (SQLException e) {
			throw new RuntimeException(e.toString(), e);
		}
		
		HashMap< Long , byte[] > states =  AT_Controller.getATsLastStates();
		
		if ( !states.isEmpty() )
		{
			Iterator< Long > ids = states.keySet().iterator();
			
			try ( PreparedStatement pstmt = Db.getConnection().prepareStatement( "INSERT INTO at_state (atId, state)"
					+ " VALUES (?,?) " ) )
			{
				while ( ids.hasNext() )
				{
					Long id = ids.next();
				
			
				int i=0;
				pstmt.setLong( ++i , id );
				DbUtils.setBytes( pstmt , ++i , states.get( id ) );
				pstmt.addBatch();
				}
				pstmt.executeBatch();
			}
			catch (SQLException e) {
				throw new RuntimeException(e.toString(), e);
			}
		}
		
		
	}
	

	public static ConcurrentSkipListMap<Integer,List<Long>> getOrderedATs(){
		return orderedATs;
	}


	static void clear() {
		AT.ATs.clear();
		AT.accountATs.clear();
		AT.orderedATs.clear();
		//sleepingATs.clear();
		//activeATs.clear();
		//zeroBalanceATs.clear();
	}

	static boolean isATAccountId(Long Id) {
		return ATs.containsKey(Id);
	}

	private final String name;    
	private final String description;
	private int previousBlock;


	private AT( byte[] atId , byte[] creator , String name , String description , byte[] creationBytes , int height ) {
		super( atId , creator , creationBytes , height );
		this.name = name;
		this.description = description;
		this.previousBlock = 0;
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
