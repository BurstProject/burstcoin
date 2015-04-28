package nxt;

import nxt.crypto.Crypto;
import nxt.Account;
import nxt.util.Convert;
import java.util.Scanner;
import java.io.Console;

public class Vanitygen{

	public Vanitygen(){
	}

	public static void main(String[] args)
	{
		Console cnsl = null;
		String pass = "";
		String token = "";
		try {
			cnsl = System.console();
			if (cnsl != null) {
				pass = new String(cnsl.readPassword("Password: "));
				String pass2 = new String(cnsl.readPassword("Password again: "));
				if (!pass.equals(pass2))
				{
					System.out.println("Passwords are not same");
					return;
				}
				token = new String(cnsl.readLine("Address: BURST-"));
				token = "BURST-" + token;

			}
		}catch(Exception ex){

			ex.printStackTrace();      
		}

		int i = 0;
		for (i=0;i< Integer.MAX_VALUE; i++)
		{
			byte[] publicKey = Crypto.getPublicKey(pass + i);
			long accountId = Account.getId(publicKey);
			String address = Convert.rsAccount(accountId);
			if (address.startsWith(token))
			{
				System.out.println("yourpass"+i + " = " +address );
				return ;
			}
		}

		System.out.println("Requested address not found");
	}

}
