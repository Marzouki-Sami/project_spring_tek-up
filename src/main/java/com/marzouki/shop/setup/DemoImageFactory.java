package com.marzouki.shop.setup;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Draws simple abstract pictures for the demo shop, so it has images to show without using anyone's
 * photographs. Shapes only (no text), so it works on servers without fonts installed.
 */
@Component
@Profile("demo")
class DemoImageFactory {

    byte[] generate(int seed, int index) {
        int width = 1200;
        int height = 900;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float hue = ((seed * 37 + index * 23) % 360) / 360f;
        Color top = Color.getHSBColor(hue, 0.45f, 0.97f);
        Color bottom = Color.getHSBColor((hue + 0.08f) % 1f, 0.65f, 0.78f);
        g.setPaint(new GradientPaint(0, 0, top, width, height, bottom));
        g.fillRect(0, 0, width, height);

        g.setColor(new Color(255, 255, 255, 46));
        g.fill(new Ellipse2D.Double(-120 + index * 60, -140, 620, 620));
        g.fill(new Ellipse2D.Double(width - 520, height - 380 + index * 20, 700, 700));
        g.setColor(new Color(0, 0, 0, 22));
        g.fill(new Ellipse2D.Double(width / 2.0 - 330, height - 230, 660, 90));

        Color body = new Color(255, 255, 255, 235);
        Color accent = Color.getHSBColor((hue + 0.5f) % 1f, 0.55f, 0.85f);
        switch (index % 4) {
            case 0 -> {
                g.setColor(body);
                g.fill(new RoundRectangle2D.Double(width / 2.0 - 230, 250, 460, 400, 70, 70));
                g.setColor(accent);
                g.fill(new RoundRectangle2D.Double(width / 2.0 - 230, 430, 460, 70, 0, 0));
            }
            case 1 -> {
                g.setColor(body);
                g.fill(new Ellipse2D.Double(width / 2.0 - 250, 220, 500, 500));
                g.setColor(accent);
                g.fill(new Ellipse2D.Double(width / 2.0 - 110, 360, 220, 220));
            }
            case 2 -> {
                g.setColor(body);
                g.fill(new RoundRectangle2D.Double(width / 2.0 - 120, 190, 240, 480, 110, 110));
                g.setColor(accent);
                g.fill(new RoundRectangle2D.Double(width / 2.0 - 70, 150, 140, 90, 30, 30));
            }
            default -> {
                g.setColor(body);
                g.fill(new RoundRectangle2D.Double(width / 2.0 - 330, 330, 360, 330, 60, 60));
                g.setColor(accent);
                g.fill(new Ellipse2D.Double(width / 2.0 - 40, 260, 340, 340));
            }
        }
        g.dispose();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}