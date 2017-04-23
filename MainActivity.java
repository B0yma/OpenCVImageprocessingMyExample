package com.boymaws.imageprocessing;

import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {


    private ImageView iv;
    private Bitmap yourSelectedImage;
    private File apppath;
    private File imgspath;
    private ProgressDialogFragment progressDialogFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        //INIT APP FOLDER
        //if sd card exist
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            //local_main_path = main_sd_path;
            System.out.println("SDcardExist");
            apppath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),getString(R.string.app_name));
            if(!apppath.exists()){
                apppath.mkdir();
            }
        }else{
            System.out.println("SDcardDontExist");
            apppath = new File(getApplicationContext().getFilesDir(),getString(R.string.app_name));
            if(!apppath.exists()){
                apppath.mkdir();
            }
            //local_main_path = main_int_path;
        }



        //INIT IMG FOLDER
        imgspath = new File(apppath.getAbsoluteFile().getAbsolutePath()+"/imgs");
        if(!imgspath.exists()){
            imgspath.mkdir();
        }

        iv = (ImageView) findViewById(R.id.imageView1);

        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, 100);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case 100:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    InputStream imageStream = null;
                    try {
                        imageStream = getContentResolver().openInputStream(selectedImage);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    yourSelectedImage = BitmapFactory.decodeStream(imageStream);

                    showProgressDialog("Jdems...");
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                startDoSmthOpenCV();
                            } catch (final OutOfMemoryError e) {

                            }
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //scannedImageView.setImageBitmap(transformed);
                                    iv.setImageBitmap(yourSelectedImage);
                                    dismissDialog();
                                }
                            });
                        }
                    });
                }
        }
    }

    @Override
    public void onResume() {
        super.onResume();



    }

    protected synchronized void showProgressDialog(String message) {
        if (progressDialogFragment != null && progressDialogFragment.isVisible()) {
            // Before creating another loading dialog, close all opened loading dialogs (if any)
            progressDialogFragment.dismissAllowingStateLoss();
        }
        progressDialogFragment = null;
        progressDialogFragment = new ProgressDialogFragment(message);
        FragmentManager fm = getFragmentManager();
        progressDialogFragment.show(fm, ProgressDialogFragment.class.toString());
    }

    protected synchronized void dismissDialog() {
        progressDialogFragment.dismissAllowingStateLoss();
    }


    private void startDoSmthOpenCV() {
        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(getApplicationContext(),"Internal OpenCV library not found",Toast.LENGTH_LONG).show();
        } else {
            Log.d("asd", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("asd", "OpenCV loaded successfully");
                    try {
                        dosmth(yourSelectedImage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    private void dosmth(Bitmap bmp) {
        Mat tmp = new Mat (bmp.getWidth(), bmp.getHeight(), CvType.CV_8UC2);
        Utils.bitmapToMat(bmp, tmp);
        Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_RGB2GRAY);
        Imgproc.GaussianBlur(tmp,tmp,new Size(11,11),0);
        Imgproc.threshold(tmp,tmp,150,255,Imgproc.THRESH_TRUNC);
        Imgproc.adaptiveThreshold(tmp,tmp,255,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,Imgproc.THRESH_BINARY,41,4);
        //Imgproc.threshold(tmp,tmp,0,255,Imgproc.THRESH_BINARY+Imgproc.THRESH_OTSU);
        //Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_GRAY2RGB, 4);
        Utils.matToBitmap(tmp, bmp);
        saveBitmap(bmp, new File(imgspath.getPath()+"/"+"asd.png"));
    }

    private void saveBitmap(Bitmap bmp, File filename) {
        System.out.println("<<<<<<<<<<<<<<saving Bitmap");
        System.out.println("loc:"+filename.getAbsolutePath());
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(filename);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
