package tetriscode;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;


public class TetrisClient extends JFrame implements Runnable {

	private static final int WIDTH = 500;
	private static final int HEIGHT = 600;

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
	int score_p1 = -1;
	int score_p2 = -1;

	public TetrisClient() {
		super("Tetris Client");
		this.setSize(TetrisClient.WIDTH, TetrisClient.HEIGHT);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setResizable(false);
		this.cardLayout = new CardLayout();
		this.mainPanel = new JPanel(this.cardLayout);

		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("File");

		JMenuItem exitItem = new JMenuItem("Exit");
		//exitItem.addActionListener((e) -> System.exit(0));
		exitItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					toServer.close();
					fromServer.close();
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
				System.exit(0);
			}
		} );

		menu.add(exitItem);
		menuBar.add(menu);
		this.setJMenuBar(menuBar);

		//Game frame
		JPanel panel_game = new JPanel(new BorderLayout());
		//score section
		JPanel panel_score = new JPanel(new GridLayout(	1, 2));
		panel_score.setBackground(new Color(51, 51, 51));
		statusbar_p1 = new JLabel("Score: 0");
		//statusbar_p1.setBackground(new Color(51, 51, 51));
		statusbar_p1.setForeground(Color.WHITE);
		statusbar_p1.setFont(new Font("", Font.PLAIN, 15));
		//statusbar_p1.setBackground(new Color(119, 119, 119));
		panel_score.add(statusbar_p1);

		statusbar_p2 = new JLabel("Waiting");
		statusbar_p2.setForeground(Color.WHITE);
		statusbar_p2.setFont(new Font("", Font.PLAIN, 15));
		panel_score.add(statusbar_p2);

		//board section
		JPanel panel_tetris = new JPanel(new GridLayout(1, 2));
		panel_tetris.setBorder(BorderFactory.createEmptyBorder(0,1,0,1));
		board_p1 = new Board(this, statusbar_p1, true);
		board_p1.setBorder(BorderFactory.createLineBorder(Color.WHITE));
		board_p1.setBackground(new Color(51, 51, 51));
		panel_tetris.add(board_p1);

		board_p2 = new Board(this, statusbar_p2, false);
		board_p2.setBorder(BorderFactory.createLineBorder(Color.WHITE));
		board_p2.setBackground(new Color(51, 51, 51));
		panel_tetris.add(board_p2);
		panel_game.add(panel_score, BorderLayout.SOUTH);
		panel_game.add(panel_tetris, BorderLayout.CENTER);

		this.mainPanel.add(panel_game, "game");

		//Start frame
		JPanel panel_start = new JPanel(new BorderLayout());

		JPanel panel_title = new JPanel();
		panel_title.setBackground(new Color(51, 51, 51));
		panel_title.setBorder(new EmptyBorder(new Insets(100, 50, 10, 50)));

		JLabel label_T = new JLabel("T");
		label_T.setForeground(new Color(112, 113, 252));
		label_T.setFont(new Font("", Font.BOLD, 40));
		panel_title.add(label_T);
		JLabel label_E = new JLabel("E");
		label_E.setForeground(new Color(252, 103, 103));
		label_E.setFont(new Font("", Font.BOLD, 40));
		panel_title.add(label_E);
		JLabel label_T2 = new JLabel("T");
		label_T2.setForeground(new Color(86, 216, 137));
		label_T2.setFont(new Font("", Font.BOLD, 40));
		panel_title.add(label_T2);
		JLabel label_R = new JLabel("R");
		label_R.setForeground(new Color(254, 217, 83));
		label_R.setFont(new Font("", Font.BOLD, 40));
		panel_title.add(label_R);
		JLabel label_I = new JLabel("I");
		label_I.setForeground(new Color(76, 213, 230));
		label_I.setFont(new Font("", Font.BOLD, 40));
		panel_title.add(label_I);
		JLabel label_S = new JLabel("S");
		label_S.setForeground(new Color(180, 66, 221));
		label_S.setFont(new Font("", Font.BOLD, 40));
		panel_title.add(label_S);

		JLabel label_B = new JLabel(" B");
		label_B.setForeground(new Color(235, 92, 130));
		label_B.setFont(new Font("", Font.BOLD, 40));
		panel_title.add(label_B);
		JLabel label_A = new JLabel("A");
		label_A.setForeground(new Color(81, 239, 108));
		label_A.setFont(new Font("", Font.BOLD, 40));
		panel_title.add(label_A);
		JLabel label_T3 = new JLabel("T");
		label_T3.setForeground(new Color(255, 60, 60));
		label_T3.setFont(new Font("", Font.BOLD, 40));
		panel_title.add(label_T3);
		JLabel label_T4 = new JLabel("T");
		label_T4.setForeground(new Color(255, 235, 54));
		label_T4.setFont(new Font("", Font.BOLD, 40));
		panel_title.add(label_T4);
		JLabel label_L = new JLabel("L");
		label_L.setForeground(new Color(107, 70, 255));
		label_L.setFont(new Font("", Font.BOLD, 40));
		panel_title.add(label_L);
		JLabel label_E2 = new JLabel("E");
		label_E2.setForeground(new Color(246, 142, 80));
		label_E2.setFont(new Font("", Font.BOLD, 40));
		panel_title.add(label_E2);

		panel_start.add(panel_title, BorderLayout.NORTH);

		JPanel panel_login = new JPanel();
		panel_login.setBackground(new Color(51, 51, 51));
		panel_login.setBorder(new EmptyBorder(new Insets(100, 200, 200, 200)));

		JLabel label_IP = new JLabel("Enter Server IP: ");
		label_IP.setFont(new Font("", Font.PLAIN, 15));
		label_IP.setForeground(new Color(255, 255, 255));
		panel_login.add(label_IP);
		JTextField textField_ip = new JTextField(10);
		textField_ip.setFont(new Font("", Font.PLAIN, 15));
		panel_login.add(textField_ip);

		JLabel label_name = new JLabel("Enter your name: ");
		label_name.setFont(new Font("", Font.PLAIN, 15));
		label_name.setForeground(new Color(255, 255, 255));
		panel_login.add(label_name);
		JTextField textField_name = new JTextField(10);
		textField_name.setFont(new Font("", Font.PLAIN, 15));
		panel_login.add(textField_name);
		JLabel info = new JLabel();
		info.setForeground(new Color(255, 255, 255));
		info.setFont(new Font("", Font.PLAIN, 15));

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
		panel_login.add(info);

		panel_start.add(panel_login, BorderLayout.CENTER);

		this.mainPanel.add(panel_start, "start");

		//Waiting frame
		JPanel panel_wait = new JPanel();
		panel_wait.setBackground(new Color(51, 51, 51));
		panel_wait.setBorder(new EmptyBorder(new Insets(230, 50, 230, 50)));

		JLabel label_wait = new JLabel("Waiting for Player....");
		label_wait.setFont(new Font("", Font.BOLD, 20));
		label_wait.setForeground(new Color(255, 255, 255));
		panel_wait.add(label_wait);
		this.mainPanel.add(panel_wait, "wait");

		this.cardLayout.show(this.mainPanel, "start");
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
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void checkEndGame(){
		if(score_p1!=-1 && score_p2!=-1)
			sendToServer("ENDGAME");
	}

	public void creatEndFrame(String[][] rank_data){
		JPanel panel_end = new JPanel(new BorderLayout(100,50));
		panel_end.setBackground(new Color(51, 51, 51));
		JLabel label_result =  new JLabel();
		if(score_p1 > score_p2)
			label_result.setText("You Win");
		else if(score_p1 < score_p2)
			label_result.setText("You Lose");
		else
			label_result.setText("Tie Game");
		label_result.setForeground(Color.WHITE);
		label_result.setHorizontalAlignment(SwingConstants.CENTER);
		label_result.setFont(new Font("", Font.BOLD, 30));
		label_result.setBorder(new EmptyBorder(new Insets(100, 30, 0, 30)));

		panel_end.add(label_result, BorderLayout.NORTH);

		String[] rank_column = {"Rank", "Name", "Score"};
		DefaultTableModel tableModel = new DefaultTableModel(null, rank_column);
		JTable table_rank = new JTable(tableModel);
		table_rank.setRowSelectionAllowed(false);
		table_rank.setCellSelectionEnabled(false);
		table_rank.setFocusable(false);
		table_rank.setFont(new Font("", Font.PLAIN, 15));
		table_rank.getColumnModel().getColumn(0).setPreferredWidth(20);
		table_rank.getColumnModel().getColumn(1).setPreferredWidth(100);
		table_rank.getColumnModel().getColumn(2).setPreferredWidth(20);

		JTableHeader tableHeader = table_rank.getTableHeader();
		tableHeader.setFont(new Font("", Font.PLAIN, 15));

		table_rank.setRowHeight(20);


		for(int i=0; i<10; i++){
			String[] res = new String[3];
			res[0] = String.valueOf(i+1);
			res[1] = rank_data[i][0];
			res[2] = rank_data[i][1];
			tableModel.addRow(res);
		}
		tableModel.fireTableDataChanged();


		JScrollPane sp_rank = new JScrollPane(table_rank);

		panel_end.add(sp_rank, BorderLayout.CENTER);
		JLabel label_empty = new JLabel("");
		label_empty.setBorder(new EmptyBorder(new Insets(0, 0, 85, 0)));
		panel_end.add(label_empty, BorderLayout.SOUTH);
		panel_end.add(new JLabel(""), BorderLayout.EAST);
		panel_end.add(new JLabel(""), BorderLayout.WEST);

		this.mainPanel.add(panel_end, "end");
		this.cardLayout.show(this.mainPanel, "end");
	}

	public void updateP1Score(int score_p1){
		this.score_p1 = score_p1;
	}

	public void updateP2Score(int score_p2){
		this.score_p2 = score_p2;
	}


	public void run() {

	}

	// Define the thread class for handling new connection
    class HandleAServer implements Runnable {
    	private Socket socket; // Server connected socket

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
						//System.out.println(playerName_p1+"     "+inText);
						board_p2.setName(playerName_p2);
					}
					else if(inText.contains("SEED:")){
						String seed = inText.substring(5);
						//System.out.println("SEED:" + seed);
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
					else if(inText.contains("RANK:")){

						ObjectInputStream inputStream = new ObjectInputStream(this.socket.getInputStream());
						String[][] inData = (String[][]) inputStream.readObject();
						System.out.println(inData[0][0]+": "+inData[0][1]);

						creatEndFrame(inData);
					}
    			}
    		} catch (IOException | ClassNotFoundException e) {
				throw new RuntimeException(e);
    		}
		}
	}

	public static void main(String[] args) {
		TetrisClient tetrisClient = new TetrisClient();
	}
}
