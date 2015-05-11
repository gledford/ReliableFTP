import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class server {
	public static void main(String[] args) throws IOException {
		if (args.length == 4) {
			String emulatorAddress = args[0];
			int receivePort = Integer.parseInt(args[1]);
			int sendPort = Integer.parseInt(args[2]);
			String fileName = args[3];

			List<packet> packetList = processPackets(emulatorAddress,
					receivePort, sendPort);
			writeFile(packetList, fileName);
		} else {
			System.out
					.println("Usage: java server <emulatorAddress> <receiveFromEmulatorPort> <sendToEmulatorPort> <filename>");
		}
	}

	private static List<packet> processPackets(String emulatorAddress,
			int receivePort, int sendPort) throws IOException {

		List<packet> packetList = new ArrayList<packet>();
		DatagramSocket receiveSocket = new DatagramSocket(receivePort);
		DatagramSocket sendSocket = new DatagramSocket();
		serializer s = new serializer();
		logger arrivalLogger = new logger("arrival.log");
		int expectedSequenceNumber = 0;
		packet ack = null;

		while (true) {
			byte[] buffer = new byte[125];
			DatagramPacket receivePacket = new DatagramPacket(buffer,
					buffer.length);
			receiveSocket.receive(receivePacket);

			packet p = s.deserializePacket(buffer);

			arrivalLogger.log(p.getSeqNum());
			p.printContents();
			if (expectedSequenceNumber == p.getSeqNum()) {
				expectedSequenceNumber = p.getSeqNum() + 1;
				if (expectedSequenceNumber > 7) {
					expectedSequenceNumber = 0;
				}
				packetList.add(p);

				if (p.getType() == 3) {
					packet eotAck = new packet(2, expectedSequenceNumber, 0, p
							.getData().toUpperCase());
					try {
						sendSocket.connect(
								InetAddress.getByName(emulatorAddress),
								sendPort);
					} catch (UnknownHostException e) {
						e.printStackTrace();
					}
					try {
						byte[] data = s.serializePacket(eotAck);
						DatagramPacket output = new DatagramPacket(data,
								data.length,
								InetAddress.getByName(emulatorAddress),
								sendPort);
						sendSocket.send(output);
					} catch (IOException ex) {
						System.err.println(ex);
					}
					break;
				} else {
					ack = new packet(0, expectedSequenceNumber, 0, p.getData()
							.toUpperCase());
					try {
						byte[] data = s.serializePacket(ack);
						DatagramPacket output = new DatagramPacket(data,
								data.length,
								InetAddress.getByName(emulatorAddress),
								sendPort);
						sendSocket.send(output);
					} catch (IOException ex) {
						System.err.println(ex);
					}
				}
			} else {
				ack = new packet(0, expectedSequenceNumber, 0, packetList
						.get(packetList.size() - 1).getData().toUpperCase());
				
				try {
					byte[] data = s.serializePacket(ack);
					DatagramPacket output = new DatagramPacket(data,
							data.length,
							InetAddress.getByName(emulatorAddress),
							sendPort);
					sendSocket.send(output);
				} catch (IOException ex) {
					System.err.println(ex);
				}
			}
		}

		arrivalLogger.endLog();
		sendSocket.close();
		receiveSocket.close();

		System.out.println("Final Packet List");
		for (int index = 0; index < packetList.size(); index ++) {
			packetList.get(index).printContents();
		}
		
		return packetList;
	}

	private static void writeFile(List<packet> packetList, String fileName)
			throws IOException {
		FileWriter fileWriter = new FileWriter(fileName);
		for (int i = 0; i < packetList.size(); i++) {
			if (packetList.get(i).getType() == 1) {
				fileWriter.write(packetList.get(i).getData()
						.substring(0, packetList.get(i).getLength()));
			}
		}
		fileWriter.close();
	}
}

class senderThread extends Thread {
	private String mHostName;
	private int mPort;
	private packet mPacket;
	private serializer mSerializer;
	private DatagramSocket mSocket;

	public senderThread(String hostName, int port, packet p)
			throws SocketException {
		mHostName = hostName;
		mPort = port;
		mPacket = p;
		mSocket = new DatagramSocket();
		mSerializer = new serializer();
		try {
			mSocket.connect(InetAddress.getByName(mHostName), port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			byte[] data = mSerializer.serializePacket(mPacket);
			DatagramPacket output = new DatagramPacket(data, data.length,
					InetAddress.getByName(mHostName), mPort);
			mSocket.send(output);
			Thread.yield();
		} catch (IOException ex) {
			System.err.println(ex);
		}
		mSocket.close();
	}
}
