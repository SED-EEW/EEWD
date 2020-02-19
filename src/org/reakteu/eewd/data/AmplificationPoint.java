/*
 * Copyright (C) 2014-2015 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reakteu.eewd.data;

/**
 *
 * @author Stephan Herrnkind herrnkind at gempa dot de
 */
public class AmplificationPoint {

    public double latitude;
    public double longitude;
    public double altitude;
    public double amplification;

    public AmplificationPoint(double latitude, double longitude,
                              double altitude, double amplification) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.amplification = amplification;
    }

}
