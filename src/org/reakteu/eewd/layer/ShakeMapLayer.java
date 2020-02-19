/*
 * Copyright (C) 2014-2015 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reakteu.eewd.layer;

import com.bbn.openmap.layer.OMGraphicHandlerLayer;
import com.bbn.openmap.omGraphics.OMGraphicList;
import com.bbn.openmap.omGraphics.OMRaster;
import com.bbn.openmap.omGraphics.OMScalingRaster;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reakteu.eewd.Application;
import org.reakteu.eewd.data.AmplificationPoint;
import org.reakteu.eewd.data.POI;
import org.reakteu.eewd.data.Shaking;
import org.reakteu.eewd.utils.Gradient;

/**
 *
 * @author Stephan Herrnkind herrnkind at gempa dot de
 */
public class ShakeMapLayer extends OMGraphicHandlerLayer implements
        ActionListener {

    private static final Logger LOG = LogManager.getLogger(ShakeMapLayer.class);
    private static final Color[] GradientColors = {
        new Color(255, 255, 255, 128), // white
        new Color(0, 255, 255, 128), // cyan
        new Color(0, 255, 0, 128), // green
        new Color(255, 255, 0, 128), // yellow
        new Color(255, 0, 0, 128) // red
    };
    private static final int ColorNaN = new Color(128, 128, 128, 128).getRGB();

    public class Point extends AmplificationPoint {

        public double value = 0.0;
        public int x;
        public int y;
        public POI site;

        public Point(double latitude, double longitude, double altitude,
                     double amplification) {
            super(latitude, longitude, altitude, amplification);
            x = 0;
            y = 0;
        }
    }

    private final OMScalingRaster mapRaster;
    private final List<Point> points;
    private final BufferedImage[] mapImages;
    private int currentImage;
    private final BufferedImage scaleImage;
    private final Gradient gradient;
    private final boolean logScale;

    private double latNorth;
    private double latSouth;
    private double lonWest;
    private double lonEast;
    private double dLat;
    private double dLon;

    public ShakeMapLayer() {

        points = new ArrayList();
        // 2 images for double buffering, 1 image for fast reset
        mapImages = new BufferedImage[3];
        currentImage = 0;

        Application app = Application.getInstance();
        gradient = new Gradient();

        double minValue = app.getProperty(Application.PropertySMMinValue, 0.0);
        double maxValue = app.getProperty(Application.PropertySMMaxValue, 1.0);
        logScale = app.getProperty(Application.PropertySMLogScale, false);

        if (logScale) {
            minValue = minValue > 0 ? Math.log10(minValue) : 0;
            maxValue = maxValue > 0 ? Math.log10(maxValue) : 0;
        }

        double[] values = new double[GradientColors.length];
        double delta = minValue < maxValue ? (maxValue - minValue) / (GradientColors.length - 1) : 0;
        int i = 0;
        for (Color c : GradientColors) {
            gradient.put(minValue, c);
            values[i++] = minValue;
            minValue += delta;
        }

        latNorth = 0;
        latSouth = 0;
        lonWest = 0;
        lonEast = 0;
        dLat = 0;
        dLon = 0;

        loadGrid();

        mapRaster = new OMScalingRaster(latNorth, lonWest, latSouth, lonEast, mapImages[2]);

        Shaking.Type smParam = app.getShakeMapParameter();
        String labelText = smParam.labelString();
        if (smParam == Shaking.Type.PGA || smParam == Shaking.Type.PSA) {
            labelText += ", g";
        } else if (smParam == Shaking.Type.PGV || smParam == Shaking.Type.DRS) {
            labelText += ", cm/s";
        }

        int w = 300; // max image width
        int h = 70;
        int xMargin = 30; // margin on left and right needed for text
        int steps = GradientColors.length - 1;
        int stepW = (w - 2 * xMargin) / steps;
        w = stepW * steps + 1 + 2 * xMargin;

        scaleImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaleImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        FontMetrics fm = g.getFontMetrics();
        int y1Text = fm.getAscent();
        int y2Text = y1Text + fm.getHeight();
        int yTick = y2Text + fm.getDescent() + 3;
        int y1Gradient = yTick + 10;
        int y2Gradient = h - 1;
        int x = xMargin;

        String str;
        for (i = 0; i < steps; ++i) {
            g.setColor(Color.BLACK);

            double v = logScale ? Math.pow(10, values[i]) : values[i];
            str = String.format("%.2f", v);
            int xText = x - fm.stringWidth(str) / 2;

            if (i == 0) {
                g.drawString(labelText, xText, y1Text);
            }
            g.drawString(str, xText, y2Text);

            g.drawLine(x, yTick, x++, y2Gradient);

            int rgb;
            Color c1 = GradientColors[i];
            Color c2 = GradientColors[i + 1];
            double ratio;
            for (int j = 1; j <= stepW; ++j, ++x) {
                ratio = j / (double) stepW;
                rgb = Gradient.blend(c1, c2, ratio);
                g.setColor(new Color(rgb));
                g.drawLine(x, y1Gradient, x, y2Gradient);
            }
        }
        --x;
        g.setColor(Color.BLACK);
        double v = logScale ? Math.pow(10, values[i]) : values[i];
        str = String.format("%.2f", v);
        g.drawString(str, x - fm.stringWidth(str) / 2, y2Text);
        g.drawLine(x, yTick, x, y2Gradient);
        g.drawLine(xMargin, h - 1, x, h - 1);
        g.dispose();
    }

    private boolean loadGrid() {
        Application app = Application.getInstance();
        String fileName = app.getProperty(Application.PropertySMFile, (String) null);
        if (fileName == null) {
            return false;
        }
        LOG.debug("loading shake map data from file: " + fileName);
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(fileName));
            String line;
            String[] parts;
            Point p, previous = null;
            double diff;
            while ((line = br.readLine()) != null) {
                parts = line.split(",", 4);
                if (parts.length != 4) {
                    continue;
                }

                try {
                    p = new Point(
                            Double.parseDouble(parts[1]), // latitude
                            Double.parseDouble(parts[0]), // longitude
                            Double.parseDouble(parts[2]), // altitude
                            Double.parseDouble(parts[3]) // amplification
                    );
                } catch (NumberFormatException nfe) {
                    continue;
                }

                // determine boundary and minimum grid resolution
                if (previous == null) {
                    latSouth = latNorth = p.latitude;
                } else {
                    if (p.longitude != previous.longitude) {
                        latSouth = Math.min(latSouth, p.latitude);
                        latNorth = Math.max(latNorth, previous.latitude);
                        diff = diffLon(previous.longitude, p.longitude);
                        dLon = dLon == 0 ? diff : Math.min(dLon, diff);
                    }

                    diff = Math.abs(p.latitude - previous.latitude);
                    dLat = dLat == 0 ? diff : Math.min(dLat, diff);
                }
                points.add(p);
                previous = p;
            }
            br.close();
        } catch (IOException ioe) {
            LOG.error(String.format("could not read POI file '%s'", fileName), ioe);
        }

        if (points.isEmpty()) {
            return false;
        }

        lonWest = points.get(0).longitude;
        lonEast = points.get(points.size() - 1).longitude;

        int width = dLon == 0 ? 1 : (int) (diffLon(lonWest, lonEast) / dLat) + 1;
        int height = dLat == 0 ? 1 : (int) ((latNorth - latSouth) / dLon) + 1;

        mapImages[0] = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        mapImages[1] = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        mapImages[2] = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // map point coordinates to image index
        for (Iterator<Point> it = points.iterator(); it.hasNext();) {
            Point p = it.next();
            p.x = (int) (diffLon(lonWest, p.longitude) / dLon);
            p.y = (int) ((latNorth - p.latitude) / dLat);

            if (p.x < 0 || p.x > width - 1 || p.y < 0 || p.y > height - 1) {
                LOG.warn(String.format("could not assign image index to "
                                       + "point (%f/%f), removing", p.latitude,
                                       p.longitude));
                it.remove();
            }
        }

        LOG.info(String.format("created shake map data structure,\n"
                               + "  points: %d\n"
                               + "  lat(min/max/d): %f/%f/%f\n"
                               + "  lon(min/max/d): %f/%f/%f\n"
                               + "  img(w/h): %d/%d", points.size(),
                               latSouth, latNorth, dLat,
                               lonWest, lonEast, dLon, width, height));
        return true;
    }

    private double diffLon(double min, double max) {
        return max < min ? max - min + 360 : max - min;
    }

    public List<Point> getPoints() {
        return points;
    }

    public void updateImage(boolean valid) {
        // get next image and reset it
        currentImage = 1 - currentImage;
        BufferedImage img = mapImages[currentImage];
        img.setData(mapImages[2].getRaster());

        if (valid) {
            int rgb;
            // assign RGB values
            if (logScale) {
                for (Point p : points) {
                    rgb = p.value > 0 && p.value == p.value
                          ? gradient.colorAt(Math.log10(p.value), false)
                          : ColorNaN;
                    img.setRGB(p.x, p.y, rgb);
                }
            } else {
                for (Point p : points) {
                    rgb = p.value == p.value
                          ? gradient.colorAt(p.value, false)
                          : ColorNaN;
                    img.setRGB(p.x, p.y, rgb);
                }
            }
        } else {
            for (Point p : points) {
                p.value = 0.0;
            }
        }

        // swap image
        mapRaster.setImage(img);
    }

    public synchronized OMGraphicList prepare() {

        OMGraphicList list = getList();
        if (list == null) {
            list = new OMGraphicList();
        } else {
            list.clear();
        }

        if (isCancelled()) {
            return null;
        }

        mapRaster.generate(getProjection());

        OMRaster scaleRaster = new OMRaster(15, getHeight() - scaleImage.getHeight() - 10, scaleImage);
        scaleRaster.generate(getProjection());

        list.add(mapRaster);
        list.add(scaleRaster);

        return list;
    }

}
