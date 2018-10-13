package com.example.vijaysankarbabu.slamnav;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import com.example.vijaysankarbabu.slamnav.slamCameraTabFragment;

public class basicSlam extends AppCompatActivity{
    private static String TAG = "BasicSlamActivity";
    public static Map<Integer,float[]> knownLandmarkPoints= null;
    //public static SparseArray landmarkPoints =null;
    public static String landmarkDump="";
    //public landmarkDictionaryObject knownLandmarkPoints= null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_slam);
        knownLandmarkPoints = new HashMap<>();
        //landmarkPoints = new SparseArray();
    }

    public void buttonClickAddNewLandmark(View view){
        float [] newCoordinates = new float[3];
        int newCoordinateId = 0;
        EditText entryID = findViewById(R.id.newLandmarkIDValue);
        if(!(entryID.getText().toString().equals(""))) {
            newCoordinateId = Integer.parseInt(entryID.getText().toString());
        }
        EditText entryX = findViewById(R.id.newLandmarkXCoordinateValue);
        if(!(entryX.getText().toString().equals(""))) {
            newCoordinates[0] = Float.parseFloat(entryX.getText().toString());
        }
        EditText entryY = findViewById(R.id.newLandmarkYCoordinateValue);
        if(!(entryY.getText().toString().equals(""))) {
            newCoordinates[1] = Float.parseFloat(entryY.getText().toString());
        }
        EditText entryZ = findViewById(R.id.newLandmarkZCoordinateValue);
        if(!(entryZ.getText().toString().equals(""))) {
            newCoordinates[2] = Float.parseFloat(entryZ.getText().toString());
        }
        //File file1 = new File("/storage/emulated/0/Download/knownlandmarks.txt");
        //printToTextFileKnownLandmarks(newCoordinates, file1);
        knownLandmarkPoints.put(newCoordinateId,newCoordinates);
        //landmarkPoints.put(newCoordinateId,newCoordinates);


        //File file = new File("/storage/emulated/0/Download/myfile.txt");
        //printtoTextFile(newCoordinates, file);

        Toast newLandmarkToast=Toast.makeText(basicSlam.this,Integer.toString(newCoordinateId)+" "+
                Float.toString(newCoordinates[0])+":"+
                Float.toString(newCoordinates[1])+":"+
                Float.toString(newCoordinates[2]),Toast.LENGTH_SHORT);
        newLandmarkToast.setGravity(Gravity.CENTER_VERTICAL,0,300);
        newLandmarkToast.show();
    }

    public void buttonClickResetEntries(View view){
        EditText entryID = findViewById(R.id.newLandmarkIDValue);
        entryID.setText("");
        EditText entryX = findViewById(R.id.newLandmarkXCoordinateValue);
        entryX.setText("");
        EditText entryY = findViewById(R.id.newLandmarkYCoordinateValue);
        entryY.setText("");
        EditText entryZ = findViewById(R.id.newLandmarkZCoordinateValue);
        entryZ.setText("");
    }

    public void buttonClickStartTracking(View view){
        if(knownLandmarkPoints.size()>0) {
            /*Map<Integer,float[]> mp= null;
            mp = knownLandmarkPoints;

            Log.i(TAG, "onCreateView: landmarkdump started iterating");
            for(Iterator i = mp.keySet().iterator(); i.hasNext();){
                Integer key = (Integer) i.next();
                float[] value = mp.get(key);
                //landmarkDump+= Integer.toString(key);
                //Log.i(TAG, "onCreateView: landmarkdump key added");
                landmarkDump+= "\n"+Float.toString(value[0])+","+Float.toString(value[1])+","+Float.toString(value[2]);
                Log.i(TAG, "onCreateView: landmarkdump value added");
            }*/
            Intent intent= new Intent(this,basicSlamCameraTracker.class);
            //Bundle extras = new Bundle();
            //extras.putParcelable("knownLandmarkPoints",landmarkPoints);
            //intent.putExtra("knownLandmarkPoints",landmarkPoints);
            startActivity(intent);
        }else{
            (Toast.makeText(basicSlam.this,"Add atleast one landmark",Toast.LENGTH_SHORT)).show();
        }
    }

    /*public void printToTextFileKnownLandmarks(float[] d, File file_g)  {
        /*Size size = d.size();
        double [] a = new double[ ( d.width() * d.height())];
        d.get(0, 0, a);//I get byte array here for the whole image*/
        /*FileOutputStream fos_g1 = null;
        DataOutputStream dos1 = null;
        



        //OutputStreamWriter ow = null;
        //BufferedWriter fwriter = null;
        try {
            fos_g1 = new FileOutputStream(file_g);
            dos1 = new DataOutputStream(fos_g1);
            //ow = new OutputStreamWriter(fos_g);
            //fwriter = new BufferedWriter(ow);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

            int l=d.length;
            for(int i=0;i<l;i++) {
                try {
                    float k = d[i];
                    dos1.writeUTF(""+k);
                    //dos1.writeUTF(Float.toString(k));
                    //dos1.writeFloat(d[i]);
                    dos1.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }       /*try {
                fwriter.write("\n");
                ow.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }*/

        //fos_g.flush();


        /*try {
            fos_g1.flush();
            fos_g1.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/

    //}
}

