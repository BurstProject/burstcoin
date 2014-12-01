package nxt.at;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import nxt.AT;
import nxt.Account;
import nxt.db.Db;
import nxt.Nxt;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Logger;

//NXT API IMPLEMENTATION

public class AT_API_Platform_Impl extends AT_API_Impl {

	private final static AT_API_Platform_Impl instance = new AT_API_Platform_Impl();


	AT_API_Platform_Impl()
	{

	}

	public static AT_API_Platform_Impl getInstance()
	{
		return instance;
	}

	@Override
	public long get_Block_Timestamp( AT_Machine_State state ) 
	{

		int height = Nxt.getBlockchain().getHeight();
		return AT_API_Helper.getLongTimestamp( height , 0 );

	}

	public long get_Creation_Timestamp( AT_Machine_State state ) 
	{
		return AT_API_Helper.getLongTimestamp( state.getCreationBlockHeight() , 0 );
	}

	@Override
	public long get_Last_Block_Timestamp( AT_Machine_State state ) 
	{

		int height = Nxt.getBlockchain().getHeight() - 1;
		return AT_API_Helper.getLongTimestamp( height , 0 );
	}

	@Override
	public void put_Last_Block_Hash_In_A( AT_Machine_State state ) {
		ByteBuffer b = ByteBuffer.allocate( state.get_A1().length * 4 );
		b.order( ByteOrder.LITTLE_ENDIAN );

		b.put( Nxt.getBlockchain().getLastBlock().getPreviousBlockHash() );

		byte[] temp = new byte[ 8 ];

		b.get( temp, 0 , 8 );
		state.set_A1( temp );

		b.get( temp , 0 , 8 );
		state.set_A2( temp );

		b.get( temp , 0 , 8 );
		state.set_A3( temp );

		b.get( temp , 0 , 8 );
		state.set_A4( temp );


	}

	@Override
	public void A_to_Tx_after_Timestamp( long val , AT_Machine_State state ) {

		int height = AT_API_Helper.longToHeight( val );
		int numOfTx = AT_API_Helper.longToNumOfTx( val );

		byte[] bId = state.getId();
		byte[] b = new byte[ 8 ];
		for ( int i = 0; i < 8; i++ )
		{
			b[ i ] = bId[ i ];
		}

		long tx = findTransaction( height , AT_API_Helper.getLong( b ) , numOfTx);
		Logger.logInfoMessage("tx with id "+tx+" found");
		clear_A( state );
		state.set_A1( AT_API_Helper.getByteArray( tx ) );

	}

	@Override
	public long get_Type_for_Tx_in_A( AT_Machine_State state ) {
		long txid = AT_API_Helper.getLong( state.get_A1() );

		Transaction tx = Nxt.getBlockchain().getTransaction( txid );

		if ( tx != null )
		{
			if (tx.getType().getType() == 1 )
			{
				return 1;
			}
		}
		return 0;
	}

	@Override
	public long get_Amount_for_Tx_in_A( AT_Machine_State state ) {
		long txId = AT_API_Helper.getLong( state.get_A1() );

		Transaction tx = Nxt.getBlockchain().getTransaction( txId );
		long amount = 0;
		if ( tx != null )
		{
			amount = tx.getAmountNQT();
		}
		return amount;
	}

	@Override
	public long get_Timestamp_for_Tx_in_A( AT_Machine_State state ) {
		long txId = AT_API_Helper.getLong( state.get_A1() );
		Logger.logInfoMessage("get timestamp for tx with id " + txId + " found");
		Transaction tx = Nxt.getBlockchain().getTransaction( txId );

		if ( tx != null )
		{
			int blockHeight = tx.getHeight();

			byte[] bId = state.getId();
			byte[] b = new byte[ 8 ];
			for ( int i = 0; i < 8; i++ )
			{
				b[ i ] = bId[ i ];
			}

			int txHeight = findTransactionHeight( txId , blockHeight , AT_API_Helper.getLong( b ) );

			return AT_API_Helper.getLongTimestamp( blockHeight , txHeight );
		}
		return AT_API_Helper.getLongTimestamp( Integer.MAX_VALUE , Integer.MAX_VALUE );
	}

