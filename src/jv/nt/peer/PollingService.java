package jv.nt.peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public class PollingService {

	private Peer peer;

	public PollingService(Peer peer) {
		this.peer = peer;
	}

	public void startPolling() {
		startListening(peer);
		startNotifying(peer);
	}

	private void startListening(final Peer peer) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				startListening2();
			}
		}).start();
	}

	private void startNotifying(final Peer peer) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				startNotifyin2();
			}
		}).start();
	}

	private void startListening2() {
		try {
			String group = Peer.GROUP;

			MulticastSocket s = new MulticastSocket(Peer.POLLING_PORT);
			s.joinGroup(InetAddress.getByName(group));

			boolean cond = true;
			while (cond) {
				byte buf[] = new byte[1024];
				DatagramPacket pack = new DatagramPacket(buf, buf.length);
				s.receive(pack);
				String[] data = new String(pack.getData()).split(Peer.DELIMETER);
				String ip = pack.getAddress().toString().replaceAll("/", "");
				if (data[0].equals("LEAVING")) {
					peer.getConnectedIps().remove(data[1]);
					System.out.println("Peer with peer-id : "+data[1]+" removed");
				} else {
					if (peer.getConnectedIps().get(data[1]) == null) {
						peer.getConnectedIps().put(data[1], ip+Peer.DELIMETER+data[0]);
						System.out.println("New Peer added : " + ip+":"+data[0]);
					}
				}
			}
			s.leaveGroup(InetAddress.getByName(group));
			s.close();

		} catch (Exception e) {
		}
	}

	private void startNotifyin2() {
		try {
			while (true) {
				sendNotification(true);
				Thread.sleep(1000);
			}
		} catch (Exception e) {
		}
	}

	public void sendNotification(boolean isJoined) throws IOException, UnknownHostException {
		String group = Peer.GROUP;
		int ttl = 1;

		MulticastSocket s = new MulticastSocket();

		byte buf[] = new byte[10];

		for (int i = 0; i < buf.length; i++) {
			buf[i] = (byte) i;
		}
		DatagramPacket pack = new DatagramPacket(buf, buf.length, InetAddress.getByName(group), Peer.POLLING_PORT);
		s.setTimeToLive((byte) ttl);
		if (isJoined) {
			pack.setData(String.valueOf(Peer.SERVER_PORT+Peer.DELIMETER+Peer.PEER_ID).getBytes());
		} else {
			pack.setData(String.valueOf("LEAVING"+Peer.DELIMETER+Peer.PEER_ID).getBytes());
		}
		s.send(pack);
		s.close();
	}

}
