import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public class client {

	private static final int WINDOW_SIZE = 7;

	public static void main(String[] args) throws IOException {
		if (args.length == 4) {
			String emulatorAddress = args[0];
			int sendPort = Integer.parseInt(args[1]);
			int receivePort = Integer.parseInt(args[2]);
			String fileName = args[3];

			File f = new File(fileName);
			if (f.exists() && !f.isDirectory()) {
				List<packet> packets = convertFileToPackets(fileName);
				sendPacketsToEmulator(packets, emulatorAddress, sendPort,
						receivePort);
			} else {
				System.out.println("Invalid file " + fileName);
			}
		} else {
			System.out
					.println("Usage: java client <emulatorAddress> <sendToEmulatorPort> <receiveFromEmulatorPort> <filename>");
		}
	}

	private static List<packet> convertFileToPackets(String fileName)
			throws IOException {
		List<packet> packets = new ArrayList<packet>();
		File f = new File(fileName);
		FileInputStream fileInputStream = new FileInputStream(f);

		byte[] file = new byte[fileInputStream.available()];
		fileInputStream.read(file);
		fileInputStream.close();

		int sequenceNumber = 0;
		for (int i = 0; i < file.length;) {
			byte[] temp = new byte[30];
			int size = 0;
			for (int j = 0; j < 30; j++) {
				if ((i + j) < file.length) {
					temp[j] = file[i + j];
					size++;
				}
			}

			if (size == 30) {
				packets.add(new packet(1, sequenceNumber, size,
						new String(temp)));
			} else {
				for (int j = size; j < 30; j++) {
					temp[j] = Byte.MAX_VALUE;
				}
				packets.add(new packet(1, sequenceNumber, size,
						new String(temp)));
			}

			i += 30;

			if (sequenceNumber < 7) {
				sequenceNumber++;
			} else {
				sequenceNumber = 0;
			}
		}

		packets.add(new packet(3, sequenceNumber, 0, "EOT"));

		return packets;
	}

	private static void sendPacketsToEmulator(List<packet> packets,
			String emulatorAddress, int sendPort, int receivePort)
			throws IOException {

		DatagramSocket emulatorReceiveSocket = new DatagramSocket();
		InetAddress emulatorIpAddress = InetAddress.getByName(emulatorAddress);
		serializer s = new serializer();
		logger seqLogger = new logger("seqnum.log");
		logger ackLogger = new logger("ack.log");

		DatagramSocket recSocket = new DatagramSocket(receivePort);
		recSocket.setSoTimeout(800);

		int packetIndex = 0;
		int baseGoBackN = 0; // base Go Back N, aka Sf
		int nextSequenceNumber = 0; // next sequence number, aka Sn
		int highestSequenceNumber = 0;

		while (true) {
			if (packetIndex < packets.size()
					&& (nextSequenceNumber - baseGoBackN != WINDOW_SIZE)) {
				byte[] bufferPack = s.serializePacket(packets.get(packetIndex));
				packets.get(packetIndex).printContents();
				DatagramPacket sendPack = new DatagramPacket(bufferPack,
						bufferPack.length, emulatorIpAddress, sendPort);
				emulatorReceiveSocket.send(sendPack);
				seqLogger.log(packets.get(packetIndex).getSeqNum());
				packetIndex++;
				nextSequenceNumber++;
			} else {
				DatagramPacket receivePacket;
				byte[] bufferRec = new byte[125];
				receivePacket = new DatagramPacket(bufferRec, bufferRec.length,
						emulatorIpAddress, receivePort);
				try {
					recSocket.receive(receivePacket);
					packet p = s.deserializePacket(bufferRec);
					p.printContents();
					ackLogger.log(p.getSeqNum());

					if (p.getSeqNum() > highestSequenceNumber) {
						highestSequenceNumber = p.getSeqNum();
						if (highestSequenceNumber == nextSequenceNumber) {
							// Move the window to the highest received ACK
							baseGoBackN = nextSequenceNumber;
						}
					}
					
					if (p.getType() == 2) {
						break;
					}
				} catch (SocketTimeoutException e) {
					packetIndex = highestSequenceNumber;
					baseGoBackN = highestSequenceNumber;
					nextSequenceNumber = highestSequenceNumber;
					continue;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		seqLogger.endLog();
		ackLogger.endLog();
		emulatorReceiveSocket.close();
		recSocket.close();
	}
}