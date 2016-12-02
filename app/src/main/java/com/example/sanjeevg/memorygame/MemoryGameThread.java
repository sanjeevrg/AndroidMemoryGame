package com.example.sanjeevg.memorygame;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import android.os.CountDownTimer;
import android.graphics.Matrix;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuff;
/**
 * Created by sanjeevg on 30/11/16.
 */
public class MemoryGameThread extends Thread  {
    private final SurfaceHolder mSurfaceHolder;
    private final Handler mStatusHandler;
    private final Handler       mScoreHandler;
    private final Context mContext;

    ImageView image;

    private static final int STATE_READY     = 0;
    private static final int STATE_PLAYING   = 1;
    private static final int STATE_PAUSE     = 2;
    private static final int STATE_GAME_OVER = 3;

    private volatile boolean mRun;
    private final    Object  mRunLock;
    private          int     mState;

    private int mCanvasHeight;
    private int mCanvasWidth;

    private Paint[] mTilesColorPalette;
    private Paint   mBackgroundPaint;
    private Paint   mHiddenTilePaint;
    private int     mRows;
    private int     mCols;

    private MemoryGrid mGrid;
    private int  solved;
    private ImageTile mMainTile;

    private MotionEvent mTouchEvent;

    private InitialTimer mStopWatch;

    private static final int PHYS_FPS = 20;


    MemoryGameThread(final SurfaceHolder surfaceHolder,
               final Context context,
               final Handler statusHandler,
               final Handler scoreHandler) {
        mSurfaceHolder = surfaceHolder;
        mStatusHandler = statusHandler;
        mScoreHandler = scoreHandler;
        mContext = context;

        mRun = false;
        mRunLock = new Object();
        mState = STATE_READY;

        mCanvasHeight = 1;
        mCanvasWidth = 1;
        String URL = "http://www.androidbegin.com/wp-content/uploads/2013/07/HD-Logo.gif";

        // based on Solarized color palette
        mTilesColorPalette = new Paint[]{paintFromColorString("#002B36"),   // background
                paintFromColorString("#073642"),   // hidden tile
                paintFromColorString("#93a1a1"),   // tile...
                paintFromColorString("#eee8d5"),    //00
                paintFromColorString("#b58900"),    //01
                paintFromColorString("#cb4b16"),    //02
                paintFromColorString("#dc322f"),    //10
                paintFromColorString("#d33682"),    //11
                paintFromColorString("#6c71c4"),    //12
                paintFromColorString("#268bd2"),    //20
                paintFromColorString("#2aa198"),    //21
//                paintFromColorString("#859900"),  // 22...color
//                paintFromColorString("#268bd2"),    //30
//                paintFromColorString("#268bd2"),    //31
                paintFromColorString("#268bd2")};    //32

        mBackgroundPaint = mTilesColorPalette[0];
        mHiddenTilePaint = mTilesColorPalette[1];

        mRows = 4;
        mCols = 3;

        mGrid = MemoryGrid.EMPTY;
        solved = 0;

        mStopWatch = new InitialTimer();
    }

    /**
     * The game loop.
     */
    @Override
    public void run() {
        int skipTicks = 1000 / PHYS_FPS;
        long mNextGameTick = SystemClock.uptimeMillis();
        while (mRun) {
            Canvas canvas = null;
            try {
                canvas = mSurfaceHolder.lockCanvas(null);
                if (canvas != null) {
                    synchronized (mSurfaceHolder) {
                        if (mState == STATE_PLAYING) {
                            updateState();
                        }
                        synchronized (mRunLock) {
                            if (mRun) {
                                updateDisplay(canvas);
                            }
                        }
                    }
                }
            } finally {
                if (canvas != null) {
                    mSurfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
            mNextGameTick += skipTicks;
            long sleepTime = mNextGameTick - SystemClock.uptimeMillis();
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    // don't care
                }
            }
        }
    }

    void setRunning(boolean running) {
        synchronized (mRunLock) {
            mRun = running;
        }
    }

    void saveState(Bundle map) {
        synchronized (mSurfaceHolder) {
            map.putInt("state", mState);
            map.putInt("solved", solved);
            map.putSerializable("grid", mGrid);
            map.putSerializable("stopWatch", mStopWatch);
        }
    }

    void restoreState(Bundle map) {
        synchronized (mSurfaceHolder) {
            setState(map.getInt("state"));
            solved = map.getInt("solved");
            mGrid = (MemoryGrid) map.getSerializable("grid");
            mStopWatch = (InitialTimer) map.getSerializable("stopWatch");
        }
    }

    void onTouch(MotionEvent event) {
        synchronized (mSurfaceHolder) {
            switch (mState) {
                case STATE_READY:
                    startNewGame();
                    break;
                case STATE_PLAYING:
                    if (mTouchEvent == null) {
                        mTouchEvent = event;
                    }
                    updateState();
                    break;
                case STATE_PAUSE:
                    unPause();
                    break;
                case STATE_GAME_OVER:
                    setState(STATE_READY);
            }
        }
    }

