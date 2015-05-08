package org.exoplatform.addon.utils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;

import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class ImageUtils {
  public static final String KEY_SEPARATOR           = "_";
  public static final String KEY_DIMENSION_SEPARATOR = "x";

  public static final String GIF_EXTENDSION          = "gif";
  private static final Log LOG = ExoLogger.getLogger(ImageUtils.class);

  public static InputStream createResizedImage(InputStream imageStream,
                                               int width,
                                               int height,
                                               String imageMimeType) {
    try {
      MimeTypeResolver mimeTypeResolver = new MimeTypeResolver();

      BufferedImage image = null;
      int minSize = 0;
      String extension = mimeTypeResolver.getExtension(imageMimeType);
      // TODO: Resize gif image. Now we skip gif because we can't resize it now
      if (extension.equalsIgnoreCase(GIF_EXTENDSION))
        return imageStream;
      image = ImageIO.read(imageStream);
      if (height <= minSize & width <= minSize) {
        LOG.warn("Fail to resize image with dimention <= 0x0");
        return null;
      }
      if (height <= minSize)
        height = image.getHeight() * width / image.getWidth();
      else if (width <= minSize)
        width = image.getWidth() * height / image.getHeight();

      // Create temp file to store resized image
      File tmp = File.createTempFile("RESIZED", null);
      
      BufferedImage bufferedImage = Scalr.resize(image, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.FIT_TO_WIDTH,
                                             width, height, Scalr.OP_ANTIALIAS);
      
      ImageIO.write(bufferedImage,extension,tmp);

      // Create new new resized image InputStream
      InputStream newImageInputStream = new FileInputStream(tmp);

      // Delete temp file
      tmp.delete();
      return newImageInputStream;
    } catch (Exception e) {
      LOG.error("Fail to resize image " + e);
      return null;
    }
  }

}
