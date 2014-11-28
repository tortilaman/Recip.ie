package com.akqa.glass.recipie;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;

import com.akqa.glass.recipie.JSONParser.RequestData;
import com.google.android.glass.content.Intents;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import com.google.android.glass.widget.Slider;

import org.json.JSONObject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.lang.annotation.Documented;

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
    private static final String URL_BASE = "http://www.ingredientpairings.com/?i=";

    String thumbnailPath;
    String picturePath;
    JSONObject json = null;
    JSONParser jParser = new JSONParser();
    private Slider mSlider;
    private Slider.Indeterminate mIndeterminate;
    private Handler getPicHandler;
    private Handler getResponseHandler;
    private boolean requestComplete = false;
    private boolean responseComplete = false;
    private RequestData token = null;
    private RequestData object = null;
    private CardBuilder thumbnail;
    private CardBuilder result;
    private String ingredientUrl = null;

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
            thumbnail = new CardBuilder(this, CardBuilder.Layout.CAPTION);
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
            // The picture is ready; process it.
//            try{
//                object = jParser.getCamFind("request", thumbnailPath);
//                Log.d(TAG, "Retrieved Request Object");
//            } catch (Exception e) {
//                Log.d(TAG, "Couldn't Retrieve Request Object");
//                e.printStackTrace();
//            }
//            try{
//                Log.d(TAG, "First request pass, moving to result.");
//                object = jParser.getCamFind("response", object.token);
//            } catch (Exception e) {
//                Log.d(TAG, "Couldn't Retrieve Response Object");
//                e.printStackTrace();
//            }
            new CamFindRequest().execute(thumbnailPath);
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

    public class CamFindRequest extends AsyncTask<String, String, RequestData>{
        @Override
        protected RequestData doInBackground(String... imgUrl) {
            if(!requestComplete){
                token = jParser.getCamFind("request", thumbnailPath);
                requestComplete = true;
                Log.d(TAG, "Request Complete, token is: " + token.token);
            }
            return token;
        }
        @Override
        protected void onPostExecute(RequestData data){
            new CamFindResponse().execute(token.token);
        }
    }

    public class CamFindResponse extends AsyncTask<String, String, RequestData>{

        @Override
        protected RequestData doInBackground(String... data) {
            if(object == null || object.name == null){
                object = jParser.getCamFind("response", data[0]);
                if(object.name != null){
                    requestComplete = true;
                    Log.d(TAG, "Response Complete, your object is: " + object.name);
                }
                else{
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            return object;
        }
        @Override
        protected void onPostExecute(RequestData data){

            if(requestComplete && !responseComplete) {
//                Drawable thumbImage = Drawable.createFromPath(thumbnailPath);
//                result = new CardBuilder(getApplicationContext(), CardBuilder.Layout.CAPTION);
//                result.setText(data.name);
//                result.setFootnote("");
//                result.setTimestamp("");
//                result.addImage(thumbImage);
//                //Start Scroller
//                setView(result.getView());
                responseComplete = true;
                new GetIngredients().execute(object.name);
            }
        }
    }

    public class GetIngredients extends AsyncTask<String, Void, String>{
        @Override
        protected String doInBackground(String... strings) {
            StringBuffer buffer = new StringBuffer();
            try {
                ingredientUrl = object.name.replaceAll(" ", "+");
                Log.d(TAG, ingredientUrl);
//                strings[0].replaceAll(" ", "+");
                Log.d(TAG, "Finding matches for " + object.name);
                Document doc  = Jsoup.connect(URL_BASE + ingredientUrl).get();
                Log.d(TAG, "Matches found for " + object.name);

                Elements ingredients = doc.select("div.main  p b");
                Log.d(TAG, "Ingredients are: " + ingredients.toString());
                for (Element ingredient : ingredients) {
                    String data = ingredient.text();
                    buffer.append("Data [" + data + "] \r\n");
                    Log.d(TAG, "Ingredient is: " + data);
                }
            }
            catch(Throwable t) {
                t.printStackTrace();
            }
            return buffer.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }
}