    boolean onBack() {
        synchronized (mSurfaceHolder) {
            if (mState == STATE_PLAYING) {
                pause();
                return false;
            }
            return true;
        }
    }

    void pause() {
        synchronized (mSurfaceHolder) {
            if (mState == STATE_PLAYING) {
                mStopWatch.pause();
                setState(STATE_PAUSE);
            }
        }
    }

    void unPause() {
        synchronized (mSurfaceHolder) {
            mStopWatch.resume();
            setState(STATE_PLAYING);
        }
    }

    void startNewGame() {
        synchronized (mSurfaceHolder) {
            setupGrid();
            mStopWatch.start();
            new CountDownTimer(10000, 1000) {

                public void onTick(long millisUntilFinished) {
                    //mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
                }

                public void onFinish() {

                    for (int row = 0; row < mGrid.getRows(); row++) {
                        for (int col = 0; col < mGrid.getCols(); col++) {
                            ImageTile tile = mGrid.getTileAt(row, col);
                            if(row == 3 && col == 0 || row ==3 && col == 2){
                                tile.setState(ImageTile.STATE_SOLVED);
                            }else if(row ==3 && col == 1){
                                tile.setState(ImageTile.STATE_SELECTED);
                            }else{
                                tile.setState(ImageTile.STATE_HIDDEN);
                            }
                        }
                    }
                    Canvas canvas = mSurfaceHolder.lockCanvas(null);
                    //Clear canvas
                    canvas.drawColor(Color.BLACK);
                    canvas.drawColor( 0, PorterDuff.Mode.CLEAR );
//                    int first = 2;
//                    if (mSurfaceHolder.getSurface().isValid()) {
//                        Canvas c = mSurfaceHolder.lockCanvas();
//                        if (first >= 0) {
//                            c.drawARGB(255, 255, 255, 255);
//                            first--;
//                        }
//                    }
                    updateDisplay(canvas);
                    setState(STATE_PLAYING);

                }
            }.start();

        }
    }

    void setSurfaceSize(int width, int height) {
        synchronized (mSurfaceHolder) {
            mCanvasWidth = width;
            mCanvasHeight = height-100;
            if (mState == STATE_PLAYING) {
                mGrid.setCanvasWidth(mCanvasWidth);
                mGrid.setCanvasHeight(mCanvasHeight);
            }
        }
    }

    private Paint paintFromColorString(String colorString) {
        Paint paint = new Paint();
        paint.setColor(Color.parseColor(colorString));
        return paint;
    }

    private void setupGrid() {
        ArrayList<ImageTile> tiles = new ArrayList<ImageTile>();
        ArrayList<Bitmap> imageViews = MemoryGame.getImageViews();
        int i=2;
        int j;
        for(j=0;j<9;j++){
            tiles.add(new ImageTile(imageViews.get(j), i++, ImageTile.STATE_SELECTED));
            i++;
        }
        i=2;
        for(j=9;j<12;j++){
            tiles.add(new ImageTile(imageViews.get(i), i, ImageTile.STATE_SELECTED));
            i++;
        }

        Collections.shuffle(tiles);

        mGrid = new MemoryGrid(mRows, mCols, mCanvasHeight, mCanvasWidth);
        for (int row = 0; row < mRows; row++) {
            for (int col = 0; col < mCols; col++) {
                ImageTile tile = tiles.get(row * mCols + col);
                if(row == 3 && col == 0 || row ==3 && col == 2){
                    tile.setState(ImageTile.STATE_SOLVED);
                }else if(row == 3 && col == 1){
                    tile.setState(ImageTile.STATE_HIDDEN);
                    mMainTile = tile;
                }
                mGrid.setTileAt(tile, row, col);
            }
        }

        solved = 0;
    }

    private void setState(int mode) {
        synchronized (mSurfaceHolder) {
            mState = mode;
            Resources res = mContext.getResources();
            switch (mState) {
                case STATE_READY:
                    hideScoreText();
                    setStatusText(res.getString(R.string.state_ready));
                    break;
                case STATE_PLAYING:
                    hideScoreText();
                    hideStatusText();
                    break;
                case STATE_PAUSE:
//                    setStatusText(res.getString(R.string.state_pause));
                    break;
                case STATE_GAME_OVER:
                    long elapsed = mStopWatch.elapsed() / 1000;
                    long minutes = elapsed / 60;
                    long seconds = elapsed - minutes * 60;
                    StringBuilder scoreText = new StringBuilder();

                    scoreText.append(res.getString(R.string.solved_in))
                            .append(' ');
                    if (minutes > 0) {
                        scoreText.append(minutes)
                                .append(' ')
                                .append(minutes == 1 ?
                                        res.getString(R.string.minute) : res.getString(R.string.minutes))
                                .append(' ');
                    }
                    scoreText.append(seconds)
                            .append(' ')
                            .append(seconds == 1 ?
                                    res.getString(R.string.second) : res.getString(R.string.seconds));

                    setStatusText(res.getString(R.string.state_game_over));
                    setScoreText(scoreText.toString());

            }
        }
    }

