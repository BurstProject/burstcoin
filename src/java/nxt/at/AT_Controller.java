package nxt.at;


import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListMap;

import nxt.AT;
import nxt.Account;
import nxt.Nxt;
import nxt.util.Convert;
import nxt.util.Logger;

public abstract class AT_Controller {


	public static int runSteps( AT_Machine_State state )
	{
		state.getMachineState().finished = false;
		state.getMachineState().steps = 0;

		AT_Machine_Processor processor = new AT_Machine_Processor( state );

		int height = Nxt.getBlockchain().getHeight();

		while ( state.getMachineState().steps < AT_Constants.getInstance().MAX_STEPS( height ))
		{
			long stepFee = AT_Constants.getInstance().STEP_FEE( height );

			if ( ( state.getG_balance() < stepFee ) )
			{
				System.out.println( "stopped - not enough balance" );
				return 3;
			}

			state.setG_balance( state.getG_balance() - stepFee );
			state.getMachineState().steps++;
			int rc = processor.processOp( false , false );

			if ( rc >= 0 )
			{
				if ( state.getMachineState().stopped )
				{
					//TODO add freeze
					System.out.println( "stopped" );
					return 2;
				}
				else if ( state.getMachineState().finished )
				{
					System.out.println( "finished" );
					return 1;
				}
			}
			else
			{
				if ( rc == -1 )
					System.out.println( "error: overflow" );
				else if ( rc==-2 )
					System.out.println( "error: invalid code" );
				else
					System.out.println( "unexpected error" );
				return 0;
			}
		}
		return 5;
	}

	public static void resetMachine( AT_Machine_State state ) {
		state.getMachineState( ).reset( );
		listCode( state , true , true );
	}

	public static void listCode( AT_Machine_State state , boolean disassembly , boolean determine_jumps ) {

		AT_Machine_Processor machineProcessor = new AT_Machine_Processor( state );

		int opc = state.getMachineState().pc;
		int osteps = state.getMachineState().steps;

		state.getAp_code().order( ByteOrder.LITTLE_ENDIAN );
		state.getAp_data().order( ByteOrder.LITTLE_ENDIAN );

		state.getMachineState( ).pc = 0;
		state.getMachineState( ).opc = opc;

		while ( true )
		{
			int rc= machineProcessor.processOp( disassembly , determine_jumps );
			if ( rc<=0 ) break;

			state.getMachineState().pc += rc;
		}

		state.getMachineState().steps = osteps;
		state.getMachineState().pc = opc;
	}

