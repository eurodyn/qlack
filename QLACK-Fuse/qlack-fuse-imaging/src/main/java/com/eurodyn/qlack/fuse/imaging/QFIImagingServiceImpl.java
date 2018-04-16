package com.eurodyn.qlack.fuse.imaging;

import com.eurodyn.qlack.fuse.imaging.dto.QFIImageFormatHandler;
import com.eurodyn.qlack.fuse.imaging.dto.QFIImageInfo;
import com.eurodyn.qlack.fuse.imaging.exception.QFIImagingException;
import com.eurodyn.qlack.fuse.imaging.util.QFIColorSpaceType;
import com.eurodyn.qlack.fuse.imaging.util.QFIICCProfile;
import com.eurodyn.qlack.fuse.imaging.util.QFIImagingUtil;
import com.eurodyn.qlack.fuse.imaging.util.QFIResamplingAlgorithm;
import com.eurodyn.qlack.fuse.imaging.util.QFITIFFCompression;
import com.twelvemonkeys.image.ResampleOp;
import javax.annotation.PostConstruct;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageOutputStream;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.awt.Color;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorConvertOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@Validated
public class QFIImagingServiceImpl  {

  /**
   * Resamples an image to the new dimensions using one of the available resampling algorithms.
   *
   * @param originalImage The image to resample.
   * @param width The new widht.
   * @param height The new height.
   * @param QFIResamplingAlgorithm The resampling algorithm to use.
   * @param imageType The type of the image (so that the resulting image is of the same type).
   * @return Returns a resampled image.
   */
  private byte[] resample(BufferedImage originalImage, int width, int height,
    QFIResamplingAlgorithm QFIResamplingAlgorithm, String imageType) throws IOException {

    try (ByteArrayOutputStream resampledImageOutputStream = new ByteArrayOutputStream()) {
      BufferedImageOp resampler = new ResampleOp(width, height, QFIResamplingAlgorithm.getVal());
      ImageIO.write(resampler.filter(originalImage, null), imageType, resampledImageOutputStream);
      resampledImageOutputStream.flush();
      return resampledImageOutputStream.toByteArray();
    }
  }

  /**
   * Initialiser in which all SPI readers/writers are registered with ImageIO.
   */
  @PostConstruct
  public void init() {
    IIORegistry registry = IIORegistry.getDefaultInstance();
    registry.registerServiceProviders(ServiceRegistry.lookupProviders(ImageReaderSpi.class));
    registry.registerServiceProviders(ServiceRegistry.lookupProviders(ImageWriterSpi.class));
  }

  public List<QFIImageFormatHandler> getSupportedReadFormats() {
    List<QFIImageFormatHandler> handlers = new ArrayList<>();

    for (String reader : ImageIO.getReaderFormatNames()) {
      final QFIImageFormatHandler imageFormatHandler = new QFIImageFormatHandler();
      imageFormatHandler.setFormat(reader);
      Iterator<ImageReader> imageReaders = ImageIO.getImageReadersByFormatName(reader);
      while (imageReaders.hasNext()) {
        final ImageReader next = imageReaders.next();
        imageFormatHandler.addHandlerClass(next.toString());
      }
      handlers.add(imageFormatHandler);
    }

    return handlers;
  }

  public List<QFIImageFormatHandler> getSupportedWriteFormats() {
    List<QFIImageFormatHandler> handlers = new ArrayList<>();

    for (String reader : ImageIO.getWriterFormatNames()) {
      final QFIImageFormatHandler imageFormatHandler = new QFIImageFormatHandler();
      imageFormatHandler.setFormat(reader);
      Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByFormatName(reader);
      while (imageWriters.hasNext()) {
        final ImageWriter next = imageWriters.next();
        imageFormatHandler.addHandlerClass(next.toString());
      }
      handlers.add(imageFormatHandler);
    }

    return handlers;
  }

  public boolean isFormatSupportedForRead(String format) {
    return getSupportedReadFormats().stream().anyMatch(o -> o.getFormat().equals(format));
  }

  public boolean isFormatSupportedForWrite(String format) {
    return getSupportedWriteFormats().stream().anyMatch(o -> o.getFormat().equals(format));
  }

