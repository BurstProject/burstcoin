package nxt.at;

public enum AT_Error {
	
	INCORRECT_VERSION( 1 , "The current AT version is not supported." ),
	INCORRECT_CODE_PAGES( 2 , "Maximum number of code pages exceeded." ),
	INCORRECT_DATA_PAGES( 3 , "Maximum number of data pages exceeded."),
	INCORRECT_CALL_PAGES( 4 , "Maximum number of call stack pages exceeded."),
	INCORRECT_USER_PAGES( 5 , "Maximum number of user stack pages exceeded."),
	INCORRECT_CODE_LENGTH( 6 , "Code length is incorrect" ),
	INCORRECT_CODE( 7 , "Invalid code."),
	INCORRECT_DATA_LENGTH( 8 , "Data length is incorrect."),
	INCORRECT_CREATION_TX( 9 , "Incorrect AT creation tx."),
	INCORRECT_CREATION_FEE( 10 , "Incorrect creation fee for given number of pages" );
	
	
	private final int code;
	private final String description;
	
	private AT_Error( int code , String description ) {
		this.code = code;
		this.description = description;
	}
	
	public String getDescription(){
		return description;
	}
	
	public int getCode(){
		return code;
	}
	
	@Override
	public String toString() {
		return "error code: " + code + " , description : " + description;
	}
	

}
