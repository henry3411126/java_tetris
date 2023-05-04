package tetriscode;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Random;

public class Board extends JPanel {

    // define how many pixel in the board (10*20)
    private final int BOARD_WIDTH = 10;
    private final int BOARD_HEIGHT = 22;

    private Timer timer;
    private boolean isFallingFinished = false;  // set if the piece cannot fall anymore
    private boolean isPaused = false;           // set if is paused
    private boolean isGameover = false;         // set if the game is over
    private boolean isPlayer1;                  // set if is main player
    private int numLinesRemoved = 0;            // score
    String playerName;                          // player's name
    private int squareWidth = 0;                // pixel size: x
    private int squareHeight = 0;               // pixel size: y


    // the coordinate(x, y) of the piece currently using
    private int curX = 0;
    private int curY = 0;

    // for Java frame
    private JLabel statusbar;
    private Piece curPiece;
    private Piece.ShapeType[] board;
    private Random r;
    TetrisClient parent;

    public Board(TetrisClient parent, JLabel statusbar, boolean isPlayer1) {
        this.parent = parent;
        this.statusbar = statusbar;
        this.isPlayer1 = isPlayer1;
        setFocusable(true);
        addKeyListener(new TKeyAdapter());
        curPiece = new Piece();
        board = new Piece.ShapeType[BOARD_WIDTH * BOARD_HEIGHT];

    }

    // waiting frame
    public void waiting(){
        // set the pixel size
        this.squareWidth = (int) getSize().getWidth() / BOARD_WIDTH;
        this.squareHeight = (int) getSize().getWidth() / BOARD_WIDTH;
        clearBoard();
        repaint();
    }

    // start the game
    public void start() {
        newPiece();
        timer = new Timer(500, new GameCycle());
        timer.start();
    }

    // streaming the game from server
    public void streamCommend(String inText){
        switch (inText) {
            case "COMMEND:PAUSE" -> pausePress();
            case "COMMEND:LEFT" -> tryMove(curPiece, curX - 1, curY);
            case "COMMEND:RIGHT" -> tryMove(curPiece, curX + 1, curY);
            case "COMMEND:DOWN" -> tryMove(curPiece.rotateClockwise(), curX, curY);
            case "COMMEND:UP" -> tryMove(curPiece.rotateCounterclockwise(), curX, curY);
            case "COMMEND:SPACE" -> dropDown();
            case "COMMEND:DROP" -> oneLineDown();
        }
    }

    // set the player's name
    public void setName(String playerName){
        this.playerName = playerName;
    }

    // set the seed for random shape
    public void setRandomSeed(int seed){
        r = new Random(seed);
    }

    // pause the game
    public void pausePress() {
        isPaused = !isPaused;

        if (isPaused) {
            statusbar.setText("Paused");
        } else{
            if(isGameover){
                var msg = String.format("Game Over. " + playerName + "'s score: %d", numLinesRemoved);
                statusbar.setText(msg);
            }
            else
                statusbar.setText(playerName + "'s score: " + numLinesRemoved);
        }

        repaint();
    }

    // get the type at (x, y)
    private Piece.ShapeType shapeAt(int x, int y) {
        return board[(y * BOARD_WIDTH) + x];
    }

    // paint the frame
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        var size = getSize();
        int boardTop = (int) size.getHeight() - BOARD_HEIGHT * squareHeight;

        g.setColor(new Color(62, 62, 62));
        for (int i = 0; i < BOARD_HEIGHT; i++) {
            for (int j = 0; j < BOARD_WIDTH; j++) {
                g.drawRect(j * squareWidth, boardTop + i * squareHeight, squareWidth, squareHeight);
            }
        }

        for (int i = 0; i < BOARD_HEIGHT; i++) {
            for (int j = 0; j < BOARD_WIDTH; j++) {
                Piece.ShapeType shape = shapeAt(j, BOARD_HEIGHT - i - 1);
                if (shape != Piece.ShapeType.None) {
                    drawSquare(g, shape, j * squareWidth, boardTop + i * squareHeight);
                }
            }
        }

