package com.example.sanjeevg.memorygame;

import android.app.Activity;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;
import java.io.InputStream;
import android.app.ProgressDialog;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;
import android.util.LruCache;

import org.json.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.HttpResponseException;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import java.io.BufferedReader;
import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import android.util.Log;
import java.util.HashMap;
import java.util.ArrayList;
import android.widget.ListView;


import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class MemoryGame extends AppCompatActivity {

//    String URL = "http://www.androidbegin.com/wp-content/uploads/2013/07/HD-Logo.gif";
    String URL = "https://api.flickr.com/services/feeds/photos_public.gne";
//    String URL = "https://farm6.staticflickr.com/5471/30539107594_620df4836c_b.jpg";
    ImageView image;
    ProgressDialog mProgressDialog;
    Button button;
    JSONObject jsonobject;
    JSONArray jsonarray;
    ListView listview;
//    ListViewAdapter adapter;
    ArrayList<HashMap<String, String>> arraylist;
    ArrayList<String> imageJpgUrls;
    static ArrayList<Bitmap> imageBitmaps;
    XmlPullParserFactory pullParserFactory;

    private static final int MENU_NEW_GAME = 1;
    private static final int MENU_RESUME   = 2;
    private static final int MENU_EXIT     = 3;

    private MemoryGameThread mGameThread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_memory_game);

//        new LoadImage().execute("http://www.androidbegin.com/wp-content/uploads/2013/07/HD-Logo.gif");

        final MemoryGameView mGameView = (MemoryGameView) findViewById(R.id.main);
        mGameView.setStatusView((TextView) findViewById(R.id.status));
        mGameView.setScoreView((TextView) findViewById(R.id.score));

        // Locate the ImageView in activity_main.xmlMemoryGrid
        image = (ImageView) findViewById(R.id.image);
        imageBitmaps = new ArrayList<Bitmap>();

        // Locate the Button in activity_main.xml
        button = (Button) findViewById(R.id.button);

//         Capture button click
        button.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                // Execute DownloadImage AsyncTask

                image.setVisibility(View.GONE);
                new GetImageUrls().execute();
                button.setVisibility(View.GONE);
            }

        });

        mGameThread = mGameView.getGameThread();
//        if (savedInstanceState != null) {
//            mGameThread.restoreState(savedInstanceState);
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGameThread.pause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mGameThread.saveState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_NEW_GAME, 0, R.string.menu_new_game);
        menu.add(0, MENU_RESUME, 0, R.string.menu_resume);
        menu.add(0, MENU_EXIT, 0, R.string.menu_exit);

        return true;
    }

    public static ArrayList<Bitmap> getImageViews(){
        return imageBitmaps;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case MENU_NEW_GAME:
                mGameThread.startNewGame();
                break;
            case MENU_EXIT:
                finish();
                break;
            case MENU_RESUME:
                mGameThread.unPause();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (mGameThread.onBack()) {
            finish();
        }
    }

    // DownloadImage AsyncTask
    private class DownloadImage extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Create a progressdialog
            mProgressDialog = new ProgressDialog(MemoryGame.this);
            // Set progressdialog title
            mProgressDialog.setTitle("Download Image Tutorial");
            // Set progressdialog message
            mProgressDialog.setMessage("Loading...");
            mProgressDialog.setIndeterminate(false);
            // Show progressdialog
    //            mProgressDialog.show();
        }

        @Override
        protected Bitmap doInBackground(String... URL) {

            String imageURL = URL[0];

            Bitmap bitmap = null;
            try {
                // Download Image from URL
                InputStream input = new java.net.URL(imageURL).openStream();
                // Decode Bitmap
                bitmap = BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            // Set the bitmap into ImageView
            image.setImageBitmap(result);
            imageBitmaps.add(result);

            // Close progressdialog
//            mProgressDialog.dismiss();
        }
    }


    /**
     * Async task class to get json by making HTTP call
     */
    private class GetImageUrls extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
//            mProgressDialog = new ProgressDialog(MemoryGame.this);
//            mProgressDialog.setMessage("Please wait...");
//            mProgressDialog.setCancelable(false);
//            mProgressDialog.show();

        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpRequestHandler sh = new HttpRequestHandler();

            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(URL);

            if (jsonStr != null) {
                try {

//                    JSONObject jsonObj = new JSONObject(jsonStr);
                    pullParserFactory = XmlPullParserFactory.newInstance();
                    XmlPullParser parser = pullParserFactory.newPullParser();
                    InputStream stream = new ByteArrayInputStream(jsonStr.getBytes(StandardCharsets.UTF_8));
                    parser.setInput(stream, null);
                     imageJpgUrls= parseXML(parser);



                } catch (XmlPullParserException e) {
                    e.printStackTrace();

                }catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Couldn't get json from server. Check LogCat for possible errors!",
                                Toast.LENGTH_LONG)
                                .show();
                    }
                });

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            for (String imageUrl : imageJpgUrls)
            {
                new DownloadImage().execute(imageUrl);
            }
            // Dismiss the progress dialog once all download is done

//            if (mProgressDialog.isShowing())
//                mProgressDialog.dismiss();
        }

    }

    private ArrayList<String> parseXML(XmlPullParser parser) throws XmlPullParserException,IOException
    {
        ArrayList<String> imageUrls = new ArrayList<String>();
        int eventType = parser.getEventType();
       String imageUrl = null;

        while (eventType != XmlPullParser.END_DOCUMENT){
            String name;
            switch (eventType){
                case XmlPullParser.START_DOCUMENT:
                    imageUrls = new ArrayList<String>();
                    break;
                case XmlPullParser.START_TAG:
                    name = parser.getName();
                    if (name.equals("link")){
                        imageUrl = new String();
                        String relType = parser.getAttributeValue(null, "type");
                       if(relType != null && relType.contains("image/jpeg")){
                            imageUrl = parser.getAttributeValue(null,"href");
                            imageUrls.add(imageUrl);
                        }
                    }

                    break;
                case XmlPullParser.END_TAG:

            }
            //break after 9 images
            if(imageUrls.size()==9)
                break;;
            eventType = parser.next();
        }

        return imageUrls;

    }
}
