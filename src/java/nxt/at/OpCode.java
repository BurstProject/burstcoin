/*
 * Copyright (c) 2014 CIYAM Developers

 Distributed under the MIT/X11 software license, please refer to the file license.txt
 in the root project directory or http://www.opensource.org/licenses/mit-license.php.

 */

package nxt.at;	

public final class OpCode
	{
		static byte e_op_code_NOP = Byte.parseByte("7f",16);
		static byte e_op_code_SET_VAL = Byte.parseByte("01",16);
		static byte e_op_code_SET_DAT = Byte.parseByte("02",16);
		static byte e_op_code_CLR_DAT = Byte.parseByte("03",16);
		static byte e_op_code_INC_DAT = Byte.parseByte("04",16);
		static byte e_op_code_DEC_DAT = Byte.parseByte("05",16);
		static byte e_op_code_ADD_DAT = Byte.parseByte("06",16);
		static byte e_op_code_SUB_DAT = Byte.parseByte("07",16);
		static byte e_op_code_MUL_DAT = Byte.parseByte("08",16);
		static byte e_op_code_DIV_DAT = Byte.parseByte("09",16);
		static byte e_op_code_BOR_DAT = Byte.parseByte("0a",16);
		static byte e_op_code_AND_DAT = Byte.parseByte("0b",16);
		static byte e_op_code_XOR_DAT = Byte.parseByte("0c",16);
		static byte e_op_code_NOT_DAT = Byte.parseByte("0d",16);
		static byte e_op_code_SET_IND = Byte.parseByte("0e",16);//
		static byte e_op_code_SET_IDX = Byte.parseByte("0f",16);//
		static byte e_op_code_PSH_DAT = Byte.parseByte("10",16);
		static byte e_op_code_POP_DAT = Byte.parseByte("11",16);
		static byte e_op_code_JMP_SUB = Byte.parseByte("12",16);
		static byte e_op_code_RET_SUB = Byte.parseByte("13",16);
		static byte e_op_code_IND_DAT = Byte.parseByte("14", 16);
		static byte e_op_code_IDX_DAT = Byte.parseByte("15", 16);
		static byte e_op_code_MOD_DAT = Byte.parseByte("16", 16);
		static byte e_op_code_SHL_DAT = Byte.parseByte("17", 16);
		static byte e_op_code_SHR_DAT = Byte.parseByte("18", 16);
		static byte e_op_code_JMP_ADR = Byte.parseByte("1a",16);
		static byte e_op_code_BZR_DAT = Byte.parseByte("1b",16);
		static byte e_op_code_BNZ_DAT = Byte.parseByte("1e",16);
		static byte e_op_code_BGT_DAT = Byte.parseByte("1f",16);
		static byte e_op_code_BLT_DAT = Byte.parseByte("20",16);
		static byte e_op_code_BGE_DAT = Byte.parseByte("21",16);
		static byte e_op_code_BLE_DAT = Byte.parseByte("22",16);
		static byte e_op_code_BEQ_DAT = Byte.parseByte("23",16);
		static byte e_op_code_BNE_DAT = Byte.parseByte("24",16);
		static byte e_op_code_SLP_DAT = Byte.parseByte("25",16);
		static byte e_op_code_FIZ_DAT = Byte.parseByte("26",16);
		static byte e_op_code_STZ_DAT = Byte.parseByte("27",16);
		static byte e_op_code_FIN_IMD = Byte.parseByte("28",16);
		static byte e_op_code_STP_IMD = Byte.parseByte("29",16);
		static byte e_op_code_SLP_IMD = Byte.parseByte("2a",16);
		static byte e_op_code_ERR_ADR = Byte.parseByte("2b", 16);
		static byte e_op_code_SET_PCS = Byte.parseByte("30",16);
		static byte e_op_code_EXT_FUN = Byte.parseByte("32",16);
		static byte e_op_code_EXT_FUN_DAT = Byte.parseByte("33",16);
		static byte e_op_code_EXT_FUN_DAT_2 = Byte.parseByte("34",16);
		static byte e_op_code_EXT_FUN_RET = Byte.parseByte("35",16);
		static byte e_op_code_EXT_FUN_RET_DAT = Byte.parseByte("36",16);
		static byte e_op_code_EXT_FUN_RET_DAT_2 = Byte.parseByte("37",16);
		
	}