	public static int checkCreationBytes( byte[] creation , int height ) throws AT_Exception{
		int totalPages = 0;
		try 
		{
			ByteBuffer b = ByteBuffer.allocate( creation.length );
			b.order( ByteOrder.LITTLE_ENDIAN );
			
			b.put(  creation );
			b.clear();
			
			AT_Constants instance = AT_Constants.getInstance();

			short version = b.getShort();
			if ( version > instance.AT_VERSION( height ) )
			{
				throw new AT_Exception( AT_Error.INCORRECT_VERSION.getDescription() );
			}

			short reserved = b.getShort(); //future: reserved for future needs

			short codePages = b.getShort();
			if ( codePages > instance.MAX_MACHINE_CODE_PAGES( height ) )
			{
				throw new AT_Exception( AT_Error.INCORRECT_CODE_PAGES.getDescription() );
			}

			short dataPages = b.getShort();
			if ( dataPages > instance.MAX_MACHINE_DATA_PAGES( height ) )
			{
				throw new AT_Exception( AT_Error.INCORRECT_DATA_PAGES.getDescription() );
			}

			short callStackPages = b.getShort();
			if ( callStackPages > instance.MAX_MACHINE_CALL_STACK_PAGES( height ) )
			{
				throw new AT_Exception( AT_Error.INCORRECT_CALL_PAGES.getDescription() );
			}

			short userStackPages = b.getShort();
			if ( userStackPages > instance.MAX_MACHINE_USER_STACK_PAGES( height ) )
			{
				throw new AT_Exception( AT_Error.INCORRECT_USER_PAGES.getDescription() );
			}
			System.out.println("codePages: " + codePages );
			int codeLen;
			if ( codePages * 256 < 257 )
			{
				codeLen = b.get();
			}
			else if ( codePages * 256 < Short.MAX_VALUE + 1 )
			{
				codeLen = b.getShort();
			}
			else if ( codePages * 256 < Integer.MAX_VALUE + 1 )
			{
				codeLen = b.getInt();
			}
			else
			{
				throw new AT_Exception( AT_Error.INCORRECT_CODE_LENGTH.getDescription() );
			}
			if ( codeLen < 1 )
			{
				throw new AT_Exception( AT_Error.INCORRECT_CODE_LENGTH.getDescription() );
			}
			byte[] code = new byte[ codeLen ];
			b.get( code , 0 , codeLen );


			int dataLen;
			if ( dataPages * 256 < 257 )
			{
				dataLen = b.get();
			}
			else if ( dataPages * 256 < Short.MAX_VALUE + 1 )
			{
				dataLen = b.getShort();
			}
			else if ( dataPages * 256 < Integer.MAX_VALUE + 1 )
			{
				dataLen = b.getInt();
			}
			else
			{
				throw new AT_Exception( AT_Error.INCORRECT_CODE_LENGTH.getDescription() );
			}
			byte[] data = new byte[ dataLen ];
			b.get( data , 0 , dataLen );
			
			totalPages = codePages + dataPages + userStackPages + callStackPages;
			/*if ( ( codePages + dataPages + userStackPages + callStackPages ) * instance.COST_PER_PAGE( height ) < txFeeAmount )
			{
				return AT_Error.INCORRECT_CREATION_FEE.getCode();
			}*/
			
			if ( b.position() != b.capacity() )
			{
				throw new AT_Exception( AT_Error.INCORRECT_CREATION_TX.getDescription() );
			}

			//TODO note: run code in demo mode for checking if is valid

		} catch ( BufferUnderflowException e ) 
		{
			throw new AT_Exception( AT_Error.INCORRECT_CREATION_TX.getDescription() );
		}
		return totalPages;
	}

	public static AT_Block getCurrentBlockATs( int freePayload , int blockHeight ){

		ConcurrentSkipListMap< Integer , List< Long > > orderedATs = AT.getOrderedATs();
		NavigableSet< Integer > heightSet = orderedATs.keySet();
		Iterator< Integer > keys = heightSet.iterator();

		List< AT > processedATs = new ArrayList< >();

		int costOfOneAT = getCostOfOneAT();
		int payload = 0;
		long totalSteps = 0;
		while ( payload < freePayload - costOfOneAT && keys.hasNext() )
		{
			int height = keys.next();
			List< Long > ats = orderedATs.get( height );

			Iterator< Long > ids = ats.iterator();
			while ( payload < freePayload - costOfOneAT && ids.hasNext() )
			{
				Long id = ids.next();
				AT at = AT.getAT( id );

				long atAccountBalance = getATAccountBalance( id );
				long atStateBalance = at.getG_balance();

				if ( at.freezeOnSameBalance() && atAccountBalance == atStateBalance )
				{
					continue;
				}

				at.setG_balance( atAccountBalance );

				if ( blockHeight - height >= at.getWaitForNumberOfBlocks() &&
						atAccountBalance >= AT_Constants.getInstance().STEP_FEE( height ) )
				{
					at.clearTransactions();
					at.setWaitForNumberOfBlocks( 0 );
					runSteps ( at );

					totalSteps += at.getMachineState().steps;

					payload += costOfOneAT;
					
					at.setP_balance( at.getG_balance() );
					processedATs.add( at );
				}
			}
		}

		long totalAmount = 0;
		for ( AT at : processedATs )
		{
			totalAmount = makeTransactions( at );
		}

		byte[] bytesForBlock = null;

		try
		{
			bytesForBlock = getBlockATBytes( processedATs , payload );
		}
		catch ( NoSuchAlgorithmException e )
		{
			//should not reach ever here
			e.printStackTrace();
		}

		AT_Block atBlock = new AT_Block( totalSteps * AT_Constants.getInstance().STEP_FEE( blockHeight ) , totalAmount , bytesForBlock );

		return atBlock;
	}

