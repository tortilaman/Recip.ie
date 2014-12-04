package com.akqa.glass.recipie;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;

import com.akqa.glass.recipie.CamFindParser.RequestData;
import com.google.android.glass.content.Intents;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import com.google.android.glass.widget.Slider;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
//public class RecipIE extends Activity implements GestureDetector.BaseListener {
public class RecipIE extends Activity{

    /**
     * {@link CardScrollView} to use as the main content view.
     * This is what contains the views, but the actual views are adapters
     */
    private CardScrollView mCardScroller;
    private CardScrollView mOptionScroller;
    private View mView;

    private static final int TAKE_PICTURE_REQUEST = 1;
    private static final String TAG = "Recip.ie";
    private static final String URL_BASE = "http://www.ingredientpairings.com/?i=";

    String thumbnailPath;
    String picturePath;
    CamFindParser jParser = new CamFindParser();
    private Slider mSlider;
    private Slider.Indeterminate mIndeterminate;
    private Handler getPicHandler;
    private Handler getResponseHandler;
    private boolean requestComplete = false;
    private boolean responseComplete = false;
    private RequestData token = null;
    private CardBuilder thumbnail;
    private CardBuilder result;
    private CardAdapter mAdapter;
    private CamFindParser.RequestData object = null;
    private String cleanName = null;

    // Index of cards.
    static final int INGREDIENTS = 0;
    static final int RECIPES = 1;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        //Keep screen on while this is happening.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

//        mGestureDetector = createGestureDetector(this);

        // Initialize the gesture detector and set the activity to listen to discrete gestures.
//        mGestureDetector = new GestureDetector(this).setBaseListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCardScroller.activate();
//        mOptionScroller.activate();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCardScroller.deactivate();
//        mOptionScroller.deactivate();
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
            //Display thumbnail while CamFind is doing it's magic.
            Drawable thumbImage = Drawable.createFromPath(thumbnailPath);
            thumbnail = new CardBuilder(this, CardBuilder.Layout.CAPTION);
            thumbnail.setText("Analyzing");
            thumbnail.setFootnote("");
            thumbnail.setTimestamp("");
            thumbnail.addImage(thumbImage);
            //Start Scroller
            setView(thumbnail.getView());
            new CamFindRequest().execute(thumbnailPath);
        }
        super.onActivityResult(requestCode, resultCode, data);
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
        mCardScroller.activate();
        mSlider = Slider.from(mCardScroller);
        mIndeterminate = mSlider.startIndeterminate();
        setContentView(mCardScroller);
    }

    /**
     * Create list of API demo cards.
     */
    private List<CardBuilder> createCards(Context context) {
        ArrayList<CardBuilder> cards = new ArrayList<CardBuilder>();
        String formattedName= cleanName.substring(0, 1).toUpperCase() + cleanName.substring(1);
        cards.add(INGREDIENTS, new CardBuilder(context, CardBuilder.Layout.TITLE)
                .addImage(R.drawable.ingredients)
                .setText(formattedName + " pairings"));
        cards.add(RECIPES, new CardBuilder(context, CardBuilder.Layout.TITLE)
                .addImage(R.drawable.recipes)
                .setText(formattedName + "Recipes"));
        return cards;
    }

    /*
     *  Show ingredients and recipes Menu
     */
    private void handleApiResults(){
        Log.d(TAG, "Handling Results");
        mAdapter = new CardAdapter(createCards(this));
        mOptionScroller = new CardScrollView(this);
        mOptionScroller.setAdapter(mAdapter);
        mOptionScroller.activate();
        setContentView(mOptionScroller);
        setCardScrollerListener();
    }

    /**
     * Different type of activities can be shown, when tapped on a card.
     */
    private void setCardScrollerListener() {
        mOptionScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Clicked view at position " + position + ", row-id " + id);
                int soundEffect = Sounds.TAP;
                switch (position) {
                    case INGREDIENTS:
                        Log.d(TAG, "Value being passed to pairings is: " + cleanName);
                        Intent pairings = new Intent(RecipIE.this, PairingsActivity.class);
                        pairings.putExtra("Object", cleanName);
                        startActivity(pairings);
                        break;

                    case RECIPES:
                        Intent recipeActivity = new Intent(RecipIE.this, RecipesActivity.class);
                        recipeActivity.putExtra("Object", cleanName);
                        startActivity(recipeActivity);
                        break;

                    default:
                        soundEffect = Sounds.ERROR;
                        Log.d(TAG, "Don't show anything");
                }

                // Play sound.
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.playSoundEffect(soundEffect);
            }
        });
    }

    /*
     *   88        88  888888888888  888888888888  88888888ba      88888888ba
     *   88        88       88            88       88      "8b     88      "8b                                                                ,d
     *   88        88       88            88       88      ,8P     88      ,8P                                                                88
     *   88aaaaaaaa88       88            88       88aaaaaa8P'     88aaaaaa8P'  ,adPPYba,   ,adPPYb,d8  88       88   ,adPPYba,  ,adPPYba,  MM88MMM  ,adPPYba,
     *   88""""""""88       88            88       88""""""'       88""""88'   a8P_____88  a8"    `Y88  88       88  a8P_____88  I8[    ""    88     I8[    ""
     *   88        88       88            88       88              88    `8b   8PP"""""""  8b       88  88       88  8PP"""""""   `"Y8ba,     88      `"Y8ba,
     *   88        88       88            88       88              88     `8b  "8b,   ,aa  "8a    ,d88  "8a,   ,a88  "8b,   ,aa  aa    ]8I    88,    aa    ]8I
     *   88        88       88            88       88              88      `8b  `"Ybbd8"'   `"YbbdP'88   `"YbbdP'Y8   `"Ybbd8"'  `"YbbdP"'    "Y888  `"YbbdP"'
     *                                                                                              88
     *                                                                                              88
     */

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
            try {
                Thread.sleep(1000);
                new CamFindResponse().execute(token.token);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //Object name cleanup is in here
    public class CamFindResponse extends AsyncTask<String, String, RequestData>{
        boolean responseHandled = false;

        @Override
        protected RequestData doInBackground(String... data) {
            if(object == null || object.name == null){
                object = jParser.getCamFind("response", data[0]);
                if(object.name != null){
                    //Clean up the camfind result
                    Pattern stopWords = Pattern.compile("\\b(?:i|a|and|about|an|are|yellow|red|orange|blue|green|purple|violet|fruit|ripe|fresh)\\b\\s*", Pattern.CASE_INSENSITIVE);
                    Matcher matcher = stopWords.matcher(object.name);
                    cleanName = matcher.replaceAll("");
                    responseComplete = true;
                    Log.d(TAG, "Response Complete, your object is: " + object.name);
                }
            }
            return object;
        }
        @Override
        protected void onPostExecute(RequestData data){
            if(requestComplete && responseComplete && !responseHandled){
                Log.d(TAG, "Handling results");
                handleApiResults();
                responseHandled = true;
            }
            else if(!responseHandled){
                try {
                    Log.d(TAG, "Waiting for Response");
                    Thread.sleep(500);
                    new CamFindResponse().execute(token.token);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}


