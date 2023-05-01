package tetriscode;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.swing.*;


public class TetrisClient extends JFrame implements Runnable {

	private static int WIDTH = 500;
	private static int HEIGHT = 600;

	// IO streams
	DataOutputStream toServer = null;
	DataInputStream fromServer = null;
	Socket socket = null;

	//add by henry
	private JLabel statusbar_p1;
	private JLabel statusbar_p2;
	Board board_p1;
	Board board_p2;
	String serverIP;
	String playerName_p1;
	String playerName_p2;
	JPanel mainPanel;
	CardLayout cardLayout;

	public TetrisClient() {
		super("Tetris Client");
		this.setSize(TetrisClient.WIDTH, TetrisClient.HEIGHT);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setResizable(false);
		this.cardLayout = new CardLayout();
		this.mainPanel = new JPanel(this.cardLayout);

		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("File");

		//JMenuItem connectItem = new JMenuItem("Connect");
		//connectItem.addActionListener((e) -> this.connectServer());
		//menu.add(connectItem);

		JMenuItem exitItem = new JMenuItem("Exit");
		exitItem.addActionListener((e) -> System.exit(0));
		menu.add(exitItem);

		menuBar.add(menu);
		this.setJMenuBar(menuBar);

		//Game frame
		JPanel panel_game = new JPanel(new BorderLayout());
		//score section
		JPanel panel_score = new JPanel(new GridLayout(	2, 2));
		statusbar_p1 = new JLabel("Score: 0");
		statusbar_p1.setForeground(Color.red);
		panel_score.add(statusbar_p1);

		statusbar_p2 = new JLabel("Waiting");
		statusbar_p2.setForeground(Color.blue);
		panel_score.add(statusbar_p2);
		panel_score.add(new JLabel());

		//board section
		JPanel panel_tetris = new JPanel(new GridLayout(1, 2));
		panel_tetris.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		board_p1 = new Board(this, statusbar_p1, true);
		board_p1.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		panel_tetris.add(board_p1);

		board_p2 = new Board(this, statusbar_p2, false);
		board_p2.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		panel_tetris.add(board_p2);
		panel_game.add(panel_score, BorderLayout.SOUTH);
		panel_game.add(panel_tetris, BorderLayout.CENTER);

		this.mainPanel.add(panel_game, "game");

		//this.add(panel_tetris, BorderLayout.CENTER);
		//this.add(panel_score, BorderLayout.SOUTH);
		//panel_tetris.setVisible(false);
		//panel_score.setVisible(false);

		//login frame
		JPanel panel_login = new JPanel();
		panel_login.add(new JLabel("Enter Server IP: "));
		JTextField textField_ip = new JTextField(10);
		panel_login.add(textField_ip);
		panel_login.add(new JLabel("Enter your name: "));
		JTextField textField_name = new JTextField(10);
		panel_login.add(textField_name);
		JLabel info = new JLabel();
		panel_login.add(info);
		JButton button_start = new JButton("START");
		button_start.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				playerName_p1 = textField_name.getText().trim();
				serverIP = textField_ip.getText().trim();
				if(connectServer(serverIP)){
					board_p1.setName(playerName_p1);
					cardLayout.show(mainPanel, "wait");
					board_p1.waiting();
					board_p2.waiting();
				}
				else{
					info.setText("Cannot connect to Server! Please try again.");
				}
			}
		} );
		panel_login.add(button_start);
		this.mainPanel.add(panel_login, "login");

		//Waiting frame
		JPanel panel_wait = new JPanel();
		panel_wait.add(new JLabel("Waiting for Player...."));
		this.mainPanel.add(panel_wait, "wait");

		this.cardLayout.show(this.mainPanel, "login");
		this.add(this.mainPanel);
		this.setVisible(true);
	}

	public boolean connectServer(String serverIP) {
		try {
			this.socket = new Socket(serverIP, 9898);
			new Thread(new HandleAServer(this.socket)).start();
			sendToServer("NAME:" + playerName_p1);
			return true;

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			statusbar_p1.setText("Connection Failure");
			return false;
		}
	}



	public void sendToServer(String outText) {

		try {
			if(outText.equals("COMMEND:PAUSE"))
				board_p2.pausePress();

			toServer = new DataOutputStream(socket.getOutputStream());

			toServer.writeUTF(outText);
			toServer.flush();

		}
		catch (IOException ex) {
			System.err.println(ex);
		}
	}


	public void run() {
		/*
		try {
			fromServer = new DataInputStream(this.socket.getInputStream());

			while(true) {
				String inText = fromServer.readUTF();
			    int clientNo = inText.charAt(0);

			    StringBuilder sb = new StringBuilder(inText);
			    sb.deleteCharAt(0);

			    textArea.append(clientNo + ": " + sb.toString() + "\n");
			}
		}
		catch (IOException ex) {
		    System.err.println(ex);
		}
		*/
	}



	// Define the thread class for handling new connection
    class HandleAServer implements Runnable {
    	private Socket socket; // A connected socket

    	/** Construct a thread */
    	public HandleAServer(Socket socket) {
    		this.socket = socket;
    	}

	    /** Run a thread */
    	public void run() {
    		try {
    			fromServer = new DataInputStream(this.socket.getInputStream());

    			while(true) {
    				String inText = fromServer.readUTF();
					System.out.println(inText);

					if(inText.contains("NAME:")){
						playerName_p2 = inText.substring(5);
						System.out.println(playerName_p1+"     "+inText);
						board_p2.setName(playerName_p2);
					}
					else if(inText.contains("SEED:")){
						String seed = inText.substring(5);
						System.out.println("SEED:" + seed);
						board_p1.setRandomSeed(Integer.parseInt(seed));
						board_p2.setRandomSeed(Integer.parseInt(seed));
						cardLayout.show(mainPanel, "game");
						board_p1.start();
						board_p2.start();
						statusbar_p1.setText(playerName_p1 + "'s score: 0");
						statusbar_p2.setText(playerName_p2 + "'s score: 0");
					}
					else if(inText.contains("COMMEND:")){
						if(inText.equals("COMMEND:PAUSE"))
							board_p1.pausePress();

						board_p2.streamCommend(inText);
					}
    			}
    		}
    		catch (IOException ex) {
    		    System.err.println(ex);
    		}
	    }
	}

	public static void main(String[] args) {
		TetrisClient tetrisClient = new TetrisClient();
	}
}
