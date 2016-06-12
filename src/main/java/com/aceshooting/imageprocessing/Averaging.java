package com.aceshooting.imageprocessing;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.awt.*;
/**
 * Created by assaa_000 on 11/06/2016.
 */
public class Averaging {

    public static void main(String[] args){
        long starttime;
        long endtime;
        double result;
//test modification Houssem
        starttime= System.nanoTime();
        blendInt("D:\\ace\\gala\\aligned\\");
        //  save(bufferedImage,"","result.png");
        endtime= System.nanoTime();
        result= ((double)(endtime-starttime))/(1000000000);
        System.out.println("Processed in " + result + " s");

    }


    public static void blendInt(final String dir){
        try {
            int count=0;
            File directory = new File(dir);
            File[] directoryListing = directory.listFiles();
            System.out.println("Found "+directoryListing.length+" files!");
            int width=0;
            int height=0;
            int[] r=new int[0];
            int[] g=new int[0];
            int[] b=new int[0];
            int filecounter=0;

            if (directoryListing != null) {
                for (File file : directoryListing) {
                    filecounter++;
                    if (file.getName().equals(".DS_Store") || !(file.getName().toLowerCase().contains("jpg") || file.getName().toLowerCase().contains("jpeg") || file.getName().toLowerCase().contains("png") || file.getName().toLowerCase().contains("bmp") || file.getName().toLowerCase().contains("tif") || file.getName().toLowerCase().contains("tiff"))) {
                        continue;
                    }
                    count++;
                    BufferedImage bi = ImageIO.read(file);
                    if(width==0){
                        width=bi.getWidth();
                        height=bi.getHeight();
                        r=new int[width*height];
                        g=new int[width*height];
                        b=new int[width*height];
                    }
                    else{
                        if(width!=bi.getWidth()||height!=bi.getHeight()){
                            throw new Exception("files not the same sizes");
                        }
                    }
                    int k=0;
                    Color c;
                    for(int i=0;i<width;i++){
                        for(int j=0;j<height;j++){
                            c=new Color(bi.getRGB(i,j));
                            r[k]+=c.getRed();
                            g[k]+=c.getGreen();
                            b[k]+=c.getBlue();
                            k++;
                        }
                    }
                    System.out.println("Processing "+file.getName()+" "+filecounter+"/"+directoryListing.length+" completed ");
                }
            }

            if(width!=0) {
                BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

                int k = 0;
                Color c;
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        int resr = r[k] / count;
                        int resg = g[k] / count;
                        int resb = b[k] / count;
                        c = new Color(resr, resg, resb);
                        result.setRGB(i, j, c.getRGB());
                        k++;
                    }
                }
                save(result, dir, "result.tiff");
            }
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }


    public static void save(BufferedImage frame,String path, String name){
        try {
            File outputfile = new File(path+name);
            ImageIO.write( frame, "TIFF", outputfile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
