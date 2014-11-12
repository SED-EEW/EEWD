/*
 * Copyright (C) 2014 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reaktEU.ewViewer.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Stephan Herrnkind <herrnkind@gempa.de>
 */
public class POI {

    private static final Logger LOG = LogManager.getLogger(POI.class);

    public String name;
    public double latitude;
    public double longitude;
    public double altitude;
    public double amplification;

    public Map<Shaking.Type, Shaking> shakingValues;

    public POI(String name, double latitude, double longitude,
               double altitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;

        // TODO
        this.amplification = 0.7;

        this.shakingValues = new ConcurrentHashMap();
    }

    @Override
    public String toString() {
        return name;
    }
}
