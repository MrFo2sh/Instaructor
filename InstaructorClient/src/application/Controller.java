package application;

import java.io.File;
import java.io.IOException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import network.Client;
import network.IPAddressValidator;

public class Controller {
	private Client client;
	private String filePath = "";
	@FXML
	private TextField usernameTxt;
	@FXML
	private TextField ipTxt;
	@FXML
	private TextField cmdPortTxt;
	@FXML
	private TextField filePortTxt;
	@FXML
	private TextField reciveDirectoryTxt;
	@FXML
	private TextField msgFieldTxt;
	@FXML
	private Button connectBtn;
	@FXML
	private Button selectDirectoryBtn;
	@FXML
	private Button notifyTastFinishedBtn;
	@FXML
	private Button helpBtn;
	@FXML
	private Button msgBtn;
	@FXML
	private ListView<String> chatSpace;
	@FXML 
	private AnchorPane AP;
	@FXML
	private Label clientStatusLbl;
	@FXML
	private Label isDownloadingLbl;
	@FXML
	private Label percentLbl;
	private FileSystem fileSystem;
	private static Stage stage;
	
	@FXML
	public void initialize() throws IOException {
		fileSystem = new FileSystem();
		Config config = fileSystem.loadServerConfig();
		//loading previous configurations
		if(config != null){
			ipTxt.setText(config.getIp());
			cmdPortTxt.setText(config.getCmdPort()+"");
			filePortTxt.setText(config.getFilePort()+"");
			usernameTxt.setText(config.getUsername());
			reciveDirectoryTxt.setText(config.getPath());
			filePath = config.getPath();
		}
		config = null;
    }
	
	public void connect(ActionEvent e) throws NumberFormatException, IOException{
		String username = usernameTxt.getText();
		String ip = ipTxt.getText();
		String cmdPort = cmdPortTxt.getText();
		String filePort = filePortTxt.getText();
		if(username.equals("") || ip.equals("") || cmdPort.equals("") || filePort.equals("")){
			alertMsg("Client Status", "Client Notification", "Username, IP, CMD Port, File Port must be filled before connecting");
			return;
		}
		if(!new IPAddressValidator().validate(ip)){
			alertMsg("Client Status", "Client Notification", "Invalid IP format");
			return;
		}
		if(filePath.equals("")){
			alertMsg("Client Status", "Client Notification", "Please select a Recive directory before connecting to the server");
			return;
		}
		try {
			client = new Client(ip, Integer.parseInt(cmdPort), Integer.parseInt(filePort), username, filePath, chatSpace);
			alertMsg("Client Status", "Client Notification", "Connected to server");
			clientStatusLbl.setText("Connected");
		} catch (NumberFormatException | IOException e1) {
			client = null;
			alertMsg("Client Status", "Client Notification", "Cannot connect to the server, please check IP address and ports");
			return;
		}
		stage = (Stage) AP.getScene().getWindow();
		stage.setOnCloseRequest(event -> {
			try {
				if(client != null){
					client.disconnect();
					client = null;
				}
			} catch (Exception err) {}
	        Platform.exit();
	        System.exit(0);
	    });
		fileSystem = new FileSystem();
		fileSystem.saveServerConfig(new Config(username, ip, filePath, Integer.parseInt(cmdPort), Integer.parseInt(filePort)));
		if(!client.checkIfConnected()){
			alertMsg("Server Notification", "Invalid Username", "The Username you selected is already in use please change it and connect again ");
			clientStatusLbl.setText("Disconnected");
			try{
				client.disconnect();
				client = null;
			}catch(Exception z){}
			return;
		}else{
			connectBtn.setDisable(true);
		}
	}
	
	
	public void selectDirectory(ActionEvent e){
		Stage stage = (Stage) AP.getScene().getWindow();
		DirectoryChooser directoryChoose = new DirectoryChooser();
		directoryChoose.setTitle("Select Directory");
		File file = directoryChoose.showDialog(stage);
		if(file != null){
			filePath = file.getAbsolutePath();
			reciveDirectoryTxt.setText(filePath);
			if(client != null){
				client.setFilePath(filePath);
			}
		}
	}
	
	public void notifyTaskFinished(ActionEvent e){
		if(client != null){
			try {
				client.sendNotification("Task Finished");
				chatSpace.getItems().add("Notification sent.");
			} catch (IOException e1) {
				alertMsg("Client Status", "Connection Error", "Unable to send notification to the server, try to reconnect");
				chatSpace.getItems().add("Notification sending error.");
				clientStatusLbl.setText("Disconnected");
			}
		}
	}
	
	public void sendMsg(ActionEvent e){
		String msg = msgFieldTxt.getText();
		if(client != null){
			if(!client.checkIfConnected()){
				try {
					System.out.println("not connected");
					alertMsg("Connection Error", "", "You are not connected to the server");
					clientStatusLbl.setText("Disconnected");
					connectBtn.setDisable(false);
					client.disconnect();
					client = null;
				} catch (Exception e1) {
					clientStatusLbl.setText("Disconnected");
					connectBtn.setDisable(false);
					client = null;
				}
				return;
			}
			if(!msg.equals("")){
				try {
					client.sendMsg(msg);
					chatSpace.getItems().add("Me: "+msg);
				} catch (IOException e1) {
					alertMsg("Client Status", "Connection Error", "Unable to send message to the server, try to reconnect");
					chatSpace.getItems().add("Message sending error.");
					clientStatusLbl.setText("Disconnected");
				}
			}
		}else{
			alertMsg("Client Status", "Client Notification", "Connect to the server first");
		}
	}
	
	public void sendHelp(ActionEvent e){
		String help = msgFieldTxt.getText();
		if(client != null){
			if(!client.checkIfConnected()){
				alertMsg("Connection Error", "", "You are not connected to the server");
				clientStatusLbl.setText("Disconnected");
				connectBtn.setDisable(false);
				try {
					client.disconnect();
					client = null;
				} catch (IOException e1) {}
				return;
			}
			if(!help.equals("")){
				try {
					client.sendHelpRequest(help);
					chatSpace.getItems().add("Help: "+help);
				} catch (IOException e1) {
					alertMsg("Client Status", "Connection Error", "Unable to send help request to the server, try to reconnect");
					chatSpace.getItems().add("Help request sending error.");
					clientStatusLbl.setText("Disconnected");
				}
			}
		}else{
			alertMsg("Client Status", "Client Notification", "Connect to the server first");
		}
	}
	
	public void alertMsg(String title, String header, String body){
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(body);
		alert.showAndWait().ifPresent(rs -> {
		    if (rs == ButtonType.OK) {
		    	
		    }
		});
	}
}
