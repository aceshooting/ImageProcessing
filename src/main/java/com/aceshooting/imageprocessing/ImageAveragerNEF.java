package com.aceshooting.imageprocessing;




import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.TIFFEncodeParam;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.out;

public class ImageAveragerNEF extends JPanel implements ActionListener, PropertyChangeListener {

    private static final String _AVG = "Average";
    private static final String _LIGHTEN ="Lighten";
    private static final String _DARKEN = "Darken";
    JComboBox<String> selection;
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
                setEnabledBtn(true);
                progressMonitor.close();
                super.done();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Override
        protected void process(List<String> chunks) {
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
                int totalFiles = fileName.size();
                int averagePixel[][][] = null;
                int w = 0;
                int h = 0;
                WritableRaster wRaster = null;
                boolean avg=false;
                boolean litn=false;
                if(selection.getSelectedItem().equals(_AVG)){
                    avg=true;
                }
                else if(selection.getSelectedItem().equals(_LIGHTEN)){
                    litn=true;
                }

                for (int i = 0; i < totalFiles; i++) {
                    File file = new File(fileName.get(i));
                    final ImageReader reader = ImageIO.getImageReaders(file).next();
                    reader.setInput(ImageIO.createImageInputStream(file));
                    BufferedImage tempRast=reader.read(0);


                    if (averagePixel == null) {
                        w = tempRast.getWidth();
                        h = tempRast.getHeight();
                        averagePixel = new int[w][h][3];
                        wRaster=Raster.createBandedRaster(DataBuffer.TYPE_INT,w,h,3,new Point(0,0));
                    } else {
                        if (tempRast.getWidth() != w || tempRast.getHeight() != h) {
                            JOptionPane.showMessageDialog(null, "All files must be the same size", "Processing is aborted", JOptionPane.ERROR_MESSAGE);
                            this.done();
                            return _ABORTED;
                        }
                    }
                    for (int width = 0; width < w; width++) {
                        if (isCancelled() || progressMonitor.isCanceled()) {
                            this.done();
                            return _ABORTED;
                        }
                        for (int height = 0; height < h; height++) {

                            int RGB=tempRast.getRGB(width,height);
                            Color c=new Color(RGB);


                            if(avg) {
                                averagePixel[width][height][0] += c.getRed();
                                averagePixel[width][height][1] += c.getGreen();
                                averagePixel[width][height][2] += c.getBlue();

                                if (i == totalFiles - 1) // update the raster while
                                // processing last file
                                {
                                    averagePixel[width][height][0] /= totalFiles;
                                    averagePixel[width][height][1] /= totalFiles;
                                    averagePixel[width][height][2] /= totalFiles;
                                    wRaster.setPixel(width, height, averagePixel[width][height]);
                                }
                            }
                            else if(litn){
                                averagePixel[width][height][0] = Math.max(c.getRed(), averagePixel[width][height][0]);
                                averagePixel[width][height][1] = Math.max(c.getGreen(), averagePixel[width][height][1]);
                                averagePixel[width][height][2] = Math.max(c.getBlue(), averagePixel[width][height][2]);

                                if (i == totalFiles - 1) // update the raster while
                                // processing last file
                                {
                                    wRaster.setPixel(width, height, averagePixel[width][height]);
                                }
                            }
                            else{
                                if(i==0){
                                    averagePixel[width][height][0] = c.getRed();
                                    averagePixel[width][height][1] = c.getGreen();
                                    averagePixel[width][height][2] = c.getBlue();
                                }
                                else {
                                    averagePixel[width][height][0] = Math.min(c.getRed(), averagePixel[width][height][0]);
                                    averagePixel[width][height][1] = Math.min(c.getGreen(), averagePixel[width][height][1]);
                                    averagePixel[width][height][2] = Math.min(c.getBlue(), averagePixel[width][height][2]);
                                }

                                if (i == totalFiles - 1) // update the raster while
                                // processing last file
                                {
                                    wRaster.setPixel(width, height, averagePixel[width][height]);
                                }
                            }
                        }
                    }
             /*   final IIOMetadata metadata = reader.getImageMetadata(0);
                final NEFMetadata nefMetadata = (NEFMetadata) metadata;
                final IFD exifIFD = nefMetadata.getExifIFD();
                final TagRational exposure = exifIFD.getExposureTime();*/


                    progressMonitor.setProgress((i+1) * 100 / totalFiles);
                    progressMonitor.setNote("File: " + (i+1) + "/" + totalFiles + " processed!");
                    if (isCancelled() || progressMonitor.isCanceled()) {
                        return _ABORTED;
                    }
                }

                String name="Output_"+selection.getSelectedItem()+"_"+ System.currentTimeMillis() + ".tiff";

                File file = new File(directoryName + "\\"+name);
                FileOutputStream fileoutput = new FileOutputStream(file);


                TIFFEncodeParam encParam = null;

                ImageEncoder enc = ImageCodec.createImageEncoder("tiff", fileoutput,encParam);

                BufferedImage newImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                newImage.setData(wRaster);
                enc.encode(newImage);
                //ImageIO.write(newImage, "png", new File(directoryName + "\\Output_" + System.currentTimeMillis() + ".png"));

                fileoutput.close();

                return _COMPLETED;
            } else {
                return _NOFILE;
            }
        }
    }

    public static void main(String[] foo) {
        new ImageAveragerNEF();
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

    public ImageAveragerNEF() {

        frame = new JFrame("Raw Image Processing - By Ace Shooting");
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        final JPanel compsToExperiment = new JPanel();
        GridLayout experimentLayout = new GridLayout(2,1);
        compsToExperiment.setLayout(experimentLayout);


        frame.setSize(this.getPreferredSize());
        frame.setVisible(true);
        selection=new JComboBox<String>(new String[]{_AVG, _LIGHTEN, _DARKEN});
        compsToExperiment.add(selection);

        go = new JButton("Select Folder");
        go.addActionListener(this);
        compsToExperiment.add(go);
        frame.add(compsToExperiment);
        frame.setVisible(true);
    }

    public void setEnabledBtn(boolean val){
        go.setEnabled(val);
        selection.setEnabled(val);
    }

    public void actionPerformed(ActionEvent e) {
        setEnabledBtn(false);


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
                        if (name.toLowerCase().endsWith("nef")
                                || name.toLowerCase().endsWith("cr2") || name.toLowerCase().endsWith("jpg") || name.toLowerCase().endsWith("tif") || name.toLowerCase().endsWith("tiff"))
                            return true;
                        else
                            return false;
                    }
                });

                progressMonitor = new ProgressMonitor(this, "Loading Files, please wait...", "", 0, 100);
                progressMonitor.setMillisToDecideToPopup(1);
                progressMonitor.setMillisToPopup(2);

                for (File file : files) {
                    if (file.isFile()) {
                        progressMonitor.setProgress(0);
                        progressMonitor.setNote("Loaded: " + file.getCanonicalPath());
                        results.add(file.getCanonicalPath());
                    }
                }


                operation = new Calculate(results, directoryName);
                operation.addPropertyChangeListener(this);
                operation.execute();

                //  marchThroughImages(results, directoryName);

            } catch (IOException e1) {
                e1.printStackTrace();
            }

        } else {
            setEnabledBtn(true);
        }
    }

    public Dimension getPreferredSize() {
        return new Dimension(400, 200);
    }

}
