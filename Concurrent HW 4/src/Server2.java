import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Server2 {

	private static int MAX_BOOKS;	//Books included with input
	private static int TCP_PORT;	//TCP port included with instructions
	static ArrayList<String> crashQ = new ArrayList<String>();
	public static class ServerThread extends Thread {

		private ConcurrentHashMap<Integer, Integer> store;
		private Socket tcp_socket = null;
		private int commandsServed = 0; 
		String crashCommand;

		public ServerThread(ConcurrentHashMap<Integer, Integer> inBS, Socket inS) 
		{
			store = inBS;
			tcp_socket = inS;
		}


		public void run() {
			try {
				Scanner in = new Scanner(tcp_socket.getInputStream());	//Get input from client
				OutputStream os = tcp_socket.getOutputStream();				//Get client output
				PrintWriter out = new PrintWriter(os);
				String crashC[] = crashQ.get(0).split(" ");
				int crashAfter = Integer.parseInt(crashC[1]);
				int timeout = Integer.parseInt(crashC[2]);
				try 
				{
					int client_num = in.nextInt();					//Take client command
					int book_num = in.nextInt();					
					String cmd_returned = in.next();				

					if (cmd_returned.equals("reserve")) 
					{
						// reserve a book_num
						if (book_num >= 1 && book_num <= MAX_BOOKS && store.putIfAbsent(book_num, client_num) == null) 
						{
							out.println(client_num + " " + book_num);
						} 
						else 
						{
							out.println("fail " + client_num + " " + book_num);
						}
					}
					else if (cmd_returned.equals("return")) 
					{
						// return a book_num
						if (book_num >= 1 && book_num <= MAX_BOOKS && store.remove(book_num, client_num)) 
						{
							out.println("free " + client_num + " " + book_num);
						} 
						else 
						{
							out.println("fail " + client_num + " " + book_num);
						}
					} 
					else 
					{
						out.println("improper command: " + cmd_returned);
					}
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
				}
				out.flush();
				UpdateAll(); //Update all the other servers' library
				commandsServed++;
				if(commandsServed == crashAfter)
				{
					crashQ.remove(0);
					Thread.sleep((long)timeout);
				 	Recover();
				}
				if (tcp_socket != null) 
				{
					tcp_socket.close();
				} 
				out.close();
				in.close();
			} 
			catch (Exception e) 
			{
				// do nothing
				e.printStackTrace();
			}

		}

	}

	public static void main(String[] args) {
		final ConcurrentHashMap<Integer, Integer> book_store;
		book_store = new ConcurrentHashMap<Integer, Integer>();
		ArrayList<String> serverProx = new ArrayList<String>();
		Scanner in = new Scanner(System.in);	//Setup server input
		int serverID = in.nextInt();			//Get Server ID
		int totalServers = in.nextInt();		//Get number of servers
		MAX_BOOKS = in.nextInt();				//Get number of books
		int setupServers = 0; 
		////InetAddress address;
		while(setupServers < totalServers)
		{
			serverProx.add(in.nextLine());
			//address = InetAddress.getByName(temp.substring(0,temp.indexOf(":")).trim());
		
			TCP_PORT = in.nextInt();//TCP_PORT = Integer.parseInt(temp.substring(temp.indexOf(":")+1));
													//in.close(); Moved to bottom
			Thread tcpThread = new Thread(new Runnable() { //Maybe put all Threads in ThreadPool to start all later?
				public void run() {						   // Still need to have them in order, maybe queue for connecting later
					try {
						ServerSocket collector = new ServerSocket(TCP_PORT);
						Socket sock;
						while ((sock = collector.accept()) != null) {
							System.out.println("New TCP connection from " + sock.getInetAddress());
							Thread t = new ServerThread(book_store, sock);
							t.start();
						}
						collector.close();
					} catch (Exception e) { }
				}
			});
			setupServers++;	//Increment number of servers setup
			//TODO:Add to threadpool
		}
		while(in.hasNext()) //Get crash instructions, may have multiple lines
		{		
			crashQ.add(in.nextLine());//put crash instruction into queue, access the instruction later
		}
		//TODO:Executor service call to run threadpool
		in.close();
	}
	//TODO:
	public static void Recover() //What a crash thread executes to sync it's own data back. Synchronized?
	{	
		//If a library is currently updating other servers, wait(); 	
		//Try to avoid copying defunct info.
		//Try and connect to another server, if timeout longer than 1000, try next
		//If connect, copy library
		//reset commandsServed
		return;
	}
	
	//TODO:
	public synchronized static void UpdateAll()
	{	
		//Method for updating all the other servers
		//Lamport mutex goes here: logical clock, queue for update requests, handling acks, etc.
		return;
	}

}
