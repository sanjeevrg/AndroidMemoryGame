package com.example.sanjeevg.memorygame;

import java.io.Serializable;

/**
 * Created by sanjeevg on 30/11/16.
 */
public class MemoryGrid implements Serializable {


    private final int      rows;
    private final int      cols;
    private final ImageTile[][] data;

    private int selectedRow;
    private int selectedCol;

    private int tileHeight;
    private int tileWidth;

    public static final MemoryGrid EMPTY = new MemoryGrid(0, 0, 0, 0);

    public MemoryGrid(int rows, int cols, int canvasHeight, int canvasWidth) {
        this.rows = rows;
        this.cols = cols;
        this.data = new ImageTile[rows][cols];

        this.selectedRow = -1;
        this.selectedCol = -1;

        setCanvasHeight(canvasHeight);
        setCanvasWidth(canvasWidth);
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public void setCanvasHeight(int canvasHeight) {
        if (rows > 0) {
            tileHeight = canvasHeight / rows;
        } else {
            tileHeight = 0;
        }
    }

    public void setCanvasWidth(int canvasWidth) {
        if (cols > 0) {
            tileWidth = canvasWidth / cols;
        } else {
            tileWidth = 0;
        }
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public ImageTile getTileAt(int row, int col) {
        if ((row >= 0 && row < rows) && (col >= 0 && col < cols)) {
            return data[row][col];
        }
        return null;
    }

    public void setTileAt(ImageTile tile, int row, int col) {
        if ((row >= 0 && row < rows) && (col >= 0 && col < cols)) {
            tile.setRow(row);
            tile.setCol(col);
            data[row][col] = tile;
        }
    }

    public ImageTile getTileAtPoint(float x, float y) {
        int row = (int) y / tileHeight;
        int col = (int) x / tileWidth;
        return getTileAt(row, col);
    }

    public void selectTile(ImageTile tile) {
        tile.setState(ImageTile.STATE_SELECTED);
        this.selectedRow = tile.getRow();
        this.selectedCol = tile.getCol();
    }

    public void clearSelectedTile() {
        ImageTile selected = getSelectedTile();
        if (selected != null) {
            selected.setState(ImageTile.STATE_HIDDEN);
        }
        this.selectedRow = -1;
        this.selectedCol = -1;
    }

    public ImageTile getSelectedTile() {
        return getTileAt(selectedRow, selectedCol);
    }

}
