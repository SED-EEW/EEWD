package org.reaktEU.ewViewer.ipe;

/*
 * Global intensity prediction equation by Allen et al., (JOSE 2012) - Rhyp-based model.
 * No site correction implemented. Users who are willing to do so, should use Eq. (4)
 * and Eq. (5) of the paper and define the amplificationProxyValueSI accordingly.
 */
// import useful packages
import org.reaktEU.ewViewer.utils.GeoCalc;
import org.quakeml.xmlns.bedRt.x12.EventParameters;
import org.reaktEU.ewViewer.data.Shaking;

public class AEA12 implements AttenuationInt {
    // Returns mean I, plus / minus one sigma
    // Mag is the magnitude from the EW message

    @Override
    public Shaking getInt(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI,
                          EventParameters eventParameters) {

        // Compute hypocentral distance
        double[] pEvent = GeoCalc.Geo2Cart(sourceLat, sourceLon, sourceDepthM);
        double[] pTarget = GeoCalc.Geo2Cart(targetLat, targetLon, targetElevM);
        double distance = GeoCalc.Distance3D(pEvent, pTarget);

        double R = distance / 1000; // in kilometers
        double I;// = 2; // initialise I
        // Compute I (any scale)

        double Mw = magnitude;

        double RM = -0.209 + 2.042 * Math.exp(Mw - 5);

        double c0 = 2.085;
        double c1 = 1.428;
        double c2 = -1.402;
        double c4 = 0.078;
        //double m1 = -0.209;
        //double m2 = 2.042;

        if (R <= 50) {
            I = c0 + c1 * Mw + c2 * Math.log(Math.sqrt(R * R + RM * RM));
        } else {
            I = c0 + c1 * Mw + c2 * Math.log(Math.sqrt(R * R + RM * RM)) + c4 * Math.log(R / 50);
        }

        Shaking Int = new Shaking();
        Int.expectedSI = I;
        Int.percentile84 = I + (0.82 + 0.37 / (1 + (R / 22.9) * (R / 22.9)));
        Int.percentile16 = I - (0.82 + 0.37 / (1 + (R / 22.9) * (R / 22.9)));

        return Int;
    }
}