  public QFIImageInfo getInfo(byte[] image) {
    QFIImageInfo imageInfo = null;

    try {
      imageInfo = new QFIImageInfo();
      try (InputStream originalImageInputStream = new ByteArrayInputStream(image)) {
        BufferedImage bufferedImage = ImageIO.read(originalImageInputStream);
        imageInfo.setBitsPerPixel(bufferedImage.getColorModel().getPixelSize());
        imageInfo.setColorType(
            QFIColorSpaceType.valueOf(
            QFIColorSpaceType.getReverseVal(bufferedImage.getColorModel().getColorSpace().getType())));
        imageInfo.setHeight(bufferedImage.getHeight());
        imageInfo.setWidth(bufferedImage.getWidth());
        try (InputStream originalImageInputStream2 = new ByteArrayInputStream(image)) {
          imageInfo.setMimeType(
            new TikaConfig().getDetector().detect(originalImageInputStream2, new Metadata())
              .toString());
        }
        imageInfo.setDotsPerInch(QFIImagingUtil.getDPI(image));
        imageInfo.setFormat(QFIImagingUtil.getType(image));
      }
    } catch (IOException | TikaException e) {
      throw new QFIImagingException("Could not obtain image info.", e);
    }

    return imageInfo;
  }

  public byte[] convert(byte[] image, String dstFormat) {
    return convert(image, dstFormat, null);
  }

