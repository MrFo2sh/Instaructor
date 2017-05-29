package InstructorSShare;

import javax.swing.*;

import java.awt.*;

import java.awt.event.*;

import java.io.DataInputStream;

import java.io.DataOutputStream;

import java.io.IOException;

import java.net.Socket;

class Authenticate{
	private Socket cSocket = null;
	DataOutputStream psswrchk = null;
	DataInputStream verification = null;
	String verify = "";
	String width = "", height = "";
	private String password;

	Authenticate(Socket cSocket, String password) {
		this.password = password;
		this.cSocket = cSocket;
		try {
			psswrchk = new DataOutputStream(cSocket.getOutputStream());
			verification = new DataInputStream(cSocket.getInputStream());
			psswrchk.writeUTF(password);
			verify = verification.readUTF();

		} catch (IOException e) {
			e.printStackTrace();
		}

		if (verify.equals("valid")) {
			try {
				width = verification.readUTF();
				height = verification.readUTF();

			} catch (IOException e) {
				e.printStackTrace();
			}
			CreateFrame abc = new CreateFrame(cSocket, width, height, password);
		} else {
			System.out.println("enter the valid password");
		}
	}
}
