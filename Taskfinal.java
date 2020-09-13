//Saquib Irtiza, NET ID: sxi190002
//Bharanidhar Reddy Anumula, NET ID: bxa190017
//Gandhar Satish Joshi, NET ID gsj190000

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Taskfinal {

	static int uid[];
	static int[] rport;
	static int[] port;
	static int[] lport;

	// -----------------------------------------ServerThread--------------------------------------------------------
	public static class ServerThread implements Runnable {
		private int port;
		private int lport;
		private int rport;
		private int uid;
		private int sendPort;
		private Socket temp = null;
		Socket client = null;
		InputStreamReader in = null;
		BufferedReader bf = null;
		String str = null;
		String[] message = null;
		Server ss = null;

		ServerThread(Socket Client, int ID, int Port, int leftPort, int rightPort, Server server) throws IOException {
			this.client = Client;
			this.uid = ID;
			this.port = Port;
			this.lport = leftPort;
			this.rport = rightPort;
			this.ss = server;
			//receiving message from the client thread
			in = new InputStreamReader(client.getInputStream());
			bf = new BufferedReader(in);
			str = bf.readLine();
			message = str.split(":");

			//printing the contents of the message
			//System.out.println(" message: " + str);
		}

		@Override
		public void run() {
			//message[0] = port number of the sender
			//message[1] = "o" for outgoing or "i" for incoming message
			//message[2] = uid of the sender
			//message[3] = phase count
			//message[4] = hop count

			//the conditions of HS algorithm
			if (message[1].equals("o") && Integer.parseInt(message[2]) == uid) {
				if (ss.leaderCount == 1) {
					System.out.println("Leader: " + uid);
					System.exit(0);
				} else {
					ss.leaderCount++;
				}
			} else if (message[1].equals("o") && Integer.parseInt(message[2]) > uid
					&& Integer.parseInt(message[4]) <= (int) Math.pow(2, Integer.parseInt(message[3]))) {
				str = Integer.toString(port) + ":o:" + message[2] + ":" + message[3] + ":"
						+ Integer.toString(Integer.parseInt(message[4]) + 1);
				if (Integer.parseInt(message[0]) == lport)
					sendPort = rport;
				else
					sendPort = lport;
				try {
					temp = new Socket("localhost", sendPort);
					PrintWriter pr = new PrintWriter(temp.getOutputStream());
					pr.println(str);
					pr.flush();
				} catch (IOException e) {
				}
			} else if (message[1].equals("o") && Integer.parseInt(message[2]) > uid
					&& Integer.parseInt(message[4]) > Math.pow(2, Integer.parseInt(message[3]))) {
				str = Integer.toString(port) + ":i:" + message[2] + ":" + message[3];
				sendPort = Integer.parseInt(message[0]);
				try {
					temp = new Socket("localhost", sendPort);
					PrintWriter pr = new PrintWriter(temp.getOutputStream());
					pr.println(str);
					pr.flush();
				} catch (IOException e) {
				}
			} else if (message[1].equals("i") && Integer.parseInt(message[2]) != uid) {
				str = Integer.toString(port) + ":i:" + message[2] + ":" + message[3];
				if (Integer.parseInt(message[0]) == lport)
					sendPort = rport;
				else
					sendPort = lport;
				try {
					temp = new Socket("localhost", sendPort);
					PrintWriter pr = new PrintWriter(temp.getOutputStream());
					pr.println(str);
					pr.flush();
				} catch (IOException e) {
				}
			} else if (message[1].equals("i") && Integer.parseInt(message[2]) == uid && ss.replyCount == 0) {
				ss.replyCount++;
			} else if (message[1].equals("i") && Integer.parseInt(message[2]) == uid && ss.replyCount == 1) {
				ss.replyCount = 0;
				str = Integer.toString(port) + ":o:" + Integer.toString(uid) + ":"
						+ Integer.toString(Integer.parseInt(message[3]) + 1) + ":" + "1";

				System.out.println("Process " + this.uid + " starts phase :" + (Integer.parseInt(message[3]) + 1));

				try {
					temp = new Socket("localhost", lport);
					PrintWriter pr = new PrintWriter(temp.getOutputStream());
					pr.println(str);
					pr.flush();
				} catch (IOException e) {
				}
				try {
					temp = new Socket("localhost", rport);
					PrintWriter pr = new PrintWriter(temp.getOutputStream());
					pr.println(str);
					pr.flush();
				} catch (IOException e) {
				}
			}
		}
	}

	// -------------------------------------------Server---------------------------------------------------------------------
	public static class Server extends Thread {
		ExecutorService pool = null;
		private int port;
		private int lport;
		private int rport;
		private int uid;
		public int replyCount = 0;
		public int leaderCount = 0;

		public Server(int ID, int Port, int leftPort, int rightPort) {
			this.uid = ID;
			this.port = Port;
			this.lport = leftPort;
			this.rport = rightPort;
			pool = Executors.newFixedThreadPool(10);
		}

		public void run() {
			try {
				@SuppressWarnings("resource")
				ServerSocket ss = new ServerSocket(port);
				while (true) {
					Socket s = ss.accept();
					ServerThread runnable = new ServerThread(s, uid, port, lport, rport, this);
					pool.execute(runnable);
				}
			} catch (IOException e) {
			}
		}
	}

	// -------------------------------------------Client---------------------------------------------------------------------
	public static class Client extends Thread {
		private int uid;
		private int port;
		private int lport;
		private int rport;
		private String message = null;

		public Client(int ID, int Port, int leftPort, int rightPort) {
			this.uid = ID;
			this.port = Port;
			this.lport = leftPort;
			this.rport = rightPort;
		}

		public void run() {
			try {
				//sending message to the left neighbor on phase 0
				Socket s = new Socket("localhost", lport);
				message = Integer.toString(port) + ":o:" + Integer.toString(uid) + ":0:1";
				System.out.println("Process " + this.uid + " starts phase : 0");

				PrintWriter pr = new PrintWriter(s.getOutputStream());
				pr.println(message);
				pr.flush();
				s.close();

				//sending message to the right neighbor on phase 0
				Socket s2 = new Socket("localhost", rport);
				PrintWriter pr2 = new PrintWriter(s2.getOutputStream());
				pr2.println(message);
				pr2.flush();
				s2.close();
			} catch (IOException e) {
			}
		}
	}

	// ------------------------------------------Node Thread----------------------------------------------------------------
	public static class slaveThread extends Thread {
		private int ownID;
		private int ownPortNo;
		private int leftPortNo;
		private int rightPortNo;

		public slaveThread(int ID, int Port, int leftPort, int rightPort) {
			this.ownID = ID;
			this.ownPortNo = Port;
			this.leftPortNo = leftPort;
			this.rightPortNo = rightPort;
		}

		@Override
		public void run() {
			try {
				//creating server and client threads for each process
				Server server = new Server(ownID, ownPortNo, leftPortNo, rightPortNo);
				Client client = new Client(ownID, ownPortNo, leftPortNo, rightPortNo);
				server.start();
				Thread.sleep(1000);
				client.start();
			} catch (InterruptedException e) {
			}
		}
	}

	// -------------------------------------------Main---------------------------------------------------------------------
	public static void main(String[] args) {

		File file = new File("input.txt");
		int noOfProcessors = 0;
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(file));

			//reads the number of processes from the file
			String st;
			noOfProcessors = Integer.parseInt(br.readLine()); 

			System.out.println("Number of Process IDs are " + noOfProcessors);

			System.out.print("Process IDs are : ");

			//reads the uids from the file
			uid = new int[noOfProcessors];
			int i = 0;
			while ((st = br.readLine()) != null) {
				uid[i] = Integer.parseInt(st);
				System.out.print(uid[i] + " ");
				i++;
			}
			System.out.println("\n");
		} catch (NumberFormatException | IOException e) {
			System.out.println("Exception : " + e.getLocalizedMessage());
		}

		int max = uid[0];
		for (int i = 1; i < uid.length; i++)
			if (uid[i] > max)
				max = uid[i];

		System.out.println("Expected Leader : " + max + "\n");

		rport = new int[noOfProcessors];
		port = new int[noOfProcessors];
		lport = new int[noOfProcessors];

		//assigning left and right neighbors of each process using unique port numbers
		for (int i = 0; i < noOfProcessors; i++) {
			port[i] = 8001 + i;
		}
		lport[0] = port[noOfProcessors - 1];
		for (int i = 1; i < noOfProcessors; i++) {
			lport[i] = port[i - 1];
		}
		rport[noOfProcessors - 1] = port[0];
		for (int i = 0; i < noOfProcessors - 1; i++) {
			rport[i] = port[i + 1];
		}

		//creating threads for each process
		for (int i = 0; i < noOfProcessors; i++) {
			System.out.println(lport[i]);
			Thread newThread = new slaveThread(uid[i], port[i], lport[i], rport[i]);
			newThread.start();
		}
	}
}
