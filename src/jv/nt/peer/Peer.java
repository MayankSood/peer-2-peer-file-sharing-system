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
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

@SuppressWarnings("resource")
public class Peer {

	public static final String GROUP = "225.4.5.6";
	public static final int POLLING_PORT = 5555;
	public static final int SERVER_PORT = 5050;
	public static final String DELIMETER = ":--:-->";
	
	public static final String FOUND = "found";

	public static final String FILES_DIRECTORY_PATH = "D:\\FILES";
	public static final String FILES_DIRECTORY_PATH_DOWNLOADED = "D:\\FILES\\DOWNLOADED";

	private static Set<String> peers = new HashSet<String>();

	public static final Peer peer = new Peer();

	public static void main(String[] args) {

		peer.createDirectory();
		peer.startPolling();
		peer.startServer();
		
		while (true) {
			System.out.println("Enter file name : ");
			String filename = new Scanner(System.in).nextLine();
			boolean fileFound = false;
			for (String ip : peers) {
				fileFound = peer.startClient(filename, ip);
			}
			if (!fileFound) {
				System.out.println("File not found. Please check the name of the file");
			}
		}

	}

	public void createDirectory() {
		File file = new File(FILES_DIRECTORY_PATH);

		if (!file.exists()) {
			file.mkdir();
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
				File file = new File(FILES_DIRECTORY_PATH);
				for (File f : file.listFiles()) {
					if (f.isFile()) {
						String fname = f.getName();
						if (fname.equals(filename)) {
							System.out.println("Sending file :-->" + filename);
							sendFileOnSocket(server, f);
						}
					}
				}
				DataOutputStream out = new DataOutputStream(server.getOutputStream());
				out.writeUTF("No File Found");
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

	public boolean startClient(String fileName, String serverName) {
		try {
			Socket client = new Socket(serverName, SERVER_PORT);
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

		FileOutputStream foss = new FileOutputStream(FILES_DIRECTORY_PATH_DOWNLOADED+"/"+fileName);
		foss.write(arr);
		foss.flush();
		foss.close();
	}

	public void startPolling() {
		peer.startListening(peer);
		peer.startNotifying(peer);
	}

	public void startListening(final Peer peer) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				peer.startListening2();
			}
		}).start();
	}

	public void startNotifying(final Peer peer) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				peer.startNotifyin2();
			}
		}).start();
	}

	public void startListening2() {
		try {
			String group = GROUP;

			MulticastSocket s = new MulticastSocket(POLLING_PORT);
			s.joinGroup(InetAddress.getByName(group));

			boolean cond = true;
			while (cond) {
				byte buf[] = new byte[1024];
				DatagramPacket pack = new DatagramPacket(buf, buf.length);
				s.receive(pack);
				String ip = pack.getAddress().toString().replaceAll("/", "");
				if (!peers.contains(ip)) {
					peers.add(ip);
					System.out.println("New Peer added : " + ip);
				}
			}
			s.leaveGroup(InetAddress.getByName(group));
			s.close();

		} catch (Exception e) {
		}
	}

	public void startNotifyin2() {
		try {
			while (true) {
				String group = GROUP;
				int ttl = 1;

				MulticastSocket s = new MulticastSocket();

				byte buf[] = new byte[10];

				for (int i = 0; i < buf.length; i++) {
					buf[i] = (byte) i;
				}
				DatagramPacket pack = new DatagramPacket(buf, buf.length, InetAddress.getByName(group), POLLING_PORT);
				s.setTimeToLive((byte) ttl);
				s.send(pack);
				s.close();
				Thread.sleep(1000);
			}
		} catch (Exception e) {
		}
	}
}
