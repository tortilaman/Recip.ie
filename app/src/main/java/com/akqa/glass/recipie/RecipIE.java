package com.akqa.glass.recipie;


//Glass Specific Hello World Imports
import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
//General Android Hello World Imports
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
//Photo Imports
import android.content.Intent;
import android.os.FileObserver;
import android.provider.MediaStore;
import com.google.android.glass.content.Intents;
import com.google.android.glass.widget.Slider;
//HTTP Imports
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
//Java Imports
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * An {@link Activity} showing a tuggable "Hello World!" card.
 * <p/>
 * The main content view is composed of a one-card {@link CardScrollView} that provides tugging
 * feedback to the user when swipe gestures are detected.
 * If your Glassware intends to intercept swipe gestures, you should set the content view directly
 * and use a {@link com.google.android.glass.touchpad.GestureDetector}.
 *
 * @see <a href="https://developers.google.com/glass/develop/gdk/touch">GDK Developer Guide</a>
 */
public class RecipIE extends Activity {

    /**
     * {@link CardScrollView} to use as the main content view.
     * This is what contains the views, but the actual views are adapters
     */
    private CardScrollView mCardScroller;
    private View mView;

    private static final int TAKE_PICTURE_REQUEST = 1;
    private static final String TAG = "Recip.ie";

    String thumbnailPath;
    String picturePath;
    JSONObject json = null;
    JSONParser jParser = new JSONParser();
    private Slider mSlider;
    private Slider.Indeterminate mIndeterminate;
    private Handler getPicHandler;
    private boolean requestJsonRequested = false;
    private boolean requestJsonRetrieved = false;


    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        //Keep screen on while this is happening.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mView = buildView();
        mCardScroller = new CardScrollView(this);
        takePicture();  //Hopefully this takes a picture...

        // Handle the TAP event.
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Plays disallowed sound to indicate that TAP actions are not supported.
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.playSoundEffect(Sounds.DISALLOWED);
            }
        });
        //setContentView(mCardScroller);
        //mSlider = Slider.from(mCardScroller);

        //Try the new camera preview functionality?
//        Intent TakePhoto = new Intent(this, PreviewActivity.class);
//        startActivity(TakePhoto);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCardScroller.activate();
    }

    @Override
    protected void onPause() {
        mCardScroller.deactivate();
        super.onPause();
    }

    /**
     * Builds a Glass styled "Hello World!" view using the {@link CardBuilder} class.
     */
    private View buildView() {
        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.TEXT);

        card.setText(R.string.hello_world);
        return card.getView();
    }

    /*
     *     ,ad8888ba,
     *    d8"'    `"8b
     *   d8'
     *   88             ,adPPYYba,  88,dPYba,,adPYba,    ,adPPYba,  8b,dPPYba,  ,adPPYYba,
     *   88             ""     `Y8  88P'   "88"    "8a  a8P_____88  88P'   "Y8  ""     `Y8
     *   Y8,            ,adPPPPP88  88      88      88  8PP"""""""  88          ,adPPPPP88
     *    Y8a.    .a8P  88,    ,88  88      88      88  "8b,   ,aa  88          88,    ,88
     *     `"Y8888Y"'   `"8bbdP"Y8  88      88      88   `"Ybbd8"'  88          `"8bbdP"Y8
     *
     *  from: https://developers.google.com/glass/develop/gdk/camera#capturing_images_or_video
     */
    private void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);    //Create Camera Intent
        startActivityForResult(intent, TAKE_PICTURE_REQUEST);           //Start Intent
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK) {
            thumbnailPath = data.getStringExtra(Intents.EXTRA_THUMBNAIL_FILE_PATH);
            picturePath = data.getStringExtra(Intents.EXTRA_PICTURE_FILE_PATH);
            Log.d("Image Debug", "Thumbnail path is: " + thumbnailPath);

            processPictureWhenReady(picturePath);
            //Display thumbnail while CamFind is doing it's magic.
            Drawable thumbImage = Drawable.createFromPath(thumbnailPath);
            CardBuilder thumbnail = new CardBuilder(this, CardBuilder.Layout.CAPTION);
            thumbnail.setText("Processing");
            thumbnail.setFootnote("");
            thumbnail.setTimestamp("");
            thumbnail.addImage(thumbImage);
            //Start Scroller
            setView(thumbnail.getView());
//            mIndeterminate = mSlider.startIndeterminate();

            //Wow this is super hacky and I shouldn't have to run this twice to get this to work...
            //Fix this google!!!
            getPicHandler = new Handler();
            getPicHandler.postDelayed(getPicRunnable, 100);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    //Creates second file observer so that it actually works.......
    private Runnable getPicRunnable = new Runnable() {
        @Override
    public void run() {
            processPictureWhenReady(picturePath);
            getPicHandler.postDelayed(this, 100);
        }
    };

    private void processPictureWhenReady(final String picturePath) {
        final File pictureFile = new File(picturePath);
        //It gets in here, but the picture doesn't exist yet..
        if (pictureFile.exists()) {
//            Log.d(TAG, "Picture Exists");
            // The picture is ready; process it.
            // TODO: CamFind stuff goes here.
            new CamFindRequest().execute();



        } else {
//            Log.d(TAG, "Picture doesn't exist");
            // The file does not exist yet. Before starting the file observer, you
            // can update your UI to let the user know that the application is
            // waiting for the picture (for example, by displaying the thumbnail
            // image and a progress indicator).

            final File parentDirectory = pictureFile.getParentFile();
            FileObserver observer = new FileObserver(parentDirectory.getPath(),
                    FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
                // Protect against additional pending events after CLOSE_WRITE
                // or MOVED_TO is handled.
                private boolean isFileWritten;

                @Override
                public void onEvent(int event, String path) {
                    Log.d(TAG, "some file has been modified");
                    if (!isFileWritten) {
                        // For safety, make sure that the file that was created in
                        // the directory is actually the one that we're expecting.
                        File affectedFile = new File(parentDirectory, path);
                        isFileWritten = affectedFile.equals(pictureFile);

                        if (isFileWritten) {
                            Log.d(TAG, "Picture is Saved");
                            stopWatching();

                            // Now that the file is ready, recursively call
                            // processPictureWhenReady again (on the UI thread).
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    processPictureWhenReady(picturePath);
                                }
                            });
                        }
                    }
                }
            };
