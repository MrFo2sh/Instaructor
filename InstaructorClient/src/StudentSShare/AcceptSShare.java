package StudentSShare;

public class AcceptSShare{
	public AcceptSShare(int port,String password) {
		new InitConnection(port, password);
	}
}