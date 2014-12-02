package com.akqa.glass.recipie;

import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.mime.TypedFile;



public class CamFindParser {
    private static final String API_URL = "https://camfind.p.mashape.com";
    private static final String TAG = "CamFindParser";
    //Request parameters
    static final String locale = "en_US";
    static final String language = "en";
    //Request response
    RequestData requestResponse = null;

    //This object will hold the response from the CamFind api (so far it only works for the request)
    class RequestData {
        String token;
        String status;
        String name;
        String reason;
    }

    interface CamFindRequest{
        @Multipart
        @Headers("X-Mashape-Key: Fhn5jZi5ixmshwnJMy7CGyj5yDCnp15DTQZjsniuwpVHfYHvFJ")
        @POST("/image_requests/")
        RequestData data(@Part("image_request[image]") TypedFile pic, @Part("image_request[language]") String language, @Part("image_request[locale]") String locale);
    }

    interface CamFindResponse{
        @Headers("X-Mashape-Key: Fhn5jZi5ixmshwnJMy7CGyj5yDCnp15DTQZjsniuwpVHfYHvFJ")
        @GET("/image_responses/{token}")
        RequestData data(@Path("token") String uniqueToken);
    }

    // constructor
    public CamFindParser() {
    }

    public RequestData getCamFind(String type, String funcInput) {
        Log.d(TAG, "Inside Parser");
        RestAdapter restAdapter = new RestAdapter.Builder()
//                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint(API_URL)
                .build();
        /*
         *  Request processing from API
         */
        if(type == "request"){
            CamFindRequest request = restAdapter.create(CamFindRequest.class);
            requestResponse = request.data(new TypedFile("image/jpeg", new File(funcInput)), language, locale);
        }
        /*
         *  Receive response from API
         */
        else if(type == "response"){
            CamFindResponse response = restAdapter.create(CamFindResponse.class);
            requestResponse = response.data(funcInput);
        }
        return requestResponse;
    }
}

