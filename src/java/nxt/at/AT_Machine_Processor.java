/*
 * Copyright (c) 2014 CIYAM Developers

 Distributed under the MIT/X11 software license, please refer to the file license.txt
 in the root project directory or http://www.opensource.org/licenses/mit-license.php.

 */

package nxt.at;

public class AT_Machine_Processor{
	
	protected AT_Machine_State machineData;
	private Fun fun = new Fun();
	
	

	private int getFun(){

		if (machineData.getMachineState().pc + 2>=machineData.getCsize())
			return -1;
		else
		{
			fun.fun = (machineData.getAp_code()).getShort(machineData.getMachineState().pc+1);
		}

		return 0;
	}

	private int getAddr(boolean is_code)
	{
		if (machineData.getMachineState().pc+4>=machineData.getCsize())
			return -1;
		else
		{
			fun.addr1 = (machineData.getAp_code()).getInt((machineData.getAp_code()).position()+machineData.getMachineState().pc+1);
			if (!validAddr(fun.addr1, is_code))
				return -1;
			else
				return 0;
		}
	}

	private int getAddrs()
	{
		if (machineData.getMachineState().pc + 4 + 4>=machineData.getCsize())
			return -1;
		else
		{
			fun.addr1 = (machineData.getAp_code()).getInt(machineData.getMachineState().pc+1);
			fun.addr2 =  (machineData.getAp_code()).getInt(machineData.getMachineState().pc+1+4);
			if (!validAddr(fun.addr1, false) || !validAddr(fun.addr2, false))
				return -1;
			else
				return 0;
		}
	}

	private int getAddrOff()
	{
		if (machineData.getMachineState().pc + 4 + 1>=machineData.getCsize())
			return -1;
		else
		{
			fun.addr1 =  (machineData.getAp_code()).getInt(machineData.getMachineState().pc+1);
			fun.off = (machineData.getAp_code()).get(   machineData.getMachineState().pc+1+4);
			//System.out.println(fun.addr1);
			if (!validAddr(fun.addr1, false) ||
					!validAddr(machineData.getMachineState().pc+fun.off, true))
				return -1;
			else
				return 0;
		}

	}

	private int getAddrsOff()
	{
		if (machineData.getMachineState().pc + 4 + 4+1>=machineData.getCsize())
			return -1;
		else
		{
			fun.addr1 =  (machineData.getAp_code()).getInt(machineData.getMachineState().pc+1);
			fun.addr2 =  (machineData.getAp_code()).getInt(machineData.getMachineState().pc+1+4);
			fun.off = (machineData.getAp_code()).get( machineData.getMachineState().pc+1+4+4);

			if (!validAddr(fun.addr1, false) ||
					!validAddr(fun.addr2, false) ||
					!validAddr(machineData.getMachineState().pc+fun.off, true))
				return -1;
			else
				return 0;
		}
	}

	private int getFunAddr()
	{
		//System.out.println("pc counter: "+machineData.getMachineState().pc);
		if (machineData.getMachineState().pc + 4 + 4>=machineData.getCsize())
			return -1;
		else
		{
			fun.fun =  (machineData.getAp_code()).getShort( machineData.getMachineState().pc+1);
			fun.addr1 =  (machineData.getAp_code()).getInt((machineData.getMachineState().pc+1+2));
			//System.out.println("fun: "+fun.fun+" fun.addr1 :"+fun.addr1);
			if (!validAddr(fun.addr1, false))
				return -1;
			else
				return 0;
		}
	}

	private int getFunAddrs()
	{

		if (machineData.getMachineState().pc + 4 + 4 + 2>=machineData.getCsize())
			return -1;
		else
		{
			fun.fun = (machineData.getAp_code()).getShort( machineData.getMachineState().pc+1);

			fun.addr3 =  (machineData.getAp_code()).getInt(machineData.getMachineState().pc+1+2);
			fun.addr2 =  (machineData.getAp_code()).getInt(machineData.getMachineState().pc+1+2+4);

			if (!validAddr(fun.addr3, false) ||
					!validAddr(fun.addr2, false))
				return -1;
			else
				return 0;
		}
	}

	private int getAddressVal()
	{
		if (machineData.getMachineState().pc + 4 + 8>=machineData.getCsize())
			return -1;
		else
		{
			fun.addr1 =  (machineData.getAp_code()).getInt(machineData.getMachineState().pc+1);
			fun.val = (machineData.getAp_code()).getLong(machineData.getMachineState().pc+1+4);

			if (!validAddr(fun.addr1, false))
				return -1;
			else
				return 0;
		}
	}
	
	private boolean validAddr(int addr, boolean is_code)
	{
		if(addr<0)
			return false;
		
		if(!is_code && (((long)addr)*8+8>((long)Integer.MAX_VALUE) ||
				addr*8+8>machineData.getDsize()))
			return false;
		
		if(is_code && addr>=machineData.getCsize())
			return false;
		
		return true;
	}

