package com.example.vijaysankarbabu.slamnav;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.ScatterChart;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.plot.Plot2d;

import java.util.Iterator;
import java.util.Map;


public class slamMapTabFragment extends Fragment {

    private final static String TAG = "slamMapFragment";
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_basic_slam_tab_map, container, false);
        ImageView iv = (ImageView) rootView.findViewById(R.id.imageView);
        // programmatically create a LineChart
        //LineChart chart = (LineChart)getView().findViewById(R.id.chart);
        //ScatterChart scatterChart = (ScatterChart) rootView.findViewById(R.id.chart1);


        //double[] xvalues = new double[1201];
        //double[] yvalues = new double[1201];
        /*for (int i=0;i<1201;i++){
            double temp = (-5+i*.01);
            xvalues[i] = temp;

            yvalues[i] = (Math.sin(temp)*Math.random());
            xval.put(0,i,xvalues[i] );
            yval.put(0,i,yvalues[i] );
        }*/

        Map<Integer,float[]> mp1= null;
        //mp = basicSlam.knownLandmarkPoints;
        //
        Log.i(TAG, "mapping mat "  +Integer.toString(slamCameraTabFragment.landmarkPoints.size()));
        mp1=slamCameraTabFragment.landmarkPoints;
        Log.i(TAG, "mapping mat "  +Boolean.toString(mp1.isEmpty())+" "+ Integer.toString(mp1.size()));

        Mat xval= new Mat(1,mp1.size(), CvType.CV_64FC1);
        Mat yval= new Mat(1,mp1.size(), CvType.CV_64FC1);


        int count=0;

        for(Iterator i = mp1.keySet().iterator(); i.hasNext();){
            Integer key = (Integer) i.next();
            float[] value = mp1.get(key);

            xval.put(0,count,(double)value[0]);
            yval.put(0,count,(double)value[1]);
            Log.i(TAG, "mapping mat "  +Integer.toString(count)+ " are "
                    + Double.toString(xval.get(0, count)[0]) + ","
                    + Double.toString(yval.get(0, count)[0]) + ","+ yval.size() );
            count++;
        }

        Plot2d plot = Plot2d.create(xval,yval);
        Mat mplot = new Mat();
        plot.setInvertOrientation(true);

        plot.setMaxX(70.00);
        plot.setMinX(-70.00);
        plot.setMaxY(70.00);
        plot.setMinY(-70.00);
        plot.setPlotLineColor(new Scalar(255,255,0));
        plot.setPlotLineWidth(2);
        plot.setNeedPlotLine(false);
        plot.setShowText(false);
        plot.setPlotAxisColor(new Scalar(0,0,255));
        plot.render(mplot);

        Bitmap bm = Bitmap.createBitmap(mplot.cols(), mplot.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mplot, bm);


        iv.setImageBitmap(bm);
        //Imgcodecs.imwrite("e:/test.png", mplot);


        return rootView;//
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
