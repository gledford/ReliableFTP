import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * This class is responsible for serializing and deserializing packets
 * @author gledford
 *
 */
public class serializer {
	
	public serializer() {
	}

	public byte [] serializePacket(packet p) throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ObjectOutputStream o = new ObjectOutputStream(b);
		o.writeObject(p);
		o.close();

		byte[] bufferPacket = b.toByteArray();
		b.close();
		
		return bufferPacket;
	}
	
	public packet deserializePacket(byte[] buffer) throws IOException {
		packet p = null;
        ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(buffer));
		try {
			p = (packet) iStream.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
        iStream.close();
        
        return p;
	}
}
