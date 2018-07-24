package brs.at;

public class AT_API_Controller {
  static AT_API_Impl atApi = new AT_API_Impl();

  public static long func (int func_num, AT_Machine_State state) {

    switch (func_num) {
      case 256: return atApi.get_A1(state);
      case 257: return atApi.get_A2(state);
      case 258: return atApi.get_A3(state);
      case 259: return atApi.get_A4(state);
      case 260: return atApi.get_B1(state);
      case 261: return atApi.get_B2(state);
      case 262: return atApi.get_B3(state);
      case 263: return atApi.get_B4(state);
      case 288:
        atApi.clear_A(state);
        break;
      case 289:
        atApi.clear_B(state);
        break;
      case 290:
        atApi.clear_A(state);
        atApi.clear_B(state);
        break;
      case 291:
        atApi.copy_A_From_B(state);
        break;
      case 292:
        atApi.copy_B_From_A(state);
        break;
      case 293: return atApi.check_A_Is_Zero(state);
      case 294: return atApi.check_B_Is_Zero(state);
      case 295: return atApi.check_A_equals_B(state);
      case 296:
        atApi.swap_A_and_B(state);
        break;
      case 297:
        atApi.or_A_with_B(state);
        break;
      case 298:
        atApi.or_B_with_A(state);
        break;
      case 299:
        atApi.and_A_with_B(state);
        break;
      case 300:
        atApi.and_B_with_A(state);
        break;
      case 301:
        atApi.xor_A_with_B(state);
        break;
      case 302:
        atApi.xor_B_with_A(state);
        break;
      case 320:
        atApi.add_A_to_B(state);
        break;
      case 321:
        atApi.add_B_to_A(state);
        break;
      case 322:
        atApi.sub_A_from_B(state);
        break;
      case 323:
        atApi.sub_B_from_A(state);
        break;
      case 324:
        atApi.mul_A_by_B(state);
        break;
      case 325:
        atApi.mul_B_by_A(state);
        break;
      case 326:
        atApi.div_A_by_B(state);
        break;
      case 327:
        atApi.div_B_by_A(state);
        break;

      case 512:
        atApi.MD5_A_to_B(state);
        break;
      case 513: return atApi.check_MD5_A_with_B(state);
      case 514:
        atApi.HASH160_A_to_B(state);
        break;
      case 515: return atApi.check_HASH160_A_with_B(state);
      case 516:
        atApi.SHA256_A_to_B(state);
        break;
      case 517: return atApi.check_SHA256_A_with_B(state);

      case 768: return atApi.get_Block_Timestamp(state);    // 0x0300
      case 769: return atApi.get_Creation_Timestamp(state); // 0x0301
      case 770: return atApi.get_Last_Block_Timestamp(state);
      case 771:
        atApi.put_Last_Block_Hash_In_A(state);
        break;
      case 773: return atApi.get_Type_for_Tx_in_A(state);
      case 774: return atApi.get_Amount_for_Tx_in_A(state);
      case 775: return atApi.get_Timestamp_for_Tx_in_A(state);
      case 776: return atApi.get_Random_Id_for_Tx_in_A(state);
      case 777:
        atApi.message_from_Tx_in_A_to_B(state);
        break;
      case 778:
        atApi.B_to_Address_of_Tx_in_A(state);
        break;
      case 779:
        atApi.B_to_Address_of_Creator(state);
        break;

      case 1024: return atApi.get_Current_Balance(state);
      case 1025: return atApi.get_Previous_Balance(state);
      case 1027:
        atApi.send_All_to_Address_in_B(state);
        break;
      case 1028:
        atApi.send_Old_to_Address_in_B(state);
        break;
      case 1029:
        atApi.send_A_to_Address_in_B(state);
        break;
    }

    /*else if ( func_num == 1280 )
      {
      atApi.put_Last_Block_Generation_Signature_In_A(state);
      }*/

    return 0;
  }

  public static long func1( int func_num, long val, AT_Machine_State state) {
    switch (func_num) {
      case 272:
        atApi.set_A1( val, state);
        break;
      case 273:
        atApi.set_A2( val, state);
        break;
      case 274:
        atApi.set_A3( val, state);
        break;
      case 275:
        atApi.set_A4( val, state);
        break;
      case 278:
        atApi.set_B1( val, state);
        break;
      case 279:
        atApi.set_B2( val, state);
        break;
      case 280:
        atApi.set_B3( val, state);
        break;
      case 281:
        atApi.set_B4( val, state);
        break;
      case 772:
        atApi.A_to_Tx_after_Timestamp( val, state);
        break;
      case 1026:
        atApi.send_to_Address_in_B( val, state);
        break;
    }

    /*else if ( func_num == 1040 )
      {
      atApi.set_Min_Activation_Amount(val, state);
      }*/

    return 0;
  }

  public static long func2(int func_num, long val1, long val2, AT_Machine_State state) {
    switch (func_num) {
      case 276:
        atApi.set_A1_A2(val1, val2, state);
        break;
      case 277:
        atApi.set_A3_A4(val1, val2, state);
        break;
      case 282:
        atApi.set_B1_B2(val1, val2, state);
        break;
      case 283:
        atApi.set_B3_B4(val1, val2, state);
        break;
      case 1030:
        return atApi.add_Minutes_to_Timestamp(val1, val2, state);
    }

    /*else if ( func_num == 1536 )
      {
      atApi.SHA256_to_B( val1, val2, state);
      }*/

    return 0;
  }
}
