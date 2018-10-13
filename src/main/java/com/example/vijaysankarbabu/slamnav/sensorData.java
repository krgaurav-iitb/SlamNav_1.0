package com.example.vijaysankarbabu.slamnav;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.List;

public class sensorData extends AppCompatActivity implements SensorEventListener{
    private static final String TAG="SensorDataActivity";
    private static SensorManager mSensorManager;
    private static Sensor mSensor;
    final int displayUpdateVal=1;
    int displayUpdateCount= 0;
    boolean debugAcceleration=false;

    static final float NS2S = 1.0f / 1000000000.0f;
    float[] last_acc_values = null;
    float[] last_vel_values = null;
    int [] endCheckCount= null;
    float[] velocity = null;
    float[] position = null;
    float[] linearAccCalibrationValues = new float[3];
    long last_timestamp = 0;
    long countForAvg=0;
    float[] avgAccValues= new float[3];
    final int FILTERING_PEAKS_CONSTANT=5;
    final float DISC_WINDOW_X=0.2f;
    final float DISC_WINDOW_Y=0.2f;
    final float DISC_WINDOW_Z=0.3f;
    final int END_CHECK_LIMIT= 2;
    int countForFilteringPeaks=0;

    //private LineGraphSeries<DataPoint> xAccSeries;
    //private int xAccLastX =0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_data);