    private void updateState() {
        if (mTouchEvent != null) {
            ImageTile touched = mGrid.getTileAtPoint(mTouchEvent.getX(), mTouchEvent.getY());
            if (touched != null && touched.getState() == ImageTile.STATE_HIDDEN) {
                ImageTile selected = mGrid.getSelectedTile();
                if(touched.getTileImage() == mMainTile.getTileImage()){
                    setState(STATE_GAME_OVER);
                }else{
                    touched.setState(ImageTile.STATE_SELECTED);
//                    Canvas canvas = mSurfaceHolder.lockCanvas(null);
//                    updateDisplay(canvas);
                }
            }
            mTouchEvent = null;
        }
    }


    private void updateDisplay(Canvas canvas) {
//        canvas.drawColor(mBackgroundPaint.getColor());

        // draw grid tiles
        for (int row = 0; row < mGrid.getRows(); row++) {
            for (int col = 0; col < mGrid.getCols(); col++) {
                if(row==3 &&col==0 || row ==3 &&col ==2){
                    break;
                }
                ImageTile tile = mGrid.getTileAt(row, col);

                int left = col * mGrid.getTileWidth();
                int top = row * mGrid.getTileHeight();
                RectF rect = new RectF(left + 2, top + 2, left + mGrid.getTileWidth() - 2, top + mGrid.getTileHeight() - 2);

                Paint paint;
                Bitmap imgBitmap = null;
                switch (tile.getState()) {
                    case ImageTile.STATE_HIDDEN:
                        paint = mHiddenTilePaint;
//                        canvas.drawRoundRect(rect, 2, 2, paint);
//                        imgBitmap = tile.getTileImage();
//                        int w = mGrid.getTileWidth(), h = mGrid.getTileHeight();
//
//                        Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
//                        Bitmap bm = Bitmap.createBitmap(w, h, conf);
////                        Bitmap bm = BitmapFactory.decodeResource(Resources.getSystem(), android.R.drawable.list_selector_background);
//                        Bitmap existingBmp = tile.getTileImage();
//                        Bitmap bmOverlay = Bitmap.createBitmap(existingBmp.getWidth(), existingBmp.getHeight(), existingBmp.getConfig());
//                        Canvas getCanvas = new Canvas(bmOverlay);
//                        getCanvas.drawBitmap(bm, new Matrix(), null);

                        break;
                    case ImageTile.STATE_SOLVED:
                        int animationSteps = tile.getAnimationSteps();
                        if (animationSteps > 0) {
                            paint = new Paint(mTilesColorPalette[tile.getColor()]);
                            paint.setAlpha(animationSteps * (255 / PHYS_FPS));
                            tile.setAnimationSteps(animationSteps - 1);
                        } else {
                            paint = mBackgroundPaint;
                            imgBitmap = tile.getTileImage();
                        }
                        break;
                    case ImageTile.STATE_SELECTED:
                    default:
//                        paint = mTilesColorPalette[tile.getColor()];
                        imgBitmap = tile.getTileImage();
                        canvas.drawBitmap(imgBitmap,null,rect,null);

                }
//                canvas.drawRoundRect(rect, 2, 2, paint);

            }
        }

//        Paint paint;
//        paint = mHiddenTilePaint;
//        int left = 1 * mGrid.getTileWidth();
//        int top = 3 * mGrid.getTileHeight();
//        RectF rect = new RectF(left + 2, top + 2, left + mGrid.getTileWidth() - 2, top + mGrid.getTileHeight() - 2);
//        if(mMainTile == null) {
//            setupMainTile();
//        }
//        switch (mMainTile.getState()) {
//            case ImageTile.STATE_HIDDEN:
//                paint = mHiddenTilePaint;
//                break;
//            case ImageTile.STATE_SOLVED:
//                int animationSteps = mMainTile.getAnimationSteps();
//                if (animationSteps > 0) {
//                    paint = new Paint(mTilesColorPalette[mMainTile.getColor()]);
//                    paint.setAlpha(animationSteps * (255 / PHYS_FPS));
//                    mMainTile.setAnimationSteps(animationSteps - 1);
//                } else {
//                    paint = mBackgroundPaint;
//                }
//                break;
//            case ImageTile.STATE_SELECTED:
//            default:
//                paint = mTilesColorPalette[mMainTile.getColor()];
//        }
//        canvas.drawRoundRect(rect, 2, 2, paint);

    }

    private void setStatusText(String text) {
        Message msg = mStatusHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putString("text", text);
        b.putInt("vis", View.VISIBLE);
        msg.setData(b);
        mStatusHandler.sendMessage(msg);
    }

    private void hideStatusText() {
        Message msg = mStatusHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putInt("vis", View.INVISIBLE);
        msg.setData(b);
        mStatusHandler.sendMessage(msg);
    }

    private void setScoreText(String text) {
        Message msg = mScoreHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putString("text", text);
        b.putInt("vis", View.VISIBLE);
        msg.setData(b);
        mScoreHandler.sendMessage(msg);
    }

    private void hideScoreText() {
        Message msg = mScoreHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putInt("vis", View.INVISIBLE);
        msg.setData(b);
        mScoreHandler.sendMessage(msg);
    }

}
