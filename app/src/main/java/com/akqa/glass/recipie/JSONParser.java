    package com.akqa.glass.recipie;

    import android.util.Log;

    //For if I'm using Unirest - don't it's not working...
    //import com.mashape.unirest.http.HttpResponse;
    //import com.mashape.unirest.http.JsonNode;
    //import com.mashape.unirest.http.Unirest;
    //import com.mashape.unirest.http.exceptions.UnirestException;

    import org.apache.http.HttpResponse;
    import org.apache.http.client.HttpClient;
    import org.apache.http.client.methods.HttpGet;
    import org.apache.http.client.methods.HttpPost;
    import org.apache.http.HttpEntity;
    import org.json.JSONException;
    import org.json.JSONObject;

    import java.io.BufferedReader;
    import java.io.File;
    import java.io.IOException;
    import java.io.InputStream;
    import java.io.InputStreamReader;
    //Crazy HttpPost shit
    import org.apache.http.entity.ContentType;
    import org.apache.http.entity.mime.MultipartEntityBuilder;

    import org.apache.http.impl.client.*;

    //import org.apache.http.HttpResponse;
    //import org.shaded.apache.http.HttpHeaders;

    public class JSONParser {
        static InputStream is = null;
        static JSONObject jObj = null;
        static String json = "";
        private static final String TAG = JSONParser.class.getSimpleName();
        // constructor
        public JSONParser() {
        }
        public JSONObject getCamFindJSON(String type, String input) {
            Log.d("PARSER", "Inside Parser");
            /*
             *  Request processing from API
             */
            if(type == "request"){
                //Let's make the post request parameters!!!
                MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create();
                File requestImage = new File(input);
                //Request Fields
                multipartEntity.addBinaryBody("image_request[image]", requestImage, ContentType.create("image/jpeg"), requestImage.getName());
                multipartEntity.addTextBody("image_request[locale]", "en_US");
                //Request time
                HttpClient client = new DefaultHttpClient();
                HttpPost post = new HttpPost("https://camfind.p.mashape.com/image_requests");
                post.setHeader("X-Mashape-Key", "Fhn5jZi5ixmshwnJMy7CGyj5yDCnp15DTQZjsniuwpVHfYHvFJ");
                HttpResponse response = null;
                try {
                    response = client.execute(post);
                    HttpEntity httpEntity = response.getEntity();
                    is = httpEntity.getContent();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            /*
             *  Receive response from API
             */
            else if(type == "response"){
                try {
                    HttpClient client = new DefaultHttpClient();
                    HttpGet get = new HttpGet("https://camfind.p.mashape.com/image_responses/" + input);
                    get.setHeader("X-Mashape-Key", "Fhn5jZi5ixmshwnJMy7CGyj5yDCnp15DTQZjsniuwpVHfYHvFJ");
                    HttpResponse responseGet = null;
                    responseGet = client.execute(get);
                    HttpEntity httpEntity = responseGet.getEntity();
                    is = httpEntity.getContent();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            /*
             *  Parse Response into readable JSON
             */
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

