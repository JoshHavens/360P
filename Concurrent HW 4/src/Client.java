import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;
// import java.lang.*;

public class Client {

	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);
		try {
			ArrayList<String> serverProx = new ArrayList<String>();
			int client_num = Integer.parseInt(in.next().substring(1));
			int setupServers = 0;
			//First line of input is different: client-id ci (whitespace) n
			//Store client id, and store number of servers into a data structure, list of proximity
			int numServers = in.nextInt();		//Take number of servers
			in.nextLine();
			while(setupServers < numServers)
			{
				serverProx.add(in.nextLine().trim());//Address also contains port number -> IP:Port#. Split up later when parsed, order of proximity is always the same
				setupServers++;	//Increment number of Servers that have been "setup"
			}
			String a = serverProx.get(0); //First server in the que
			InetAddress address = InetAddress.getByName(a.substring(0,a.indexOf(":")).trim());
			int port_num = Integer.parseInt(a.substring(a.indexOf(":")+1).trim());
			while (in.hasNextLine()) {
				if (in.findInLine("sleep") != null) {
					int sleep = in.nextInt();
					Thread.sleep((long) (sleep));
				}
				else if (in.findInLine("b") != null) {
					int book_num = in.nextInt();
					String cmd_num = in.next();

					String msg = new String(client_num + " " + book_num + " " + cmd_num + "\n");
					Scanner reader;
					Socket client_socket = null;
					
						client_socket = new Socket(address, port_num);
						client_socket.setSoTimeout(1000);
						PrintWriter send = new PrintWriter(client_socket.getOutputStream());
						reader = new Scanner(client_socket.getInputStream());

						send.println(msg);
						send.flush();
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
