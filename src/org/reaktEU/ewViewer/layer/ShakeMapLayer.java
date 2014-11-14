/*
 * Copyright (C) 2014 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reaktEU.ewViewer.layer;

import com.bbn.openmap.layer.OMGraphicHandlerLayer;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.omGraphics.OMGraphicList;
import com.bbn.openmap.omGraphics.OMScalingRaster;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.util.Debug;
import java.awt.Color;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reaktEU.ewViewer.Application;
import org.reaktEU.ewViewer.data.AmplificationPoint;
import org.reaktEU.ewViewer.data.POI;
import org.reaktEU.ewViewer.utils.Gradient;

/**
 *
 * @author Stephan Herrnkind <herrnkind@gempa.de>
 */
public class ShakeMapLayer extends OMGraphicHandlerLayer implements
        ActionListener {

    private static final Logger LOG = LogManager.getLogger(ShakeMapLayer.class);
    private static Color[] GradientColors = {
        new Color(255, 255, 255, 128), // white
        new Color(0, 255, 255, 128), // cyan
        new Color(0, 255, 0, 128), // green
        new Color(255, 255, 0, 128), // yellow
        new Color(255, 0, 0, 128) // red
    };

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

    private final OMScalingRaster scalingRaster;
    private final List<Point> points;
    private final BufferedImage[] images;
    private int currentImage;
    private final Gradient gradient;

    private double latNorth;
    private double latSouth;
    private double lonWest;
    private double lonEast;
    private double dLat;
    private double dLon;

    public ShakeMapLayer() {

        points = new ArrayList();
        // 2 images for double buffering, 1 image for fast reset
        images = new BufferedImage[3];
        currentImage = 0;

        Application app = Application.getInstance();
        gradient = new Gradient();

        double minValue = app.getProperty(Application.PropertySMMinValue, 0.0);
        double maxValue = app.getProperty(Application.PropertySMMaxValue, 1.0);
        double delta = minValue < maxValue ? (maxValue - minValue) / GradientColors.length : 0;
        for (Color c : GradientColors) {
            gradient.put(minValue, c);
            minValue += delta;
        }

        latNorth = 0;
        latSouth = 0;
        lonWest = 0;
        lonEast = 0;
        dLat = 0;
        dLon = 0;

        loadGrid();

        scalingRaster = new OMScalingRaster(latNorth, lonWest, latSouth, lonEast, images[2]);
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

        images[0] = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        images[1] = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        images[2] = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

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

    public void updateImage() {
        // get next image and reset it
        currentImage = 1 - currentImage;
        BufferedImage img = images[currentImage];
        img.setData(images[2].getRaster());

        // assign RGB values
        for (Point p : points) {
            img.setRGB(p.x, p.y, gradient.colorAt(p.value, false));
        }

        // swap image
        scalingRaster.setImage(img);
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

        scalingRaster.generate(getProjection());
        list.add(scalingRaster);

        return list;
    }

}
