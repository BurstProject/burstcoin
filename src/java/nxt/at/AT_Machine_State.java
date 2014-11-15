/*
 * Copyright (c) 2014 CIYAM Developers

 Distributed under the MIT/X11 software license, please refer to the file license.txt
 in the root project directory or http://www.opensource.org/licenses/mit-license.php.

 */

package nxt.at;

import java.util.LinkedList;
import java.util.TreeSet;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class AT_Machine_State
{

	public class Machine_State 
	{
		transient boolean running;
		transient boolean stopped;
		transient boolean finished;

		int pc;
		int pcs;

		transient int opc;

		int cs;
		int us;

		int steps;

		private byte[] A1 = new byte[ 8 ];
		private byte[] A2 = new byte[ 8 ];
		private byte[] A3 = new byte[ 8 ];
		private byte[] A4 = new byte[ 8 ];

		private byte[] B1 = new byte[ 8 ];
		private byte[] B2 = new byte[ 8 ];
		private byte[] B3 = new byte[ 8 ];
		private byte[] B4 = new byte[ 8 ];

		byte[] flags = new byte[ 4 ];

		TreeSet<Integer> jumps = new TreeSet<Integer>();

		Machine_State()
		{
			pcs=0;
			reset();
		}

		boolean isRunning()
		{
			return running;
		}

		void reset()
		{
			pc = pcs;
			opc = 0;
			cs = 0;
			us = 0;
			steps = 0;
			if ( !jumps.isEmpty() )
				jumps.clear();
			stopped = false;
			finished = false;
		}

		void run()
		{
			running = true;
		}
		
		public long getSteps()
		{
			return steps;
		}

		protected byte[] getMachineStateBytes()
		{

			int size = 4 + 1 + 4 + 4 + 4 + 4 + 4;
			ByteBuffer bytes = ByteBuffer.allocate(size);
			bytes.order( ByteOrder.LITTLE_ENDIAN );
			bytes.put( flags );
			bytes.put( ( byte ) ( machineState.running == true ? 1 : 0 ) );
			bytes.putInt( machineState.pc );
			bytes.putInt( machineState.pcs );
			bytes.putInt( machineState.cs );
			bytes.putInt( machineState.us );
			bytes.putInt( machineState.steps );
			/*Iterator< Integer > iter = machineState.jumps.iterator();
			while ( iter.hasNext() )
			{
				bytes.putInt( iter.next() );
			}
			 */
			return bytes.array();
		}
	}

	private short version;

	private long g_balance;
	private long p_balance;

	private Machine_State machineState;

	private int csize;
	private int dsize;
	private int c_user_stack_bytes;
	private int c_call_stack_bytes;

	private byte[] atID = new byte[ AT_Constants.AT_ID_SIZE ];
	private byte[] creator = new byte[ AT_Constants.AT_ID_SIZE ];

	private long minimumFee;

	private int creationBlockHeight;

	private int waitForNumberOfBlocks;

	private boolean freezeWhenSameBalance;


	private transient ByteBuffer ap_data;

	private transient ByteBuffer ap_code;

	private LinkedList<AT_Transaction> transactions;

	public AT_Machine_State( byte[] atId , int cSize , byte[] machineCode , int dSize , byte[] machineData , int stackSize , long minimumFee , int creationBlockHeight , int waitForNumberOfBlocks , int sleepBetweenBlocks )
	{
		/*this.atID = atId;
		this.csize = cSize;
		this.dsize = dSize;
		this.c_call_stack_bytes = stackSize;
		this.c_user_stack_bytes = stackSize;

		this.ap_code = ByteBuffer.allocate(csize);
		this.ap_code.order(ByteOrder.LITTLE_ENDIAN);
		this.ap_code.put(machineCode);
		this.ap_data = ByteBuffer.allocate(dsize+c_call_stack_bytes+c_user_stack_bytes);
		this.ap_data.order(ByteOrder.LITTLE_ENDIAN);
		this.ap_data.put(machineData);
		this.ap_code.clear();
		this.ap_data.clear();

		this.g_balance = 0L;
		this.p_balance = 0L;

		this.machineState = new Machine_State();
		this.minimumFee = minimumFee;

		this.creationBlockHeight = creationBlockHeight;
		this.waitForNumberOfBlocks = waitForNumberOfBlocks;
		this.sleepBetweenBlocks = sleepBetweenBlocks;
		transactions = new LinkedList<>();*/
	}

	public AT_Machine_State( byte[] atId , byte[] creator , byte[] creationBytes , int height ) 
	{
		this.version = AT_Constants.getInstance().AT_VERSION( height );
		this.atID = atId;
		this.creator = creator;

		ByteBuffer b = ByteBuffer.allocate( creationBytes.length );
		b.order( ByteOrder.LITTLE_ENDIAN );
		
		b.put( creationBytes );
		b.clear();
		
		this.version = b.getShort();

		b.getShort(); //future: reserved for future needs

		int pageSize = ( int ) AT_Constants.getInstance().PAGE_SIZE( height );
		short codePages = b.getShort();
		short dataPages = b.getShort();
		short callStackPages = b.getShort();
		short userStackPages = b.getShort();

		this.csize = codePages * pageSize;
		this.dsize = dataPages * pageSize;
		this.c_call_stack_bytes = callStackPages * pageSize;
		this.c_user_stack_bytes = userStackPages * pageSize;

		int codeLen = 0;
		if ( codePages * pageSize < pageSize + 1 )
		{
			codeLen = b.get();
		}
		else if ( codePages * pageSize < Short.MAX_VALUE + 1 )
		{
			codeLen = b.getShort();
		}
		else if ( codePages * pageSize < Integer.MAX_VALUE + 1 )
		{
			codeLen = b.getInt();
		}
		byte[] code = new byte[ codeLen ];
		b.get( code , 0 , codeLen );

		this.ap_code = ByteBuffer.allocate( csize );
		this.ap_code.order( ByteOrder.LITTLE_ENDIAN );
		this.ap_code.put( code );

		int dataLen = 0;
		if ( dataPages * pageSize < 257 )
		{
			dataLen = b.get();
		}
		else if ( dataPages * pageSize < Short.MAX_VALUE + 1 )
		{
			dataLen = b.getShort();
		}
		else if ( dataPages * pageSize < Integer.MAX_VALUE + 1 )
		{
			dataLen = b.getInt();
		}
		byte[] data = new byte[ dataLen ];
		b.get( data , 0 , dataLen );
		
		this.ap_data = ByteBuffer.allocate( this.dsize + this.c_call_stack_bytes + this.c_user_stack_bytes );
		this.ap_data.order( ByteOrder.LITTLE_ENDIAN );
		this.ap_data.put( data );

		this.creationBlockHeight = height;
		this.minimumFee = ( codePages + 
				dataPages + 
				callStackPages +
				userStackPages ) * AT_Constants.getInstance().COST_PER_PAGE( height );
		this.waitForNumberOfBlocks = 0;
		this.freezeWhenSameBalance = false;
		this.transactions = new LinkedList<>();
		this.g_balance = 0;
		this.p_balance = 0;
		this.machineState = new Machine_State();
	}

	protected byte[] get_A1()
	{
		return machineState.A1;
	}

	protected byte[] get_A2()
	{
		return machineState.A2;
	}

	protected byte[] get_A3()
	{
		return machineState.A3;
	}

	protected byte[] get_A4()
	{
		return machineState.A4;
	}

	protected byte[] get_B1()
	{
		return machineState.B1;
	}

	protected byte[] get_B2()
	{
		return machineState.B2;
	}

	protected byte[] get_B3()
	{
		return machineState.B3;
	}

	protected byte[] get_B4()
	{
		return machineState.B4;
	}

	protected void set_A1( byte[] A1 )
	{
		this.machineState.A1 = A1.clone();
	}

	protected void set_A2( byte[] A2 ){
		this.machineState.A2 = A2.clone();
	}

	protected void set_A3( byte[] A3 )
	{
		this.machineState.A3 = A3.clone();
	}

	protected void set_A4( byte[] A4 )
	{
		this.machineState.A4 = A4.clone();
	}

	protected void set_B1( byte[] B1 )
	{
		this.machineState.B1 = B1.clone();
	}

	protected void set_B2( byte[] B2 )
	{
		this.machineState.B2 = B2.clone();
	}

	protected void set_B3( byte[] B3 )
	{
		this.machineState.B3 = B3.clone();
	}

	protected void set_B4( byte[] B4 )
	{
		this.machineState.B4 = B4.clone();
	}

	protected void addTransaction(AT_Transaction tx)
	{
		transactions.add(tx);
	}

	protected void clearTransactions()
	{
		transactions.clear();
	}

	public LinkedList<AT_Transaction> getTransactions()
	{
		return transactions;
	}

	protected ByteBuffer getAp_code() 
	{
		return ap_code;
	}

	public ByteBuffer getAp_data() 
	{
		return ap_data;
	}

	protected int getC_call_stack_bytes() 
	{
		return c_call_stack_bytes;
	}

	protected int getC_user_stack_bytes() 
	{
		return c_user_stack_bytes;
	}

	protected int getCsize() 
	{
		return csize;
	}

	protected int getDsize() 
	{
		return dsize;
	}

	public Long getG_balance() 
	{
		return g_balance;
	}

	public Long getP_balance() 
	{
		return p_balance;
	}

	public byte[] getId()
	{
		return atID;
	}

	public Machine_State getMachineState() 
	{
		return machineState;
	}

	public long getMinimumFee()
	{
		return minimumFee;
	}

	protected void setC_call_stack_bytes(int c_call_stack_bytes) 
	{
		this.c_call_stack_bytes = c_call_stack_bytes;
	}

	protected void setC_user_stack_bytes(int c_user_stack_bytes) 
	{
		this.c_user_stack_bytes = c_user_stack_bytes;
	}

	protected void setCsize(int csize) 
	{
		this.csize = csize;
	}

	protected void setDsize(int dsize) 
	{
		this.dsize = dsize;
	}

	public void setG_balance(Long g_balance) 
	{
		this.g_balance = g_balance;
	}

	public void setP_balance(Long p_balance) 
	{
		this.p_balance = p_balance;
	}

	public void setMachineState(Machine_State machineState) 
	{
		this.machineState = machineState;
	}

	public void setWaitForNumberOfBlocks(int waitForNumberOfBlocks) 
	{
		this.waitForNumberOfBlocks = waitForNumberOfBlocks;
	}

	public int getWaitForNumberOfBlocks()
	{
		return this.waitForNumberOfBlocks;
	}

	public byte[] getCreator() 
	{
		return this.creator;
	}

	public int getCreationBlockHeight() 
	{
		return this.creationBlockHeight;
	}

	public boolean freezeOnSameBalance()
	{
		return this.freezeWhenSameBalance;
	}
	
	public short getVersion()
	{
		return version;
	}

	public byte[] getTransactionBytes( )
	{
		ByteBuffer b = ByteBuffer.allocate( (creator.length + 8 ) * transactions.size() );
		b.order( ByteOrder.LITTLE_ENDIAN );
		for (AT_Transaction tx : transactions )
		{
			b.put( tx.getRecipientId() );
			b.putLong( tx.getAmount() );
		}
		return b.array();

	}

	public byte[] getBytes()
	{
		byte[] txBytes = getTransactionBytes();
		byte[] stateBytes = machineState.getMachineStateBytes();
		byte[] dataBytes = ap_data.array();

		ByteBuffer b = ByteBuffer.allocate( atID.length + txBytes.length + stateBytes.length + dataBytes.length );
		b.order( ByteOrder.LITTLE_ENDIAN );

		b.put( atID );
		b.put( stateBytes );
		b.put( dataBytes );
		b.put( txBytes );

		return b.array();

	}

}
