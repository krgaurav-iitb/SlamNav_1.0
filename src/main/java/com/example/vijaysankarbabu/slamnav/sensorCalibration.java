package com.example.vijaysankarbabu.slamnav;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.text.DecimalFormat;

public class sensorCalibration extends AppCompatActivity implements SensorEventListener{
    private static final String TAG = "SensorCalibrationResult";
    private static SensorManager mSensorManager;
    private static Sensor mSensor;

    float[] avgAccValues= new float[3];
    private int countForAvg;
    ProgressDialog pd;
    static DecimalFormat df = new DecimalFormat("#.###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_calibration);
        countForAvg =0 ;
        avgAccValues[0]=0;
        avgAccValues[1]=0;
        avgAccValues[2]=0;
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorManager.unregisterListener(this);
    }

    public void startSensorCalibration(View view){
        countForAvg =0 ;
        avgAccValues[0]=0;
        avgAccValues[1]=0;
        avgAccValues[2]=0;
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        pd = new ProgressDialog(sensorCalibration.this);
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.setMessage("Calibrating Sensors..");
        pd.setIndeterminate(true);
        pd.setCanceledOnTouchOutside(false);
        pd.show();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() != Sensor.TYPE_LINEAR_ACCELERATION) {
            return;
        }
        avgAccValues[0] += sensorEvent.values[0];
        avgAccValues[1] += sensorEvent.values[1];
        avgAccValues[2] += sensorEvent.values[2];
        countForAvg++;
        if(countForAvg==1000){
            mSensorManager.unregisterListener(this);
            avgAccValues[0] =avgAccValues[0]/(countForAvg-1);
            avgAccValues[1] =avgAccValues[1]/(countForAvg-1);
            avgAccValues[2] =avgAccValues[2]/(countForAvg-1);
            pd.dismiss();
            (Toast.makeText(sensorCalibration.this, "Calibration Done with"+Integer.toString(countForAvg)+" samples\n" +
                    "xAcc : "+ df.format(avgAccValues[0])+
                    "\nyAcc: "+df.format(avgAccValues[1])+
                    "\nzAcc: "+df.format(avgAccValues[2]), Toast.LENGTH_SHORT)).show();
            saveSensorCalibrationData(avgAccValues, sensorCalibration.this);
        }
    }

    private void saveSensorCalibrationData(float[] avgAccValues, Activity activity){
        SharedPreferences sharedPref = activity.getSharedPreferences("com.example.slamNav.sensorCalibrationValues", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        for(int i=0;i<3;i++) {
            editor.putFloat(Integer.toString(i), (float) avgAccValues[i]);
        }
        Log.i(TAG, "Calibration results saved" + "xAcc : "+ df.format(avgAccValues[0])+
                "\nyAcc: "+df.format(avgAccValues[1])+
                "\nzAcc: "+df.format(avgAccValues[2])) ;
        editor.commit();
    }

    public static boolean readSensorCalibrationData(float[] avgAccValues,Activity activity){
        SharedPreferences sharedPref = activity.getSharedPreferences("com.example.slamNav.sensorCalibrationValues", Context.MODE_PRIVATE);
        if (sharedPref.getFloat("0", -1) == -1) {
            Log.i(TAG, "No previous calibration results found");
            return false;
        }
        for(int i=0;i<3;i++) {
            avgAccValues[i]=sharedPref.getFloat(Integer.toString(i), -1);
        }
        Log.i(TAG, "Calibration results read are" + "xAcc : "+ df.format(avgAccValues[0])+
                "\nyAcc: "+df.format(avgAccValues[1])+
                "\nzAcc: "+df.format(avgAccValues[2]));
        return true;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    protected void onDestroy(){
        super.onDestroy();
        mSensorManager.unregisterListener(this);
    }
}
