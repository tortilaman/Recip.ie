package com.akqa.glass.recipie;

import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;

import retrofit.RestAdapter;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.mime.TypedFile;



public class JSONParser {
    private static final String API_URL = "https://camfind.p.mashape.com";
    static InputStream is = null;
    static JSONObject jObj = null;
    static String json = "";
    private static final String TAG = JSONParser.class.getSimpleName();
    //Request parameters
    static final String locale = "en_US";
    static final String language = "en";
    File img = null;
    TypedFile photo = null;
    //Request response
    String token = null;
    RequestData requestResponse = null;

    //This object will hold the response from the CamFind api (so far it only works for the request)
    class RequestData {
        String token;
        String status;
        String name;
        String reason;
    }

    interface CamFindRequest{
        @FormUrlEncoded
        @Headers("X-Mashape-Key: Fhn5jZi5ixmshwnJMy7CGyj5yDCnp15DTQZjsniuwpVHfYHvFJ")
        @POST("/image_requests/")
//        RequestData data(@Field("image_request[image]") TypedFile pic, @Field("image_request[language") String language, @Field("image_request[locale]") String locale, Callback<RequestData> cb);
        RequestData data(@Field("image_request[remote_image_url]") String pic, @Field("image_request[language") String language, @Field("image_request[locale]") String locale);
    }

    interface CamFindResponse{
        @Headers("X-Mashape-Key: Fhn5jZi5ixmshwnJMy7CGyj5yDCnp15DTQZjsniuwpVHfYHvFJ")
        @GET("/image_responses/{token}")
        RequestData data(@Path("token") String uniqueToken);
    }

    // constructor
    public JSONParser() {
    }
    public RequestData getCamFind(String type, String funcInput) {
        Log.d(TAG, "Inside Parser");
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint(API_URL)
                .build();
        /*
         *  Request processing from API
         */
        if(type == "request"){
            CamFindRequest request = restAdapter.create(CamFindRequest.class);
//            TypedFile typedFile = new TypedFile("image/jpeg", new File(input));
//            RequestData requestResponse = request.data(typedFile,language, locale, requestResponse);
            //This works, but using an already uploaded image. Need to upload my image first or something...
            requestResponse = request.data("http://upload.wikimedia.org/wikipedia/commons/1/15/Red_Apple.jpg", language, locale);
        }
        /*
         *  Receive response from API
         */
        else if(type == "response"){
            //put retrofit code here...
            CamFindResponse response = restAdapter.create(CamFindResponse.class);
            requestResponse = response.data(funcInput);
        }
        return requestResponse;
    }
}

