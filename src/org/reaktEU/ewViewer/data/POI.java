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
public class POI extends AmplificationPoint {

    private static final Logger LOG = LogManager.getLogger(POI.class);

    public String name;
    public Map<Shaking.Type, Shaking> shakingValues;

    public POI(double latitude, double longitude, double altitude,
               double amplification, String name) {
        super(latitude, longitude, altitude, amplification);

        this.name = name;
        this.shakingValues = new ConcurrentHashMap();
    }

    @Override
    public String toString() {
        return name;
    }
}
