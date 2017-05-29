package application;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import network.Server;

public class Controller {
	private static Server server;
	private File file;
	
	@FXML
	private Button startServerBtn;
	@FXML
	private Button viewScreenBtn;
	@FXML
	private Button stopServerBtn;
	@FXML
	private Button executeCMDBtn;
	@FXML
	private Button selectFileBtn;
	@FXML
	private Button sendFilesBtn;
	@FXML
	private ListView<String> connectedDevices;
	@FXML
	private TextField cmdTxt;
	@FXML
	private TextField filePathTxt;
	@FXML 
	private AnchorPane AP;
	@FXML
	private Label serverStateLbl;
	@FXML
	private Label serverIpLbl;
	@FXML
	private ListView<String> chatRoomList;
	@FXML
	private Button sendMsg;
	@FXML
	private TextField msgTxt;
	@FXML
	private Button clearBtn;
	@FXML
	private Button notificationBtn;
	@FXML
	private Button addCommandBtn;
	@FXML
	private Button executeSelectedCommands;
	@FXML
	private Button removeSelectedCommands;
	@FXML
	private ListView<String> commandsList;
	@FXML
	private TextField addCommandText;
	private FileSystem fileSystem;
	
	@FXML
	public void initialize() throws IOException {
		fileSystem = new FileSystem();
		ArrayList<String> loadedCommands = fileSystem.loadCommands();
		server = new Server(12345, 12346, connectedDevices, chatRoomList);
		for(String cmd : loadedCommands)
			commandsList.getItems().add(cmd);
    }
	
	public void viewScreen(ActionEvent e) throws IOException{
		if(server !=null){
			ObservableList<String> selectedDevices;
			selectedDevices = connectedDevices.getSelectionModel().getSelectedItems();
			if(selectedDevices.size() != 1){
				alertMsg("Notification", "Instructor Request error", "One device must be select");
				return;
			}
			server.screenShareRequest(selectedDevices.get(0));
		}
	}
	
	public void startServer(ActionEvent e) throws IOException{
		if(server == null)
			server = new Server(12345, 12346, connectedDevices, chatRoomList);
		connectedDevices.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		//start cmd server
		Runnable task1 = () -> server.startCmdServer();
		new Thread(task1).start();
		
		//start file server
		Runnable task2 = () -> server.startFileServer();
		new Thread(task2).start();
		
		server.listenForRequests();
		
		alertMsg("Server Status", "Server Notification", "Server running and waiting for connections");
		serverStateLbl.setText("Running");
		serverIpLbl.setText(server.getIpAddr());
		commandsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		Stage stage = (Stage) AP.getScene().getWindow();
		
		server.checkForDevices();
		
		stage.setOnCloseRequest(event -> {
			try {
				server.stopServers();
				server = null;
			} catch (Exception err) {}
	        Platform.exit();
	        System.exit(0);
	    });
		
		startServerBtn.setDisable(true);
		stopServerBtn.setDisable(false);
	}
	
	public void sendMsg(ActionEvent e){
		if(!msgTxt.getText().equals("") && server !=null){
			String msg = msgTxt.getText();
			ObservableList<String> selectedDevices;
			selectedDevices = connectedDevices.getSelectionModel().getSelectedItems();
			if(selectedDevices.size() == 0)
				return;
			chatRoomList.getItems().add("TA MSG: "+msg);
			server.bcMsg(msg, selectedDevices);
		}
	}
	
	public void clearChatRoom(ActionEvent e){
		chatRoomList.getItems().clear();
	}
	
	public void sendNotification(ActionEvent e){
		if(!msgTxt.getText().equals("") && server !=null){
			String notification = msgTxt.getText();
			ObservableList<String> selectedDevices;
			selectedDevices = connectedDevices.getSelectionModel().getSelectedItems();
			if(selectedDevices.size() == 0)
				return;
			chatRoomList.getItems().add("TA Notification: "+notification);
			server.bcNotification(notification, selectedDevices);
		}
	}
	
	public void stopServer(ActionEvent e) throws IOException{
		if(server != null){
			server.stopDeciveChecking();
			server.stopServers();
			startServerBtn.setDisable(false);
			stopServerBtn.setDisable(true);
			server = null;
		}
		serverStateLbl.setText("Offline");
		serverIpLbl.setText("0.0.0.0");
	}
	
	
	public void broadcastFile(ActionEvent e){
		ObservableList<String> selectedDevices;
		selectedDevices = connectedDevices.getSelectionModel().getSelectedItems();
		System.out.println(selectedDevices.size());
		if(server != null && selectedDevices.size()!=0 && file != null)
			server.bcFile(selectedDevices, file);
	}
	
	public void broadcastCommand(ActionEvent e){
		ObservableList<String> selectedDevices;
		selectedDevices = connectedDevices.getSelectionModel().getSelectedItems();
		if(server != null && selectedDevices.size()!=0){
			String cmd = cmdTxt.getText();
			if(!cmd.trim().equals("")){
				server.bcCmd(cmd, selectedDevices);
			}
		}
	}
	
	public void selectFile(ActionEvent e){
		Stage stage = (Stage) AP.getScene().getWindow();
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select File");
		File file = fileChooser.showOpenDialog(stage);
		if(file != null){
			this.file = file;
			filePathTxt.setText(file.getAbsolutePath());
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
	
	public void addCommand(ActionEvent e) throws IOException{
		String command = addCommandText.getText();
		if(!command.equals("")){
			commandsList.getItems().add(command);
			//save at file
			fileSystem.addCommand(command);
		}
	}
	
	public void removeSelectedCommands(ActionEvent e) throws IOException{
		ObservableList<String> selectedCommands;
		selectedCommands = commandsList.getSelectionModel().getSelectedItems();
		commandsList.getItems().removeAll(selectedCommands);
		selectedCommands = commandsList.getItems();
		fileSystem.clearAllCommands();
		fileSystem.addAllCommands(selectedCommands);
	}
	
	public void executeSelectedCommands(ActionEvent e){
		ObservableList<String> selectedDevices;
		selectedDevices = connectedDevices.getSelectionModel().getSelectedItems();
		ObservableList<String> selectedCommands;
		selectedCommands = commandsList.getSelectionModel().getSelectedItems();
		if(server != null && selectedDevices.size()!=0 && selectedCommands.size()!=0){
			String cmd = "";
			for(int i = 0 ; i < selectedCommands.size() ; i ++){
				if(i<selectedCommands.size()-1){
					cmd+=selectedCommands.get(i);
				}else{
					cmd+=selectedCommands.get(i)+" && ";
				}
			}
			server.bcCmd(cmd, selectedDevices);
		}
	}
}
