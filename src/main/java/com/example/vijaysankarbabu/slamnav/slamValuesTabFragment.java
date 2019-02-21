package com.example.vijaysankarbabu.slamnav;


import android.nfc.Tag;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Map;

public class slamValuesTabFragment extends Fragment {
    private final static String TAG = "slamValuesFragment";
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_basic_slam_tab_values, container, false);
        TextView tv = view.findViewById(R.id.textViewValuesFragment);
    /*    Map<Integer,float[]> mp= null;
        //mp = basicSlam.knownLandmarkPoints;
        Log.i(TAG, "mapping mat "  +Integer.toString(slamCameraTabFragment.landmarkPoints.size()));
        mp=slamCameraTabFragment.landmarkPoints;

        String landmarkDump="";
        Log.i(TAG, "onCreateView: landmarkdump started iterating");
        for(Iterator i=mp.keySet().iterator();i.hasNext();){
            Integer key = (Integer) i.next();
            float[] value = mp.get(key);
            landmarkDump+= Integer.toString(key);
            Log.i(TAG, "onCreateView: landmarkdump key added");
            landmarkDump+= "("+Float.toString(value[0])+","+Float.toString(value[1])+","+Float.toString(value[2])+")\n";
            Log.i(TAG, "onCreateView: landmarkdump value added");
        }
        /*for(int i = 0; i < basicSlam.landmarkPoints.size(); i++) {
            int key = basicSlam.landmarkPoints.keyAt(i);
            // get the object by the key.
            Object value = basicSlam.landmarkPoints.get(key);
            Object[] objects = (Object[]) value;
            float[] values = (float[]) objects[0];
            landmarkDump+= Integer.toString(key);
            Log.i(TAG, "onCreateView: landmarkdump key added");
            landmarkDump+= "("+Float.toString(values[0])+","+Float.toString(values[1])+","+Float.toString(values[2])+")\n";
            Log.i(TAG, "onCreateView: landmarkdump value added");
        }*/

        /*Log.i(TAG, "onCreateView: landmarks received "+landmarkDump);
        tv.setText(landmarkDump);
        tv.setText("Camera Coordinate");*/
        tv.setText(slamCameraTabFragment.CoordinateDump);
        return view;
    }
    @Override
    public void onPause() {
        super.onPause();

    }
    @Override
    public void onDestroy() {
        super.onDestroy();

    }
    @Override
    public void onResume() {
        super.onResume();

    }


}
