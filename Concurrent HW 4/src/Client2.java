import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;
// import java.lang.*;

public class Client2 {

	public static void main(String[] args)
	{
		Scanner in = new Scanner(System.in);		//Setup standard input for client
		try {
			ArrayList<String> serverProx = new ArrayList<String>();
			int setupServers = 0;
			//First line of input is different: client-id ci (whitespace) n
			//Store client id, and store number of servers into a data structure, list of proximity
			int client_num = Integer.parseInt(in.next().substring(1));		//Change to take client ID
			int numServers = in.nextInt();		//Take number of servers
			in.nextLine();
			while(setupServers < numServers)
			{
				serverProx.add(in.nextLine().trim());//Address also contains port number -> IP:Port#. Split up later when parsed, order of proximity is always the same
				setupServers++;	//Increment number of Servers that have been "setup"
			}
			while (in.hasNextLine()) //While there is more client code, maybe should change while loop condition to while true? 
			{
				if (in.findInLine("sleep") != null) 
				{	//Execute sleep if sleep command
					int sleep = in.nextInt();
					Thread.sleep((long) (sleep));
				}
				else if (in.findInLine("b") != null) 
				{
					int book_num = in.nextInt();	//Get command client number
					int serverIndex = 0;
					boolean notConnected = true;
					String cmd_num = in.next();		//reserve or return				
					String msg = new String(client_num + " " + book_num + " " + cmd_num + "\n");	//Setup input to send to server
					Scanner reader;
					Socket client_socket = null;
					
					//TCP connection, need to adapt to iterate through list of servers
					while(notConnected)
					{	//notConnected initially true
						try
						{
							//New initialization of port_num/address:
							String a = serverProx.get(serverIndex); //First server in the que
							InetAddress address = InetAddress.getByName(a.substring(0,a.indexOf(":")).trim());
							int port_num = Integer.parseInt(a.substring(a.indexOf(":")+1).trim());
							client_socket = new Socket(address, port_num);
							System.out.println("Client port " + port_num);
							client_socket.setSoTimeout(1000);//Throws SocketTimeoutException if time longer than 100
							PrintWriter send = new PrintWriter(client_socket.getOutputStream());//Get client input and try to output to server
							reader = new Scanner(client_socket.getInputStream());//Setup input stream for server to write to 
							send.println(msg);	//Send server the message provided by client
							send.flush();		//Flush the stream
							notConnected=false;
							serverIndex = 0;
							
							
							String command_returned = "";
							if (reader.findInLine("free") != null) 
							{
								System.out.println(reader.findInLine("free"));
								command_returned = "";				//If input from server contains free, it succeeded. Is "free" needed? Output is only c#, b#
																		//If needs change, change command_returned=""
							} 
							else if (reader.findInLine("fail") != null) 
							{
								System.out.println(reader.findInLine("fail") + " not equal null");
								command_returned = "fail ";				//If input from server contains fail, command failed
							}
							int client_num_returned = reader.nextInt();	//Get client number
							int book_num_returned = reader.nextInt();	//Get book number

							System.out.println(command_returned + "c" + client_num_returned + " b" + book_num_returned); //Write result of command to client output stream
							//  + " : " + System.nanoTime()
							reader.close();	//Close input to client
							if (client_socket != null) client_socket.close(); //No more commands. Close socket
						}
						catch(SocketTimeoutException e) //Server chosen has crashed. Move checked to back of the queue and get new address and port num
						{		
							//headofqueue-> add to back of the queue;
							System.out.println("ahhh");
							serverIndex++; //Increment que of index
						}
					
					} //Loop back and try to establish connection
				} 
				else 
				{
					in.nextLine();	//Get next command to execute
				}
			}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			System.out.println("Malformed input.");
			System.exit(1);
		}
		in.close();
	}

}