//            Log.d(TAG, "Started watching for file to write");
            observer.startWatching();
        }
    }

    /*
     *    ,ad8888ba,                                                                88
     *   d8"'    `"8b                                                               88
     *  d8'                                                                         88
     *  88              ,adPPYba,  8b,dPPYba,    ,adPPYba,  8b,dPPYba,  ,adPPYYba,  88
     *  88      88888  a8P_____88  88P'   `"8a  a8P_____88  88P'   "Y8  ""     `Y8  88
     *  Y8,        88  8PP"""""""  88       88  8PP"""""""  88          ,adPPPPP88  88
     *   Y8a.    .a88  "8b,   ,aa  88       88  "8b,   ,aa  88          88,    ,88  88
     *    `"Y88888P"    `"Ybbd8"'  88       88   `"Ybbd8"'  88          `"8bbdP"Y8  88
     */

    private void setView(View cardView){
        final View theCardView = cardView;
        mCardScroller.setAdapter(new CardScrollAdapter() {
            @Override
            public int getCount() {
                return 1;
            }

            @Override
            public Object getItem(int position) {
                return theCardView;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return theCardView;
            }

            @Override
            public int getPosition(Object item) {
                if (mView.equals(item)) {
                    return 0;
                }
                return AdapterView.INVALID_POSITION;
            }
        });
        setContentView(mCardScroller);
    }
    public class CamFindRequest extends AsyncTask<String, String, JSONObject> {
        @Override
        protected void onPreExecute() {
        //            Log.d(TAG, "Inside CamFind Request Code");
        }

        @Override
        protected JSONObject doInBackground(String... strings) {
            if(!requestJsonRetrieved){
                try{
                    //Commented out so I don't get charged a billion fucking dollars for calling the api
                    //10,000 times. FFS program work right...
                    //json = jParser.getCamFindJSON("request", picturePath);
                    json = jParser.getCamFindJSON("request", thumbnailPath);
//                    json = new JSONObject();//this is because the above line is commented out, remove it later.
                    requestJsonRetrieved = true;
                    Log.d(TAG, "Retrieved Request JSON");
                } catch (Exception e) {
                    Log.d(TAG, "Could not Retrieve JSON");
                    e.printStackTrace();
                }
            }
            else{
                json = null;
            }
            return json;
        }

        @Override
        protected void onPostExecute(JSONObject json){
            if(json != null){
                try {
                    //Start the response
                    String token = json.getString("token");
                    if(token != null){
                        Log.d(TAG, "First request pass, moving to result.");
                        new CamFindResponse().execute(token);
                    }
                } catch (JSONException e) {
                    Log.d(TAG, "No token in JSON");
                }
            }
        }
    }

    //Called by the request when it completes.
    public class CamFindResponse extends AsyncTask<String, String, JSONObject> {
        @Override
        protected void onPreExecute() {
        }

        @Override
        protected JSONObject doInBackground(String... strings) {
            try{
                //Commented out for now to prevent me from getting charged a fuckton of money...
//                json = jParser.getCamFindJSON("response", strings[0]);
                Log.d(TAG, "Retrieved Response JSON");
            } catch (Exception e) {
                Log.d(TAG, "Could not Retrieve JSON");
                e.printStackTrace();
            }
            return json;
        }

//        @Override
//        protected void onPostExecute(JSONObject json){
//            String status = null;
//            if(json != null){
//                try {
//                    status = json.getString("status");
//                    if(status.equals("completed")){
//                        String result = json.getString("result");
//                        Log.d(TAG, "CamFind result is: " + result);
//                    }
//                } catch (JSONException e) {
//                    Log.d(TAG, "String not found");
//                    e.printStackTrace();
//                }
//            }
//            else{Log.d(TAG, "JSON is null");}
//
//        }
    }
}