	@Override
	public long get_Ticket_Id_for_Tx_in_A( AT_Machine_State state ) {
		long txId = AT_API_Helper.getLong( state.get_A1() );

		Transaction tx = Nxt.getBlockchain().getTransaction( txId );

		if ( tx !=null )
		{
			int txBlockHeight = tx.getHeight();


			int blockHeight = Nxt.getBlockchain().getHeight();

			if ( blockHeight - txBlockHeight < AT_Constants.getInstance().BLOCKS_FOR_TICKET( blockHeight ) ){ //for tests - for real case 1440
				state.setWaitForNumberOfBlocks( (int)AT_Constants.getInstance().BLOCKS_FOR_TICKET( blockHeight ) - ( blockHeight - txBlockHeight ) );
				state.getMachineState().pc -= 11;
				state.getMachineState().stopped = true;
				return 0;
			}

			MessageDigest digest = Crypto.sha256();

			byte[] senderPublicKey = tx.getSenderPublicKey();

			ByteBuffer bf = ByteBuffer.allocate( 2 * Long.SIZE + senderPublicKey.length );
			bf.order( ByteOrder.LITTLE_ENDIAN );
			bf.putLong( Nxt.getBlockchain().getLastBlock().getId() );
			bf.putLong( tx.getId() );
			bf.put( senderPublicKey);

			digest.update(bf.array());
			byte[] byteTicket = digest.digest();

			long ticket = Math.abs( AT_API_Helper.getLong( byteTicket ) );

			System.out.println( "info: ticket for txid: " + Convert.toUnsignedLong( tx.getId() ) + "is: " + ticket );
			return ticket;
		}
		return 0;
	}

	@Override
	public long message_from_Tx_in_A_to_B( AT_Machine_State state ) {
		long txid = AT_API_Helper.getLong( state.get_A1() );

		Transaction tx = Nxt.getBlockchain().getTransaction( txid );
		byte[] message = tx.getMessage().getMessage();

		if ( message.length > state.get_A1().length * 4 )
			return 0;

		ByteBuffer b = ByteBuffer.allocate( state.get_A1().length * 4 );
		b.order( ByteOrder.LITTLE_ENDIAN );
		b.put( message );
		b.clear();

		byte[] temp = new byte[ 8 ];

		b.get( temp, 0 , 8 );
		state.set_B1( temp );

		b.get( temp , 0 , 8 );
		state.set_B2( temp );

		b.get( temp , 0 , 8 );
		state.set_B3( temp );

		b.get( temp , 0 , 8 );
		state.set_B4( temp );

		return 1;
	}
	@Override
	public long B_to_Address_of_Tx_in_A( AT_Machine_State state ) {

		long tx = AT_API_Helper.getLong( state.get_A1() );
		
		long address = Nxt.getBlockchain().getTransaction( tx ).getSenderId();

		clear_B( state );

		state.set_B1( AT_API_Helper.getByteArray( address ) );

		return 1;
	}

	@Override
	public void B_to_Address_of_Creator( AT_Machine_State state ) {
		long creator = AT_API_Helper.getLong( state.getCreator() );

		clear_B( state );

		state.set_B1( AT_API_Helper.getByteArray( creator ) );

	}

	@Override
	public long get_Current_Balance( AT_Machine_State state ) {
		long balance = Account.getAccount( AT_API_Helper.getLong(state.getId()) ).getBalanceNQT();
		return balance;
	}

	@Override
	public long get_Previous_Balance( AT_Machine_State state ) {
		return state.getP_balance();
	}

	@Override
	public long send_to_Address_in_B( long val , AT_Machine_State state ) {
		/*ByteBuffer b = ByteBuffer.allocate( state.get_B1().length * 4 );
		b.order( ByteOrder.LITTLE_ENDIAN );

		b.put( state.get_B1() );
		b.put( state.get_B2() );
		b.put( state.get_B3() );
		b.put( state.get_B4() );
		*/
		
		long atId = AT_API_Helper.getLong( state.getId() );
		AT at = AT.getAT( atId );
		if ( val > at.getG_balance() )
		{
		
			AT_Transaction tx = new AT_Transaction( state.getId() , state.get_B1().clone() , val );
			state.addTransaction( tx );
		
			at.setG_balance( at.getG_balance() - val );
		}
		else
		{
			AT_Transaction tx = new AT_Transaction( state.getId() , state.get_B1().clone() , at.getG_balance() );
			state.addTransaction( tx );
		
			at.setG_balance( 0L );
		}

		return 1;
	}

