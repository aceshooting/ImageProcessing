package com.aceshooting.imageprocessing;


import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codec.TIFFDecodeParam;
import com.sun.media.jai.codec.TIFFEncodeParam;
import com.sun.media.jai.codecimpl.TIFFImage;

public class ImageAverager16Bit extends JPanel implements ActionListener {

    JFileChooser chooser;
    String choosertitle;
    JButton go;
    JFrame frame;

    public static void main(String[] foo) {
        new ImageAverager16Bit();
    }

    private void marchThroughImages(List<String> fileName, String directoryName)
            throws IOException {

        Raster raster[] = new Raster[fileName.size()];
        String message = "";
        WritableRaster wRaster = null;
        ColorModel cm = null;
        int totalFiles = fileName.size();

        for (int i = 0; i < totalFiles; i++) {

            File file = new File(fileName.get(i));

            SeekableStream s = new FileSeekableStream(file);

            TIFFDecodeParam param = null;

            ImageDecoder dec = ImageCodec.createImageDecoder("tiff", s, param);

            raster[i] = dec.decodeAsRaster();

            TIFFImage image = (TIFFImage) dec.decodeAsRenderedImage();

            if (wRaster == null) { // Create a writable raster and get the color
                // model
                // We'll use this to write the image
                cm = image.getColorModel();
                wRaster = image.getData().createCompatibleWritableRaster();

            }

            message += "Images Processed " + fileName.get(i) + " width: "
                    + raster[0].getWidth() + " height: "
                    + raster[0].getHeight() + " Pixel Size: "
                    + image.getColorModel().getPixelSize() + "\n";
            s.close();

        }
        int w = raster[0].getWidth(), h = raster[0].getHeight();
        int averagePixel[][][] = new int[w][h][3];
        for (int i = 0; i < totalFiles; i++) {
            for (int width = 0; width < w; width++) {
                for (int height = 0; height < h; height++) {
                    int[] pixelA = null;

                    pixelA = raster[i].getPixel(width, height, pixelA);

                    averagePixel[width][height][0] += pixelA[0];
                    averagePixel[width][height][1] += pixelA[1];
                    averagePixel[width][height][2] += pixelA[2];
                    if (width == 0 && height == 0) {
                        System.out.println(i + ":" + pixelA[0] + ":"
                                + pixelA[1] + ":" + pixelA[1]);
                        System.out.println(i + ":"
                                + averagePixel[width][height][0] + ":"
                                + averagePixel[width][height][1] + ":"
                                + averagePixel[width][height][2]);
                    }

                    if (i == totalFiles - 1) // update the raster while
                    // processing last file
                    {
                        averagePixel[width][height][0] /= totalFiles;
                        averagePixel[width][height][1] /= totalFiles;
                        averagePixel[width][height][2] /= totalFiles;
                        wRaster.setPixel(width, height,
                                averagePixel[width][height]);
                    }
                }
            }
        }

        File file = new File(directoryName + "\\Output_"
                + System.currentTimeMillis() + ".tiff");
        FileOutputStream fileoutput = new FileOutputStream(file);

        TIFFEncodeParam encParam = null;

        ImageEncoder enc = ImageCodec.createImageEncoder("tiff", fileoutput,
                encParam);
        enc.encode(wRaster, cm);

        fileoutput.close();

        System.out.println(message);
    }

    public ImageAverager16Bit() {

        frame = new JFrame("TIFF Image Calibrator");
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        frame.getContentPane().add(this, "Center");
        frame.setSize(this.getPreferredSize());
        frame.setVisible(true);
        go = new JButton("Select Folder");
        go.addActionListener(this);
        add(go);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        go.setEnabled(false);

        chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File(
                "."));
        chooser.setDialogTitle(choosertitle);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        String directoryName = "";
        //
        // disable the "All files" option.
        //
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {

            List<String> results = new ArrayList<String>();
            File[] files = null;
            try {
                directoryName = chooser.getSelectedFile().getCanonicalPath();
                files = new File(directoryName).listFiles(new FilenameFilter() {

                    @Override
                    public boolean accept(File dir, String name) {
                        if (name.toLowerCase().endsWith("tif")
                                || name.toLowerCase().endsWith("tiff"))
                            return true;
                        else
                            return false;
                    }
                });

                for (File file : files) {
                    if (file.isFile()) {
                        System.out.println(file.getCanonicalPath());
                        results.add(file.getCanonicalPath());
                    }
                }

                marchThroughImages(results, directoryName);

            } catch (IOException e1) {
                e1.printStackTrace();
            }

        } else {
            System.out
                    .println("No Selection.Please Select the Folder containing TIFF Images.");
        }

        go.setEnabled(true);
    }

    public Dimension getPreferredSize() {
        return new Dimension(200, 200);
    }

}
