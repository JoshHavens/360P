import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

	private static int MAX_BOOKS = 10;
	private static int TCP_PORT = 7776;

	public static class ServerThread extends Thread {

		private ConcurrentHashMap<Integer, Integer> store;
		private Socket tcp_socket = null;

		public ServerThread(ConcurrentHashMap<Integer, Integer> inBS, Socket inS) {
			store = inBS;
			tcp_socket = inS;
		}

		public void run() {
			try {
				Scanner in;
				OutputStream os;
				PrintWriter out;
				in = new Scanner(tcp_socket.getInputStream());
				os = tcp_socket.getOutputStream();
				out = new PrintWriter(os);

				try {
					int client_num = in.nextInt();
					int book_num = in.nextInt();
					String cmd_returned = in.next();

					if (cmd_returned.equals("reserve")) {
						// reserve a book_num
						if (book_num >= 1 && book_num <= MAX_BOOKS &&
								store.putIfAbsent(book_num, client_num) == null) {
							out.println(client_num + " " + book_num);
						} else {
							out.println("fail " + client_num + " " + book_num);
						}
					} else if (cmd_returned.equals("return")) {
						// return a book_num
						if (book_num >= 1 && book_num <= MAX_BOOKS &&
								store.remove(book_num, client_num)) {
							out.println("free " + client_num + " " + book_num);
						} else {
							out.println("fail " + client_num + " " + book_num);
						}
					} else {
						out.println("improper command: " + cmd_returned);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				out.flush();
				tcp_socket.close();
				out.close();
				in.close();
			} catch (Exception e) {
				// do nothing
				e.printStackTrace();
			}

		}

	}

	public static void main(String[] args) {
		final ConcurrentHashMap<Integer, Integer> book_store;
		book_store = new ConcurrentHashMap<Integer, Integer>();
		Scanner in = new Scanner(System.in);
		int serverID = in.nextInt();
		int totalServers = in.nextInt();	
		MAX_BOOKS = in.nextInt();
		String s = in.nextLine();
		ArrayList<String> serverProx = new ArrayList<String>();
		for(int i = 0; i < totalServers; i++)
		{
			s = in.nextLine();
			serverProx.add(s);	//Initialize request timestamps as -1 as a flag to indicate that there is no request
		}
		in.close();
		
		TCP_PORT = Integer.parseInt(serverProx.get(serverID - 1).substring(serverProx.get(serverID - 1).indexOf(":")+1));
		Thread tcpThread = new Thread(new Runnable() {
			public void run() {
				try {
					InetAddress locIP = InetAddress.getByName(serverProx.get(serverID - 1).substring(0, serverProx.get(serverID - 1).indexOf(":")));
					System.out.println(locIP);
					System.out.println("before");
					System.out.println(TCP_PORT);
					ServerSocket collector = new ServerSocket(TCP_PORT, 0, locIP);
					System.out.println("after");
					Socket sock;
					while ((sock = collector.accept()) != null) {
						System.out.println("New TCP connection from " + sock.getInetAddress());
						Thread t = new ServerThread(book_store, sock);
						t.start();
					}
					collector.close();
				} catch (Exception e) 
				{
					e.printStackTrace();
					System.out.println("exception!");
				}
			}
		});

		tcpThread.start();
	}	

}