	@Override
	public long send_All_to_Address_in_B( AT_Machine_State state ) {
		/*ByteBuffer b = ByteBuffer.allocate( state.get_B1().length * 4 );
		b.order( ByteOrder.LITTLE_ENDIAN );

		b.put( state.get_B1() );
		b.put( state.get_B2() );
		b.put( state.get_B3() );
		b.put( state.get_B4() );
		 */
		/*byte[] bId = state.getId();
		byte[] temp = new byte[ 8 ];
		for ( int i = 0; i < 8; i++ )
		{
			temp[ i ] = bId[ i ];
		}*/

		long atId = AT_API_Helper.getLong( state.getId() );
		AT_Transaction tx = new AT_Transaction( state.getId() , state.get_B1().clone() , AT.getAT( atId ).getG_balance() );
		state.addTransaction( tx );
		AT.getAT( atId ).setG_balance( 0L );

		return 1;
	}

	@Override
	public long send_Old_to_Address_in_B( AT_Machine_State state ) {
		
		AT at = AT.getAT( state.getId() );
		
		if ( at.getP_balance() > at.getG_balance()  )
		{
			AT_Transaction tx = new AT_Transaction( state.getId() , state.get_B1() , state.getG_balance() );
			state.addTransaction( tx );
			
			at.setG_balance( 0L );
			at.setP_balance( 0L );
		
		}
		else
		{
			AT_Transaction tx = new AT_Transaction( state.getId() , state.get_B1() , state.getP_balance() );
			state.addTransaction( tx );
			
			at.setG_balance( at.getG_balance() - at.getP_balance() );
			at.setP_balance( 0l );
			
		}

		return 1;
	}

	@Override
	public long send_A_to_Address_in_B( AT_Machine_State state ) {
		
		AT at = AT.getAT( state.getId() );

		long amount = AT_API_Helper.getLong( state.get_A1() );

		if ( at.getG_balance() > amount )
		{
		
			AT_Transaction tx = new AT_Transaction( state.getId() , state.get_B1() , amount );
			state.addTransaction( tx );
			
			state.setG_balance( state.getG_balance() - amount );
			
		}
		else
		{
			AT_Transaction tx = new AT_Transaction( state.getId() , state.get_B1() , at.getG_balance() );
			state.addTransaction( tx );
			
			state.setG_balance( 0L );
		}
		return 1;
	}

	public long add_Minutes_to_Timestamp( long val1 , long val2 , AT_Machine_State state) {
		int height = AT_API_Helper.longToHeight( val1 );
		int numOfTx = AT_API_Helper.longToNumOfTx( val1 );
		int addHeight = height + (int) (val2 / AT_Constants.getInstance().AVERAGE_BLOCK_MINUTES( Nxt.getBlockchain().getHeight() ));

		return AT_API_Helper.getLongTimestamp( addHeight , numOfTx );
	}

	protected static Long findTransaction(int startHeight ,Long atID, int numOfTx){
		try (Connection con = Db.getConnection();
				PreparedStatement pstmt = con.prepareStatement("SELECT id FROM transaction WHERE height>= ? AND height <= ? and recipient_id = ?")){
			pstmt.setInt(1, startHeight);
			pstmt.setInt(2, Nxt.getBlockchain().getHeight());
			pstmt.setLong(3, atID);
			ResultSet rs = pstmt.executeQuery();
			Long transactionId = 0L;
			int counter = 0;
			while (rs.next()) {
				System.out.println("tx id "+rs.getLong("id"));
				if (counter == numOfTx){
					transactionId = rs.getLong("id");
					rs.close();
					return transactionId;
				}
				counter++;
			}
			rs.close();
			return transactionId;

		} catch (SQLException e) {
			throw new RuntimeException(e.toString(), e);
		}

	}

	protected static int findTransactionHeight(Long transactionId, int height, Long atID){
		try (Connection con = Db.getConnection();
				PreparedStatement pstmt = con.prepareStatement("SELECT id FROM transaction WHERE height= ? and recipient_id = ?")){
			pstmt.setInt( 1, height );
			pstmt.setLong( 2, atID );
			ResultSet rs = pstmt.executeQuery();

			int counter = 0;
			while ( rs.next() ) {
				if (rs.getLong( "id" ) == transactionId){
					counter++;
					break;
				}
				counter++;
			}
			rs.close();
			return counter;

		} catch ( SQLException e ) {
			throw new RuntimeException(e.toString(), e);
		}
	}


}
