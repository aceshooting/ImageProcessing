package com.aceshooting.imageprocessing;


import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codec.TIFFDecodeParam;
import com.sun.media.jai.codec.TIFFEncodeParam;
import com.sun.media.jai.codecimpl.TIFFImage;

public class ImageAverager16Bit extends JPanel implements ActionListener, PropertyChangeListener {

    JFileChooser chooser;
    String choosertitle;
    JButton go;
    JFrame frame;
    ProgressMonitor progressMonitor;
    Calculate operation;

    private class Calculate extends SwingWorker<String, String> {
        private static final String _ABORTED = "Processing is aborted";
        private static final String _COMPLETED = "Processing is completed, output image is saved in the same input folder";
        private static final String _NOFILE = "No tif files found to process";

        private List<String> fileName;
        private String directoryName;

        public Calculate(List<String> fileName, String directoryName) {
            this.fileName = fileName;
            this.directoryName = directoryName;
        }

        @Override
        protected void done() {
            try {
                String result = get();

                JOptionPane.showMessageDialog(null, result, "Processing is over", JOptionPane.INFORMATION_MESSAGE);
                go.setEnabled(true);
                progressMonitor.close();
                super.done();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Override
        protected void process(java.util.List<String> chunks) {
            if (isCancelled()) {
                return;
            }

            for (String s : chunks) {
                progressMonitor.setNote(s);
            }
        }

        @Override
        protected String doInBackground() throws Exception {
            if (fileName.size() != 0) {
                Raster raster[] = new Raster[fileName.size()];
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
                    s.close();


                }
                int w = raster[0].getWidth(), h = raster[0].getHeight();
                int averagePixel[][][] = new int[w][h][3];
                for (int i = 0; i < totalFiles; i++) {
                    for (int width = 0; width < w; width++) {
                        if (isCancelled()) {
                            this.done();
                            return _ABORTED;
                        }
                        for (int height = 0; height < h; height++) {
                            int[] pixelA = null;

                            pixelA = raster[i].getPixel(width, height, pixelA);

                            averagePixel[width][height][0] += pixelA[0];
                            averagePixel[width][height][1] += pixelA[1];
                            averagePixel[width][height][2] += pixelA[2];

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
                        if (isCancelled()||progressMonitor.isCanceled()) {
                            return _ABORTED;
                        }
                    }
                    progressMonitor.setProgress(i * 100 / totalFiles);
                    progressMonitor.setNote("File: " + i + "/" + totalFiles + " processed!");
                    if (isCancelled()||progressMonitor.isCanceled()) {
                        return _ABORTED;
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

                return _COMPLETED;
            } else {
                return _NOFILE;
            }

        }
    }

    public static void main(String[] foo) {
        new ImageAverager16Bit();
    }

    // executes in event dispatch thread
    public void propertyChange(PropertyChangeEvent event) {
        // if the operation is finished or has been canceled by
        // the user, take appropriate action

        if (progressMonitor.isCanceled()) {
            operation.cancel(true);
        } else if (event.getPropertyName().equals("progress")) {
            // get the % complete from the progress event
            // and set it on the progress monitor
            int progress = (Integer) event.getNewValue();
            progressMonitor.setProgress(progress);
        }
    }

    public ImageAverager16Bit() {

        frame = new JFrame("TIFF Image Averaging - By Ace Shooting");
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

    public void actionPerformed(ActionEvent e) {
        go.setEnabled(false);

        chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(
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

                progressMonitor = new ProgressMonitor(this, "Loading values...", "", 0, 100);
                progressMonitor.setMillisToDecideToPopup(10);

                operation = new Calculate(results, directoryName);
                operation.addPropertyChangeListener(this);
                operation.execute();

                //  marchThroughImages(results, directoryName);

            } catch (IOException e1) {
                e1.printStackTrace();
            }

        } else {
            System.out
                    .println("No Selection.Please Select the Folder containing TIFF Images.");
            go.setEnabled(true);
        }
    }

    public Dimension getPreferredSize() {
        return new Dimension(200, 200);
    }

}
