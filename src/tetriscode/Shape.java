package tetriscode;

import java.util.Random;

public class Shape {
    protected enum ShapeType { NoShape, LineShape, ZShape, TShape, SShape, SquareShape, LShape, MirroredLShape }
    private int[][] coordinate;
    private int[][][] coordinateTable;
    private ShapeType pieceShape;

    public Shape() {
        coordinate = new int[4][2];

        coordinateTable = new int[][][] {
                { { 0, 0 },   { 0, 0 },   { 0, 0 },   { 0, 0 } },    // NoShape
                { { 0, -1 },  { 0, 0 },   { 0, 1 },   { 0, 2 } },    // LineShape
                { { 0, -1 },  { 0, 0 },   { -1, 0 },  { -1, 1 } },   // ZShape
                { { -1, 0 },  { 0, 0 },   { 1, 0 },   { 0, 1 } },    // TShape
                { { 0, -1 },  { 0, 0 },   { 1, 0 },   { 1, 1 } },    // SShape
                { { 0, 0 },   { 1, 0 },   { 0, 1 },   { 1, 1 } },    // SquareShape
                { { -1, -1 }, { 0, -1 },  { 0, 0 },   { 0, 1 } },    // LShape
                { { 1, -1 },  { 0, -1 },  { 0, 0 },   { 0, 1 } }     // MirroredLShape
        };

        setShape(ShapeType.NoShape);
    }

    protected void setShape(ShapeType shape) {
        for (int i = 0; i < 4 ; i++) {
            System.arraycopy(coordinateTable[shape.ordinal()][i], 0, coordinate[i], 0, 2);
        }

        pieceShape = shape;
    }

    private void setX(int index, int x) { coordinate[index][0] = x; }
    private void setY(int index, int y) { coordinate[index][1] = y; }
    public ShapeType getShape()  { return pieceShape; }

    public void setRandomShape(Random r) {
        int x = Math.abs(r.nextInt()) % 7 + 1;

        ShapeType[] values = ShapeType.values();
        setShape(values[x]);
    }

    public int minY() {
        int m = coordinate[0][1];

        for (int i=0; i < 4; i++)
            m = Math.min(m, coordinate[i][1]);

        return m;
    }

    public int x(int index) { return coordinate[index][0]; }
    public int y(int index) { return coordinate[index][1]; }

    public Shape rotateClockwise() {
        if (pieceShape == ShapeType.SquareShape)
            return this;

        var result = new Shape();
        result.pieceShape = pieceShape;

        for (int i = 0; i < 4; ++i) {
            result.setX(i, y(i));
            result.setY(i, -x(i));
        }

        return result;
    }

    public Shape rotateCounterclockwise() {
        if (pieceShape == ShapeType.SquareShape)
            return this;

        var result = new Shape();
        result.pieceShape = pieceShape;

        for (int i = 0; i < 4; ++i) {
            result.setX(i, -y(i));
            result.setY(i, x(i));
        }

        return result;
    }
}

