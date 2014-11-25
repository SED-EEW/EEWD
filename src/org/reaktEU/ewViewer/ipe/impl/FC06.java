package org.reaktEU.ewViewer.ipe.impl;

/*
 * Intensity prediction equation for the Euro-Mediterranean region by Faccioli and Cauzzi (ECEES, 2006)
 */
// import useful packages
import org.reaktEU.ewViewer.utils.GeoCalc;
import org.reaktEU.ewViewer.gmpe.AttenuationPGV;
import org.reaktEU.ewViewer.gmpe.AttenuationPGA;
import org.quakeml.xmlns.bedRt.x12.EventParameters;
import org.reaktEU.ewViewer.data.*;

import static java.lang.Math.*;

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
        double I = 1.0157 + 1.2566 * Mw - 0.6547 * log (sqrt(R * R + 4));
        		
        Shaking Int = new Shaking();
        Int.expectedSI = I;
        Int.percentile84 = I + 0.5344;
        Int.percentile16 = I - 0.5344;

        return Int;
    }

    @Override
    public Shaking getPGV(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI,
                          EventParameters eventParameters) {
        // Compute hypocentral distance
        double[] pEvent = GeoCalc.Geo2Cart(sourceLat, sourceLon, -sourceDepthM);
        double[] pTarget = GeoCalc.Geo2Cart(targetLat, targetLon, targetElevM);
        double distance = GeoCalc.Distance3D(pEvent, pTarget);

        double R = distance / 1000; // in kilometers

        // Compute log10(PGV_cm)
        double logpgv, logpgvsigma;
        if (magnitude >= 4) {
            // Akkar and Bommer (BSSA,2007)
            logpgv = -1.36 + 1.063 * magnitude - 0.079 * pow(magnitude, 2) + (-2.948 + 0.306 * magnitude) * log10(sqrt(pow(R, 2) + pow(5.547, 2)));
            logpgvsigma = sqrt(pow(0.85 - 0.096 * magnitude, 2) + pow(0.313 - 0.040 * magnitude, 2));
        } else {
            // Emolo et al. (JGE,2010)
            logpgv = -3.943 + 0.540 * magnitude - 1.458 * log10(R) + 2;
            logpgvsigma = 0.359;
        }

        // Return shaking in cm/s
        Shaking PGV = new Shaking();
        PGV.expectedSI = pow(10, logpgv);
        PGV.percentile84 = pow(10, logpgv + logpgvsigma);
        PGV.percentile16 = pow(10, logpgv - logpgvsigma);

        return PGV;
    }
}
