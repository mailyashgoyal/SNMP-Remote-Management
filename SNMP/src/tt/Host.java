import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;
import java.nio.channels.*;
import java.nio.charset.*;

public class Host{
	 static SocketChannel channel=null;
	 static int hostID;
	 static String ip;
	 static int port;
	 static String configfile;
	 static String mgroupfile;
	 static int myrouterID; 
	 static int mgroup;
	 static String sourceString = "C:\\Users\\Yash\\workspace\\TCP\\src\\";
	 private static BufferedReader inputLine = null;
	 static String sentence=null;
		public void mGroupWriteFunc() throws IOException
		{
			
			  PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(sourceString+"mgroupfile+.txt", true)));
			  //FileWriter out = new FileWriter("C:\\workspace\\MCAST\\mgroupfile+.txt");
			  BufferedWriter br = new BufferedWriter(out);
			  sentence= ""+hostID+" "+mgroup+"\n";
			  br.write(sentence);
			  //sendDataToServer(channel,sentence);
			  br.close();
		}
		public void mGroupReadFunc(String input) throws IOException{
			FileReader in = new FileReader(sourceString+"mgroupfile+.txt");
			File filename = new File(sourceString+"mgroupfile+.txt");
			File tempFile = new File("myTempFile");
			BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
			BufferedReader reader = new BufferedReader(in);
			sentence="";
			while((sentence=reader.readLine()) != null)
			{
				
				    // trim newline when comparing with lineToRemove
				    
				    if(sentence.equals(input)) continue;
				    writer.write(sentence);
				
			}
			reader.close();
			writer.close();
			tempFile.delete();
			boolean successful = tempFile.renameTo(filename);
		}
		public void EstConnection(int hostID, String configfile) throws IOException
		{
			  
			    FileReader in =  new FileReader(sourceString+configfile);
			    BufferedReader reader = new BufferedReader(in);
			    String strLine="";
			    while ((strLine= reader.readLine()) != null) {
			  System.out.println(strLine);
			  String strings[] = strLine.split(" ");
			  if(Integer.parseInt(strings[0])==myrouterID)
			  {
				  System.out.println(""+strings[1]+" "+strings[2]);
				  //ClientSocket=new Socket("127.0.0.1",12347);}
				  ip=strings[1];
				  port=Integer.parseInt(strings[2]);
				  
			 //   clientSocket = new Socket(strings[1],Integer.parseInt(strings[2]));
			   }}
			   // inputLine = new BufferedReader(new InputStreamReader(System.in));
			     // os = new PrintStream(clientSocket.getOutputStream());
			      //is = new DataInputStream(clientSocket.getInputStream());
			    reader.close();
				
		}
		void REPORT() throws IOException{
			String myID=""+hostID;
			String mgroupID=""+mgroup;
			sentence="REPORT"+" "+myID+" "+mgroupID+"\n";
			//DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			System.out.println("writing");
			//outToServer.writeBytes(sentence); 
			System.out.println("writing");
			sendDataToServer(channel);
	        mGroupWriteFunc();
		}
		void SEND(String filepath) throws IOException{
//		if(new File(filepath).isFile()){
//			String input="";
			String myID=""+hostID;
			String mgroupID="";
			System.out.println("Enter mgroup iD");
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			mgroupID=reader.readLine();
			String sentence1="";
	    	sentence="SEND"+" "+myID+" "+mgroupID+" ";
//			FileReader in =  new FileReader(filepath);
//		    BufferedReader reader = new BufferedReader(in);
////		    while ((input = reader.readLine()) != null) {
////		    	System.out.println("Loop Sentence is:"+sentence);
////		    	sentence+=input;
////		    	System.out.println("Loop Sentence out:"+sentence);
////		    }
			System.out.println("Enter data to be sent");

			sentence1=reader.readLine();
			sentence+=sentence1;
		    //DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			System.out.println("Sentence is:"+sentence);
		    //outToServer.writeBytes(sentence1+"\n");
			sendDataToServer(channel);
		    //reader.close();
		
		}
		void JOIN() throws IOException{
			File check= new File(sourceString+"mgroupfile+.txt");
			
			if(!check.exists()){
		    check.createNewFile();
			}
			FileReader in = new FileReader(sourceString+"mgroupfile+.txt");
			BufferedReader reader = new BufferedReader(in);
			sentence="";
			String input=""+hostID+" "+mgroup+"\n";
			boolean value=false;
			while((sentence=reader.readLine()) != null)
			{
				if(sentence.equals(input))
					value=true;
			}
			if(value==false)
			REPORT();
				
			reader.close();
		}
		void LEAVE() throws IOException{
			sentence=""+hostID+" "+mgroup+"\n";
			sendDataToServer(channel);
			//outToServer.writeBytes(sentence);
			mGroupReadFunc(sentence);
			
		}
		void LIST() throws IOException{
			FileReader in = new FileReader(sourceString+"mgroupfile+.txt");
			BufferedReader reader = new BufferedReader(in);
			sentence="";
			while((sentence=reader.readLine()) != null)
			{
				System.out.println(sentence);
			}
			reader.close();
		}
	 
	private static void readIncomingData(SocketChannel channel){

		try {
		ByteBuffer buffer = ByteBuffer.allocate( 2048 );
		buffer.clear();

		channel.read( buffer );

		// Create a StringBuffer so that we can convert the bytes to a String
		StringBuffer response = new StringBuffer();

		buffer.flip();

		// Create a CharSet that knows how to encode and decode standard text (UTF-8)
		Charset charset = Charset.forName("UTF-8");

		// Decode the buffer to a String using the CharSet and append it to our buffer
		response.append( charset.decode( buffer ) );

		// Output the response
		System.out.println( "Data read from client" + response );

		} catch (IOException e) {
		e.printStackTrace();
		}

		}
	
	
	private static void sendDataToServer(SocketChannel channel){

		try
		{
		// Create a ByteBuffer to hold 64KB that we’ll read data into; note that we do
		// not need to clear the buffer the first time, but I wanted to show how to do it
		// here because you’ll need to clear the buffer before reading more data into it the
		// next time
		ByteBuffer buffer = ByteBuffer.allocate( 9999);
		buffer.clear();

		// Read bytes from the channel into the buffer; count denotes the total number of bytes
		// read. Depending on your server you may need to continue reading until some event occurs,
		// such as is NTTP we might be looking for a line with only “.”
		// int count = channel.read( buffer );

		// Create a StringBuffer so that we can convert the bytes to a String
		//StringBuffer response = new StringBuffer();

		// Now that we’re read information into the buffer, we need to “flip” it so that we
		// can now get data out of the buffer.
		//buffer.flip();
        
		// Create a CharSet that knows how to encode and decode standard text (UTF-8)
		Charset charset = Charset.forName("UTF-8");

		// Decode the buffer to a String using the CharSet and append it to our buffer
		//response.append( charset.decode( buffer ) );

		// Output the response
		//System.out.println( “Response: ” + response );
		byte[] byteArray = sentence.getBytes();
		buffer.put(byteArray);
		// Now let’s write something to the channel. We need to use the CharSet to properly
		// encode the String and then call the channel’s write() method
		System.out.println("Data is:"+sentence);
		//channel.write( charset.encode(""+Data) );
		if(channel==null)
		System.out.println("Channel is null");
		else channel.write(buffer);
		// Close the channel
		//channel.close();
		}
		catch( Exception e )
		{
		e.printStackTrace();
		}
		}
	
  public static void main(String args[]) throws Exception
  {
  InetSocketAddress ISA=null;
  SocketChannel clientNIO=null;
  Selector selector=null;
  SelectionKey key=null;
  Host Client = new Host();
  int i;
  //int port=5000;
  hostID=Integer.parseInt(args[0]);
  configfile=args[1];
  System.out.println(""+configfile);
  myrouterID=Integer.parseInt(args[2]);
  Client.EstConnection(myrouterID, configfile);
  if(args.length==4){
    mgroup=Integer.parseInt(args[3]);
	Client.REPORT();
	}
  
  try{
     clientNIO=SocketChannel.open();
     clientNIO.configureBlocking(false);     
     }catch(IOException e)
        {System.out.println(e.getMessage());}

 try{
    InetAddress addr=InetAddress.getByName(ip);
    System.out.println("ip"+ip+" "+"port"+port);
    ISA=new InetSocketAddress("127.0.0.1",port);           
    }catch(UnknownHostException e)
      {System.out.println(e.getMessage());}
     
 try{
    clientNIO.connect(ISA); 
    }catch(IOException e)
      {System.out.println(e.getMessage());}
    
  try{
     selector = Selector.open(); 
     }catch(IOException e)
          {System.out.println(e.getMessage());}
     
  try{     
     SelectionKey clientKey=clientNIO.register(selector,SelectionKey.OP_CONNECT); 
     }catch(Exception e)        
        {System.out.println(e.getMessage());}
           
  try{       
	 // while (!closed) {
  	do{
		
		System.out.println("Please Select the Operation you want to perform");
		System.out.println("1-> for SEND");
		System.out.println("2-> for JOIN");
		System.out.println("3-> for LEAVE");
		System.out.println("4-> for LIST");
		System.out.println("5-> to  EXIT");
		inputLine = new BufferedReader(new InputStreamReader(System.in));
		i= Integer.parseInt(inputLine.readLine());
		if(i==1){
			System.out.println("please enter the file path");
		    String file = inputLine.readLine();
		    Client.SEND(file);
		    }
		if(i==2){
			System.out.println("please enter the ID of the group you want to JOIN");
			mgroup= Integer.parseInt(inputLine.readLine());
			Client.JOIN();
			
		}
		if(i==3){
			System.out.println("please enter the ID of the group you want to LEAVE");
			mgroup= Integer.parseInt(inputLine.readLine());
			Client.LEAVE();
		}
		if(i==4){
			Client.LIST();
		}
		//System.out.println("Remote port"+clientSocket.getPort());
		//System.out.println("local port"+clientSocket.getLocalPort());
		//BufferedReader m = 
			//	new BufferedReader(new
				//InputStreamReader(clientSocket.getInputStream())); 
		//}while(i!=5);
      while(selector.select(1000)>0)
             {                          
             Set keys=selector.selectedKeys();
             Iterator iter=keys.iterator();
             while(iter.hasNext())
                  {
                  key=(SelectionKey)iter.next();
                  iter.remove();
                  channel=(SocketChannel)key.channel();
                  if ((key.readyOps() & SelectionKey.OP_CONNECT)
                          == SelectionKey.OP_CONNECT) {
                	  System.out.println("waiting"); 
                	  while(! channel.finishConnect() ){
                		       
                		}
                	  System.out.println("wait complete"); 
                      if(channel.isConnectionPending())channel.finishConnect();
                      ByteBuffer serverBuf = null;
                      
                      System.out.println("Connected...");
                      }
                  if ((key.readyOps() & SelectionKey.OP_WRITE)
                          == SelectionKey.OP_WRITE) { 
                	  sendDataToServer(channel);
                     
                      }
                  if ((key.readyOps() & SelectionKey.OP_READ)
                          == SelectionKey.OP_READ) {
                  readIncomingData(channel);
                     
                     }
                   }  
              }
  	}while(i!=5);
     }catch(IOException e)
        {System.out.println(e.getMessage());
               try{
                  key.channel().close();key.cancel();
                  }catch(Exception ex)
                     {System.out.println(e.getMessage());}}

  }}
