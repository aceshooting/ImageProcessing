package com.aceshooting.solarsimulation;

import net.e175.klaus.solarpositioning.AzimuthZenithAngle;
import net.e175.klaus.solarpositioning.DeltaT;
import net.e175.klaus.solarpositioning.SPA;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Created by assaa_000 on 01/05/2016.
 */
public class Ehden {

    private static double latitude=34.2882; //ehden lat
    private static double longitude= 35.9860;
    private static double elevation=1435.53 ; // elevation (m)
    private static double airPressure=818;
    private static double temperature=8; // avg. air temperature (°C)

    public static void main(String[] args){
        GregorianCalendar dateTime= new GregorianCalendar(TimeZone.getTimeZone("Asia/Beirut"));

        try {
            //15 December	sunrise 06:34	sunset 16:27
            // sunrise: 06:30 sunset 16:30 => 600 min => 36000 sec
            //21 july: noon: 12:43 -> 99 s/ day
            //calc: 9h+
            double f;
            PrintWriter out=new PrintWriter(new File("ehden.csv"));

            double a,b,c;
            double v1,v2,v3,v4,v5,v6;
            dateTime.set(2016, Calendar.DECEMBER, 21, 6, 39, 0);
            AzimuthZenithAngle result = getPosition(dateTime);
            System.out.println("sunrise: "+result.getAzimuth()+" , "+result.getZenithAngle());
            v1=result.getAzimuth();
            v2=result.getZenithAngle();

            dateTime.set(2017, Calendar.JUNE, 21, 12, 38, 0);
            result = getPosition(dateTime);
            System.out.println("noon: "+result.getAzimuth()+" , "+result.getZenithAngle());
            v3=result.getAzimuth();
            v4=result.getZenithAngle();

            dateTime.set(2017, Calendar.DECEMBER, 21, 16, 30, 0);
            result = getPosition(dateTime);
            System.out.println("sunrise: "+result.getAzimuth()+" , "+result.getZenithAngle());
            v5=result.getAzimuth();
            v6=result.getZenithAngle();

            a=(v6-v4)/(v1*v1-2*v1*v3+v3*v3);
            c=v4+a*v3*v3;
            b=-2*a*v3;

//            double x=a*v1*v1+b*v1+c;
//            double y=a*v3*v3+b*v3+c;
//            double z=a*v5*v5+b*v5+c;
//            System.out.println("V2: "+v2+" , "+x);
//            System.out.println("V4: "+v4+" , "+y);
//            System.out.println("V6: "+v6+" , "+z);



            SimpleDateFormat sdf= new SimpleDateFormat("dd/MM/yyyy HH:mm:ss ");
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Beirut"));

            double totalerr=0;
            double localx,localy;
            dateTime.set(2016, Calendar.DECEMBER, 21, 6, 39, 0);
            for(int i=0;i<365;i++){
                localx=(v5-v1)*i/364+v1;
                localy=a*localx*localx+b*localx+c;
                dateTime=resolveDate(dateTime,localx,localy);
                result =getPosition(dateTime);
                out.println(sdf.format(dateTime.getTime())+" , "+result.getAzimuth()+" , "+result.getZenithAngle());
                totalerr+=Math.sqrt((localx-result.getAzimuth())*(localx-result.getAzimuth())+(localy-result.getZenithAngle())*(localy-result.getZenithAngle()));

                dateTime.add(Calendar.SECOND,3600*24);
            }

            System.out.println("total err "+totalerr);
            System.out.println("avg: "+(totalerr/365));
            out.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


    }

    private static GregorianCalendar resolveDate(GregorianCalendar dateTime, double localx, double localy) {
        GregorianCalendar date = (GregorianCalendar) dateTime.clone();
        double maxErr=Double.MAX_VALUE;
        int bestm=0;
        for(int m=0;m<500;m++){
            date.add(Calendar.SECOND,1);
            AzimuthZenithAngle res= getPosition(date);
            if(res.getAzimuth()<localx){
                continue;
            }
            double err=Math.sqrt((localx-res.getAzimuth())*(localx-res.getAzimuth())+(localy-res.getZenithAngle())*(localy-res.getZenithAngle()));
            if(err<maxErr){
                maxErr=err;
                bestm=m;
            }

        }
        date = (GregorianCalendar) dateTime.clone();
        date.add(Calendar.SECOND,bestm);
        System.out.println("Best m: "+bestm+" best err "+maxErr);
        return date;
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
