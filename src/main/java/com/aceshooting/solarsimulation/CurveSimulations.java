package com.aceshooting.solarsimulation;

import net.e175.klaus.solarpositioning.AzimuthZenithAngle;
import net.e175.klaus.solarpositioning.DeltaT;
import net.e175.klaus.solarpositioning.SPA;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Created by assaa_000 on 01/05/2016.
 */
public class CurveSimulations {

    private static double latitude=34.2882; //ehden lat
    private static double longitude= 35.9860;
    private static double elevation=1435.53 ; // elevation (m)
    private static double airPressure=818;
    private static double temperature=8; // avg. air temperature (°C)

    public static void main(String[] args){
        GregorianCalendar dateTime= new GregorianCalendar(TimeZone.getTimeZone("Asia/Beirut"));

        try {
            PrintWriter out=new PrintWriter(new File("curves.csv"));
            dateTime.set(2016,8,1,6,0,0);
            SimpleDateFormat sdf= new SimpleDateFormat("dd/MM/yyyy HH:mm:ss ");
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Beirut"));
            int ncurve=24;

            for(int j=0;j<ncurve;j++) {
                for (int i = 0; i < 24 * 60; i++) {
                    AzimuthZenithAngle result = getPosition(dateTime);
                    dateTime.add(Calendar.MINUTE, 1);
                    out.println(sdf.format(dateTime.getTime()) + " , " + result.getAzimuth() + " , " + result.getZenithAngle());
                }
                dateTime.add(Calendar.DAY_OF_YEAR,365/ncurve);
            }
            out.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


    }


    public static AzimuthZenithAngle getPosition( GregorianCalendar dateTime){
        return SPA.calculateSolarPosition(
                dateTime, //dateTime
                latitude, // latitude (degrees)
                longitude, // longitude (degrees)
                elevation, // elevation (m)
                DeltaT.estimate(dateTime), // delta T (s)
                airPressure, // avg. air pressure (hPa)
                temperature); // avg. air temperature (°C)
    }
}
