// Author: Maxwell Young
// Date: May 6, 2015


public class packet implements java.io.Serializable
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int type;      // 0 if an ACK, 1 if a data packet, 2 if EOT Server-Client, 3 if EOT Client-Server
	private int seqnum;    // sequence number {0,1,2,3,4,5,6,7}
	private int length;    // number of characters carried in data field 
	private String data;   // should be 0 for ACK packets  
    
    // constructor
	public packet(int t, int s, int l, String d){
	    type = t;
	    seqnum = s;
	    length = l;
	    data = d;
	}
	
	public int getType(){
	    return type;
	}
	
	public int getSeqNum(){
	     return seqnum;   
	}
	
	public int getLength(){
	     return length;   
	}
	
	public String getData(){
	     return data;   
	}
	
    // print function for testing
	public void printContents(){
	     System.out.println("type: " + type + "  seqnum: " + seqnum + " length: " + length);
	     System.out.println("data: " + data);
	     System.out.println();
	}
	
} // end of class
