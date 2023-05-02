package tetriscode;

import tetriscode.Shape.Tetrominoe;

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

    private final int BOARD_WIDTH = 10;
    private final int BOARD_HEIGHT = 22;
    private final int PERIOD_INTERVAL = 300;

    private Timer timer;
    private boolean isFallingFinished = false;
    private boolean isPaused = false;
    private boolean isGameover = false;
    private int numLinesRemoved = 0;
    private int curX = 0;
    private int curY = 0;
    private JLabel statusbar;
    private Shape curPiece;
    private Tetrominoe[] board;

    // add by henry
    private Random r;
    TetrisClient parent;
    String playerName;
    boolean isPlayer1;  //is main player


    public Board(TetrisClient parent, JLabel statusbar, boolean isPlayer1) {
        this.parent = parent;
        this.statusbar = statusbar;
        this.isPlayer1 = isPlayer1;
        initBoard();
        curPiece = new Shape();
        board = new Tetrominoe[BOARD_WIDTH * BOARD_HEIGHT];
        //clearBoard();
        //repaint();
    }

    private void initBoard() {

        setFocusable(true);
        addKeyListener(new TAdapter());
    }

    private int squareWidth() {

        return (int) getSize().getWidth() / BOARD_WIDTH;
    }

    private int squareHeight() {

        return (int) getSize().getHeight() / BOARD_HEIGHT;
    }

    private Tetrominoe shapeAt(int x, int y) {

        return board[(y * BOARD_WIDTH) + x];
    }

    void waiting(){
        clearBoard();
        repaint();
    }

    void start() {
        newPiece();
        timer = new Timer(PERIOD_INTERVAL, new GameCycle());
        timer.start();
    }


    void pausePress(){
        pause();
    }

    void streamCommend(String inText){
        //System.out.println(inText);
        switch (inText) {
            case "COMMEND:PAUSE":
                pause();
                break;
            case "COMMEND:LEFT":
                tryMove(curPiece, curX - 1, curY);
                break;
            case "COMMEND:RIGHT":
                tryMove(curPiece, curX + 1, curY);
                break;
            case "COMMEND:DOWN":
                tryMove(curPiece.rotateRight(), curX, curY);
                break;
            case "COMMEND:UP":
                tryMove(curPiece.rotateLeft(), curX, curY);
                break;
            case "COMMEND:SPACE":
                dropDown();
                break;
            case "COMMEND:DROP":
                oneLineDown();
                break;
        }
    }

    public void setName(String playerName){
        this.playerName = playerName;
    }

    public void setRandomSeed(int seed){
        r = new Random(seed);
    }

    private void pause() {

        isPaused = !isPaused;

        if (isPaused) {

            statusbar.setText("Paused");
        } else{
            if(isGameover){
                var msg = String.format("Game over. " + playerName + "'s score: %d", numLinesRemoved);
                statusbar.setText(msg);
            }
            else
                statusbar.setText(playerName + "'s score: " + String.valueOf(numLinesRemoved));
        }

        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {

        super.paintComponent(g);
        doDrawing(g);
    }

    private void doDrawing(Graphics g) {

        var size = getSize();
        int boardTop = (int) size.getHeight() - BOARD_HEIGHT * squareHeight();

        for (int i = 0; i < BOARD_HEIGHT; i++) {

            for (int j = 0; j < BOARD_WIDTH; j++) {

                Tetrominoe shape = shapeAt(j, BOARD_HEIGHT - i - 1);

                if (shape != Tetrominoe.NoShape) {

                    drawSquare(g, j * squareWidth(),
                            boardTop + i * squareHeight(), shape);
                }
            }
        }

        if (curPiece.getShape() != Tetrominoe.NoShape) {

            for (int i = 0; i < 4; i++) {

                int x = curX + curPiece.x(i);
                int y = curY - curPiece.y(i);

                drawSquare(g, x * squareWidth(),
                        boardTop + (BOARD_HEIGHT - y - 1) * squareHeight(),
                        curPiece.getShape());
            }
        }
    }

    private void dropDown() {

        int newY = curY;

        while (newY > 0) {

            if (!tryMove(curPiece, curX, newY - 1)) {

                break;
            }

            newY--;
        }

        pieceDropped();
    }

    private void oneLineDown() {

        if (!tryMove(curPiece, curX, curY - 1)) {

            pieceDropped();
        }
    }

    private void clearBoard() {

        for (int i = 0; i < BOARD_HEIGHT * BOARD_WIDTH; i++) {

            board[i] = Tetrominoe.NoShape;
        }
    }

    private void pieceDropped() {

        for (int i = 0; i < 4; i++) {

            int x = curX + curPiece.x(i);
            int y = curY - curPiece.y(i);
            board[(y * BOARD_WIDTH) + x] = curPiece.getShape();
        }

        removeFullLines();

        if (!isFallingFinished) {

            newPiece();
        }
    }



    private void newPiece() {

        curPiece.setRandomShape(r);
        curX = BOARD_WIDTH / 2 + 1;
        curY = BOARD_HEIGHT - 1 + curPiece.minY();

        if (!tryMove(curPiece, curX, curY)) {

            curPiece.setShape(Tetrominoe.NoShape);
            timer.stop();
            var msg = String.format("Game over. " + playerName + "'s score: %d", numLinesRemoved);
            statusbar.setText(msg);
            isGameover = true;

            if(isPlayer1){
                parent.sendToServer("GAMEOVER:"+String.valueOf(numLinesRemoved));
                parent.updateP1Score(numLinesRemoved);
            }
            else{
                parent.updateP2Score(numLinesRemoved);
            }

            this.parent.checkEndGame();
        }
    }

    private boolean tryMove(Shape newPiece, int newX, int newY) {

        for (int i = 0; i < 4; i++) {

            int x = newX + newPiece.x(i);
            int y = newY - newPiece.y(i);

            if (x < 0 || x >= BOARD_WIDTH || y < 0 || y >= BOARD_HEIGHT) {

                return false;
            }

            if (shapeAt(x, y) != Tetrominoe.NoShape) {

                return false;
            }
        }

        curPiece = newPiece;
        curX = newX;
        curY = newY;

        repaint();

        return true;
    }

    private void removeFullLines() {

        int numFullLines = 0;

        for (int i = BOARD_HEIGHT - 1; i >= 0; i--) {

            boolean lineIsFull = true;

            for (int j = 0; j < BOARD_WIDTH; j++) {

                if (shapeAt(j, i) == Tetrominoe.NoShape) {

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

            statusbar.setText(playerName + "'s score: " + String.valueOf(numLinesRemoved));
            isFallingFinished = true;
            curPiece.setShape(Tetrominoe.NoShape);
        }
    }

    private void drawSquare(Graphics g, int x, int y, Tetrominoe shape) {

        Color colors[] = {new Color(0, 0, 0), new Color(112, 113, 252),
                new Color(255, 60, 60), new Color(86, 216, 137),
                new Color(254, 217, 83), new Color(76, 213, 230),
                new Color(246, 142, 80), new Color(235, 92, 130)
        };

        var color = colors[shape.ordinal()];

        g.setColor(color);
        g.fillRect(x + 1, y + 1, squareWidth() - 2, squareHeight() - 2);

        g.setColor(color.brighter());
        g.drawLine(x, y + squareHeight() - 1, x, y);
        g.drawLine(x, y, x + squareWidth() - 1, y);

        g.setColor(color.darker());
        g.drawLine(x + 1, y + squareHeight() - 1,
                x + squareWidth() - 1, y + squareHeight() - 1);
        g.drawLine(x + squareWidth() - 1, y + squareHeight() - 1,
                x + squareWidth() - 1, y + 1);
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

    class TAdapter extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {

            if (curPiece.getShape() == Tetrominoe.NoShape) {
                return;
            }

            int keycode = e.getKeyCode();

            // Java 12 switch expressions
            switch (keycode) {
                case KeyEvent.VK_P:
                    parent.sendToServer("COMMEND:PAUSE");
                    pause();
                    break;
                case KeyEvent.VK_LEFT:
                    parent.sendToServer("COMMEND:LEFT");
                    tryMove(curPiece, curX - 1, curY);
                    break;
                case KeyEvent.VK_RIGHT:
                    parent.sendToServer("COMMEND:RIGHT");
                    tryMove(curPiece, curX + 1, curY);
                    break;
                case KeyEvent.VK_DOWN:
                    parent.sendToServer("COMMEND:DOWN");
                    tryMove(curPiece.rotateRight(), curX, curY);
                    break;
                case KeyEvent.VK_UP:
                    parent.sendToServer("COMMEND:UP");
                    tryMove(curPiece.rotateLeft(), curX, curY);
                    break;
                case KeyEvent.VK_SPACE:
                    parent.sendToServer("COMMEND:SPACE");
                    dropDown();
                    break;
                case KeyEvent.VK_D:
                    parent.sendToServer("COMMEND:DROP");
                    oneLineDown();
                    break;
            }
        }
    }
}
