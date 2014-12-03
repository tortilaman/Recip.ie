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
    private ArrayList<String> imgUrls;
    private ArrayList<Bitmap> bgImages;
    private BitmapDrawable bgBitmap;
    private boolean imagesDownloaded = false;
    private Context context;
    private Integer resultIterator = 0;

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent recipeActivity = getIntent();
        context = this;
        object = recipeActivity.getStringExtra("Object");
        recipeArray = new JSONArray();
        bgImages = new ArrayList<Bitmap>();
        new DownloadFilesTask().execute(object);
    }

    private List<CardBuilder> createCards(Context context) {
        ArrayList<CardBuilder> cards = new ArrayList<CardBuilder>();
        for(int i=0; i < recipeArray.length(); i++){
            try {
                //Get Data
                title = recipeArray.getJSONObject(i).getString("title");
                bgImg = recipeArray.getJSONObject(i).getString("bgImg");
//                new ImageDownloader().execute(bgImg);
//                bgBitmap = new BitmapDrawable(bgImages.get(resultIterator));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            cards.add(i, new CardBuilder(context, CardBuilder.Layout.CAPTION)
                            .setText(title)
//                            .addImage(bgBitmap)
            );
//            resultIterator++;
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
                        for(int i = 0 ; i < 5 ; i++){
                            JSONObject jObj = jArr.getJSONObject(i);
                            JSONObject recipeJson = new JSONObject();
                            recipeJson.put("title", jObj.getString("title"));
                            recipeJson.put("bgImg", jObj.getString("thumbnail"));
                            recipeJson.put("link", jObj.getString("href"));
                            imgUrls.add(jObj.getString("thumbnail"));
//                            new ImageDownloader().execute(imgUrls.get(i));
//                            Log.d("JSON Debug " + Integer.toString(i), recipeJson.toString());
                            recipeArray.put(i, recipeJson);
                            resultIterator++;
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
        Bitmap tempBGIMG = null;
        @Override
        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            try {
                InputStream in = new java.net.URL(url).openStream();
                tempBGIMG = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
            }
            return tempBGIMG;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            bgImages.add(resultIterator, tempBGIMG);
        }
    }

    private void startCards(){
        mAdapter = new CardAdapter(createCards(context));
        mCardScroller = new CardScrollView(context);
        mCardScroller.setAdapter(mAdapter);
        mCardScroller.activate();
        setContentView(mCardScroller);
    }
}
