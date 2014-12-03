package com.akqa.glass.recipie;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by torti_000 on 12/1/2014.
 */
public class RecipesActivity extends Activity {

    String object;
    private String url;
    String bgImg = null;
    String title = null;
    private JSONObject json;
    private JSONArray recipeArray;
    JSONParser jParser = new JSONParser();
    private static final String TAG = "Recipes Activity";
    private CardScrollAdapter mAdapter;
    private CardScrollView mCardScroller;
    private CardScrollView mRecipeScroller;
    private ArrayList<String> imgUrls;
    private ArrayList<Bitmap> bgImages;
    private BitmapDrawable bgBitmap;
    private boolean imagesDownloaded = false;
    private Context context;
    private Integer resultIterator = 0;
    private View mView;
    private static final Integer NUM_RECIPES = 5;

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent recipeActivity = getIntent();
        context = this;
        object = recipeActivity.getStringExtra("Object");
        //Loading
        mView = buildView();
        mCardScroller = new CardScrollView(this);
        mCardScroller.setAdapter(new CardScrollAdapter() {
            @Override
            public int getCount() {
                return 1;
            }

            @Override
            public Object getItem(int position) {
                return mView;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return mView;
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
        setContentView(mCardScroller);
        recipeArray = new JSONArray();
        bgImages = new ArrayList<Bitmap>();
        new DownloadFilesTask().execute(object);
    }

    private View buildView() {
        Log.d(TAG, "Creating Loading Card");
        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.TITLE);
        card.setText("Loading");
        return card.getView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.recipe_share, menu);
        return true;
    }

    private List<CardBuilder> createCards(Context context) {
        ArrayList<CardBuilder> cards = new ArrayList<CardBuilder>();
        for(int i=0; i < recipeArray.length(); i++){
            try {
                //Get Data
                title = recipeArray.getJSONObject(i).getString("title");
                bgBitmap = new BitmapDrawable(bgImages.get(i));

            } catch (JSONException e) {
                e.printStackTrace();
            }
            cards.add(i, new CardBuilder(context, CardBuilder.Layout.CAPTION)
                            .setText(title)
                            .addImage(bgBitmap)
            );
        }
        return cards;
    }

    public class DownloadFilesTask extends AsyncTask<String, String, JSONObject> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            String objectUrl = object.replace(" ", "%20");
            url = "http://www.recipepuppy.com/api/?i=" + objectUrl + "&p=1";
            Log.d("URL Output", url);
        }

        @Override
        protected JSONObject doInBackground(String... args) {
            try{
                json = jParser.getJSONFromUrl(url);
                Log.d(TAG, "Retrieved JSON");
            } catch (Exception e) {
                Log.d(TAG, "Could not Retrieve JSON");
                e.printStackTrace();
            }
            return json;
        }
        @Override
        protected void onPostExecute(JSONObject json) {
            if(json != null){
                Log.v(TAG, json.toString());
                try {
                    JSONArray jArr = json.getJSONArray("results");
                    if(jArr == null){
                        Log.d(TAG, "No Results");
                    }
                    else{
                        imgUrls = new ArrayList<String>();
                        for(int i = 0 ; i < NUM_RECIPES ; i++){
                            JSONObject jObj = jArr.getJSONObject(i);
                            JSONObject recipeJson = new JSONObject();
                            recipeJson.put("title", jObj.getString("title"));
                            recipeJson.put("bgImg", jObj.getString("thumbnail"));
                            recipeJson.put("link", jObj.getString("href"));
                            imgUrls.add(jObj.getString("thumbnail"));
                            new ImageDownloader().execute(imgUrls.get(i));
                            resultIterator++;
//                            Log.d("JSON Debug " + Integer.toString(i), recipeJson.toString());
                            recipeArray.put(i, recipeJson);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                startCards();
            }
        }
    }

    public class ImageDownloader extends AsyncTask<String, Void, Bitmap> {
        Integer iterator = resultIterator;
        Bitmap tempBGIMG = null;
        @Override
        protected Bitmap doInBackground(String... urls) {
            Log.d(TAG, "Getting image " + iterator);
            String url = urls[0];
            if(url == null){
                url = "http://www.joyfulbelly.com/Ayurveda/images/recipe_book.gif";
            }
            Log.d(TAG, "url is: " + url);
            try {
                InputStream in = new java.net.URL(url).openStream();
                tempBGIMG = BitmapFactory.decodeStream(in);
                Log.d(TAG, tempBGIMG.toString());
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
            }
            if(tempBGIMG != null){
//                bgImages.add(iterator, tempBGIMG);
                bgImages.add(tempBGIMG);
                Log.d(TAG, "Image added to array. There are now " + bgImages.size() + " images");
            }
            else{ Log.d(TAG, "Downloaded image is null");}
            return tempBGIMG;
        }

//        @Override
//        protected void onPostExecute(Bitmap result) {
//            Log.d(TAG, "In post execute");
//            if(tempBGIMG != null){
////                bgImages.add(iterator, tempBGIMG);
//                bgImages.add(tempBGIMG);
//                Log.d(TAG, "Image added to array. There are now " + bgImages.size() + " images");
//            }
//            else{ Log.d(TAG, "Downloaded image is null");}
//        }
    }

    private void startCards(){
        if(bgImages.size()== NUM_RECIPES){
            resultIterator = 0;
            mAdapter = new CardAdapter(createCards(context));
            mRecipeScroller = new CardScrollView(context);
            mRecipeScroller.setAdapter(mAdapter);
            mRecipeScroller.activate();
            setContentView(mRecipeScroller);
            // Handle the TAP event.
            mRecipeScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    openOptionsMenu();
                }
            });
        }
        else{
            try {
                Log.d(TAG, "Waiting for " + (NUM_RECIPES - bgImages.size()) + " images");
                Thread.sleep(1000);
                startCards();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
