package jv.nt.peer;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@SuppressWarnings("resource")
public class Peer {

	public static String GROUP;
	public static final int POLLING_PORT = 5555;
	public static int SERVER_PORT;
	public static final String DELIMETER = ":--:-->";
	
	public static final String FOUND = "found";
	public static String PEER_ID;

	private Map<String, String> peers = new HashMap<String, String>();

	public static final Peer peer = new Peer();

	public static void main(String[] args) {

		init(args);
		
		new PollingService(peer).startPolling();
		peer.startServer();
		
		while (true) {
			System.out.println("Enter file name : ");
			String filename = new Scanner(System.in).nextLine();
			boolean fileFound = false;
			fileFound = getFile(filename, fileFound);
			if (!fileFound) {
				System.out.println("File not found. Please check the name of the file");
			}
		}

	}

	private static boolean getFile(String filename, boolean fileFound) {
		for (String ip : peer.getConnectedIps().values()) {
			if (SERVER_PORT != Integer.parseInt(ip.split(DELIMETER)[1])) {
				fileFound = peer.startClient(filename, ip.split(DELIMETER)[0], Integer.parseInt(ip.split(DELIMETER)[1]));
				if (fileFound == true) {
					return true;
				}
			}
		}
		return false;
	}

	private static void init(String[] args) {
		try {
			GROUP = args[0];
			SERVER_PORT = Integer.parseInt(args[1]);
			PEER_ID = args[2];
		} catch (Exception e) {
			System.out.println("Please enter the variables as GROUP_ID PORT PEER_ID");
			System.exit(0);
		}
	}

	public void startServer() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				startSocketServer2();
			}
		}).start();
	}

	public void startSocketServer2() {
		try {
			ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
			serverSocket.setSoTimeout(0);
			while (true) {
				Socket server = serverSocket.accept();
				DataInputStream in = new DataInputStream(server.getInputStream());
				String filename = in.readUTF().split(DELIMETER)[1];
				//File file = new File(Peer.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
				File file = new File(".");
				boolean fileFound = false;
				for (File f : file.listFiles()) {
					if (f.isFile()) {
						String fname = f.getName();
						if (fname.equals(filename)) {
							fileFound = true;
							System.out.println("Sending file :-->" + filename);
							sendFileOnSocket(server, f);
						}
					}
				}
				if (!fileFound) {
					DataOutputStream out = new DataOutputStream(server.getOutputStream());
					out.writeUTF("No File Found");
				}
				server.close();
			}
		} catch (Exception s) {
		}
	}

	private void sendFileOnSocket(Socket server, File f) throws IOException {
		DataOutputStream out = new DataOutputStream(server.getOutputStream());
		byte[] arr = getBytesFromFile(f);
		out.writeUTF(FOUND);
		out.writeInt(arr.length);
		out.write(arr, 0, arr.length);
		out.write(arr);
	}

	public byte[] getBytesFromFile(File f) {
		byte[] bFile = new byte[(int) f.length()];

		FileInputStream fileInputStream = null;
		try {
			// convert file into array of bytes
			fileInputStream = new FileInputStream(f);
			fileInputStream.read(bFile);
			fileInputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return bFile;
	}

	public boolean startClient(String fileName, String serverName, int serverPort) {
		try {
			Socket client = new Socket(serverName, serverPort);
			OutputStream outToServer = client.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);
			out.writeUTF("fileName" + DELIMETER + fileName);
			InputStream inFromServer = client.getInputStream();
			
			DataInputStream in = new DataInputStream(inFromServer);
			if (in.readUTF().equals(FOUND)) {
				System.out.println("File found");
				writeFileToDisk(fileName, client, in);
				client.close();
				return true;
			}
			
			client.close();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return false;
	}

	private void writeFileToDisk(String fileName, Socket client, DataInputStream in) throws IOException,
			FileNotFoundException {
		byte[] arr = new byte[in.readInt()];
		DataInputStream i = new DataInputStream(new BufferedInputStream(client.getInputStream()));

		i.read(arr);

		FileOutputStream foss = new FileOutputStream("download-"+fileName);
		foss.write(arr);
		foss.flush();
		foss.close();
	}
	
	public Map<String, String> getConnectedIps() {
		return peers;
	}

}
