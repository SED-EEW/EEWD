package org.reaktEU.ewViewer.ipe;

/*
 * Intensity prediction equation for the Euro-Mediterranean region by Faccioli and Cauzzi (ECEES, 2006)
 */
// import useful packages
import org.reaktEU.ewViewer.utils.GeoCalc;
import org.quakeml.xmlns.bedRt.x12.EventParameters;
import org.reaktEU.ewViewer.data.*;

import static java.lang.Math.*;
import org.reaktEU.ewViewer.ipe.AttenuationInt;

public class FC06 implements AttenuationInt {
    // Returns mean I, plus / minus one sigma
    // Mag is the magnitude from the EW message

    @Override
    public Shaking getInt(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI,
                          EventParameters eventParameters) {
        // Compute epicentral distance
        double[] pEvent = GeoCalc.Geo2Cart(sourceLat, sourceLon, 0);
        double[] pTarget = GeoCalc.Geo2Cart(targetLat, targetLon, 0);
        double distance = GeoCalc.Distance3D(pEvent, pTarget);

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
