package nxt.at;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import nxt.Appendix;
import nxt.Constants;
import nxt.db.Db;
import nxt.Nxt;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.LoggerConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//NXT API IMPLEMENTATION

public class AT_API_Platform_Impl extends AT_API_Impl {

	private static final Logger logger = LoggerFactory.getLogger(AT_API_Platform_Impl.class);

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

		int height = state.getHeight();
		return AT_API_Helper.getLongTimestamp( height , 0 );

	}

	public long get_Creation_Timestamp( AT_Machine_State state ) 
	{
		return AT_API_Helper.getLongTimestamp( state.getCreationBlockHeight() , 0 );
	}

	@Override
	public long get_Last_Block_Timestamp( AT_Machine_State state ) 
	{

		int height = state.getHeight() - 1;
		return AT_API_Helper.getLongTimestamp( height , 0 );
	}

	@Override
	public void put_Last_Block_Hash_In_A( AT_Machine_State state ) {
		ByteBuffer b = ByteBuffer.allocate( state.get_A1().length * 4 );
		b.order( ByteOrder.LITTLE_ENDIAN );

		b.put( Nxt.getBlockchain().getBlockAtHeight(state.getHeight() - 1).getBlockHash() );
		
		b.clear();

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

		byte[] b = state.getId();

		long tx = findTransaction( height , state.getHeight() , AT_API_Helper.getLong( b ) , numOfTx , state.minActivationAmount() );
		logger.debug("tx with id "+tx+" found");
		clear_A( state );
		state.set_A1( AT_API_Helper.getByteArray( tx ) );

	}

	@Override
	public long get_Type_for_Tx_in_A( AT_Machine_State state ) {
		long txid = AT_API_Helper.getLong( state.get_A1() );

		Transaction tx = Nxt.getBlockchain().getTransaction( txid );
		
		if ( tx != null && tx.getHeight() >= state.getHeight() )
		{
			tx = null;
		}

		if ( tx != null )
		{
			if (tx.getMessage() != null )
			{
				return 1;
			}
			else
			{
				return 0;
			}
		}
		return -1;
	}

	@Override
	public long get_Amount_for_Tx_in_A( AT_Machine_State state ) {
		long txId = AT_API_Helper.getLong( state.get_A1() );

		Transaction tx = Nxt.getBlockchain().getTransaction( txId );
		
		if ( tx != null && tx.getHeight() >= state.getHeight() )
		{
			tx = null;
		}
		
		long amount = -1;
		if ( tx != null )
		{
			if( (tx.getMessage() == null || state.getHeight() >= Constants.AT_FIX_BLOCK_2) && state.minActivationAmount() <= tx.getAmountNQT() )
			{
				amount = tx.getAmountNQT() - state.minActivationAmount();
			}
			else
			{
				amount = 0;
			}
		}
		return amount;
	}

	@Override
	public long get_Timestamp_for_Tx_in_A( AT_Machine_State state ) {
		long txId = AT_API_Helper.getLong( state.get_A1() );
		logger.debug("get timestamp for tx with id " + txId + " found");
		Transaction tx = Nxt.getBlockchain().getTransaction( txId );
		
		if ( tx != null && tx.getHeight() >= state.getHeight() )
		{
			tx = null;
		}

		if ( tx != null )
		{
			int blockHeight = tx.getHeight();

			byte[] b = state.getId();

			int txHeight = findTransactionHeight( txId , blockHeight , AT_API_Helper.getLong( b ) , state.minActivationAmount() );

			return AT_API_Helper.getLongTimestamp( blockHeight , txHeight );
		}
		return -1;
	}

	@Override
	public long get_Random_Id_for_Tx_in_A( AT_Machine_State state ) {
		long txId = AT_API_Helper.getLong( state.get_A1() );

		Transaction tx = Nxt.getBlockchain().getTransaction( txId );
		
		if ( tx != null && tx.getHeight() >= state.getHeight() )
		{
			tx = null;
		}

		if ( tx !=null )
		{
			int txBlockHeight = tx.getHeight();


			int blockHeight = state.getHeight();

			if ( blockHeight - txBlockHeight < AT_Constants.getInstance().BLOCKS_FOR_RANDOM( blockHeight ) ){ //for tests - for real case 1440
				state.setWaitForNumberOfBlocks( (int)AT_Constants.getInstance().BLOCKS_FOR_RANDOM( blockHeight ) - ( blockHeight - txBlockHeight ) );
				state.getMachineState().pc -= 7;
				state.getMachineState().stopped = true;
				return 0;
			}

			MessageDigest digest = Crypto.sha256();

			byte[] senderPublicKey = tx.getSenderPublicKey();

			ByteBuffer bf = ByteBuffer.allocate( 32 + Long.SIZE + senderPublicKey.length );
			bf.order( ByteOrder.LITTLE_ENDIAN );
			bf.put(Nxt.getBlockchain().getBlockAtHeight(blockHeight - 1).getGenerationSignature());
			bf.putLong( tx.getId() );
			bf.put( senderPublicKey);

			digest.update(bf.array());
			byte[] byteRandom = digest.digest();

			long random = Math.abs( AT_API_Helper.getLong( Arrays.copyOfRange(byteRandom, 0, 8) ) );

			//System.out.println( "info: random for txid: " + Convert.toUnsignedLong( tx.getId() ) + "is: " + random );
			return random;
		}
		return -1;
	}

	@Override
	public void message_from_Tx_in_A_to_B( AT_Machine_State state ) {
		long txid = AT_API_Helper.getLong( state.get_A1() );

		Transaction tx = Nxt.getBlockchain().getTransaction( txid );
		if ( tx != null && tx.getHeight() >= state.getHeight() )
		{
			tx = null;
		}
		ByteBuffer b = ByteBuffer.allocate( state.get_A1().length * 4 );
		b.order( ByteOrder.LITTLE_ENDIAN );
		if( tx != null )
		{
			Appendix.Message txMessage = tx.getMessage();
			if(txMessage != null)
			{
				byte[] message = txMessage.getMessage();
				if ( message.length <= state.get_A1().length * 4 )
				{
					b.put( message );
				}
			}
		}

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

	}
	@Override
	public void B_to_Address_of_Tx_in_A( AT_Machine_State state ) {

		long txId = AT_API_Helper.getLong( state.get_A1() );
		
		clear_B( state );
		
		Transaction tx = Nxt.getBlockchain().getTransaction( txId );
		if ( tx != null && tx.getHeight() >= state.getHeight() )
		{
			tx = null;
		}
		if( tx != null )
		{
			long address = tx.getSenderId();
			state.set_B1( AT_API_Helper.getByteArray( address ) );
		}
	}

	@Override
	public void B_to_Address_of_Creator( AT_Machine_State state ) {
		long creator = AT_API_Helper.getLong( state.getCreator() );

		clear_B( state );

		state.set_B1( AT_API_Helper.getByteArray( creator ) );

	}
	
	@Override
	public void put_Last_Block_Generation_Signature_In_A( AT_Machine_State state ) {
		ByteBuffer b = ByteBuffer.allocate( state.get_A1().length * 4 );
		b.order( ByteOrder.LITTLE_ENDIAN );

		b.put( Nxt.getBlockchain().getBlockAtHeight(state.getHeight() - 1).getGenerationSignature() );

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
	public long get_Current_Balance( AT_Machine_State state ) {
		if(state.getHeight() < Constants.AT_FIX_BLOCK_2 )
			return 0;
		
		//long balance = Account.getAccount( AT_API_Helper.getLong(state.getId()) ).getBalanceNQT();
		return state.getG_balance();
	}

	@Override
	public long get_Previous_Balance( AT_Machine_State state ) {
		if(state.getHeight() < Constants.AT_FIX_BLOCK_2 )
			return 0;
		
		return state.getP_balance();
	}

	@Override
	public void send_to_Address_in_B( long val , AT_Machine_State state ) {
		/*ByteBuffer b = ByteBuffer.allocate( state.get_B1().length * 4 );
		b.order( ByteOrder.LITTLE_ENDIAN );

		b.put( state.get_B1() );
		b.put( state.get_B2() );
		b.put( state.get_B3() );
		b.put( state.get_B4() );
		*/
		
		if ( val < 1 ) return;
		
		if ( val < state.getG_balance() )
		{
		
			AT_Transaction tx = new AT_Transaction( state.getId() , state.get_B1().clone() , val , null );
			state.addTransaction( tx );
		
			state.setG_balance( state.getG_balance() - val );
		}
		else
		{
			AT_Transaction tx = new AT_Transaction( state.getId() , state.get_B1().clone() , state.getG_balance() , null );
			state.addTransaction( tx );
		
			state.setG_balance( 0L );
		}

	}

	@Override
	public void send_All_to_Address_in_B( AT_Machine_State state ) {
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

		AT_Transaction tx = new AT_Transaction( state.getId() , state.get_B1().clone() , state.getG_balance() , null );
		state.addTransaction( tx );
		state.setG_balance( 0L );

	}

	@Override
	public void send_Old_to_Address_in_B( AT_Machine_State state ) {
		
		if ( state.getP_balance() > state.getG_balance()  )
		{
			AT_Transaction tx = new AT_Transaction( state.getId() , state.get_B1() , state.getG_balance() , null );
			state.addTransaction( tx );
			
			state.setG_balance( 0L );
			state.setP_balance( 0L );
		
		}
		else
		{
			AT_Transaction tx = new AT_Transaction( state.getId() , state.get_B1() , state.getP_balance() , null );
			state.addTransaction( tx );
			
			state.setG_balance( state.getG_balance() - state.getP_balance() );
			state.setP_balance( 0l );
			
		}

	}

	@Override
	public void send_A_to_Address_in_B( AT_Machine_State state ) {
		
		ByteBuffer b = ByteBuffer.allocate(32);
		b.order( ByteOrder.LITTLE_ENDIAN );
		b.put(state.get_A1());
		b.put(state.get_A2());
		b.put(state.get_A3());
		b.put(state.get_A4());
		b.clear();

		AT_Transaction tx = new AT_Transaction( state.getId(), state.get_B1(), 0L, b.array() );
		state.addTransaction(tx);
		
	}

	public long add_Minutes_to_Timestamp( long val1 , long val2 , AT_Machine_State state) {
		int height = AT_API_Helper.longToHeight( val1 );
		int numOfTx = AT_API_Helper.longToNumOfTx( val1 );
		int addHeight = height + (int) (val2 / AT_Constants.getInstance().AVERAGE_BLOCK_MINUTES( state.getHeight() ));

		return AT_API_Helper.getLongTimestamp( addHeight , numOfTx );
	}

	protected static Long findTransaction(int startHeight , int endHeight , Long atID, int numOfTx, long minAmount){
		try (Connection con = Db.getConnection();
				PreparedStatement pstmt = con.prepareStatement("SELECT id FROM transaction "
						+ "WHERE height>= ? AND height < ? and recipient_id = ? AND amount >= ? "
						+ "ORDER BY height, id "
						+ "LIMIT 1 OFFSET ?")){
			pstmt.setInt(1, startHeight);
			pstmt.setInt(2, endHeight);
			pstmt.setLong(3, atID);
			pstmt.setLong(4, minAmount);
			pstmt.setInt(5, numOfTx);
			ResultSet rs = pstmt.executeQuery();
			Long transactionId = 0L;
			if(rs.next()) {
				transactionId = rs.getLong("id");
			}
			rs.close();
			return transactionId;

		} catch (SQLException e) {
			throw new RuntimeException(e.toString(), e);
		}

	}

	protected static int findTransactionHeight(Long transactionId, int height, Long atID, long minAmount){
		try (Connection con = Db.getConnection();
				PreparedStatement pstmt = con.prepareStatement("SELECT id FROM transaction "
						+ "WHERE height= ? and recipient_id = ? AND amount >= ? "
						+ "ORDER BY height, id")){
			pstmt.setInt( 1, height );
			pstmt.setLong( 2, atID );
			pstmt.setLong(3, minAmount);
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
