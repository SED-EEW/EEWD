package org.reakteu.eewd.ipe.impl;

/*
 * Intensity prediction equation for the Euro-Mediterranean region by Faccioli and Cauzzi (ECEES, 2006)
 */
// import useful packages
import org.reakteu.eewd.data.Shaking;
import org.reakteu.eewd.utils.GeoCalc;
import org.quakeml.xmlns.bedRt.x12.EventParameters;

import static java.lang.Math.*;
import org.reakteu.eewd.ipe.AttenuationInt;

public class FC06 implements AttenuationInt {
    // Returns mean I, plus / minus one sigma
    // Mag is the magnitude from the EW message

    @Override
    public Shaking getInt(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI,
                          EventParameters eventParameters,
                          Float ruptureLength,
                          Float ruptureStrike) {
    	// Compute distance
        //double[] pEvent = GeoCalc.Geo2Cart(sourceLat, sourceLon, -sourceDepthM); deprecated
        //double[] pTarget = GeoCalc.Geo2Cart(targetLat, targetLon, targetElevM); deprectaed
        
        double[] pEvent = {sourceLat, sourceLon, -sourceDepthM};
        double[] pTarget = {targetLat, targetLon, targetElevM};
        
        
        double distance;
        
        if (ruptureLength != null) {
        	
        	double[] lExtremes = GeoCalc.CentroidToExtremes(ruptureStrike, ruptureLength, sourceLon, sourceLat, -sourceDepthM);
            double[] start = {lExtremes[1],lExtremes[0],lExtremes[2]};
            double[] end = {lExtremes[4],lExtremes[3],lExtremes[5]};
            double[] current = {pTarget[0],pTarget[1]};
            double d = GeoCalc.DistanceFromLine(start, end, current);
            distance = Math.sqrt(d * d + (sourceDepthM + targetElevM) * (sourceDepthM + targetElevM));
             
            
        } else {
        
        	distance = GeoCalc.Distance3DDegToM(pEvent, pTarget);
        }


        double R = distance / 1000; // in kilometers

        // Compute I (EMS98 scale)
        double Mw = magnitude;
        double I = 1.0157 + 1.2566 * Mw - 0.6547 * log(sqrt(R * R + 4));

        Shaking Int = new Shaking();
        Int.expectedSI = I;
        Int.percentile84 = I + 0.5344;
        Int.percentile16 = I - 0.5344;

        return Int;
    }
}
