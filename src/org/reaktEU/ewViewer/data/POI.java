/*
 * Copyright (C) 2014 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reaktEU.ewViewer.data;

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

    public POI(String name, double latitude, double longitude,
               double altitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }

    @Override
    public String toString() {
        return name;
    }
}