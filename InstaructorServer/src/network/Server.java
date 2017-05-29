package network;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import InstructorSShare.ConnectScreen;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.util.Duration;
import tray.notification.NotificationType;
import tray.notification.TrayNotification;

public class Server {
	private static List<ConnectedClient> connectedClients = null;
	private static ServerSocket fileSSocket = null;
	private static ServerSocket cmdSSocket = null;
	private static int cmdPort,filePort;
	private static boolean Working = true;
	ListView<String> connectedList;
	ListView<String> ChatRoom;
	Timer timer;
	
	public Server(int cmdPort, int filePort, ListView<String> connectedList, ListView<String> ChatRoom){
		this.connectedList = connectedList;
		this.ChatRoom = ChatRoom;
		Server.cmdPort = cmdPort;
		Server.filePort = filePort;
	}
	
	public String getIpAddr(){
		return cmdSSocket.getInetAddress().getHostAddress();
	}
	
	public void checkForDevices(){
		if(timer == null){
			timer = new Timer();
			try{
				timer.scheduleAtFixedRate(new TimerTask() {
					  @Override
					  public void run() {
						  for(ConnectedClient c : connectedClients){
							  if(!checkIfConnected(c)){
								  removeFromConnectedDevices(c.getPcName());
								  connectedClients.remove(c);
							  }
						  }
					  }
					}, 5*1000, 5*1000);
			}catch(Exception e){}
		}
	}
	
	public void stopDeciveChecking(){
		if(timer != null){
			timer.cancel();
			timer = null;
		}
	}
	
	public void listenForRequests(){
		new Thread(new Runnable() {
			public void run() {
				try {
					while(Working){
						for(ConnectedClient c : connectedClients){
							BufferedReader br = new BufferedReader(new InputStreamReader(c.getCmdSocket().getInputStream()));
							if(br.ready()){
								String request = br.readLine();
								processRequests(request, c.getPcName(), br, new PrintStream(c.getCmdSocket().getOutputStream()));
							}
						}
							Thread.sleep(1);
					}
				} catch (IOException e) {
				} catch (InterruptedException e) {}
				
			}
		}).start();
	}
	
	private void addToChatList(String text){
		Platform.runLater(()->{
			ChatRoom.getItems().add(text);
		});
	}
	
	private void addToConnectedDevices(String pcName){
		Platform.runLater(()->{
			connectedList.getItems().add(pcName);
		});
	}
	
	private void removeFromConnectedDevices(String pcName){
		Platform.runLater(()->{
			connectedList.getItems().remove(pcName);
		});
	}
	
	private void processRequests(String request, String pcName, BufferedReader br, PrintStream ps) throws IOException {
		switch(request.toLowerCase()){
			case "help":
				String help = br.readLine();
				addToChatList("Help Notification: "+pcName+"\n"+help);
				showNotification(pcName + " needs help", help, NotificationType.NOTICE);
			break;
			case "msg":
				String msg = br.readLine();
				addToChatList(pcName+": \""+msg+"\"");
			break;
			case "notification":
				String notification = br.readLine();
				addToChatList(pcName+": \""+notification+"\"");
				showNotification(pcName, notification, NotificationType.SUCCESS);
			break;
			case "ping":
				pong(ps);
			break;
		}
	}

	private void showNotification(String title,String notification ,NotificationType type){
		Platform.runLater(
				  () -> {
					  TrayNotification tray = new TrayNotification(title, notification,type);
					  tray.showAndDismiss(Duration.seconds(4));
				  }
				);
	}
	
	private void requestPath(ConnectedClient c) throws IOException{
		PrintStream ps = new PrintStream(c.getCmdSocket().getOutputStream());
		ps.println("path");
		ps.flush();
		BufferedReader br = new BufferedReader(new InputStreamReader(c.getCmdSocket().getInputStream()));
		String path = br.readLine();
		c.setFilePath(path);
	}
	
	public void startFileServer(){
		try {
			fileSSocket = new ServerSocket(filePort);
			while(Working){
				Socket soc = fileSSocket.accept();
				BufferedReader br = new BufferedReader(new InputStreamReader(soc.getInputStream()));
				String pcName = br.readLine();
				boolean found = false;
				for(ConnectedClient c: connectedClients){
					if(pcName.equals(c.getPcName())){
						c.setFileSocket(soc);
						if(c.getFilePath().equals("/")){
							requestPath(c);
						}
						found = true;
					}
				}
				if(!found)
					soc.close();
			}
		} catch (IOException e) {}
	}
	
