import java.io.FileWriter;
import java.io.IOException;

class logger {
	private FileWriter mFileWriter;
	
	public logger(String fileName) throws IOException {
		mFileWriter = new FileWriter(fileName);
	}
	
	public void log(int sequenceNumber) throws IOException {
		mFileWriter.write(sequenceNumber + "\n");
	}
	
	public void endLog() throws IOException {
		mFileWriter.close();
	}
}