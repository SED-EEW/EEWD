/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.reakteu.eewd.data;

/**
 *
 * @author Stephan Herrnkind <herrnkind@gempa.de>
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