	public void startCmdServer(){
		Working = true;
		connectedClients = new CopyOnWriteArrayList<ConnectedClient>();
		try {
			cmdSSocket = new ServerSocket(cmdPort);
			while(Working){
				Socket soc = cmdSSocket.accept();
				BufferedReader br = new BufferedReader(new InputStreamReader(soc.getInputStream()));
				String pcName = br.readLine();
				ConnectedClient checkClient = null;
				int index=-1;
				for(int i = 0; i < connectedClients.size() ; i++){
					if(pcName.equals(connectedClients.get(i).getPcName())){
						checkClient = connectedClients.get(i);
						index = i;
					}
				}
				
				if(checkClient !=null && !checkIfConnected(checkClient) && index !=-1){
					checkClient.setCmdSocket(soc);
					connectedClients.set(index, checkClient);
					if(connectedList.getItems().indexOf(checkClient.getPcName())<0){
						final String name = checkClient.getPcName();
						addToConnectedDevices(name);
					}
				}else if(checkClient == null){
					ConnectedClient cc = new ConnectedClient();
					cc.setPcName(pcName);
					cc.setCmdSocket(soc);
					cc.setIpAddr(((InetSocketAddress)soc.getRemoteSocketAddress()).getAddress().toString().replace("/", ""));
					addToConnectedDevices(pcName);
					connectedClients.add(cc);
					requestPath(cc);
				}else if(checkIfConnected(checkClient) && checkClient.getCmdSocket().getRemoteSocketAddress().equals(soc.getRemoteSocketAddress())){
					checkClient.setCmdSocket(soc);
					connectedClients.set(index, checkClient);
				}else{
					usernameAlreadyConnected(soc);
					soc.close();
				}
			}
		} catch (IOException e) {}
	}
	
	private void usernameAlreadyConnected(Socket soc) throws IOException{
		PrintStream ps = new PrintStream(soc.getOutputStream());
		ps.println("exist");
		ps.flush();
	}
	
	private void terminateConnections(){
		for(ConnectedClient c : connectedClients)
			c.disconnect();
	}
	
	private void clearConnectedList(){
		Platform.runLater(()->{
			connectedList.getItems().clear();
		});
	}
	
	public void stopServers() throws IOException{
		terminateConnections();
		Working = false;
		cmdSSocket.close();
		fileSSocket.close();
		clearConnectedList();
		connectedClients.clear();
		connectedClients = null;
		fileSSocket = null;
		cmdSSocket = null;
		System.gc();
	}
	
	public void bcCmd(String cmd, ObservableList<String> PcNames){
		for(String name : PcNames){
			singleCmd(cmd, name);
		}
	}
	
	public void bcMsg(String msg, ObservableList<String> PcNames){
		for(String name : PcNames){
			singleMsg(msg, name);
		}
	}
	
	public void bcNotification(String notification, ObservableList<String> PcNames){
		for(String name : PcNames){
			singleNotification(notification, name);
		}
	}
	
	public void screenShareRequest(String pcName) throws IOException{
		ConnectedClient cc = null;
		for(ConnectedClient c : connectedClients){
			if(pcName.equals(c.getPcName())){
				cc=c;
			}
		}
		if(cc == null){
			return;
		}
		if(!checkIfConnected(cc)){
			removeFromConnectedDevices(pcName);
			connectedClients.remove(cc);
			cc.disconnect();
			return;
		}
		try {
			PrintStream ps = new PrintStream(cc.getCmdSocket().getOutputStream());
			ps.println("share");
			ps.flush();
		} catch (IOException e) {
			removeFromConnectedDevices(pcName);
			connectedClients.remove(cc);
			cc.disconnect();
		}
		
		String status = waitForAcceptance(cc.getCmdSocket());
		
		if(!status.equals("readytorecive")){
			return;
		}
		final String ipAddr = cc.getIpAddr();
		final String password = cc.getPcName();
		new Thread(
				()->{
					new ConnectScreen(ipAddr, 12347, password);
				}
			).start();
	}
	
	private void singleMsg(String msg, String pcName){
		ConnectedClient cc = null;
		for(ConnectedClient c : connectedClients){
			if(pcName.equals(c.getPcName())){
				cc=c;
			}
		}
		if(cc == null){
			return;
		}
		if(!checkIfConnected(cc)){
			removeFromConnectedDevices(pcName);
			connectedClients.remove(cc);
			cc.disconnect();
			return;
		}
		try {
			PrintStream ps = new PrintStream(cc.getCmdSocket().getOutputStream());
			ps.println("msg");
			ps.println(msg);
			ps.flush();
		} catch (IOException e) {
			removeFromConnectedDevices(pcName);
			connectedClients.remove(cc);
			cc.disconnect();
		}
	}
	
	private void singleNotification(String notification, String pcName){
		ConnectedClient cc = null;
		for(ConnectedClient c : connectedClients){
			if(pcName.equals(c.getPcName())){
				cc=c;
			}
		}
		if(cc == null){
			return;
		}
		if(!checkIfConnected(cc)){
			removeFromConnectedDevices(pcName);
			connectedClients.remove(cc);
			cc.disconnect();
			return;
		}
		try {
			PrintStream ps = new PrintStream(cc.getCmdSocket().getOutputStream());
			ps.println("notification");
			ps.println(notification);
			ps.flush();
		} catch (IOException e) {
			removeFromConnectedDevices(pcName);
			connectedClients.remove(cc);
			cc.disconnect();
		}
	}
	
