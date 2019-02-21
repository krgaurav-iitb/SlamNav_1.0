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


public class slamMapTabFragment1 extends Fragment {

    private final static String TAG = "slamMapFragment1";
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_basic_slam_tab_map1, container, false);
        ImageView iv2 = (ImageView) rootView.findViewById(R.id.imageView);
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


        //camera plotting

        Map<Integer,float[]> mp2= null;
        //mp = basicSlam.knownLandmarkPoints;
        //

        mp2=slamCameraTabFragment.cameraPoints;


        Mat xval2= new Mat(1,mp2.size(), CvType.CV_64FC1);
        Mat yval2= new Mat(1,mp2.size(), CvType.CV_64FC1);


        int count2=0;

        for(Iterator i = mp2.keySet().iterator(); i.hasNext();){
            Integer key = (Integer) i.next();
            float[] value = mp2.get(key);

            xval2.put(0,count2,(double)value[0]);
            yval2.put(0,count2,(double)value[1]);
            /*Log.i(TAG, "mapping mat "  +Integer.toString(count)+ " are "
                    + Double.toString(xval.get(0, count)[0]) + ","
                    + Double.toString(yval.get(0, count)[0]) + ","+ yval.size() );*/
            count2++;
        }

        Plot2d plot2 = Plot2d.create(xval2,yval2);
        Mat mplot2 = new Mat();
        plot2.setInvertOrientation(true);

        plot2.setMaxX(150.00);
        plot2.setMinX(-150.00);
        plot2.setMaxY(150.00);
        plot2.setMinY(-150.00);
        plot2.setPlotLineColor(new Scalar(0,255,0));
        plot2.setPlotLineWidth(2);
        plot2.setNeedPlotLine(true);
        plot2.setShowText(false);
        plot2.setPlotAxisColor(new Scalar(0,0,255));
        plot2.render(mplot2);

        Bitmap bm2 = Bitmap.createBitmap(mplot2.cols(), mplot2.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mplot2, bm2);


        iv2.setImageBitmap(bm2);
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
