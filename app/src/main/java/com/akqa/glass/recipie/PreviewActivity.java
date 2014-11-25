package com.akqa.glass.recipie;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Toast;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.view.WindowUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by torti_000 on 11/23/2014.
 */
public class PreviewActivity extends Activity
{
    private SurfaceView mPreview;
    private SurfaceHolder mPreviewHolder;
    private android.hardware.Camera mCamera;
    private boolean mInPreview = false;
    private boolean mCameraConfigured = false;
    private static final String TAG = "PreviewActivity";
    public static final int MEDIA_TYPE_IMAGE = 1;
    // code copied from http://developer.android.com/guide/topics/media/camera.html
    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e){
            // Camera is not available (in use or does not exist)
            System.out.println("Camera is not available");
        }
        return c; // returns null if camera is unavailable
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preview);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mPreview = (SurfaceView)findViewById(R.id.preview);
        mPreviewHolder = mPreview.getHolder();
        mPreviewHolder.addCallback(surfaceCallback);
        try{
            mCamera = Camera.open();
        } catch(Exception e){
            Log.d(TAG, "Can't access Camera");
            e.printStackTrace();
        }
        if (mCamera != null)
            startPreview();
    }
    private void configPreview(int width, int height) {
        if ( mCamera != null && mPreviewHolder.getSurface() != null) {
            try {
                mCamera.setPreviewDisplay(mPreviewHolder);
            }
            catch (IOException e) {
                Toast.makeText(PreviewActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
            if ( !mCameraConfigured ) {
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setPreviewFpsRange(30000, 30000);
                parameters.setPreviewSize(640, 360);
                mCamera.setParameters(parameters);
                mCameraConfigured = true;
            }
        }
    }
    private void startPreview() {
        if ( mCameraConfigured && mCamera != null ) {
            mCamera.startPreview();
            mInPreview = true;
        }
    }
    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        public void surfaceCreated( SurfaceHolder holder ) {
        }
        public void surfaceChanged( SurfaceHolder holder, int format, int width, int height ) {
            configPreview(width, height);
            startPreview();
        }
        public void surfaceDestroyed( SurfaceHolder holder ) {
            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
            }
        }
    };
    @Override
    public void onResume() {
        super.onResume();
        // Re-acquire the camera and start the preview.
        if (mCamera == null) {
            mCamera = getCameraInstance();
            if (mCamera != null) {
                configPreview(640, 360);
                startPreview();
            }
        }
    }
    @Override
    public void onPause() {
        if ( mInPreview ) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            mInPreview = false;
        }
        super.onPause();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //If you tap the touchpad
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            if ( mInPreview ) {
                // Plays sound.
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.playSoundEffect(Sounds.TAP);
//                mCamera.takePicture(null, null, mPicture);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
                mInPreview = false;
                String dir = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_PICTURES;
                Log.d(TAG, dir);
            }
            return false;
        } else return super.onKeyDown(keyCode, event);
    }

    // Add a listener to the Capture button
//    Button captureButton = (Button) findViewById(id.button_capture);
//    captureButton.setOnClickListener(
//            new View.OnClickListener() {
//    @Override
//    public void onClick(View v) {
//        // get an image from the camera
//        mCamera.takePicture(null, null, mPicture);
//    }
//}
//    );

    // Handle the TAP event.
//    mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//    @Override
//    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//        // Plays sound.
//        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//        am.playSoundEffect(Sounds.TAP);
//        // Toggles voice menu. Invalidates menu to flag change.
//        mVoiceMenuEnabled = !mVoiceMenuEnabled;
//        getWindow().invalidatePanelMenu(WindowUtils.FEATURE_VOICE_COMMANDS);
//    }
//});

    private PictureCallback mPicture = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "Picture taken");
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }
}
