package com.marzouki.shop.media;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.marzouki.shop.common.ConflictException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * Turns an uploaded picture into the two JPEG files the shop serves. The upload is decoded and
 * re-encoded, never stored as received, so metadata (such as GPS position) is dropped and anything that
 * is not a real image is refused. Photos taken in portrait on a phone are turned upright first.
 */
public final class ImageProcessor {

    public static final int MAX_BYTES = 5 * 1024 * 1024;
    static final long MAX_PIXELS = 25_000_000L;
    static final int LARGE = 1200;
    static final int THUMB = 400;
    private static final Set<String> FORMATS = Set.of("jpeg", "png", "webp");

    private ImageProcessor() {
    }

    public record Processed(byte[] large, byte[] thumb, int width, int height) {
    }

    public static Processed process(byte[] data) {
        if (data == null || data.length == 0) {
            throw new ConflictException("The file is empty");
        }
        if (data.length > MAX_BYTES) {
            throw new ConflictException("Each image can be at most 5 MB");
        }
        BufferedImage image = decode(data);
        image = upright(image, orientationOf(data));
        BufferedImage flat = onWhite(image);
        BufferedImage large = scaleDown(flat, LARGE);
        BufferedImage thumb = scaleDown(large, THUMB);
        return new Processed(jpeg(large), jpeg(thumb), large.getWidth(), large.getHeight());
    }

    private static BufferedImage decode(byte[] data) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new ConflictException("That file is not a supported image. Use JPG, PNG or WebP.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                if (!FORMATS.contains(reader.getFormatName().toLowerCase())) {
                    throw new ConflictException("That file is not a supported image. Use JPG, PNG or WebP.");
                }
                // Check the declared size first, so a tiny file cannot claim to be gigantic.
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > MAX_PIXELS) {
                    throw new ConflictException("That image has too many pixels (the limit is 25 megapixels)");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof ConflictException conflict) {
                throw conflict;
            }
            throw new ConflictException("That image could not be read. It may be damaged.");
        }
    }

    private static int orientationOf(byte[] data) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(data));
            ExifIFD0Directory exif = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (exif != null && exif.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                return exif.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
        } catch (Exception e) {
            // No readable metadata: assume the picture is already upright.
        }
        return 1;
    }

    private static BufferedImage upright(BufferedImage image, int orientation) {
        int quarterTurns = switch (orientation) {
            case 6 -> 1;
            case 3 -> 2;
            case 8 -> 3;
            default -> 0;
        };
        if (quarterTurns == 0) {
            return image;
        }
        int w = image.getWidth();
        int h = image.getHeight();
        boolean swap = quarterTurns % 2 == 1;
        BufferedImage turned = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_ARGB);
        AffineTransform transform = new AffineTransform();
        transform.translate(turned.getWidth() / 2.0, turned.getHeight() / 2.0);
        transform.rotate(Math.PI / 2 * quarterTurns);
        transform.translate(-w / 2.0, -h / 2.0);
        Graphics2D g = turned.createGraphics();
        g.drawImage(image, transform, null);
        g.dispose();
        return turned;
    }

    /** JPEG has no transparency, so transparent areas become white. */
    private static BufferedImage onWhite(BufferedImage source) {
        BufferedImage flat = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = flat.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, flat.getWidth(), flat.getHeight());
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return flat;
    }

    /** Shrinks in steps of at most half, which keeps edges crisp, until the longest side fits. */
    static BufferedImage scaleDown(BufferedImage source, int longestSide) {
        BufferedImage current = source;
        while (Math.max(current.getWidth(), current.getHeight()) > longestSide) {
            double longest = Math.max(current.getWidth(), current.getHeight());
            double factor = Math.max(longestSide / longest, 0.5);
            int w = Math.max(1, (int) Math.round(current.getWidth() * factor));
            int h = Math.max(1, (int) Math.round(current.getHeight() * factor));
            BufferedImage next = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = next.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(current, 0, 0, w, h, null);
            g.dispose();
            current = next;
        }
        return current;
    }

    private static byte[] jpeg(BufferedImage image) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(out)) {
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(0.85f);
            writer.setOutput(stream);
            writer.write(null, new IIOImage(image, null, null), params);
            stream.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not encode the image", e);
        } finally {
            writer.dispose();
        }
    }
}