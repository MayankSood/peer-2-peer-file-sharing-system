package jv.nt.peer;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

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
				String ip = pack.getAddress().toString().replaceAll("/", "");
				if (!peer.getConnectedIps().contains(ip)) {
					peer.getConnectedIps().add(ip);
					System.out.println("New Peer added : " + ip);
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
				String group = Peer.GROUP;
				int ttl = 1;

				MulticastSocket s = new MulticastSocket();

				byte buf[] = new byte[10];

				for (int i = 0; i < buf.length; i++) {
					buf[i] = (byte) i;
				}
				DatagramPacket pack = new DatagramPacket(buf, buf.length, InetAddress.getByName(group),
						Peer.POLLING_PORT);
				s.setTimeToLive((byte) ttl);
				s.send(pack);
				s.close();
				Thread.sleep(1000);
			}
		} catch (Exception e) {
		}
	}

}