  public byte[] removeAlphaChannel(byte[] image) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      final String type = QFIImagingUtil.getType(image);
      try (InputStream originalImageInputStream = new ByteArrayInputStream(image)) {
        BufferedImage originalImage = ImageIO.read(originalImageInputStream);
        BufferedImage newImage = new BufferedImage(originalImage.getWidth(),
          originalImage.getHeight(),
          BufferedImage.TYPE_INT_RGB);
        newImage.createGraphics().drawImage(originalImage, 0, 0, Color.BLACK, null);
        ImageIO.write(newImage, type, baos);
      }
      // Return image.
      return baos.toByteArray();
    } catch (Exception e) {
      throw new QFIImagingException("Could not remove alpha channel.", e);
    }
  }

  public byte[] convert(byte[] image, String dstFormat, QFIICCProfile dstColorspace) {
    try (ByteArrayOutputStream dstImage = new ByteArrayOutputStream()) {
      // Read image.
      try (InputStream originalImageInputStream = new ByteArrayInputStream(image)) {
        BufferedImage originalImage = ImageIO.read(originalImageInputStream);

        // Convert colospace if requested.
        if (dstColorspace != null) {
          String iccProfileFile = "icc/" + dstColorspace.name() + ".icc";
          ColorSpace cmykColorSpace = new ICC_ColorSpace(ICC_Profile.getInstance(
            this.getClass().getClassLoader().getResource(iccProfileFile).openStream()));
          ColorConvertOp op = new ColorConvertOp(originalImage.getColorModel().getColorSpace(),
            cmykColorSpace, null);
          originalImage = op.filter(originalImage, null);
        }

        // Write destination image.
        if (!ImageIO.write(originalImage, dstFormat, dstImage)) {
          throw new QFIImagingException(MessageFormat.format(
            "Could not write destination format: {0}", dstFormat));
        }
      }
      // Return image.
      return dstImage.toByteArray();
    } catch (IOException e) {
      throw new QFIImagingException("Could not convert image.", e);
    }
  }

  public byte[] convertToTIFF(byte[] image, QFITIFFCompression tiffCompression) {
    return convertToTIFF(image, null, tiffCompression);
  }

  public byte[] convertToTIFF(byte[] image, QFIICCProfile dstColorspace,
      QFITIFFCompression tiffCompression) {
    try (ByteArrayOutputStream convertedImage = new ByteArrayOutputStream()) {
      // Read image.
      try (InputStream originalImageInputStream = new ByteArrayInputStream(image)) {
        BufferedImage originalImage = ImageIO.read(originalImageInputStream);

        // Convert colorspace.
        if (dstColorspace != null) {
          String iccProfileFile = "icc/" + dstColorspace.name() + ".icc";
          ColorSpace cmykColorSpace = new ICC_ColorSpace(ICC_Profile.getInstance(
            this.getClass().getClassLoader().getResource(iccProfileFile).openStream()));
          ColorConvertOp op = new ColorConvertOp(originalImage.getColorModel().getColorSpace(),
            cmykColorSpace, null);
          originalImage = op.filter(originalImage, null);
        }

        // Compress.
        final ImageWriteParam params = ImageIO.getImageWritersByFormatName("TIFF").next()
          .getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionType(tiffCompression.getVal());
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(convertedImage)) {
          ImageWriter writer = ImageIO.getImageWritersByFormatName("TIFF").next();
          writer.setOutput(ios);
          writer.write(null, new IIOImage(originalImage, null, null), params);
        }
      }
      return convertedImage.toByteArray();
    } catch (IOException e) {
      throw new QFIImagingException("Could not convert image.", e);
    }
  }

  public byte[] resampleByPercent(byte[] image, int percent,
    QFIResamplingAlgorithm QFIResamplingAlgorithm) {
    try (InputStream originalImageInputStream = new ByteArrayInputStream(image)) {
      BufferedImage originalBufferedImage = ImageIO.read(originalImageInputStream);
      return resample(originalBufferedImage,
        (int) (originalBufferedImage.getWidth() * ((float) percent / 100f)),
        (int) (originalBufferedImage.getHeight() * ((float) percent / 100f)), QFIResamplingAlgorithm,
        QFIImagingUtil.getType(image));
    } catch (IOException e) {
      throw new QFIImagingException("Could not resample image by percent.", e);
    }
  }

  public byte[] resampleByFactor(byte[] image, float factor,
    QFIResamplingAlgorithm QFIResamplingAlgorithm) {
    try (InputStream originalImageInputStream = new ByteArrayInputStream(image)) {
      BufferedImage originalBufferedImage = ImageIO.read(originalImageInputStream);
      return resample(originalBufferedImage, (int) (originalBufferedImage.getWidth() * factor),
        (int) (originalBufferedImage.getHeight() * factor), QFIResamplingAlgorithm,
        QFIImagingUtil.getType(image));
    } catch (IOException e) {
      throw new QFIImagingException("Could not resample image by factor.", e);
    }
  }

  public byte[] resampleByWidth(byte[] image, int width, QFIResamplingAlgorithm QFIResamplingAlgorithm) {
    try (InputStream originalImageInputStream = new ByteArrayInputStream(image)) {
      BufferedImage originalBufferedImage = ImageIO.read(originalImageInputStream);
      float newYRatio = (float) width / (float) originalBufferedImage.getWidth();
      return resample(originalBufferedImage, width,
        (int) (originalBufferedImage.getHeight() * newYRatio), QFIResamplingAlgorithm,
        QFIImagingUtil.getType(image));
    } catch (IOException e) {
      throw new QFIImagingException("Could not resample image by width.", e);
    }
  }

  public byte[] resampleByHeight(byte[] image, int height,
      QFIResamplingAlgorithm QFIResamplingAlgorithm) {
    try (InputStream originalImageInputStream = new ByteArrayInputStream(image)) {
      BufferedImage originalBufferedImage = ImageIO.read(originalImageInputStream);
      float newXRatio = (float) height / (float) originalBufferedImage.getHeight();
      return resample(originalBufferedImage, (int) (originalBufferedImage.getWidth() * newXRatio),
        height, QFIResamplingAlgorithm, QFIImagingUtil.getType(image));
    } catch (IOException e) {
      throw new QFIImagingException("Could not resample image by height.", e);
    }
  }

  public byte[] resample(byte[] image, int width, int height,
      QFIResamplingAlgorithm QFIResamplingAlgorithm) {
    try (InputStream originalImageInputStream = new ByteArrayInputStream(image)) {
      BufferedImage originalBufferedImage = ImageIO.read(originalImageInputStream);
      return resample(originalBufferedImage, width, height, QFIResamplingAlgorithm,
        QFIImagingUtil.getType(image));
    } catch (IOException e) {
      throw new QFIImagingException("Could not resample image.", e);
    }
  }
}
