/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.reaktEU.ewViewer.data;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reaktEU.ewViewer.Application;
import org.reaktEU.ewViewer.utils.Gradient;

/**
 *
 * @author Stephan Herrnkind <herrnkind@gempa.de>
 */
public class ShakeMap {

    private static final Logger LOG = LogManager.getLogger(ShakeMap.class);

    public class Point extends AmplificationPoint {

        public double result;
        public int x;
        public int y;
        public POI site;

        public Point(double latitude, double longitude, double altitude,
                     double amplification) {
            super(latitude, longitude, altitude, amplification);
            result = 0.0;
            x = 0;
            y = 0;
        }
    }

    private List<Point> points;
    private boolean dirty;
    private BufferedImage image;
    private Gradient gradient;

    private double latNorth;
    private double latSouth;
    private double lonWest;
    private double lonEast;
    private double dLat;
    private double dLon;

    public ShakeMap() {
        points = new ArrayList();
        dirty = false;
        image = null;
        gradient = new Gradient();
        gradient.put(0.0, Color.WHITE);
        gradient.put(0.25, Color.CYAN);
        gradient.put(0.5, Color.GREEN);
        gradient.put(0.75, Color.YELLOW);
        gradient.put(1.0, Color.RED);

        latNorth = 0;
        latSouth = 0;
        lonWest = 0;
        lonEast = 0;
        dLat = 0;
        dLon = 0;

        loadGrid();
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

                p = new Point(
                        Double.parseDouble(parts[1]), // latitude
                        Double.parseDouble(parts[0]), // longitude
                        Double.parseDouble(parts[2]), // altitude
                        Double.parseDouble(parts[3]) // amplification
                );

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

        double latSpan = latNorth - latSouth;
        double lonSpan = diffLon(lonWest, lonEast);

        image = new BufferedImage(dLon == 0 ? 1 : (int) (lonSpan / dLon) + 1,
                                  dLat == 0 ? 1 : (int) (latSpan / dLat) + 1,
                                  BufferedImage.TYPE_INT_RGB);

        // map point coordinates to image index
        for (Point p : points) {
            p.x = (int) (diffLon(lonWest, p.longitude) / dLon);
            p.y = (int) ((p.latitude - latSouth) / dLat);
        }

        LOG.info(String.format("created shake map data structure,\n"
                               + "  points: %d\n"
                               + "  lat(min/max/d): %f/%f/%f\n"
                               + "  lon(min/max/d): %f/%f/%f\n"
                               + "  img(w/h): %d/%d", points.size(),
                               latSouth, latNorth, dLat,
                               lonWest, lonEast, dLon,
                               image.getWidth(), image.getHeight()));

        return true;
    }

    private double diffLon(double min, double max) {
        return max < min ? max - min + 360 : max - min;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isDirty() {
        return dirty;
    }

    public List<Point> getPoints() {
        return points;
    }

    public Image getImage() {
        return image;
    }

}
