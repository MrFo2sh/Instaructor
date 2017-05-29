package application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;


public class FileSystem {
	
	private Scanner scanner;
	private File file;
	
	public FileSystem() throws IOException {
		file = new File("Server.config");
		file.createNewFile();
	}
	
	public void saveServerConfig(Config config) throws FileNotFoundException{
		PrintWriter writer = new PrintWriter(file);
		writer.println(config.getUsername());
		writer.println(config.getIp());
		writer.println(config.getPath());
		writer.println(config.getCmdPort());
		writer.println(config.getFilePort());
		writer.close();
	}
	
	public Config loadServerConfig() throws FileNotFoundException{
		scanner = new Scanner(file);
		if(scanner.hasNextLine()){
			try {
				String username = scanner.nextLine();
				String ip = scanner.nextLine();
				String path = scanner.nextLine();
				int cmdPort = scanner.nextInt();
				int filePort = scanner.nextInt();
				scanner.close();
				return new Config(username, ip, path, cmdPort, filePort);
			} catch (Exception e) {
				return null;
			}
		}
		scanner.close();
		return null;
	}
}
