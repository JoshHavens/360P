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
		Scanner in = new Scanner(System.in);
		try {
			int client_num = in.nextInt();
			InetAddress address = InetAddress.getByName(in.nextLine().trim());
			while (in.hasNextLine()) {
				if (in.findInLine("sleep") != null) {
					int sleep = in.nextInt();
					Thread.sleep((long) (sleep));
				} else if (in.findInLine("b") != null) {
					int book_num = in.nextInt();
					String cmd_num = in.next();
					int port_num = in.nextInt();

					String msg = new String(client_num + " " + book_num + " " + cmd_num + "\n");
					Scanner reader;
					Socket client_socket = null;
					if (in.next().equals("U")) {
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
					} else {
						client_socket = new Socket(address, port_num);
						client_socket.setSoTimeout(1000);
						PrintWriter send = new PrintWriter(client_socket.getOutputStream());
						reader = new Scanner(client_socket.getInputStream());

						send.println(msg);
						send.flush();
					}
					String command_returned = "";
					if (reader.findInLine("free") != null) {
						command_returned = "free ";
					} else if (reader.findInLine("fail") != null) {
						command_returned = "fail ";
					}
					int client_num_returned = reader.nextInt();
					int book_num_returned = reader.nextInt();

					System.out.println(command_returned + "c" + client_num_returned + " b" + book_num_returned);
					//  + " : " + System.nanoTime()
					reader.close();
					if (client_socket != null) client_socket.close();
				} else {
					in.nextLine();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Malformed input.");
			System.exit(1);
		}
	}

}