	public static AT_Block validateATs( byte[] blockATs , int blockHeight ) throws NoSuchAlgorithmException, AT_Exception {

		LinkedHashMap< byte[] , byte[] > ats = getATsFromBlock( blockATs );
		
		List< AT > processedATs = new ArrayList< >();
		
		boolean validated = true;
		long totalSteps = 0;
		MessageDigest digest = MessageDigest.getInstance( "MD5" );
		byte[] md5 = new byte[ getCostOfOneAT() ];
		for ( byte[] atId : ats.keySet() )
		{
			AT at = AT.getAT( atId );
			at.clearTransactions();
			at.setWaitForNumberOfBlocks( 0 );
			
			long atAccountBalance = getATAccountBalance( AT_API_Helper.getLong( atId ) );
			
			at.setG_balance( atAccountBalance );
			
			
			runSteps( at );
			
			totalSteps += at.getMachineState().steps;
			
			at.setP_balance( at.getG_balance() );
			processedATs.add( at );
			
			md5 = digest.digest( at.getBytes() );
			if ( !Arrays.equals( md5 , ats.get( atId ) ) )
			{
				throw new AT_Exception( "Calculated md5 and recieved md5 are not matching" );
			}
		}
		
		long totalAmount = 0;
		for ( AT at : processedATs )
		{
			totalAmount = makeTransactions( at );
		}
		AT_Block atBlock = new AT_Block( totalSteps * AT_Constants.getInstance().STEP_FEE( blockHeight ) , totalAmount , new byte[ 1 ] , validated );

		return atBlock;
	}

	public static LinkedHashMap< byte[] , byte[] > getATsFromBlock( byte[] blockATs ) throws AT_Exception
	{
		if ( blockATs.length > 0 )
		{
			if ( blockATs.length % (getCostOfOneAT() ) != 0 )
			{
				throw new AT_Exception("blockATs must be a multiple of cost of one AT ( " + getCostOfOneAT() +" )" );
			}
		}
		
		ByteBuffer b = ByteBuffer.wrap( blockATs );
		b.order( ByteOrder.LITTLE_ENDIAN );

		byte[] temp = new byte[ AT_Constants.AT_ID_SIZE ];
		byte[] md5 = new byte[ 16 ];

		LinkedHashMap< byte[] , byte[] > ats = new LinkedHashMap<>();

		while ( b.position() < b.capacity() )
		{
			b.get( temp , 0 , temp.length );
			b.get( md5 , 0 , md5.length );
			ats.put( temp , md5 ); 
		}
		
		if ( b.position() != b.capacity() )
		{
			throw new AT_Exception("bytebuffer not matching");
		}
		
		return ats;
	}

	private static byte[] getBlockATBytes(List<AT> processedATs , int payload ) throws NoSuchAlgorithmException {

		ByteBuffer b = ByteBuffer.allocate( payload );
		b.order( ByteOrder.LITTLE_ENDIAN );

		MessageDigest digest = MessageDigest.getInstance( "MD5" );
		for ( AT at : processedATs )
		{
			b.put( at.getId() );
			digest.update( at.getBytes() );
			b.put( digest.digest() );
		}

		return b.array();
	}

	private static int getCostOfOneAT() {
		return AT_Constants.AT_ID_SIZE + 16;
	}
	
	//platform based implementations
	//platform based 
	private static long makeTransactions( AT at ) {
		long totalAmount = 0;
		for (AT_Transaction tx : at.getTransactions() )
		{
			totalAmount += tx.getAmount();
			Logger.logInfoMessage("Transaction to " + Convert.toUnsignedLong(AT_API_Helper.getLong(tx.getRecipientId())) + " amount " + tx.getAmount() );
			
		}
		return totalAmount;
	}
	