        if(debugAcceleration) avgAccValues[0]=avgAccValues[1]=avgAccValues[2]=0f;

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        /*
        GraphView xAccGraph= findViewById(R.id.graphViewXAcceleration);
        xAccSeries = new LineGraphSeries<DataPoint>();
        xAccGraph.addSeries(xAccSeries);
        xAccGraph.getViewport().setScalable(true);
        xAccGraph.getViewport().setXAxisBoundsManual(true);
        xAccGraph.getViewport().setYAxisBoundsManual(true);
        xAccGraph.getViewport().setMinX(0);
        xAccGraph.getViewport().setMaxX(40);*/
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event){
        if (event.sensor.getType() != Sensor.TYPE_LINEAR_ACCELERATION)
            return;
        //float mobPosition[]=findPosition(event.values);
        //Log.i(TAG, "onSensorChanged: position estimation started"+Double.toString(event.values[0])+" " +Double.toString(event.values[1])+" "+ Double.toString(event.values[2]));
        //add graph points
        //xAccSeries.appendData(new DataPoint(xAccLastX++,event.values[0]),true,40);

        //taking average for debugging
        if(debugAcceleration) {
            avgAccValues[0] = (avgAccValues[0] * countForAvg + event.values[0]) / (countForAvg + 1);
            avgAccValues[1] = (avgAccValues[1] * countForAvg + event.values[1]) / (countForAvg + 1);
            avgAccValues[2] = (avgAccValues[2] * countForAvg + event.values[2]) / (countForAvg + 1);
            countForAvg++;
        }

        if(countForFilteringPeaks != FILTERING_PEAKS_CONSTANT){
            avgAccValues[0] += event.values[0];
            avgAccValues[1] += event.values[1];
            avgAccValues[2] += event.values[2];
            countForFilteringPeaks++;
        }else{
            event.values[0] = avgAccValues[0] / FILTERING_PEAKS_CONSTANT;
            event.values[1] = avgAccValues[1] / FILTERING_PEAKS_CONSTANT;
            event.values[2] = avgAccValues[2] / FILTERING_PEAKS_CONSTANT;
            avgAccValues[0]= avgAccValues[1] = avgAccValues [2] =0;
            countForFilteringPeaks =0;
            //getting Calibrated values and substracting them from the readings
            sensorCalibration.readSensorCalibrationData(linearAccCalibrationValues, sensorData.this);
            //event.values[0] = event.values[0]-linearAccCalibrationValues[0];
            //event.values[1] = event.values[1]-linearAccCalibrationValues[1];
            //event.values[2] = event.values[2]-linearAccCalibrationValues[2];

            //Log.i(TAG, "onSensorChanged: removed the bias using calibrated values" +Double.toString(linearAccCalibrationValues[0])+" "+Double.toString(linearAccCalibrationValues[1])+" "+ Double.toString(linearAccCalibrationValues[2]));
            //event.values[0]=event.values[0]-0.05267f;
            //event.values[1]=event.values[1]-0.125f;
            //event.values[2]=event.values[2]+0.1425f;
            //Log.i(TAG, "onSensorChanged: Discrimination window started"+Double.toString(event.values[0])+" " +Double.toString(event.values[1])+" "+ Double.toString(event.values[2]));
            //setting discrimination window
            for (int i = 0; i < 2; i++) {
                if (event.values[i] < 0.2 && event.values[i] > -0.2) {
                    event.values[i] = 0;
                }
            }
            if (event.values[2] < 0.3 && event.values[2] > -0.3) {
                event.values[2] = 0;
            }
            //Log.i(TAG, "onSensorChanged: Discrimination window done"+Double.toString(event.values[0])+" " +Double.toString(event.values[1])+" "+ Double.toString(event.values[2]));
            //integrating for velocity and position
            if (last_acc_values != null) {
                float dt = (event.timestamp - last_timestamp) * NS2S;

                for (int index = 0; index < 3; ++index) {
                    velocity[index] += last_acc_values[index] + (event.values[index] - last_acc_values[index]) * (dt / 2);
                    position[index] += last_vel_values[index] + (velocity[index] - last_vel_values[index]) * (dt / 2);
                    //velocity[index] += (event.values[index] + last_values[index])/2 * dt;
                    //position[index] += velocity[index] * dt;
                }
            }else{
                endCheckCount = new int[3];
                endCheckCount[0] = endCheckCount[1] = endCheckCount[2] = 0;
                last_acc_values = new float[3];
                last_vel_values = new float[3];
                velocity = new float[3];
                position = new float[3];
                velocity[0] = velocity[1] = velocity[2] = 0f;
                position[0] = position[1] = position[2] = 0f;
            }
            System.arraycopy(event.values, 0, last_acc_values, 0, 3);
            System.arraycopy(velocity, 0, last_vel_values, 0, 3);
            last_timestamp = event.timestamp;
            //movement end check
            for (int i = 0; i < 3; i++) {
                if (event.values[i] == 0) {
                    endCheckCount[i]++;
                }
                if (endCheckCount[i] == END_CHECK_LIMIT) {
                    endCheckCount[i] = 0;
                    velocity[i] = 0;
                    last_vel_values[i] = 0;
                }
            }

            //finding average acceleration values when phone is static

            if (displayUpdateCount == displayUpdateVal) {
                TextView accVal = (TextView) findViewById(R.id.xAcceleration);
                if (!debugAcceleration) accVal.setText(Double.toString(event.values[0]));
                if (debugAcceleration) accVal.setText(Double.toString(avgAccValues[0]));
                accVal = (TextView) findViewById(R.id.yAcceleration);
                if (!debugAcceleration) accVal.setText(Double.toString(event.values[1]));
                if (debugAcceleration) accVal.setText(Double.toString(avgAccValues[1]));
                accVal = (TextView) findViewById(R.id.zAcceleration);
                if (!debugAcceleration) accVal.setText(Double.toString(event.values[2]));
                if (debugAcceleration) accVal.setText(Double.toString(avgAccValues[2]));
                accVal = (TextView) findViewById(R.id.xPosition);
                accVal.setText(Double.toString(position[0]));
                accVal = (TextView) findViewById(R.id.yPosition);
                accVal.setText(Double.toString(position[1]));
                accVal = (TextView) findViewById(R.id.zPosition);
                accVal.setText(Double.toString(position[2]));
                displayUpdateCount = 0;
            }
            displayUpdateCount++;
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.sensor_data_optionmenu,menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        ListView lv1;
        TableLayout lv2;
        lv1 = findViewById (R.id.sensorList);
        lv2 = findViewById(R.id.sensorValuesTable);
        switch(item.getItemId()){
            case R.id.listSensorsOption:
                List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
                lv1.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceSensors));
                lv1.setVisibility(View.VISIBLE);
                lv2.setVisibility(View.INVISIBLE);
                return true;

            case R.id.listSensorDataOption:
                lv2.setVisibility(View.VISIBLE);
                lv1.setVisibility(View.INVISIBLE);
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    protected void onDestroy(){
        super.onDestroy();
        mSensorManager.unregisterListener(this);
    }

}
