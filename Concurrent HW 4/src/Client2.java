import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Pattern;
// import java.lang.*;

public class Client {

	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);		//Setup standard input for client
		try {
			//int setupServers, numServers;
			//First line of input is different: client-id ci (whitespace) n
			//Store client id, and store number of servers into a data structure, list of proximity
			int client_num = in.nextInt();			//Change to take client ID
			//int numServers = in.nextInt();		//Take number of servers
			//String temp = in.nextLine();				//Move to next line, get ready to get all the InetAddresses
			//while(setupServers!=numServers){
			//temp = in.nextLine();
			InetAddress address = InetAddress.getByName(in.nextLine().trim()); //Address also contains port number -> IP:Port#. Need to split them up
			//Proposed new address/port code: push temp to queue as whole String. Every iteration of port/address will parse the head of queue for the address and the port number
			//push temp to back of queue					
			//setupServers++;						//Increment number of Servers that have been "setup"
			//}
			while (in.hasNextLine()) {				//While there is more client code, maybe should change while loop condition to while true?
				if (in.findInLine("sleep") != null) {	//Execute sleep if sleep command
					int sleep = in.nextInt();
					Thread.sleep((long) (sleep));
				} else if (in.findInLine("b") != null) {
					int book_num = in.nextInt();	//Get command client number
					String cmd_num = in.next();		//reserve or return
					int port_num = in.nextInt();	//Line not needed for HW4, already found in setting up proximity queue. Moved this code to TCP
					
					String msg = new String(client_num + " " + book_num + " " + cmd_num + "\n");	//Setup input to send to server
					Scanner reader;
					Socket client_socket = null;
					
					if (in.next().equals("U")) {	//Only TCP connections for HW4, UDP setup not needed
						DatagramSocket socket = new DatagramSocket();
						socket.setSoTimeout(2000);
						byte[] sendBuffer = msg.getBytes();
						byte[] receiveBuffer = new byte[256];
						
						DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, port_num);
						DatagramPacket recvPacket = new DatagramPacket(receiveBuffer, receiveBuffer.length, address, port_num);
						
						socket.send(sendPacket);
						socket.receive(recvPacket);
						String received = new String(recvPacket.getData(), 0, recvPacket.getLength());
						reader = new Scanner(received);

						socket.close();
						
					} else {//TCP connection, need to adapt to iterate through list of servers
						//while(notConnected){	//notConnected initially true
						//try{
						//New initialization of port_num/address:
						//InetAddress address = InetAddress.getByName(headofqueue.substring(0,temp.indexOf(":")).trim());
						//int port_num = Integer.parseInt(headofqueue.substring(temp.indexOf(":")+1).trim());
						client_socket = new Socket(address, port_num);
						client_socket.setSoTimeout(1000);//Throws SocketTimeoutException if time longer than 1000
						PrintWriter send = new PrintWriter(client_socket.getOutputStream());//Get client input and try to output to server
						reader = new Scanner(client_socket.getInputStream());//Setup input stream for server to write to 

						send.println(msg);	//Send server the message provided by client
						send.flush();		//Flush the stream
						//notConnected=false;
						//}
						//catch(SocketTimeoutException e){		//Server chosen has crashed. Move checked to back of the queue and get new address and port num
						//headofqueue-> add to back of the queue;}
						//
						//} Loop back and try to establish connection
					}
					String command_returned = "";
					if (reader.findInLine("free") != null) {
						command_returned = "free ";				//If input from server contains free, it succeeded. Is "free" needed? Output is only c#, b#
																//If needs change, change command_returned=""
					} else if (reader.findInLine("fail") != null) {
						command_returned = "fail ";				//If input from server contains fail, command failed
					}
					int client_num_returned = reader.nextInt();	//Get client number
					int book_num_returned = reader.nextInt();	//Get book number

					System.out.println(command_returned + "c" + client_num_returned + " b" + book_num_returned); //Write result of command to client output stream
					//  + " : " + System.nanoTime()
					reader.close();	//Close input to client
					if (client_socket != null) client_socket.close(); //No more commands. Close socket
				} else {
					in.nextLine();	//Get next command to execute
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Malformed input.");
			System.exit(1);
		}
	}

}