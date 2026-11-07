package com.marzouki.shop.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marzouki.shop.common.ConflictException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ImageProcessorTest {

    static byte[] image(int width, int height, String format, int type) throws IOException {
        BufferedImage image = new BufferedImage(width, height, type);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(200, 80, 60));
        g.fillRect(0, 0, width / 2, height);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    private static BufferedImage read(byte[] data) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(data));
    }

    @Test
    void largePicturesAreScaledDownToTwoJpegSizes() throws Exception {
        ImageProcessor.Processed result = ImageProcessor.process(image(3000, 2000, "png", BufferedImage.TYPE_INT_RGB));

        BufferedImage large = read(result.large());
        BufferedImage thumb = read(result.thumb());
        assertThat(large.getWidth()).isEqualTo(1200);
        assertThat(large.getHeight()).isEqualTo(800);
        assertThat(thumb.getWidth()).isEqualTo(400);
        assertThat(thumb.getHeight()).isBetween(266, 267);
        assertThat(result.width()).isEqualTo(1200);
    }

    @Test
    void smallPicturesAreNotEnlarged() throws Exception {
        ImageProcessor.Processed result = ImageProcessor.process(image(300, 200, "png", BufferedImage.TYPE_INT_RGB));
        assertThat(read(result.large()).getWidth()).isEqualTo(300);
        assertThat(read(result.thumb()).getWidth()).isEqualTo(300);
    }

    @Test
    void transparencyBecomesWhite() throws Exception {
        BufferedImage source = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(source, "png", out);

        BufferedImage result = read(ImageProcessor.process(out.toByteArray()).large());
        assertThat(new Color(result.getRGB(50, 50)).getRed()).isGreaterThan(240);
    }

    @Test
    void phonePhotosAreTurnedUprightUsingTheirOrientationTag() throws Exception {
        byte[] sideways = withOrientation(image(600, 300, "jpg", BufferedImage.TYPE_INT_RGB), 6);

        ImageProcessor.Processed result = ImageProcessor.process(sideways);
        assertThat(result.width()).isEqualTo(300);
        assertThat(result.height()).isEqualTo(600);
    }

    @Test
    void thingsThatAreNotSupportedImagesAreRefused() throws Exception {
        assertThatThrownBy(() -> ImageProcessor.process("not an image".getBytes())).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> ImageProcessor.process(new byte[0])).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> ImageProcessor.process(image(50, 50, "gif", BufferedImage.TYPE_INT_RGB)))
                .isInstanceOf(ConflictException.class).hasMessageContaining("supported");
        assertThatThrownBy(() -> ImageProcessor.process("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void oversizedFilesAndHugeDimensionsAreRefused() throws Exception {
        assertThatThrownBy(() -> ImageProcessor.process(new byte[ImageProcessor.MAX_BYTES + 1]))
                .isInstanceOf(ConflictException.class).hasMessageContaining("5 MB");
        byte[] huge = image(5100, 5100, "png", BufferedImage.TYPE_BYTE_GRAY);
        assertThatThrownBy(() -> ImageProcessor.process(huge))
                .isInstanceOf(ConflictException.class).hasMessageContaining("pixels");
    }

    @Test
    void outputCarriesNoMetadata() throws Exception {
        byte[] output = ImageProcessor.process(withOrientation(image(200, 100, "jpg", BufferedImage.TYPE_INT_RGB), 1)).large();
        assertThat(new String(output, java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain("Exif");
    }

    /** Inserts a minimal EXIF block holding only an orientation value right after the JPEG start marker. */
    static byte[] withOrientation(byte[] jpeg, int orientation) {
        byte[] exif = {
            'E', 'x', 'i', 'f', 0, 0,
            'M', 'M', 0, 42, 0, 0, 0, 8,
            0, 1,
            0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, (byte) orientation, 0, 0,
            0, 0, 0, 0
        };
        int length = exif.length + 2;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(jpeg[0]);
        out.write(jpeg[1]);
        out.write(0xFF);
        out.write(0xE1);
        out.write(length >> 8);
        out.write(length & 0xFF);
        out.writeBytes(exif);
        out.write(jpeg, 2, jpeg.length - 2);
        return out.toByteArray();
    }
}