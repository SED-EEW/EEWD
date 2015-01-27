/*
 * Copyright (C) 2014-2015 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reakteu.eewd.data;

import java.util.ArrayList;
import java.util.List;
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
    public List<Shaking> spectralValues;
    public boolean triggered;

    public POI(double latitude, double longitude, double altitude,
               double amplification, String name) {
        super(latitude, longitude, altitude, amplification);

        this.name = name;
        this.shakingValues = new ConcurrentHashMap();
        this.spectralValues = new ArrayList();
        this.triggered = false;
    }

    @Override
    public String toString() {
        return name;
    }

    public void clearValues() {
        this.shakingValues.clear();
        this.spectralValues.clear();
        this.triggered = false;
    }
}
