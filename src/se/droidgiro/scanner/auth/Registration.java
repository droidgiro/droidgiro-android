package se.droidgiro.scanner.auth;

public class Registration {

	private String channel;
	
	private String message;
	
	public String getChannel() {
		return channel;
	}
	
	public String getMessage() {
		return message;
	}
	
	public boolean isSucessful() {
		return channel != null;
	}
	
	@Override
	public String toString() {
		return "Registration [channel: " + channel + "]";
	}
}
