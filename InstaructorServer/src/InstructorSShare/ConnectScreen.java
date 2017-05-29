package InstructorSShare;

import java.net.Socket;

import javax.swing.JOptionPane;

public class ConnectScreen {
	public ConnectScreen(String ip, int port, String password) {
		try {
			System.out.println("Will connect");
			Socket sc = new Socket(ip, port);
			System.out.println("Connecting to the Server");
			// Authenticate class is responsible for security purposes
			Authenticate auth = new Authenticate(sc,password);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
