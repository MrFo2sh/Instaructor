package application;

public class Config {
	private String ip;
	private String path;
	private String username;
	private int cmdPort;
	private int filePort;
	
	public Config(String username, String ip, String path, int cmdPort, int filePort) {
		this.ip = ip;
		this.username = username;
		this.path = path;
		this.cmdPort = cmdPort;
		this.filePort = filePort;
	}
	
	
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public int getCmdPort() {
		return cmdPort;
	}
	public void setCmdPort(int cmdPort) {
		this.cmdPort = cmdPort;
	}
	public int getFilePort() {
		return filePort;
	}
	public void setFilePort(int filePort) {
		this.filePort = filePort;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	
}
