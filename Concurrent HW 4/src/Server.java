import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

	private static int MAX_BOOKS = 10;
	private static int TCP_PORT = 7776;
	private static int UDP_PORT = 7776;
	private static int PACKET_SIZE = 4096;

	public static class ServerThread extends Thread {

		private ConcurrentHashMap<Integer, Integer> store;
		private Socket tcp_socket = null;
		private DatagramSocket udp_socket = null;
		private DatagramPacket udp_packet = null;

		public ServerThread(ConcurrentHashMap<Integer, Integer> inBS, Socket inS) {
			store = inBS;
			tcp_socket = inS;
		}

		public ServerThread(ConcurrentHashMap<Integer, Integer> inBS,
				DatagramSocket inS, DatagramPacket inP) {
			store = inBS;
			udp_socket = inS;
			udp_packet = inP;
		}

		public void run() {
			try {
				Scanner in;
				OutputStream os;
				PrintWriter out;
				if (tcp_socket != null) {
					in = new Scanner(tcp_socket.getInputStream());
					os = tcp_socket.getOutputStream();
				} else {
					InputStream is = new ByteArrayInputStream(
							udp_packet.getData());
					in = new Scanner(is);
					os = new ByteArrayOutputStream();
				}
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
				if (tcp_socket != null) {
					tcp_socket.close();
				} else {
					byte[] buffer = ((ByteArrayOutputStream) os).toByteArray();
					DatagramPacket p = new DatagramPacket(buffer, buffer.length,
							udp_packet.getAddress(), udp_packet.getPort());

					udp_socket.send(p);
				}
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
		MAX_BOOKS = in.nextInt();
		UDP_PORT = in.nextInt();
		TCP_PORT = in.nextInt();
		in.close();
		
		Thread tcpThread = new Thread(new Runnable() {
			public void run() {
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

		Thread udpThread = new Thread(new Runnable() {
			public void run() {
				try {
					DatagramSocket collector = new DatagramSocket(UDP_PORT);
					while (true) {
						byte[] buffer = new byte[PACKET_SIZE];
						DatagramPacket pack = new DatagramPacket(buffer, buffer.length);
						collector.receive(pack);

						System.out.println("New UDP connection from " + pack.getAddress());
						Thread t = new ServerThread(book_store, collector, pack);
						t.start();
					}
				} catch (Exception e) { }
			}
		});

		tcpThread.start();
		udpThread.start();
	}

}
