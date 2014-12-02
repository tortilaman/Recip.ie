package com.akqa.glass.recipie;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;

import com.google.android.glass.widget.CardBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by torti_000 on 12/1/2014.
 */
public class RecipesActivity extends Activity {

    String object;
    private String url;
    String bgImg = null;
    private JSONObject json;
    private JSONArray recipeArray;
    JSONParser jParser = new JSONParser();
    private static final String TAG = "Recipes Activity";

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent recipeActivity = getIntent();
        object = recipeActivity.getStringExtra("Object");
        recipeArray = new JSONArray();
        new DownloadFilesTask().execute(object);
    }

    private List<CardBuilder> createCards(Context context) {
        ArrayList<CardBuilder> cards = new ArrayList<CardBuilder>();
        for(int i=0; i < recipeArray.length(); i++){
            try {
                //Get Data
//                time = (int) recipeArray.getJSONObject(i).getInt("cookTime");
//                servings = recipeArray.getJSONObject(i).getString("servings");
//                chef = recipeArray.getJSONObject(i).getString("chef");
//                title = recipeArray.getJSONObject(i).getString("title");
                bgImg = recipeArray.getJSONObject(i).getString("bgImg");
//                new ImageDownloader().execute(bgImg).get();
//                bgBitmap = new BitmapDrawable(bgImages.get(resultIterator));
                //Combine stuff
//                footnote = time + " minutes, " + servings + " Servings";
            } catch (JSONException e) {
                e.printStackTrace();
            }
            cards.add(i, new CardBuilder(context, CardBuilder.Layout.CAPTION)
//                            .setFootnote(footnote)
//                            .setTimestamp(chef)
//                            .setText(title)
//                            .addImage(bgBitmap)
//                    .getView()//For when you're using views and not CardBuilder...
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
                    JSONArray jArr = json.getJSONArray("Results");
                    if(jArr == null){
                        Log.d(TAG, "No Results");
                    }
                    else{
                        for(int i = 0 ; i < 5 ; i++){
                            JSONObject jObj = jArr.getJSONObject(i);
                            JSONObject recipeJson = new JSONObject();
                            recipeJson.put("title", jObj.getString("Title"));
                            recipeJson.put("bgImg", jObj.getString("ImageURL"));
                            recipeJson.put("link", jObj.getString("href"));
                            Log.d("JSON Debug " + Integer.toString(i), recipeJson.toString());
                            recipeArray.put(i, recipeJson);
//                            Log.d("Recipes", recipes.toString());
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //TODO: Add this back in...
//                startCards();
            }
        }
    }
}
