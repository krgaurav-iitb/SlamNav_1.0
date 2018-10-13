package com.example.vijaysankarbabu.slamnav;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;
import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java3");
        System.loadLibrary("arucoMarker");
        //System.loadLibrary("opencv_java");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        //TextView tv = (TextView) findViewById(R.id.sample_text);
        //tv.setText(stringFromJNI());

        /*TextView textView = (TextView)findViewById(R.id.sample_text);
        textView.setText(stringFromJNI());

        //DRS 20160822b - Added 'if/else'
        if (!OpenCVLoader.initDebug()) {
            textView.setText(textView.getText() + "\n OpenCVLoader.initDebug(), not working.");
        } else {
            textView.setText(textView.getText() + "\n OpenCVLoader.initDebug(), WORKING.");
            //DRS 20160822c Added 1
            textView.setText(textView.getText() + "\n" + validate(0L, 0L));
        }*/
    }



    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    public native String validate(long matAddrGr, long matAddrRgba);

    public void launchCalibration(View view) {
        Intent intent= new Intent(this,CameraCalibrationActivity.class);
        startActivity(intent);
    }

    public void launchSensorActivity(View view) {
        Intent intent= new Intent(this,sensorData.class);
        startActivity(intent);
    }

    public void launchMarkerDetectionActivity(View view) {
        Intent intent= new Intent(this,markerDetection.class);
        startActivity(intent);
    }

    public void launchSensorCalibration(View view){
        Intent intent= new Intent(this,sensorCalibration.class);
        startActivity(intent);
    }

    public void launchBasicSlamActivity(View view){
        Intent intent= new Intent(this,basicSlam.class);
        startActivity(intent);
    }

}
