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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Server2 {

	private static int MAX_BOOKS;	//Books included with input
	private static int TCP_PORT;	//TCP port included with instructions
	private static int serverID;
	private static AtomicInteger commandsServed = new AtomicInteger();
	final static ConcurrentHashMap<Integer, Integer> store = new ConcurrentHashMap<Integer, Integer>();;
	final static ArrayList<String> serverProx = new ArrayList<String>(); //List of other servers
	
	public static class ServerThread extends Thread {

		private static int requests[];
		private Socket tcp_socket = null;
		private static DirectClock clock;

		public ServerThread(Socket inS, int[] req, DirectClock v) 
		{
			requests = req;
			tcp_socket = inS;
			clock = v;
		}
		//TODO: THE ONLY THING WE HAVE LEFT TO DO IS DETERMINE WHERE SERVERS ARE GOING TO TAKE IN DATA/ACKS
		public synchronized static void UpdateAll(String change)
		{	//requestCS
			clock.tick();									//Increment clock
			requests[serverID] = clock.getValue(serverID);	//Add own timestamp to the request queue
			//broadcastMsg("request", requests[serverID]);	//Send request message, activating the other server threads' listeners
			while(!okayCS()){								//If update is not the earliest
				checkForMessage();						//Check for message
			}
			//Send changes to other servers
			//broadcastMsg("change", change)
			
			//releaseCS
			requests[serverID] = -1;
			
			//broadcastMsg("release", clock.getValue(serverID));
			//Lamport mutex goes here: logical clock, queue for update requests, handling acks, etc.
			//THe server will push the state of its bookstore to other servers
			//Server will communicate changes by forwarding the client's command to the other servers
			return;
		}
		public static void broadcastMessage(String change, int id)
		{
			
		}
		public static void checkForMessage()
		{
			
		}
		public static boolean okayCS(){
			for(int j = 0; j < serverID; j++){
				if(isGreater(requests[serverID],serverID,requests[j],j))
					return false;
				if(isGreater(requests[serverID],serverID, clock.getValue(j),j))
					return false;
				
			}
			return true;
		}
		public static boolean isGreater(int entry1, int pid1, int entry2, int pid2)
		{
			if(entry2==-1) return false;
			return((entry1>entry2)||((entry1==entry2)&& (pid1>pid2)));
		}
		
		
		public void run() {
			try {
				Scanner in = new Scanner(tcp_socket.getInputStream());	//Get input from client
				OutputStream os = tcp_socket.getOutputStream();				//Get client output
				PrintWriter out = new PrintWriter(os);
				
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
							UpdateAll(client_num + " " + book_num + " " + cmd_returned + "\n");
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
							UpdateAll(client_num + " " + book_num + " " + cmd_returned + "\n");
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
				commandsServed.incrementAndGet();
				tcp_socket.close();
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

	public static void Recover() //What a crash thread executes to sync it's own data back. Synchronized?
	{	
		//Try to avoid copying defunct info.
		//Try and connect to another server, if timeout longer than 1000, try next
		//Get acknoledgement from other servers
		//If connect, copy library
		commandsServed.set(0);
		return;
	}
	
	public static void main(String[] args) {
		commandsServed.set(0);;
		ConcurrentLinkedQueue<String> crashQ = new ConcurrentLinkedQueue<String>(); //List of crash commands
		Scanner in = new Scanner(System.in);	//Setup server input
		serverID = in.nextInt();			//Get Server ID
		int totalServers = in.nextInt();		//Get number of servers
		int requests[] = new int[totalServers];
		MAX_BOOKS = in.nextInt();				//Get number of books
		for(int i = 0; i < totalServers; i++)
		{
			serverProx.add(in.nextLine());
			requests[i]=-1;		//Initialize request timestamps as -1 as a flag to indicate that there is no request
		}
		while(in.hasNext()) //Get crash instructions, may have multiple lines
		{		
			crashQ.add(in.nextLine());//put crash instruction into queue, access the instruction later
		}
		in.close();
		DirectClock v = new DirectClock(totalServers,serverID);
		TCP_PORT = Integer.parseInt(serverProx.get(serverID - 1).substring(serverProx.get(serverID - 1).indexOf(":")+1));
		Thread tcpThread = new Thread(new Runnable() 
		{ 
			public void run() 
			{						   
				try
				{
					ServerSocket collector = new ServerSocket(TCP_PORT);
					Socket sock;
					/*
					 * HERE could be where we will check if there is input from servers because it is checking one socket, we can have another socket for server communication
					 * We could have a separate socket for server-server communication
					 * Check if there is an input from a server and apply changes to the variables here before the serverThreads are created
					 * We should move the crash logic here too because we went the whole server to sleep not just one of the threads
					 */
					String crashC = crashQ.peek();
					if(crashC != null)
					{
						String crashCa[] = crashQ.peek().split(" ");
						int crashAfter = Integer.parseInt(crashCa[1]);
						int timeout = Integer.parseInt(crashCa[2]);
						if(commandsServed.get() >= crashAfter) // Assuming that 
						{
							crashQ.poll();
							store.clear();
							System.out.println("Server " + serverID + " has crashed for + " + timeout);
							Thread.sleep((long)timeout);
						 	Recover();
						}
					}
					while ((sock = collector.accept()) != null) 
					{
						System.out.println("New TCP connection from " + sock.getInetAddress());
						//Add check to see if the new tcp connection is from the server
						Thread t = new ServerThread(sock, requests, v);
						t.start();
					}
					collector.close();
				} 
				catch (Exception e) { }
			}
		});
		
		//Run all of the servers
		tcpThread.start();
		
	}
}