	private class Fun
	{
		short fun;
		int addr1;
		int addr2;
		long val;
		byte off;
		int addr3;
	}

	public AT_Machine_Processor( AT_Machine_State machineData )
	{	
		this.machineData = machineData;
		//NXT_AT_Controller.dumpBytes(machineData.getAp_code(), 0, machineData.getAp_code().array().length);
	}

	protected int processOp(boolean disassemble,boolean determine_jumps) {

		int rc = 0;
		
		if (machineData.getCsize()<1 || machineData.getMachineState().pc>=machineData.getCsize())
			return 0;

		if (determine_jumps)
		{
			machineData.getMachineState().jumps.add(machineData.getMachineState().pc);
		}

		byte op = (machineData.getAp_code()).get(machineData.getMachineState().pc);
		//System.out.println("OpCode : "+op);
		if (op>0 && disassemble && !determine_jumps)
		{
			System.out.print(String.format("%8x", machineData.getMachineState().pc).replace(' ', '0'));
			if (machineData.getMachineState().pc == machineData.getMachineState().opc)
				System.out.print("* ");
			else
				System.out.print("  ");
		}

		if ( op == OpCode.e_op_code_NOP){
			if ( disassemble ){
				if (! determine_jumps )
					System.out.println("NOP");
				++rc;
			}
			else {
				++rc;
				++machineData.getMachineState().pc;
			}
		}
		else if ( op == OpCode.e_op_code_SET_VAL)
		{
			rc = getAddressVal();
			
			if (rc==0 || disassemble)
			{
				rc = 1 + 4 + 8;
				if (disassemble)
				{
					if (!determine_jumps)
						System.out.println("SET @"+String.format("%8s",fun.addr1).replace(' ','0')+" "+String.format("#%16s",Long.toHexString(fun.val)).replace(' ', '0'));
				}
				else
				{
					machineData.getMachineState().pc += rc;
					machineData.getAp_data().putLong(fun.addr1*8,fun.val);
					machineData.getAp_data().clear();

				}
			}

		}
		else if (op== OpCode.e_op_code_SET_DAT)
		{
			rc  = getAddrs();

			if (rc==0 || disassemble)
			{
				rc = 1+4+4;

				if (disassemble)
				{
					if (!determine_jumps)
						System.out.println(	"SET @"+String.format("%8s", fun.addr1).replace(' ', '0')+
								" $"+String.format("%8s",fun.addr2).replace(' ', '0'));
				}
				else
				{
					machineData.getMachineState().pc+=rc;
					machineData.getAp_data().putLong(fun.addr1*8,machineData.getAp_data().getLong(fun.addr2*8));
					machineData.getAp_data().clear();

				}
			}
		}
		else if( op == OpCode.e_op_code_CLR_DAT )
		{
			rc = getAddr(false );

			if( rc == 0 || disassemble)
			{
				rc = 1 + 4;

				if( disassemble )
				{
					if( !determine_jumps )
						System.out.println("CLR @"+String.format("%8s",fun.addr1));
				}
				else
				{
					machineData.getMachineState().pc += rc;
					machineData.getAp_data().putLong( fun.addr1*8,(long)0);
					machineData.getAp_data().clear();
				}
			}
		}
		else if (op==OpCode.e_op_code_INC_DAT||op==OpCode.e_op_code_DEC_DAT||op==OpCode.e_op_code_NOT_DAT)
		{
			rc = getAddr(false);
			if (rc==0 || disassemble)
			{
				rc = 1+4;
				if (disassemble )
				{
					if (!determine_jumps)
					{
						if (op==OpCode.e_op_code_INC_DAT)
						{
							System.out.print("INC @");
						}
						else if (op==OpCode.e_op_code_DEC_DAT)
						{
							System.out.print("DEC @");
						}
						else if (op==OpCode.e_op_code_NOT_DAT)
						{
							System.out.print("NOT @");
						}
						System.out.println(String.format("%8", fun.addr1).replace(' ', '0'));
					}
				}
				else
				{
					machineData.getMachineState().pc +=rc;
					if (op==OpCode.e_op_code_INC_DAT)
					{
						long incData = machineData.getAp_data().getLong((fun.addr1*8));
						incData++;
						machineData.getAp_data().putLong((fun.addr1*8), incData);
						machineData.getAp_data().clear();
					}
					else if (op==OpCode.e_op_code_DEC_DAT)
					{
						long incData = machineData.getAp_data().getLong((fun.addr1*8));
						incData--;
						machineData.getAp_data().putLong((fun.addr1*8), incData);
						machineData.getAp_data().clear();
					}
					else if (op==OpCode.e_op_code_NOT_DAT)
					{
						long incData = machineData.getAp_data().getLong((fun.addr1*8));
						machineData.getAp_data().putLong((fun.addr1*8), ~incData);
						machineData.getAp_data().clear();
					}
				}
			}
		}
		else if (op==OpCode.e_op_code_ADD_DAT||op==OpCode.e_op_code_SUB_DAT||op==OpCode.e_op_code_MUL_DAT||op==OpCode.e_op_code_DIV_DAT)
		{
			rc = getAddrs();

			if (rc==0 || disassemble)
			{
				rc = 1+4+4;
				if (disassemble)
				{
					if (!determine_jumps)
					{
						if (op==OpCode.e_op_code_ADD_DAT)
						{
							System.out.print("ADD @");
						}
						else if (op==OpCode.e_op_code_SUB_DAT)
						{
							System.out.print("SUB @");
						}
						else if (op==OpCode.e_op_code_MUL_DAT)
						{
							System.out.print("MUL @");
						}
						else if (op==OpCode.e_op_code_DIV_DAT)
						{
							System.out.print("DIV @");
						}
						System.out.println(String.format("%8x", fun.addr1).replace(' ', '0')+" $"+String.format("%8s", fun.addr2).replace(' ','0'));
					}
				}
				else
				{
					long val = machineData.getAp_data().getLong( fun.addr2*8);
					if (op==OpCode.e_op_code_DIV_DAT && val==0)
						rc=-2;
					else
					{
						machineData.getMachineState().pc+=rc;
						if (op==OpCode.e_op_code_ADD_DAT)
						{
							long addData1 = machineData.getAp_data().getLong((fun.addr1*8));
							long addData2 = machineData.getAp_data().getLong((fun.addr2*8));
							machineData.getAp_data().putLong((fun.addr1*8), addData1+addData2);
							machineData.getAp_data().clear();
						}
						else if (op==OpCode.e_op_code_SUB_DAT)
						{
							long addData1 = machineData.getAp_data().getLong((fun.addr1*8));
							long addData2 = machineData.getAp_data().getLong((fun.addr2*8));
							machineData.getAp_data().putLong((fun.addr1*8), addData1-addData2);
							machineData.getAp_data().clear();
						}
						else if (op==OpCode.e_op_code_MUL_DAT)
						{
							long addData1 = machineData.getAp_data().getLong((fun.addr1*8));
							long addData2 = machineData.getAp_data().getLong((fun.addr2*8));
							machineData.getAp_data().putLong((fun.addr1*8), addData1*addData2);
							machineData.getAp_data().clear();
						}
						else if (op==OpCode.e_op_code_DIV_DAT)
						{
							long addData1 = machineData.getAp_data().getLong((fun.addr1*8));
							long addData2 = machineData.getAp_data().getLong((fun.addr2*8));
							machineData.getAp_data().putLong((fun.addr1*8), addData1/addData2);
							machineData.getAp_data().clear();
						}
					}
				}
			}
		}
		else if (op==OpCode.e_op_code_BOR_DAT||op==OpCode.e_op_code_AND_DAT||op==OpCode.e_op_code_XOR_DAT)
		{
			rc = getAddrs();

			if (rc==0 || disassemble)
			{
				rc = 1+4+4;
				if (disassemble)
				{
					if (!determine_jumps)
					{
						if (op==OpCode.e_op_code_BOR_DAT)
						{
							System.out.print("BOR @");
						}
						else if (op==OpCode.e_op_code_AND_DAT)
						{
							System.out.print("AND @");
						}
						else if (op==OpCode.e_op_code_XOR_DAT)
						{
							System.out.print("XOR @");
						}
						System.out.println(String.format("%16s $%16s", fun.addr1,fun.addr2).replace(' ', '0'));
					}
				}
				else
				{
					machineData.getMachineState().pc +=rc;
					long val = machineData.getAp_data().getLong( fun.addr2*8);

					if (op==OpCode.e_op_code_BOR_DAT)
					{
						long incData = machineData.getAp_data().getLong((fun.addr1*8));
						machineData.getAp_data().putLong((fun.addr1*8), incData|val);
						machineData.getAp_data().clear();
					}
					else if (op==OpCode.e_op_code_AND_DAT)
					{
						long incData = machineData.getAp_data().getLong((fun.addr1*8));
						machineData.getAp_data().putLong((fun.addr1*8), incData&val);
						machineData.getAp_data().clear();
					}
					else if (op==OpCode.e_op_code_XOR_DAT)
					{
						long incData = machineData.getAp_data().getLong((fun.addr1*8));
						machineData.getAp_data().putLong((fun.addr1*8), incData^val);
						machineData.getAp_data().clear();
					}
				}
			}
		}
		else if (op==OpCode.e_op_code_SET_IND)
		{
			rc = getAddrs();
			
			if (rc==0)
			{
				rc = 1+4+4;

				if (disassemble)
				{
					if (!determine_jumps)
						System.out.println("SET @"+
								String.format("%8s",fun.addr1).replace(' ', '0')+" "+
								String.format("$($%8s",fun.addr2).replace(' ', '0'));
				}
				else
				{
					long addr = machineData.getAp_data().getLong( fun.addr2*8);
					
					if (!validAddr((int)addr, false))
						rc=-1;
					else
					{
						machineData.getMachineState().pc+=rc;
						long val = machineData.getAp_data().getLong( (int)addr*8 );
						machineData.getAp_data().putLong(fun.addr1*8, val);
						machineData.getAp_data().clear();
					}
				}
			}
		}
		else if ( op == OpCode.e_op_code_SET_IDX) {
			int addr1,addr2;
			rc=getAddrs();
			addr1=fun.addr1;
			addr2=fun.addr2;
			int size = 4 + 4;
			if (rc==0 || disassemble){
				(machineData.getAp_code()).position(size);
				rc = getAddr( false );
				(machineData.getAp_code()).position((machineData.getAp_code()).position()-size);
			

				if (rc==0 || disassemble){
					rc=13;

					if (disassemble){
						if (!determine_jumps)
							System.out.println("");
					}
					else
					{
						long base=machineData.getAp_data().getLong( addr2*8);
						long offs=machineData.getAp_data().getLong( fun.addr1*8);

						long addr=base+offs;

						System.out.println(fun.addr1);
						if (!validAddr((int)addr, false))
							rc=-1;
						else
						{
							machineData.getMachineState().pc+=rc;
							machineData.getAp_data().putLong(addr1*8,machineData.getAp_data().getLong((int)addr*8));
							machineData.getAp_data().clear();
						}

					}
				}
			}
		}
		else if (op==OpCode.e_op_code_PSH_DAT||op==OpCode.e_op_code_POP_DAT)
		{
			rc = getAddr(false);
			if (rc==0 || disassemble)
			{
				rc  = 1 + 4;
				if (disassemble)
				{
					if (!determine_jumps)
					{
						if (op==OpCode.e_op_code_PSH_DAT)
							System.out.print("PSH $");
						else
							System.out.print("POP @");

						System.out.println(String.format("%8s",fun.addr1).replace(' ', '0'));
					}
				}
			
			else if ((op==OpCode.e_op_code_PSH_DAT&&machineData.getMachineState().us==(machineData.getC_user_stack_bytes()/8))||
					(op==OpCode.e_op_code_POP_DAT && machineData.getMachineState().us==0))
			{
				rc=-1;
			}
			else
			{
				machineData.getMachineState().pc+=rc;
				if (op==OpCode.e_op_code_PSH_DAT)
				{
					long val = machineData.getAp_data().getLong(fun.addr1*8);
					machineData.getMachineState().us++;
					machineData.getAp_data().putLong(machineData.getDsize()+machineData.getC_call_stack_bytes()+machineData.getC_user_stack_bytes()-((machineData.getMachineState().us)*8), val);
					machineData.getAp_data().clear();
				}
				else
				{
					long val = machineData.getAp_data().getLong(machineData.getDsize()+machineData.getC_call_stack_bytes()+machineData.getC_user_stack_bytes()-(machineData.getMachineState().us*8));
					machineData.getMachineState().us--;
					machineData.getAp_data().putLong(fun.addr1*8, val);
					machineData.getAp_data().clear();
				}
			}
			}
		}
		else if( op == OpCode.e_op_code_JMP_SUB )
		{
			rc = getAddr(true);

			if( rc == 0 || disassemble)
			{
				rc = 1 + 4;

				if( disassemble )
				{
					if( !determine_jumps )
						System.out.println("JSR :"+String.format("%8s", fun.addr1).replace(' ', '0'));
				}
				else
				{
					if( machineData.getMachineState().cs == ( machineData.getC_call_stack_bytes() / 8 ) )
						rc = -1;
					else if( machineData.getMachineState().jumps.contains(fun.addr1) )
					{
						
						machineData.getMachineState().cs++;
						machineData.getAp_data().putLong( machineData.getDsize()+machineData.getC_call_stack_bytes()-(machineData.getMachineState().cs*8), (long)(machineData.getMachineState().pc+rc));
						machineData.getAp_data().clear();
						machineData.getMachineState().pc = fun.addr1;
					}
					else
						rc = -2;
				}
			}
		}
		else if( op == OpCode.e_op_code_RET_SUB )
		{
			rc = 1;

			if( disassemble )
			{
				if( !determine_jumps )
					System.out.println("RET\n");
			}
			else
			{
				if( machineData.getMachineState().cs == 0 )
					rc = -1;
				else
				{
					long val = machineData.getAp_data().getLong( machineData.getDsize()+machineData.getC_call_stack_bytes()-machineData.getMachineState().cs*8);
					machineData.getMachineState().cs--;
					int addr = (int)val;
					if( machineData.getMachineState().jumps.contains(addr ) )
						machineData.getMachineState().pc = addr;
					else
						rc = -2;
				}
			}
		}
		else if (op==OpCode.e_op_code_IND_DAT)
		{
			rc = getAddrs();
			
			if (rc==0)
			{
				rc = 1+4+4;
				
				if (disassemble)
				{
					if(!determine_jumps)
						System.out.println("SET @"+
								String.format("($%8s)", fun.addr1).replace(' ', '0')+" "+
								String.format("$%8s", fun.addr2).replace(' ', '0'));
				}
				else
				{
					long addr = machineData.getAp_data().getLong( fun.addr1*8 );
					
					if (!validAddr((int)addr, false))
						rc=-1;
					else
					{
						machineData.getMachineState().pc+=rc;
						machineData.getAp_data().putLong((int)addr*8, machineData.getAp_data().getLong((int)fun.addr2*8));
						machineData.getAp_data().clear();
					}
				}
			}
		}
		else if (op == OpCode.e_op_code_IDX_DAT)
		{
			int addr1,addr2;
			rc = getAddrs();
			addr1 = fun.addr1;
			addr2 = fun.addr2;
			int size = 4 + 4;
			if (rc==0 || disassemble)
			{
				(machineData.getAp_code()).position(size);
				rc = getAddr(false);
				(machineData.getAp_code()).position((machineData.getAp_code()).position()-size);
				
				if (rc==0 || disassemble)
				{
					rc=13;
					
					if (disassemble)
					{
						if (!determine_jumps)
							System.out.println("SET @"+
									String.format("($%8s+$%8s)", addr1, addr2).replace(' ', '0')+" "+
									String.format("$%8s", fun.addr1).replace(' ', '0'));
					}
					else
					{
						long addr = machineData.getAp_data().getLong( addr1*8 ) +
								machineData.getAp_data().getLong( addr2*8 );
						
						if(!validAddr((int)addr, false))
							rc=-1;
						else
						{
							machineData.getMachineState().pc+=rc;
							machineData.getAp_data().putLong((int)addr*8, machineData.getAp_data().getLong((int)fun.addr1*8));
							machineData.getAp_data().clear();
						}
					}
				}
			}
		}
		else if ( op == OpCode.e_op_code_MOD_DAT )
		{
			rc = getAddrs();
			
			if( rc == 0 || disassemble )
			{
				rc = 1 + 4 + 4;
				if (disassemble)
				{
					if ( !determine_jumps )
						System.out.println("MOD @"+String.format("%8x", fun.addr1).replace(' ', '0')+
								" $"+String.format("%8s", fun.addr2).replace(' ','0'));
				}
				else
				{
					long modData1 = machineData.getAp_data().getLong(fun.addr1 * 8);
					long modData2 = machineData.getAp_data().getLong(fun.addr2 * 8);
					
					if(modData2 == 0)
						rc = -2;
					else
					{
						machineData.getMachineState().pc += rc;
						machineData.getAp_data().putLong(fun.addr1 * 8, modData1 % modData2);
					}
				}
			}
		}
		else if( op == OpCode.e_op_code_SHL_DAT || op == OpCode.e_op_code_SHR_DAT )
		{
			rc = getAddrs();
			
			if( rc == 0 || disassemble )
			{
				rc = 1 + 4 + 4;
				if( disassemble )
				{
					if( !determine_jumps )
					{
						if( op == OpCode.e_op_code_SHL_DAT )
							System.out.println("SHL @"+String.format("%8x", fun.addr1).replace(' ', '0')+
									" $"+String.format("%8x", fun.addr2).replace(' ', '0'));
						else
							System.out.println("SHR @"+String.format("%8x", fun.addr1).replace(' ', '0')+
									" $"+String.format("%8x", fun.addr2).replace(' ', '0'));
					}
				}
				else
				{
					machineData.getMachineState().pc+=rc;
					long val = machineData.getAp_data().getLong(fun.addr1 * 8);
					long shift = machineData.getAp_data().getLong(fun.addr2 * 8);
					if(shift < 0)
						shift = 0;
					else if(shift > 63)
						shift = 63;
					
					if( op == OpCode.e_op_code_SHL_DAT )
						machineData.getAp_data().putLong(fun.addr1 * 8, val << shift);
					else
						machineData.getAp_data().putLong(fun.addr1 * 8, val >>> shift);
				}
			}
		}
		else if( op == OpCode.e_op_code_JMP_ADR )
		{
			rc = getAddr(true);

			if( rc == 0 || disassemble)
			{
				rc = 1 + 4;

				if( disassemble )
				{
					if( !determine_jumps )
						System.out.println("JMP :"+String.format("%8x",fun.addr1));
				}
				else if( machineData.getMachineState().jumps.contains( fun.addr1 ) )
					machineData.getMachineState().pc = fun.addr1;
				else
					rc = -2;
			}
		}
		else if( op == OpCode.e_op_code_BZR_DAT || op == OpCode.e_op_code_BNZ_DAT )
		{
			rc = getAddrOff();

			if( rc == 0 || disassemble)
			{
				rc = 1 + 4 + 1;

				if( disassemble )
				{
					if( !determine_jumps )
					{
						if( op == OpCode.e_op_code_BZR_DAT )
							System.out.print("BZR $");
						else
							System.out.print("BNZ $");

						System.out.println(String.format("%8x",fun.addr1).replace(' ', '0')+", :"+String.format("%8x", machineData.getMachineState().pc+fun.off).replace(' ','0'));

					}
				}
				else
				{
					long val = machineData.getAp_data().getLong( fun.addr1*8);
					if( ( op == OpCode.e_op_code_BZR_DAT && val == 0 )
							|| ( op == OpCode.e_op_code_BNZ_DAT && val != 0 ) )
					{
						if( machineData.getMachineState().jumps.contains( machineData.getMachineState().pc + fun.off ) )
							machineData.getMachineState().pc += fun.off;
						else
							rc = -2;
					}
					else
						machineData.getMachineState().pc += rc;
				}
			}
		}
		else if( op == OpCode.e_op_code_BGT_DAT || op == OpCode. e_op_code_BLT_DAT
				|| op == OpCode. e_op_code_BGE_DAT || op == OpCode. e_op_code_BLE_DAT
				|| op == OpCode. e_op_code_BEQ_DAT || op == OpCode. e_op_code_BNE_DAT )
		{
			rc = getAddrsOff();

			if( rc == 0 || disassemble )
			{
				rc = 1 + 4 + 4 + 1;

				if( disassemble )
				{
					if( !determine_jumps )
					{
						if( op == OpCode. e_op_code_BGT_DAT )
							System.out.print("BGT $");
						else if( op == OpCode. e_op_code_BLT_DAT )
							System.out.print("BLT $");
						else if( op == OpCode. e_op_code_BGE_DAT )
							System.out.print("BGE $");
						else if( op == OpCode. e_op_code_BLE_DAT )
							System.out.print("BLE $");
						else if( op == OpCode. e_op_code_BEQ_DAT )
							System.out.print("BEQ $");
						else
							System.out.print("BNE $");

						System.out.println(String.format("%8x",fun.addr1).replace(' ','0')+
								" $"+String.format("%8x",fun.addr2).replace(' ','0')+
								" :"+String.format("%8x",machineData.getMachineState().pc+fun.off).replace(' ','0'));
					}
				}
				else
				{
					long val1 = machineData.getAp_data().getLong( fun.addr1*8);
					long val2 = machineData.getAp_data().getLong( fun.addr2*8);

					if( ( op == OpCode. e_op_code_BGT_DAT && val1 > val2 )
							|| ( op == OpCode. e_op_code_BLT_DAT && val1 < val2 )
							|| ( op == OpCode. e_op_code_BGE_DAT && val1 >= val2 )
							|| ( op == OpCode. e_op_code_BLE_DAT && val1 <= val2 )
							|| ( op == OpCode. e_op_code_BEQ_DAT && val1 == val2 )
							|| ( op == OpCode. e_op_code_BNE_DAT && val1 != val2 ) )
					{
						if( machineData.getMachineState().jumps.contains( machineData.getMachineState().pc + fun.off ) )
							machineData.getMachineState().pc +=fun.off;
						else
							rc = -2;
					}
					else
						machineData.getMachineState().pc += rc;
				}
			}
		}
		else if( op == OpCode.e_op_code_SLP_DAT )
		{
			rc = getAddr( true );
			
			if ( rc==0 || disassemble )
			{
				rc = 1 + 4;
				
				if ( disassemble )
				{
					if ( !determine_jumps )
						System.out.println("SLP @"+String.format("%8x",fun.addr1));
					
				}
				else
				{
					machineData.getMachineState().pc += rc;
					int numBlocks = (int)machineData.getAp_data().getLong((int)fun.addr1*8);
					if(numBlocks < 0)
						numBlocks = 0;
					int maxNumBlocks = (int)AT_Constants.getInstance().get_MAX_WAIT_FOR_NUM_OF_BLOCKS(machineData.getCreationBlockHeight());
					if(numBlocks > maxNumBlocks)
						numBlocks = maxNumBlocks;
					machineData.setWaitForNumberOfBlocks(numBlocks);
					machineData.getMachineState().stopped = true;
				}
				
			}
			
			/*int addr1,addr2;
			rc = getAddrs();
			addr1 = fun.addr1;
			addr2 = fun.addr2;
			int size = 4 + 4;
			if( rc == 0 || disassemble)
			{
				(machineData.getAp_code()).position(size);
				rc = getAddr( true );
				(machineData.getAp_code()).position((machineData.getAp_code()).position()-size);
			
				
				if( rc == 0 )
				{
					rc = 1 + size + 4;

					if( disassemble )
					{
						if( !determine_jumps )
							System.out.println("SLE @"+String.format("%8x", addr1).replace(' ','0')+
									" $"+String.format("%8x", addr2).replace(' ','0')+
									" :"+String.format("%8x", fun.addr1).replace(' ','0'));		                  
					}
					else
					{
						long val1 = machineData.getAp_data().getLong( addr1*8);
						long val2 = machineData.getAp_data().getLong( addr2*8);
						
						machineData.getAp_data().putLong( addr1*8,val1-val2);
						machineData.getAp_data().clear();

						if( machineData.getAp_data().getLong( addr1*8) <= 0 )
						{
							if( machineData.getMachineState().jumps.contains( fun.addr1 ) )
								machineData.getMachineState().pc = fun.addr1;
							else
								rc = -2;
						}
						else
							machineData.getMachineState().pc += rc;
					}
				}
			}*/
		}

		else if( op == OpCode.e_op_code_FIZ_DAT || op == OpCode.e_op_code_STZ_DAT )
		{
			rc = getAddr(false );

			if( rc == 0 || disassemble)
			{
				rc = 1 + 4;
				
				if( disassemble )
				{
					if( !determine_jumps )
					{
						if( op == OpCode.e_op_code_FIZ_DAT )
							System.out.print("FIZ @");
						else
							System.out.print("STZ @");

						System.out.println(String.format("%8x",fun.addr1).replace(' ', '0'));
					}
				}
				else
				{
					if(machineData.getAp_data().getLong( fun.addr1*8) == 0 )
					{
						if( op == OpCode.e_op_code_STZ_DAT )
						{
							machineData.getMachineState().pc += rc;
							machineData.getMachineState().stopped = true;
							machineData.setFreeze( true );
						}
						else
						{
							machineData.getMachineState().pc = machineData.getMachineState().pcs;
							machineData.getMachineState().finished = true;
							machineData.setFreeze( true );
						}
					}
					else
					{
						rc = 1 + 4;
						machineData.getMachineState().pc += rc;
					}
				}
			}
		}
		else if( op == OpCode.e_op_code_FIN_IMD || op == OpCode.e_op_code_STP_IMD )
		{
			rc = 1;
			
			if( disassemble )
			{
				if( !determine_jumps )
				{
					if( op == OpCode.e_op_code_FIN_IMD )
						System.out.println("FIN\n");
					else
						System.out.println("STP");
				}
			}
			else if( op == OpCode.e_op_code_STP_IMD )
			{
				machineData.getMachineState().pc += rc;
				machineData.getMachineState().stopped = true;
				machineData.setFreeze( true );
			}
			else
			{
				machineData.getMachineState().pc = machineData.getMachineState().pcs;
				machineData.getMachineState().finished = true;
				machineData.setFreeze( true );
			}
		}
		else if ( op == OpCode.e_op_code_SLP_IMD )
		{
			rc = 1;
			
			if( disassemble )
			{
				if( !determine_jumps )
				{
					System.out.println("SLP\n");
				}
			}
			else
			{
				machineData.getMachineState().pc += rc;
				machineData.getMachineState().stopped = true;
				machineData.setFreeze( true );
			}
			
		}
		else if( op == OpCode.e_op_code_SET_PCS )
		{
			rc = 1;

			if( disassemble )
			{
				if( !determine_jumps )
					System.out.println("PCS");
			}
			else
			{
				machineData.getMachineState().pc += rc;
				machineData.getMachineState().pcs = machineData.getMachineState().pc;
			}
		}
		else if( op == OpCode.e_op_code_EXT_FUN )
		{
			rc = getFun();

			if( rc == 0 || disassemble)
			{
				rc = 1 + 2;

				if( disassemble )
				{
					if( !determine_jumps )
						System.out.println("FUN "+fun.fun);
				}
				else
				{

					machineData.getMachineState().pc += rc;
					AT_API_Controller.func( fun.fun , machineData );
				}
			}
		}
		else if( op == OpCode.e_op_code_EXT_FUN_DAT )
		{
			rc = getFunAddr();
			if( rc == 0 )
			{
				rc = 1 + 2 + 4;

				if( disassemble )
				{
					if( !determine_jumps )
						System.out.println( "FUN "+fun.fun+" $"+
								String.format( "%8x", fun.addr1 ).replace( ' ','0' ) );
				}
				else
				{
					machineData.getMachineState().pc += rc;
					long val = (machineData.getAp_data()).getLong( fun.addr1*8 );
					AT_API_Controller.func1( fun.fun, val, machineData );
				}
			}
		}
		else if( op == OpCode.e_op_code_EXT_FUN_DAT_2 )
		{
			rc = getFunAddrs();

			if( rc == 0 || disassemble)
			{
				rc = 1 + 2 + 4 + 4;

				if( disassemble )
				{
					if( !determine_jumps )
						System.out.println("FUN "+fun.fun+" $"+
								String.format("%8x",fun.addr3).replace(' ','0')+
								" $"+
								String.format("%8x",fun.addr2).replace(' ','0'));
				}
				else
				{
					machineData.getMachineState().pc += rc;
					long val1 = machineData.getAp_data().getLong(   ( fun.addr3 * 8 ) );
					long val2 = machineData.getAp_data().getLong(   ( fun.addr2 * 8 ) );

					AT_API_Controller.func2( fun.fun, val1, val2 ,machineData);
				}
			}
		}
		else if( op == OpCode.e_op_code_EXT_FUN_RET )
		{
			rc = getFunAddr();

			if( rc == 0 || disassemble)
			{
				rc = 1 + 2 + 4;

				if( disassemble )
				{
					if( !determine_jumps )
						System.out.println("FUN @"+
								String.format("%8x", fun.addr1).replace(' ', '0')+" "+fun.fun);
				}
				else
				{
					machineData.getMachineState().pc += rc;

					machineData.getAp_data().putLong( fun.addr1*8,AT_API_Controller.func(fun.fun,machineData));
					machineData.getAp_data().clear();
				}
			}
		}
		else if( op == OpCode.e_op_code_EXT_FUN_RET_DAT || op == OpCode.e_op_code_EXT_FUN_RET_DAT_2 )
		{
			rc = getFunAddrs();
			int size = 2 + 4 + 4;


			if( (rc == 0  || disassemble )&& op == OpCode.e_op_code_EXT_FUN_RET_DAT_2 )
			{
				(machineData.getAp_code()).position(size);
				rc = getAddr( false );
				(machineData.getAp_code()).position((machineData.getAp_code()).position()-size);
			}

			if( rc == 0 )
			{
				rc = 1 + size + (( op == OpCode.e_op_code_EXT_FUN_RET_DAT_2) ? 4 : 0) ;
				
				if( disassemble )
				{
					if( !determine_jumps )
					{
						System.out.print("FUN @"+String.format("%8x",fun.addr3).replace(' ','0')+" "+fun.fun+" $"+String.format("%8x", fun.addr2).replace(' ','0'));

						if( op == OpCode.e_op_code_EXT_FUN_RET_DAT_2 )
							System.out.print(" $"+String.format("%8x", fun.addr1).replace(' ','0'));
						

						System.out.println("");
					}
				}
				else
				{
					machineData.getMachineState().pc += rc;
					long val = machineData.getAp_data().getLong(   ( fun.addr2 * 8 ) );

					if( op != OpCode.e_op_code_EXT_FUN_RET_DAT_2 )
						machineData.getAp_data().putLong(   ( fun.addr3 * 8 ),AT_API_Controller.func1( fun.fun, val,machineData));

					else
					{
						long val2 = machineData.getAp_data().getLong(   ( fun.addr1 * 8 ) );
						machineData.getAp_data().putLong(   ( fun.addr3 * 8 ), AT_API_Controller.func2( fun.fun, val, val2,machineData ));
					}
					machineData.getAp_data().clear();
				}
			}
		}
		else if( op == OpCode.e_op_code_ERR_ADR )
		{
			rc = getAddr(true);
			
			// don't check rc to allow for unsetting handler with -1
			
			rc = 1 + 4;
			
			if( disassemble )
			{
				if( !determine_jumps )
					System.out.println("ERR :"+String.format("%8x",fun.addr1));
			}
			else
			{
				if( fun.addr1 == -1 || machineData.getMachineState().jumps.contains( fun.addr1 ))
				{
					machineData.getMachineState().pc += rc;
					machineData.getMachineState().err = fun.addr1;
				}
				else
					rc = -2;
			}
		}
		else
		{
			if( !disassemble )
				rc = -2;
		}

		if( rc == -1 && disassemble && !determine_jumps )
			System.out.println("\n(overflow)");

		if( rc == -2 && disassemble && !determine_jumps )
			System.out.println("\n(invalid op)");

		/*if( rc >= 0 )
			++machineData.getMachineState().steps;
		*/

		return rc;
	}	

}