	/*private static void saveTransaction(Connection con, TransactionImpl transaction)
	{
		 try {
	                try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO transaction (id, deadline, sender_public_key, "
	                        + "recipient_id, amount, fee, referenced_transaction_full_hash, height, "
	                        + "block_id, signature, timestamp, type, subtype, sender_id, attachment_bytes, "
	                        + "block_timestamp, full_hash, version, has_message, has_encrypted_message, has_public_key_announcement, "
	                        + "has_encrypttoself_message, ec_block_height, ec_block_id) "
	                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
	                    int i = 0;
	                    pstmt.setLong(++i, transaction.getId());
	                    pstmt.setShort(++i, transaction.getDeadline());
	                    pstmt.setBytes(++i, transaction.getSenderPublicKey());
	                    if (transaction.getType().hasRecipient() && transaction.getRecipientId() != null) {
	                        pstmt.setLong(++i, transaction.getRecipientId());
	                    } else {
	                        pstmt.setNull(++i, Types.BIGINT);
	                    }
	                    pstmt.setLong(++i, transaction.getAmountNQT());
	                    pstmt.setLong(++i, transaction.getFeeNQT());
	                    if (transaction.getReferencedTransactionFullHash() != null) {
	                        pstmt.setBytes(++i, Convert.parseHexString(transaction.getReferencedTransactionFullHash()));
	                    } else {
	                        pstmt.setNull(++i, Types.BINARY);
	                    }
	                    pstmt.setInt(++i, transaction.getHeight());
	                    pstmt.setLong(++i, transaction.getBlockId());
	                    pstmt.setBytes(++i, transaction.getSignature());
	                    pstmt.setInt(++i, transaction.getTimestamp());
	                    pstmt.setByte(++i, transaction.getType().getType());
	                    pstmt.setByte(++i, transaction.getType().getSubtype());
	                    pstmt.setLong(++i, transaction.getSenderId());
	                    int bytesLength = 0;
	                    for (Appendix appendage : transaction.getAppendages()) {
	                        bytesLength += appendage.getSize();
	                    }
	                    if (bytesLength == 0) {
	                        pstmt.setNull(++i, Types.VARBINARY);
	                    } else {
	                        ByteBuffer buffer = ByteBuffer.allocate(bytesLength);
	                        buffer.order(ByteOrder.LITTLE_ENDIAN);
	                        for (Appendix appendage : transaction.getAppendages()) {
	                            appendage.putBytes(buffer);
	                        }
	                        pstmt.setBytes(++i, buffer.array());
	                    }
	                    pstmt.setInt(++i, transaction.getBlockTimestamp());
	                    pstmt.setBytes(++i, Convert.parseHexString(transaction.getFullHash()));
	                    pstmt.setByte(++i, transaction.getVersion());
	                    pstmt.setBoolean(++i, transaction.getMessage() != null);
	                    pstmt.setBoolean(++i, transaction.getEncryptedMessage() != null);
	                    pstmt.setBoolean( ++i, false );
	                    pstmt.setBoolean(++i, transaction.getEncryptToSelfMessage() != null);
	                    pstmt.setInt(++i, transaction.getECBlockHeight());
	                    if (transaction.getECBlockId() != null) {
	                        pstmt.setLong(++i, transaction.getECBlockId());
	                    } else {
	                        pstmt.setNull(++i, Types.BIGINT);
	                    }
	                    pstmt.executeUpdate();
	                }
	        } catch (SQLException e) {
	            throw new RuntimeException(e.toString(), e);
	        }
	}*/
	
	

	//platform based
	

	//platform based
	private static long getATAccountBalance( Long id ) {
		//Long accountId = AT_API_Helper.getLong( id );
		Account atAccount = Account.getAccount( id );

		if ( atAccount != null )
		{
			return atAccount.getBalanceNQT();
		}

		return 0;

	}



}