	private void singleCmd(String cmd, String pcName){
		ConnectedClient cc = null;
		for(ConnectedClient c : connectedClients){
			if(pcName.equals(c.getPcName())){
				cc=c;
			}
		}
		if(cc == null){
			return;
		}
		if(!checkIfConnected(cc)){
			removeFromConnectedDevices(pcName);
			connectedClients.remove(cc);
			cc.disconnect();
			return;
		}
		try {
			if(cc.getFilePath().equals("/")){
				requestPath(cc);
			}
			PrintStream ps = new PrintStream(cc.getCmdSocket().getOutputStream());
			ps.println("command");
			ps.println("cd "+cc.getFilePath()+" && "+cmd);
			ps.flush();
		} catch (IOException e) {
			removeFromConnectedDevices(pcName);
			connectedClients.remove(cc);
			cc.disconnect();
		}
	}
	
	private String waitForAcceptance(Socket soc) throws IOException{
		BufferedReader br =null;
		br = new BufferedReader(new InputStreamReader(soc.getInputStream()));
		String State ="";
		State = br.readLine();
		return State;
	}
	
	private void singleFileTrans(String pcName, File file){
		new Thread(new Runnable() {
			@Override
			public void run() {
				ConnectedClient cc = null;
				for(ConnectedClient c : connectedClients){
					if(pcName.equals(c.getPcName())){
						cc=c;
					}
				}
				if(cc == null)
					return;
				if(!checkIfConnected(cc)){
					removeFromConnectedDevices(pcName);
					connectedClients.remove(cc);
					cc.disconnect();
					return;
				}
				PrintStream ps = null;
				String reciverState="";
				double gFileSize = 0;
				try {
					ps = new PrintStream(cc.getCmdSocket().getOutputStream());
					String fileSize = ""+(int)file.length();
					gFileSize = file.length();
					String fileName = file.getName();
					ps.println("file");
					ps.println(fileSize);
					ps.println(fileName);
					ps.flush();
					//wait for acceptance
					reciverState =waitForAcceptance(cc.getCmdSocket());
				} catch (IOException e2) {
					if(!checkIfConnected(cc)){
						removeFromConnectedDevices(pcName);
						connectedClients.remove(cc);
						cc.disconnect();
					}
					else{
						cc.disconnectFileStream();
						return;
					}
				}
				if(!reciverState.equals("readytorecive")){
					if(!checkIfConnected(cc)){
						removeFromConnectedDevices(pcName);
						connectedClients.remove(cc);
						cc.disconnect();
					}
					return;
				}
				while(cc.getFileSocket()==null);
				DataOutputStream dos =null;
				try {
					dos = new DataOutputStream(cc.getFileSocket().getOutputStream());
				} catch (Exception e) {
					if(!checkIfConnected(cc)){
						removeFromConnectedDevices(pcName);
						connectedClients.remove(cc);
						cc.disconnect();
					}
					return;
				}
				FileInputStream fis =null;
				try {
					fis = new FileInputStream(file.getAbsolutePath());
				} catch (FileNotFoundException e) {
					if(!checkIfConnected(cc)){
						removeFromConnectedDevices(pcName);
						connectedClients.remove(cc);
						cc.disconnect();
					}
					return;
				}
				byte[] buffer = new byte[8192];
				try {
					while (fis.read(buffer) > 0) {
						dos.write(buffer);
					}
				} catch (IOException e) {
					if(!checkIfConnected(cc)){
						removeFromConnectedDevices(pcName);
						connectedClients.remove(cc);
						cc.disconnect();
					}
					cc.disconnectFileStream();
					try {
						fis.close();
					} catch (IOException e1) {}
					return;
				}
				addToChatList("File sent sucessfully to "+pcName);
				showNotification("File transfer notification", "File sent successfully to "+pcName, NotificationType.SUCCESS);
				try {
					fis.close();
				} catch (IOException e1) {}
				try {
					dos.close();
				} catch (IOException e) {}
			}
		}).start();
	}
	
	public void bcFile(ObservableList<String> names, File file){
		for(String name : names){
			singleFileTrans(name, file);
		}
	}
	
	private void pong(PrintStream ps){
			PrintStream printStream = ps;
			printStream.println("pong");
			printStream.flush();
	}
	
	public boolean checkIfConnected(ConnectedClient c) {
		try {
			PrintStream ps = new PrintStream(c.getCmdSocket().getOutputStream());
			ps.println("ping");
			ps.flush();
			BufferedReader br = new BufferedReader(new InputStreamReader(c.getCmdSocket().getInputStream()));
			String pong = br.readLine();
			if(pong.equals("pong"))
				return true;
		} catch (Exception e) {
			return false;
		}
		return false;
	}
}
