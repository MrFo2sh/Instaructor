package network;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import StudentSShare.AcceptSShare;
import javafx.application.Platform;
import javafx.scene.control.ListView;
import javafx.util.Duration;
import tray.notification.NotificationType;
import tray.notification.TrayNotification;

public class Client {
	private static Socket CommandSocket;
	private static Socket FileSocket;
	private static int ServerCmdPort;
	private static int ServerFilePort;
	private static String ServerIP;
	private static boolean Working = true;
	private static BufferedReader bufferedReader;
	private static String Path;
	private static String pcName;
	private static PrintStream printStream;
	private static ListView<String> chatSpace;
	private static int isDownloading = 0;
	
	public Client(String ip, int cmdPort , int filePort, String pcName, String filePath, ListView<String> chatSpace) throws IOException{
		Client.chatSpace = chatSpace;
		Client.pcName = pcName;
		Client.ServerIP = ip;
		Client.ServerFilePort = filePort;
		Client.ServerCmdPort = cmdPort;
		setFilePath(filePath);
		connectToCmdCh();
	}
	
	public void setFilePath(String Path){
		Client.Path = Path+"/"; 
	}
	
	private void listenForCmds(){
		new Thread(new Runnable() {
			public void run() {
				try {
					bufferedReader = new BufferedReader(new InputStreamReader(CommandSocket.getInputStream()));
					while(Working){
						if(bufferedReader.ready()){
							String command = bufferedReader.readLine();
							processCmd(command);
						}
							Thread.sleep(1);
					}
				} catch (IOException e) {
				} catch (InterruptedException e) {}
				
			}
		}).start();
	}
	
	public void disconnect() throws IOException{
		CommandSocket.close();
		CommandSocket = null;
		FileSocket = null;
	}
	
	private void addToChat(String text){
		Platform.runLater(()->{
			chatSpace.getItems().add(text);
		});
	}
	
	public void sendMsg(String msg) throws IOException{
		printStream = new PrintStream(CommandSocket.getOutputStream());
		printStream.println("msg");
		printStream.println(msg);
		printStream.flush();
	}
	
	public void sendHelpRequest(String helpRequest) throws IOException{
		printStream = new PrintStream(CommandSocket.getOutputStream());
		printStream.println("help");
		printStream.println(helpRequest);
		printStream.flush();
	}
	
	public void sendNotification(String notification) throws IOException{
		printStream = new PrintStream(CommandSocket.getOutputStream());
		printStream.println("notification");
		printStream.println(notification);
		printStream.flush();
	}
	public void sendPath() throws IOException{
		printStream = new PrintStream(CommandSocket.getOutputStream());
		printStream.println(Path);
		printStream.flush();
	}
	
	private synchronized void incIsDownloading(){
		isDownloading++;
	}
	
	private synchronized void decIsDownloading(){
		isDownloading--;
	}
	
	private void processCmd(String command) throws IOException {
		switch(command.toLowerCase()){
		  	case "share":
		  		processScreenShare();
	  		break;
			case "path":
				sendPath();
			break;
			case"msg":
				processMsg();
			break;
			case "notification":
				processNotification();
			break;
			case "ping":
				pong();
			break;
			case "file":
				String fileName;
				int fileSize;
				try {
					if(bufferedReader.ready()){
						try {
							fileSize = Integer.parseInt(bufferedReader.readLine());
							fileName = bufferedReader.readLine();
							new Thread(new Runnable() {
								@Override
								public void run() {
									try {
										saveFile(Path+fileName,fileSize);
										addToChat("File downloaded");
										decIsDownloading();
									} catch (IOException e) {
										decIsDownloading();
										showNotification("Download Status", "Error occured while downloading the file.",NotificationType.WARNING);
									}
								}
							}).start();
						} catch (IOException e) {
						}
						processCmd(command);
					}
				} catch (IOException e) {
				}
			break;	
			case "command":
				try {
					String cmd = bufferedReader.readLine();
					Runtime.getRuntime().exec("cmd /c "+cmd);
				} catch (IOException e) {
				}catch(IllegalArgumentException e){
				}
			break;
			case "exist":
				processExist();
			break;
		}
	}
	
	private void processScreenShare() {
		new Thread(()->{
			new AcceptSShare(12347, pcName);
		}).start();
		Accepted();
	}
	
	private void processExist(){
		showNotification("Server Notification", "Username already exist", NotificationType.ERROR);
	}

	public void processMsg() throws IOException{
		String msg = bufferedReader.readLine();
		chatSpace.getItems().add("Server :"+msg);
		addToChat("Instructor :"+msg);
	}
	public void processNotification() throws IOException{
		String notification = bufferedReader.readLine();
		addToChat("Notification :"+notification);
		showNotification("Instructor Notification", notification, NotificationType.INFORMATION);
	}
	
	private void showNotification(String title,String notification ,NotificationType type){
		Platform.runLater(
				  () -> {
					  TrayNotification tray = new TrayNotification(title, notification,type);
					  tray.showAndDismiss(Duration.seconds(4));
				  }
				);
	}
	
	public void connectToCmdCh() throws IOException{
		CommandSocket = new Socket();
		CommandSocket.connect(new InetSocketAddress(ServerIP, ServerCmdPort));
		printStream = new PrintStream(CommandSocket.getOutputStream());
		printStream.println(pcName);
		printStream.flush();
		listenForCmds();
	}
	
	private void connectToFileCh(){
		try {
			FileSocket = new Socket();
			FileSocket.connect(new InetSocketAddress(ServerIP, ServerFilePort));
			printStream = new PrintStream(FileSocket.getOutputStream());
			printStream.println(pcName);
			printStream.flush();
		} catch (IOException e) {
		}
	}
	
	private void pong(){
		try{
			printStream = new PrintStream(CommandSocket.getOutputStream());
			printStream.println("pong");
			printStream.flush();
		}catch(IOException e){}
	}
	
	private void Accepted(){
		try{
			printStream = new PrintStream(CommandSocket.getOutputStream());
			printStream.println("readytorecive");
			printStream.flush();
		}catch(IOException e){}
	}
	
	private void saveFile(String FilePath,int FileSize) throws IOException {
		incIsDownloading();
		connectToFileCh();
		Accepted();
		addToChat("Downloading file");
		DataInputStream dis = new DataInputStream(FileSocket.getInputStream());
		FileOutputStream fos = new FileOutputStream(FilePath);

		byte[] buffer = new byte[8192];
		int filesize = FileSize ;
		int read = 0;
		int remaining = filesize;
		double totalRead = 0;
		
		while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
			totalRead += read;
			remaining -= read;
			fos.write(buffer, 0, read);
		}
		if(isDownloading == 1){
			fos.close();
			dis.close();
			FileSocket.close();
			FileSocket = null;
		}
		showNotification("Download Status", "File downloaded successfully",NotificationType.SUCCESS);
	}
	
	public boolean checkIfConnected() {
		try {
			PrintStream ps = new PrintStream(CommandSocket.getOutputStream());
			ps.println("ping");
			ps.flush();
			BufferedReader br = new BufferedReader(new InputStreamReader(CommandSocket.getInputStream()));
			String pong = br.readLine();
			if(pong.equals("pong"))
				return true;
		} catch (Exception e) {
			return false;
		}
		return false;
	}
}
