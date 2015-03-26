import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Server2 {

	private static int MAX_BOOKS;	//Books included with input
	private static int TCP_PORT;	//TCP port included with instructions
	private static int SERVCOMM_PORT;	//Server request port from other servers
	private static int serverID;
	private static AtomicInteger commandsServed = new AtomicInteger(); //Represents how many commands the server has serviced
	final static ConcurrentHashMap<Integer, Integer> store = new ConcurrentHashMap<Integer, Integer>(); //This is the library store
	final static ArrayList<String> serverProx = new ArrayList<String>(); //List of other servers
	
	public static class ServerThread extends Thread //This thread handles server requests/communication
	{
		private Socket serverSocket = null;

		public ServerThread(Socket serverSocket) 
		{
			super();
			this.serverSocket = serverSocket;
		}
		
		public void run()
		{
			
			try 
			{
				Scanner in = new Scanner(serverSocket.getInputStream());	//Get input from client
				OutputStream os = serverSocket.getOutputStream();
				PrintWriter out = new PrintWriter(os);
			} 
			catch (IOException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}
	
	public static class ClientThread extends Thread {

		private static int requests[];
		private Socket tcp_socket = null;
		private static DirectClock clock;
		private static AtomicInteger serverAcks;

		public ClientThread(Socket inS, int[] req, DirectClock v) 
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
			broadcastMessage("request", requests[serverID]);	//Send request message, activating the other server threads' listeners
			while(!okayCS()){							//Request CS
				//wait(); 	//checkForMessage();		
			}
			//Send changes to other servers
			broadcastMessage("change"+change, 0);
			broadcastMessage("release", clock.getValue(serverID));
			//Lamport mutex goes here: logical clock, queue for update requests, handling acks, etc.
			//THe server will push the state of its bookstore to other servers
			//Server will communicate changes by forwarding the client's command to the other servers
			return;
		}
		public static void broadcastMessage(String operation, int id)
		{
			if(operation.equals("request"))
			{	
				//send request to all other servers
				serverAcks = new AtomicInteger(0);
				while(serverAcks.get()!=serverProx.size()-1)
				{
					checkForMessage();//Check for acknowledgement messages
				}
			}
			else if(operation.contains("change"))
			{
				operation=operation.substring(6);		//Remove "change" from change string
				//send change to all other servers
			}
			else if(operation.equals("release"))
			{
				requests[serverID-1]=-1;	//delete own request from queue
				//send release to all other servers
			}
		}
		public static void checkForMessage()
		{
			//Check the server port for server messages
			//if()		//if message found, and it's an "ack" message
			serverAcks.getAndIncrement();
			return;
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
		ArrayList<Integer> ports = new ArrayList<Integer>(); 
		for(int i = 0; i < totalServers; i++)
		{
			String s = in.nextLine();
			ports.add(Integer.parseInt(s.substring(s.indexOf(":")+1)));
			serverProx.add(s);
			requests[i]=-1;		//Initialize request timestamps as -1 as a flag to indicate that there is no request
		}
		while(in.hasNext()) //Get crash instructions, may have multiple lines
		{		
			crashQ.add(in.nextLine());//put crash instruction into queue, access the instruction later
		}
		in.close();
		DirectClock v = new DirectClock(totalServers,serverID);
		TCP_PORT = Integer.parseInt(serverProx.get(serverID - 1).substring(serverProx.get(serverID - 1).indexOf(":")+1));
		SERVCOMM_PORT = TCP_PORT + 1;
		while(ports.contains(SERVCOMM_PORT))
		{
			SERVCOMM_PORT++;
		}
		Thread tcpThread = new Thread(new Runnable() 
		{ 
			public void run() 
			{						   
				try
				{
					ServerSocket collector = new ServerSocket(TCP_PORT);
					ServerSocket serverCollector = new ServerSocket(SERVCOMM_PORT);
					Socket sock = null;
					Socket ServerSock = null;
					/*
					 * HERE could be where we will check if there is input from servers because it is checking one socket, we can have another socket for server communication
					 * We could have a separate socket for server-server communication
					 * Check if there is an input from a server and apply changes to the variables here before the serverThreads are created
					 * We should move the crash logic here too because we went the whole server to sleep not just one of the threads
					 */
					
					while ((sock = collector.accept()) != null || (ServerSock = serverCollector.accept()) != null) 
					{
						if(sock != null)
						{
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
							
							System.out.println("New TCP connection from " + sock.getInetAddress());
							//Add check to see if the new tcp connection is from the server
							Thread t = new ClientThread(sock, requests, v);
							t.start();
						}
						if(ServerSock != null)
						{
							//Handle server requests here
							System.out.println("New Server connection from " + ServerSock.getInetAddress());
							Thread s = new ServerThread(ServerSock);
							
						}
					}
					collector.close();
					serverCollector.close();
				} 
				catch (Exception e) { }
			}
		});
		
		//Run all of the servers
		tcpThread.start();
		
	}
}
