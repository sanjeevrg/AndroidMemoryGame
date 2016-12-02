package com.example.sanjeevg.memorygame;

import android.graphics.Bitmap;
import android.widget.ImageView;

import java.io.Serializable;

/**
 * Created by sanjeevg on 30/11/16.
 */
public class ImageTile implements Serializable {
    public static final int STATE_HIDDEN   = 0;
    public static final int STATE_SELECTED = 1;
    public static final int STATE_SOLVED   = 2;

    private int color;
    private Bitmap tileImage;;
    private int state;
    private int animationSteps;

    private int row;
    private int col;

    public ImageTile(Bitmap image, int color, int state) {
        this.tileImage = image;
        this.color          = color;
        this.state          = state;
        this.row            = -1;
        this.col            = -1;
        this.animationSteps = 0;
    }

    public int getColor() {
        return color;
    }

    public void setTileImage(Bitmap image) { this.tileImage = image; }

    public Bitmap getTileImage() { return this.tileImage; }

    public void setColor(int color) {
        this.color = color;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public int getAnimationSteps() {
        return animationSteps;
    }

    public void setAnimationSteps(int animationCount) {
        this.animationSteps = animationCount;
    }


}
