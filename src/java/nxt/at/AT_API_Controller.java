package nxt.at;

public class AT_API_Controller{
	
	static AT_API_Impl atApi = new AT_API_Impl();
	
	public static long func ( int func_num , AT_Machine_State state ){

		long rc = 0;
		
		if ( func_num == 256 ) 
		{
			return atApi.get_A1( state );
		}
		else if ( func_num == 257 )
		{
			return atApi.get_A2( state );
		}
		else if ( func_num == 258 )
		{
			return atApi.get_A3( state );
		}
		else if ( func_num == 259 )
		{
			return atApi.get_A4( state );
		}
		else if ( func_num == 260 )
		{
			return atApi.get_B1( state );
		}
		else if ( func_num == 261 )
		{
			return atApi.get_B2( state );
		}
		else if ( func_num == 262 )
		{
			return atApi.get_B3( state );
		}
		else if ( func_num == 263 )
		{
			return atApi.get_B4( state );
		}
		else if ( func_num == 288  )
		{
			atApi.clear_A( state );
		}
		else if ( func_num == 289  )
		{
			atApi.clear_B( state );
		}
		else if ( func_num == 290  )
		{
			atApi.clear_A( state );
			atApi.clear_B( state );
		}
		else if ( func_num == 291 )
		{
			atApi.copy_A_From_B( state );
		}
		else if ( func_num == 292 )
		{
			atApi.copy_B_From_A( state );
		}
		else if ( func_num == 293 )
		{
			return atApi.check_A_Is_Zero( state );
		}
		else if ( func_num == 294 )
		{
			return atApi.check_B_Is_Zero( state );
		}
		else if ( func_num == 295)
		{
			return atApi.check_A_equals_B( state );
		}
		else if ( func_num == 296 )
		{
			atApi.swap_A_and_B( state );
		}
		else if ( func_num == 297 )
		{
			atApi.or_A_with_B( state );
		}
		else if ( func_num == 298 )
		{
			atApi.or_B_with_A( state );
		}
		else if ( func_num == 299 )
		{
			atApi.and_A_with_B( state );
		}
		else if ( func_num == 300 )
		{
			atApi.and_B_with_A( state );
		}
		else if ( func_num == 301 )
		{
			atApi.xor_A_with_B( state );
		}
		else if ( func_num == 302 )
		{
			atApi.xor_B_with_A( state );
		}
		else if ( func_num == 320)
		{
			atApi.add_A_to_B( state );
		}
		else if ( func_num == 321 )
		{
			atApi.add_B_to_A( state );
		}
		else if ( func_num == 322 )
		{
			atApi.sub_A_from_B( state );
		}
		else if ( func_num == 323 )
		{
			atApi.sub_B_from_A( state );
		}
		else if ( func_num == 324 )
		{
			atApi.mul_A_by_B( state );
		}
		else if ( func_num == 325 )
		{
			atApi.mul_B_by_A( state );
		}
		else if ( func_num == 326 )
		{
			atApi.div_A_by_B( state );
		}
		else if ( func_num == 327 )
		{
			atApi.div_B_by_A( state );
		}
		else if ( func_num == 512 )
		{
			atApi.MD5_A_to_B( state );
		}
		else if ( func_num == 513 )
		{
			return atApi.check_MD5_A_with_B( state );
		}
		else if ( func_num == 514 )
		{
			atApi.HASH160_A_to_B( state );
		}
		else if ( func_num == 515 )
		{
			return atApi.check_HASH160_A_with_B( state );
		}
		else if ( func_num == 516 )
		{
			atApi.SHA256_A_to_B( state );
		}
		else if ( func_num == 517 )
		{
			return atApi.check_SHA256_A_with_B( state );
		}
		else if ( func_num == 768 ) // 0x0300
		{
			return atApi.get_Block_Timestamp( state );
		}
		else if ( func_num == 769 ) // 0x0301
		{
			return atApi.get_Creation_Timestamp( state );
		}
		else if ( func_num == 770 ) 
		{
			return atApi.get_Last_Block_Timestamp( state );
		}
		else if ( func_num == 771 )
		{
			atApi.put_Last_Block_Hash_In_A( state );
		}
		else if ( func_num == 773 )
		{
			return atApi.get_Type_for_Tx_in_A( state );
		}
		else if ( func_num == 774 )
		{
			return atApi.get_Amount_for_Tx_in_A( state );
		}
		else if ( func_num == 775 )
		{
			return atApi.get_Timestamp_for_Tx_in_A( state );
		}
		else if ( func_num == 776 )
		{
			return atApi.get_Random_Id_for_Tx_in_A( state );
		}
		else if ( func_num == 777 )
		{
			atApi.message_from_Tx_in_A_to_B( state );
		}
		else if ( func_num == 778 )
		{
			atApi.B_to_Address_of_Tx_in_A( state );
		}
		else if ( func_num == 779 )
		{
			atApi.B_to_Address_of_Creator( state );
		}
		else if ( func_num == 1024 )
		{
			return atApi.get_Current_Balance( state );
		}
		else if ( func_num == 1025 )
		{
			return atApi.get_Previous_Balance( state );
		}
		else if ( func_num == 1027 )
		{
			atApi.send_All_to_Address_in_B( state );
		}
		else if ( func_num == 1028 )
		{
			atApi.send_Old_to_Address_in_B( state );
		}
		else if ( func_num == 1029 )
		{
			atApi.send_A_to_Address_in_B( state );
		}
		/*else if ( func_num == 1280 )
		{
			atApi.put_Last_Block_Generation_Signature_In_A( state );
		}*/
		
		
		return rc;
	}

	public static long func1( int func_num , long val , AT_Machine_State state )
	{
		long rc = 0;
		
		if ( func_num == 272 )
		{
			atApi.set_A1( val , state );
		}
		else if ( func_num == 273 )
		{
			atApi.set_A2( val , state );
		}
		else if ( func_num == 274 )
		{
			atApi.set_A3( val , state );
		}
		else if ( func_num == 275 )
		{
			atApi.set_A4( val , state );
		}
		else if ( func_num == 278 )
		{
			atApi.set_B1( val , state );
		}
		else if ( func_num == 279 )
		{
			atApi.set_B2( val , state );
		}
		else if ( func_num == 280 )
		{
			atApi.set_B3( val , state );
		}
		else if ( func_num == 281 )
		{
			atApi.set_B4( val , state );
		}
		else if ( func_num == 772 )
		{
			atApi.A_to_Tx_after_Timestamp( val , state );
		}
		else if ( func_num == 1026 )
		{
			atApi.send_to_Address_in_B( val , state );
		}
		/*else if ( func_num == 1040 )
		{
			atApi.set_Min_Activation_Amount(val, state);
		}*/
		
		return rc;
	}

	public static long func2( int func_num , long val1 , long val2 , AT_Machine_State state )
	{
		long rc = 0;
		
		if ( func_num == 276 )
		{
			atApi.set_A1_A2( val1 , val2 , state );
		}
		else if ( func_num == 277 )
		{
			atApi.set_A3_A4( val1 , val2 , state );
		}
		else if ( func_num == 282 )
		{
			atApi.set_B1_B2( val1 , val2 , state );
		}
		else if ( func_num == 283 )
		{
			atApi.set_B3_B4( val1 , val2 , state );
		}
		else if ( func_num == 1030 )
		{
			return atApi.add_Minutes_to_Timestamp( val1 , val2 , state );
		}
		/*else if ( func_num == 1536 )
		{
			atApi.SHA256_to_B( val1 , val2 , state );
		}*/
		
		return rc;
		
	}
}
