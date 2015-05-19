import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for reliably receiving packets
 * from the client and writing that data to a file.
 * @author gledford
 *
 */
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

	/**
	 * Receives the packets from the client and sends the acks back to the client
	 * @param emulatorAddress
	 * @param receivePort
	 * @param sendPort
	 * @return
	 * @throws IOException
	 */
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
			byte[] buffer = new byte[125]; //125 is size of packet with data
			DatagramPacket receivePacket = new DatagramPacket(buffer,
					buffer.length);
			receiveSocket.receive(receivePacket);

			packet p = s.deserializePacket(buffer);

			arrivalLogger.log(p.getSeqNum());
			p.printContents();
			
			//If we have received the expected sequence number
			if (expectedSequenceNumber == p.getSeqNum()) {
				expectedSequenceNumber = p.getSeqNum() + 1;
				
				//% 8 the sequence number
				if (expectedSequenceNumber > 7) {
					expectedSequenceNumber = 0;
				}
				packetList.add(p);

				//If EOT packet, send EOT ack
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
			}
			//Else we received an out of order packet, send the expected sequence number ack back
			else {
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

		//Close the logger and sockets
		arrivalLogger.endLog();
		sendSocket.close();
		receiveSocket.close();
		
		return packetList;
	}

	/**
	 * Writes all of the data received from the client to the file
	 * @param packetList
	 * @param fileName
	 * @throws IOException
	 */
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