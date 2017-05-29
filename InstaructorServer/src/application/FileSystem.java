package application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import javafx.collections.ObservableList;

public class FileSystem {
	
	private Scanner scanner;
	private File file;
	
	public FileSystem() throws IOException {
		file = new File("commands.txt");
		file.createNewFile();
	}
	public ArrayList<String> loadCommands() throws FileNotFoundException{
		ArrayList<String> commands = new ArrayList<>();
		scanner = new Scanner(file);
		while(scanner.hasNextLine()){
			String command = scanner.nextLine();
			if(!command.equals(""))
				commands.add(command);
		}
		scanner.close();
		return commands;
	}
	
	public void addCommand(String command) throws IOException{
		FileWriter writer = new FileWriter(file, true);
		writer.append(command+"\n");
		writer.close();
	}
	
	public void addAllCommands(ObservableList<String> commands) throws IOException{
		FileWriter writer = new FileWriter(file, true);
		for(String command : commands){
			writer.append(command+"\n");
		}
		writer.close();
	}
	
	public void clearAllCommands() throws FileNotFoundException{ 
		PrintWriter writer = new PrintWriter(file);
		writer.print("");
		writer.close();
	}
	
}
