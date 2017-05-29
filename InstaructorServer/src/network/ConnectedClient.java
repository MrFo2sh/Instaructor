package network;
import java.io.IOException;
import java.net.Socket;

public class ConnectedClient {
	private Socket fileSocket ;
	private Socket cmdSocket ;
	private String pcName;
	private String filePath;
	private String ipAddr;
	
	public String getIpAddr() {
		return ipAddr;
	}
	
	public void setIpAddr(String ipAddr) {
		this.ipAddr = ipAddr;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getPcName() {
		return pcName;
	}

	public void setPcName(String pcName) {
		this.pcName = pcName;
	}

	public ConnectedClient() {
		fileSocket = null;
		cmdSocket = null;
		filePath = "/";
	}

	public Socket getFileSocket() {
		return fileSocket;
	}

	public void setFileSocket(Socket fileSocket) {
		this.fileSocket = fileSocket;
	}

	public Socket getCmdSocket() {
		return cmdSocket;
	}

	public void setCmdSocket(Socket cmdSocket) {
		this.cmdSocket = cmdSocket;
	}
	
	public void disconnect(){
		try {
			fileSocket.close();
		} catch (Exception e) {}
		try {
			cmdSocket.close();
		} catch (Exception e) {}
		fileSocket = null;
		cmdSocket = null;
		pcName = null;
	}
	
	public void disconnectFileStream(){
		try {
			fileSocket.close();
		} catch (IOException e) {}
		fileSocket = null;
	}
	
}
