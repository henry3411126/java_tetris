package tetriscode;


import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.*;
import java.util.Date;

import javax.swing.*;


public class TetrisServer extends JFrame implements Runnable {

	// frame size
	private static final int WIDTH = 400;
	private static final int HEIGHT = 300;
	private JTextArea ta;                                                      // for print the in and out data
	private int clientNo = 0;                                                  // current number of client in database
	private Map<Integer, Socket> clientMap = new HashMap<Integer, Socket>();   // for keeping the client's ID and client's socket
	private Map<Integer, String> clientNameMap = new HashMap<Integer, String>();             // for keeping the client's ID and client's name

	// for MySQL database
	PreparedStatement insertStatement;
	Connection con = null;

	public TetrisServer() {
		super("Tetris Server");
		this.setSize(TetrisServer.WIDTH, TetrisServer.HEIGHT);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("File");
		JMenuItem exitItem = new JMenuItem("Exit");
		exitItem.addActionListener((e) -> System.exit(0));
		menu.add(exitItem);
		menuBar.add(menu);
		this.setJMenuBar(menuBar);
		ta = new JTextArea(10,10);

		JScrollPane sp = new JScrollPane(ta);
		sp.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				e.getAdjustable().setValue(e.getAdjustable().getMaximum());
			}
		});
		this.add(sp);
		this.setVisible(true);

		// connect to database
		try{
			Class.forName("com.mysql.jdbc.Driver");

			// connect the database
			con = DriverManager.getConnection("jdbc:mysql://localhost:8889/tetris_java","user01","user01");
			System.out.println("Database connected");

			// get the current ID of database
			Statement st = con.createStatement();
			String sql = ("SELECT * FROM player ORDER BY id DESC LIMIT 1;");
			ResultSet rs = st.executeQuery(sql);

			if(rs.next()) {
				clientNo = rs.getInt("ID");
				System.out.println("clientNo: "+clientNo);
			}
		}catch (Exception e) {
			System.err.println(e);
			System.exit(0);
		}

		String insertSQL = "Insert Into player (ID,Name,Time,Score) " + "Values (?,?,?,?)";
		try {
			insertStatement = con.prepareStatement(insertSQL);
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}

		Thread t = new Thread(this);
		t.start();
	}

	// insert the data to database
	private void insertName(int player_id, String player_name) {
		try {
			insertStatement.setInt(1, player_id);
			insertStatement.setString(2, player_name);
			insertStatement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
			insertStatement.setInt(4, 0);
			insertStatement.execute();
			System.out.println("Database Inserted Successfully");
		} catch(Exception e) {
			System.err.println(e.getMessage());
		}
	}

	// update the data to database
	private void updateScore(int player_id, int score) {
		try {
			Timestamp cur_time = new Timestamp(System.currentTimeMillis());
			String sql = "Update player Set Score='"+score+"'," + "Time='"+cur_time+"' Where ID='"+player_id+"'";
			Statement statement = con.createStatement();
			statement.execute(sql);
			System.out.println("Database Updated Successfully");
		} catch(Exception e) {
			System.err.println(e.getMessage());
		}
	}

	// get the top 10 scores name and ID
	private String[][] getScoreRank() {
		try {
			Statement st = con.createStatement();
			String sql = ("SELECT * FROM player ORDER BY Score DESC LIMIT 10");
			ResultSet rs = st.executeQuery(sql);

			String[][] rank_data = new String[10][2];

			for(int i=0; i<10; i++){
				if(rs.next()) {
					rank_data[i][0] = rs.getString("Name");
					rank_data[i][1] = rs.getString("Score");
				}
			}
			return rank_data;

		} catch(Exception e) {
			System.err.println(e.getMessage());
			return null;
		}
	}

	// delay the thread (ms)
	public void delay(int ms){
		try {
			Thread.sleep(ms);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void run() {
		try {
			// create a server socket
			ServerSocket serverSocket = new ServerSocket(9898);
			ta.append("Server started at " + new Date() + '\n');

			while (true) {
				// listen for a new connection request
				Socket socket = serverSocket.accept();

				// increment clientNo
				clientNo++;

				// Find the client's host name, and IP address
				InetAddress inetAddress = socket.getInetAddress();
				ta.append("Client " + clientNo + "'s host name is " + inetAddress.getHostName() + "\n");
				ta.append("Client " + clientNo + "'s IP Address is " + inetAddress.getHostAddress() + "\n");

				DataInputStream inputFromClient = new DataInputStream(socket.getInputStream());
				String text = inputFromClient.readUTF();

				ta.append("Client " + clientNo + "'s name is " + text.substring(5) + "\n");
				insertName(clientNo, text.substring(5));

				this.clientMap.put(clientNo, socket);
				this.clientNameMap.put(clientNo, text);

				// if server match two player
				if(this.clientMap.size() == 2){
					var r = new Random();
					int seed = Math.abs(r.nextInt()) % 1000;
					DataOutputStream outputToClient1 = new DataOutputStream(this.clientMap.get(clientNo-1).getOutputStream());
					DataOutputStream outputToClient2 = new DataOutputStream(this.clientMap.get(clientNo).getOutputStream());

					outputToClient1.writeUTF(this.clientNameMap.get(clientNo));
					outputToClient1.flush();
					outputToClient2.writeUTF(this.clientNameMap.get(clientNo-1));
					outputToClient2.flush();

					// delay to let client store the name
					delay(10);

					outputToClient1.writeUTF("SEED:"+seed);
					outputToClient1.flush();
					outputToClient2.writeUTF("SEED:"+seed);
					outputToClient2.flush();
					new Thread(new HandleAClient(this.clientMap.get(clientNo-1), outputToClient2,clientNo-1)).start();
					new Thread(new HandleAClient(this.clientMap.get(clientNo), outputToClient1,clientNo)).start();
					this.clientMap.remove(clientNo-1);
					this.clientMap.remove(clientNo);
				}
			}
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	// define the thread class for handling new connection
    class HandleAClient implements Runnable {
		private Socket socket_p1;                 // socket of p1
		private int clientNum;                    // the player's ID
		DataOutputStream outputToClient = null;   // for sending data to client
		/** Construct a thread */
		public HandleAClient(Socket socket_p1, DataOutputStream outputToClient, int clientNum) {
			this.socket_p1 = socket_p1;
			this.outputToClient = outputToClient;
			this.clientNum = clientNum;
		}

		/** Run a thread */
		public void run() {
			try {

				// create data input and output streams
				ta.append("Starting thread for client "+ this.clientNum + " at " + new Date() + '\n');
				DataInputStream inputFromClient = new DataInputStream(this.socket_p1.getInputStream());
				//DataOutputStream outputToClient = new DataOutputStream(this.socket_p2.getOutputStream());

				// continuously serve the client
				while (true) {
					// receive message from the client and decode them
					String text = inputFromClient.readUTF();
					if(text.contains("COMMEND:")){
						ta.append("client " + this.clientNum + " move: "+ text.substring(8) + '\n');

						// send text back to other client
						outputToClient.writeUTF(text);
						outputToClient.flush();
					} else if(text.contains("GAMEOVER:")){
						ta.append("client " + this.clientNum + " game over, score: "+ text.substring(9) + '\n');
						updateScore(this.clientNum,Integer.parseInt(text.substring(9)));
					} else if(text.contains("ENDGAME")){
						System.out.println(clientNum + " :ENDGAME "+socket_p1);
						String[][] data = getScoreRank();
						System.out.println(Arrays.deepToString(data));

						delay(100);
						outputToClient.writeUTF("RANK:");
						outputToClient.flush();
						delay(100);

						// use ObjectOutStream to send rank data
						ObjectOutputStream outputStream = new ObjectOutputStream(this.socket_p1.getOutputStream());
						outputStream.writeObject(data);
						outputStream.flush();
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static void main(String[] args) {
		TetrisServer tetrisServer = new TetrisServer();
	}
}


