package com.akqa.glass.recipie;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by torti_000 on 11/29/2014.
 */
public class PairingsActivity extends Activity {

    private CardAdapter mAdapter;
    private CardScrollView mCardScroller;
    private CardScrollView mIngredientScroller;

    private int mTapPosition;
    private final Handler handler = new Handler();
    private boolean mVoiceMenuEnabled = true;
    private String ingredientUrl = null;
    private static final String TAG = "Ingredients";
    private static final String URL_BASE = "http://www.ingredientpairings.com/?i=";
    private ArrayList<String> iList;
    //Number of ingredients to display per card.
    private static final int ITEMS_PER_CARD = 4;
    private int numCards;
    String object = null;
    Context context;
    private View mView;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
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
        mCardScroller.activate();
        Log.d(TAG, "View has been set");
        //Recipes
        iList = new ArrayList<String>();
        context = this;
        Intent pairings = getIntent();
        object = pairings.getStringExtra("Object");
        Log.i(TAG, "Pairings thinks object is: " + object);
        new GetIngredients().execute(object);
    }

    /**
     * Create Loding Page
     */
    private View buildView() {
        Log.d(TAG, "Creating Loading Card");
        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.TITLE);
        card.setText("Loading");
        return card.getView();
    }

    /**
     * Creates list of cards that showcase different type of {@link com.google.android.glass.widget.CardBuilder} API.
     */
    private List<CardBuilder> createCards(Context context) {
        ArrayList<CardBuilder> cards = new ArrayList<CardBuilder>();
        Log.i(TAG, "Creating Cards");
        /*
         *  Multiple ingredients per card attempt...
         */
//        Log.i(TAG, "There are " + numCards + " cards");
//        for(int i = 0; i < numCards; i++){
//            cards.add(new CardBuilder(context, CardBuilder.Layout.EMBED_INSIDE)
//                  .setEmbeddedLayout(R.layout.pairings_list));
//            int data = i * ITEMS_PER_CARD;
//            ArrayList<TextView> pairings = new ArrayList<TextView>();
//            ArrayList<Integer> ids = new ArrayList<Integer>();
//            ids.add(R.id.ingredient1);
//            ids.add(R.id.ingredient2);
//            ids.add(R.id.ingredient3);
//            ids.add(R.id.ingredient4);
//            for(int j = 0; j < ITEMS_PER_CARD; j++){
//                if(data+j < iList.size()){
//                    pairings.add((TextView) cards.get(i).getView().findViewById(ids.get(j)));
//                    pairings.get(j).setText(iList.get(data + j));
//                    Log.i("iList", iList.get(data+j));
//                    cards.get(i).getRemoteViews().setTextViewText(ids.get(j),iList.get(data+j));
////                    ((TextView) cards.get(i).getView().findViewById(ids.get(j))).setText(iList.get(data+j));
//                    Log.i("Views", ((TextView) cards.get(i).getView().findViewById(ids.get(j))).getText().toString());
//                    Log.i("ArrayList", iList.get(data+j));//The last logcat it's getting to
//                }
//            }
//        }
        /*
         *  One ingredient per card...
         */
        for(int i = 0; i < iList.size(); i++){
            cards.add(new CardBuilder(context, CardBuilder.Layout.TEXT)
                .setText(iList.get(i)));
            Log.d(TAG, "Ingredient is: " + iList.get(i));
        }
        Log.d(TAG, "Done adding pairings?");
        return cards;
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

    public class GetIngredients extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            StringBuffer buffer = new StringBuffer();
            try {
                ingredientUrl = object.replaceAll(" ", "+");
                Log.d(TAG, ingredientUrl);
//                strings[0].replaceAll(" ", "+");
                Log.d(TAG, "Finding matches for " + object);
                Document doc  = Jsoup.connect(URL_BASE + ingredientUrl).get();
                Log.d(TAG, "Matches found for " + object);
                Elements ingredients = doc.select("div.main  p b");
//                Log.d(TAG, "Ingredients are: " + ingredients.toString());
                for (Element ingredient : ingredients) {
                    String data = ingredient.text();
                    iList.add(data);
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
            // Compute the number of cards needed to display the items with 4 per card (rounding up to
            // capture the remainder).
            numCards = (int) Math.ceil((double) iList.size() / ITEMS_PER_CARD);
            startCards();
        }
    }
    private void startCards(){
        mAdapter = new CardAdapter(createCards(this));
        mIngredientScroller = new CardScrollView(this);
        mIngredientScroller.setAdapter(mAdapter);
        mIngredientScroller.activate();
        setContentView(mIngredientScroller);
    }
}
