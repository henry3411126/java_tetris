package tetriscode;

import java.util.Random;

public class Piece {

    // every shape in Tetris game
    public enum ShapeType { None, Line, Z, T, S, Square, L, MirroredL}

    // the coordinate(x, y) of one type of shape
    private int[][] coordinate = new int[4][2];

    // the coordinate(x, y) of all type of shape
    private final int[][][] coordinateTable = new int[][][] {
            { { 0, 0 },   { 0, 0 },   { 0, 0 },   { 0, 0 } },    // No Shape
            { { 0, -1 },  { 0, 0 },   { 0, 1 },   { 0, 2 } },    // Line Shape
            { { 0, -1 },  { 0, 0 },   { -1, 0 },  { -1, 1 } },   // Z Shape
            { { -1, 0 },  { 0, 0 },   { 1, 0 },   { 0, 1 } },    // T Shape
            { { 0, -1 },  { 0, 0 },   { 1, 0 },   { 1, 1 } },    // S Shape
            { { 0, 0 },   { 1, 0 },   { 0, 1 },   { 1, 1 } },    // Square Shape
            { { -1, -1 }, { 0, -1 },  { 0, 0 },   { 0, 1 } },    // L Shape
            { { 1, -1 },  { 0, -1 },  { 0, 0 },   { 0, 1 } }     // Mirrored L Shape
    };

    // the type of shape that is using
    private ShapeType curShape;

    public Piece() {
        setShape(ShapeType.None);
    }

    // get the shape currently using
    public ShapeType getShape()  { return curShape; }

    // set the shape currently using in to coordinate
    protected void setShape(ShapeType shape) {
        for (int i = 0; i < 4 ; i++)
            System.arraycopy(coordinateTable[shape.ordinal()][i], 0, coordinate[i], 0, 2);

        curShape = shape;
    }

    // get x, y in coordinate
    public int x(int index) { return coordinate[index][0]; }
    public int y(int index) { return coordinate[index][1]; }

    // set (x, y) in coordinate
    private void setX(int index, int x) { coordinate[index][0] = x; }
    private void setY(int index, int y) { coordinate[index][1] = y; }

    // set the shape randomly
    public void setRandomShape(Random r) {
        int x = Math.abs(r.nextInt()) % 7 + 1;

        ShapeType[] values = ShapeType.values();
        setShape(values[x]);
    }

    // get the lowest y in the shape currently using
    public int minY() {
        int m = coordinate[0][1];
        for (int i=0; i < 4; i++)
            m = Math.min(m, coordinate[i][1]);

        return m;
    }

    // clockwise rotate the piece
    public Piece rotateClockwise() {
        if (curShape == ShapeType.Square)
            return this;
        var result = new Piece();
        result.curShape = curShape;

        for (int i = 0; i < 4; ++i) {
            result.setX(i, y(i));
            result.setY(i, -x(i));
        }
        return result;
    }

    // counterclockwise rotate the piece
    public Piece rotateCounterclockwise() {
        if (curShape == ShapeType.Square)
            return this;

        var result = new Piece();
        result.curShape = curShape;

        for (int i = 0; i < 4; ++i) {
            result.setX(i, -y(i));
            result.setY(i, x(i));
        }
        return result;
    }
}