        if (curPiece.getShape() != Piece.ShapeType.None) {
            for (int i = 0; i < 4; i++) {
                int x = curX + curPiece.x(i);
                int y = curY - curPiece.y(i);
                drawSquare(g, curPiece.getShape(), x * squareWidth, boardTop + (BOARD_HEIGHT - y - 1) * squareHeight);
            }
        }
    }

    // drop the piece down to the bottom
    private void dropDown() {
        int newY = curY;

        while (newY > 0) {
            if (!tryMove(curPiece, curX, newY - 1))
                break;
            newY--;
        }
        pieceDropped();
    }

    // drop the piece down for one pixel
    private void oneLineDown() {
        if (!tryMove(curPiece, curX, curY - 1))
            pieceDropped();
    }

    // clear all the board
    private void clearBoard() {
        for (int i = 0; i < BOARD_HEIGHT * BOARD_WIDTH; i++)
            board[i] = Piece.ShapeType.None;
    }

    // check of it need to clear whole line and add new piece
    private void pieceDropped() {
        for (int i = 0; i < 4; i++) {
            int x = curX + curPiece.x(i);
            int y = curY - curPiece.y(i);
            board[(y * BOARD_WIDTH) + x] = curPiece.getShape();
        }

        removeFullLines();

        if (!isFallingFinished)
            newPiece();
    }

    // add new piece
    private void newPiece() {
        curPiece.setRandomShape(r);
        curX = BOARD_WIDTH / 2 + 1;
        curY = BOARD_HEIGHT - 1 + curPiece.minY();

        // if no space for new piece
        if (!tryMove(curPiece, curX, curY)) {
            curPiece.setShape(Piece.ShapeType.None);
            timer.stop();
            var msg = String.format("Game Over. " + playerName + "'s score: %d", numLinesRemoved);
            statusbar.setText(msg);
            isGameover = true;
            if(isPlayer1){
                //send to the sever GAME OVER
                parent.sendToServer("GAMEOVER:"+numLinesRemoved);
                parent.updateP1Score(numLinesRemoved);
            }
            else{
                parent.updateP2Score(numLinesRemoved);
            }

            this.parent.checkEndGame();
        }
    }

    // check the piece can move or not
    private boolean tryMove(Piece newPiece, int newX, int newY) {
        for (int i = 0; i < 4; i++) {
            int x = newX + newPiece.x(i);
            int y = newY - newPiece.y(i);
            if (x < 0 || x >= BOARD_WIDTH || y < 0 || y >= BOARD_HEIGHT)
                return false;

            if (shapeAt(x, y) != Piece.ShapeType.None)
                return false;
        }
        curPiece = newPiece;
        curX = newX;
        curY = newY;
        repaint();
        return true;
    }

    // clear the bottom line
    private void removeFullLines() {
        int numFullLines = 0;

        for (int i = BOARD_HEIGHT - 1; i >= 0; i--) {
            boolean lineIsFull = true;

            for (int j = 0; j < BOARD_WIDTH; j++) {
                if (shapeAt(j, i) == Piece.ShapeType.None) {
                    lineIsFull = false;
                    break;
                }
            }

            if (lineIsFull) {
                numFullLines++;

                for (int k = i; k < BOARD_HEIGHT - 1; k++) {
                    for (int j = 0; j < BOARD_WIDTH; j++) {
                        board[(k * BOARD_WIDTH) + j] = shapeAt(j, k + 1);
                    }
                }
            }
        }

        if (numFullLines > 0) {
            numLinesRemoved += numFullLines;

            statusbar.setText(playerName + "'s score: " + numLinesRemoved);
            isFallingFinished = true;
            curPiece.setShape(Piece.ShapeType.None);
        }
    }

    // draw the piece
    private void drawSquare(Graphics g, Piece.ShapeType shape, int x, int y) {

        Color[] colors = {new Color(0, 0, 0), new Color(112, 113, 252),
                new Color(255, 60, 60), new Color(86, 216, 137),
                new Color(254, 217, 83), new Color(76, 213, 230),
                new Color(246, 142, 80), new Color(235, 92, 130)
        };

        var color = colors[shape.ordinal()];

        g.setColor(color);
        g.fillRect(x + 1, y + 1, squareWidth - 2, squareHeight - 2);

        g.setColor(color.brighter());
        g.drawLine(x, y + squareHeight - 1, x, y);
        g.drawLine(x, y, x + squareWidth - 1, y);

        g.setColor(color.darker());
        g.drawLine(x + 1, y + squareHeight - 1,
                x + squareWidth - 1, y + squareHeight - 1);
        g.drawLine(x + squareWidth - 1, y + squareHeight - 1,
                x + squareWidth - 1, y + 1);
    }

    private class GameCycle implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            doGameCycle();
        }
    }

    private void doGameCycle() {
        update();
        repaint();
    }

    private void update() {
        if (isPaused) {
            return;
        }

        if (isFallingFinished) {
            isFallingFinished = false;
            newPiece();
        } else {
            oneLineDown();
        }
    }

    // handel key press and send the commend to server
    class TKeyAdapter extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {
            if (curPiece.getShape() == Piece.ShapeType.None) {
                return;
            }

            int keycode = e.getKeyCode();

            switch (keycode) {
                case KeyEvent.VK_P -> {
                    parent.sendToServer("COMMEND:PAUSE");
                    pausePress();
                }
                case KeyEvent.VK_LEFT -> {
                    parent.sendToServer("COMMEND:LEFT");
                    tryMove(curPiece, curX - 1, curY);
                }
                case KeyEvent.VK_RIGHT -> {
                    parent.sendToServer("COMMEND:RIGHT");
                    tryMove(curPiece, curX + 1, curY);
                }
                case KeyEvent.VK_DOWN -> {
                    parent.sendToServer("COMMEND:DOWN");
                    tryMove(curPiece.rotateClockwise(), curX, curY);
                }
                case KeyEvent.VK_UP -> {
                    parent.sendToServer("COMMEND:UP");
                    tryMove(curPiece.rotateCounterclockwise(), curX, curY);
                }
                case KeyEvent.VK_SPACE -> {
                    parent.sendToServer("COMMEND:SPACE");
                    dropDown();
                }
                case KeyEvent.VK_D -> {
                    parent.sendToServer("COMMEND:DROP");
                    oneLineDown();
                }
            }
        }
    }
}
