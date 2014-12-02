package com.akqa.glass.recipie;

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

//import org.shaded.apache.http.HttpHeaders;
public class JSONParser {
    static InputStream is = null;
    static JSONObject jObj = null;
    static String json = "";
    private static final String TAG = JSONParser.class.getSimpleName();
    // constructor
    public JSONParser() {
    }
    public JSONObject getJSONFromUrl(String url) {
        // Making HTTP request
        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet get = new HttpGet(url);
//            get.setHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
            HttpResponse responseGet = client.execute(get);
            HttpEntity httpEntity = responseGet.getEntity();
            is = httpEntity.getContent();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "The Arduino webserver isn't running");
            e.printStackTrace();
        } catch(Exception e){
            Log.d(TAG, "Unknown exception");
            e.printStackTrace();
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "n");
                Log.d("Raw Data", line);
            }
            is.close();
            json = sb.toString();
        } catch (Exception e) {
            Log.e("Buffer Error", "Error converting result " + e.toString());
        }
        // try parse the string to a JSON object
        try {
            jObj = new JSONObject(json);
        } catch (JSONException e) {
            Log.e("JSON Parser", "Error parsing data " + e.toString());
            e.printStackTrace();
        } catch (Exception e) {
            Log.e("JSON Parse", "Unknown Error");
            e.printStackTrace();
        }
        // return JSON String
        return jObj;
    }
}

