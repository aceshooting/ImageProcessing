package com.aceshooting.imageprocessing;

import it.tidalwave.imageio.nef.NEFMetadata;
import it.tidalwave.imageio.raw.TagRational;
import it.tidalwave.imageio.tiff.IFD;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;

import java.io.File;

import static java.lang.System.out;

/**
 * Created by assaa_000 on 11/06/2016.
 */
public class RawFile {
    public static void main(String[] arg){
        try {
            File file = new File("DSC_6059.NEF");
            final ImageReader reader = (ImageReader) ImageIO.getImageReaders(file).next();
            reader.setInput(ImageIO.createImageInputStream(file));
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final NEFMetadata nefMetadata = (NEFMetadata) metadata;
            final IFD exifIFD = nefMetadata.getExifIFD();
            final TagRational focalLength = exifIFD.getExposureTime();
            out.println(focalLength.doubleValue());
        }
        catch (Exception ex){
            ex.printStackTrace();;
        }
    }
}
