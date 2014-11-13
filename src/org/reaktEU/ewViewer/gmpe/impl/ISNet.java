package org.reaktEU.ewViewer.gmpe.impl;

/*
 * PGA GMPE used by PRESTo 0.2.8 for the ISNet network (Southern Italy):
 * Akkar and Bommer (BSSA,2007) for M >= 4, Emolo et al. (JGE,2010) for smaller magnitudes.
 */
// import useful packages
import org.reaktEU.ewViewer.utils.GeoCalc;
import org.reaktEU.ewViewer.gmpe.AttenuationPGV;
import org.reaktEU.ewViewer.gmpe.AttenuationPGA;
import org.quakeml.xmlns.bedRt.x12.EventParameters;
import org.reaktEU.ewViewer.data.*;

import static java.lang.Math.*;

public class ISNet implements AttenuationPGA, AttenuationPGV {
    // Returns median PGA, 16th-percentile PGA, 84th percentile PGA
    // Mag is the magnitude from the EW message

    public Shaking getPGA(double Mag, double sourceLat, double sourceLon, double sourceDepthM, double targetLat, double targetLon, double ElevM, String ampType, double deltaIvalue, EventParameters ParamfromQuakeML) {
        // Compute hypocentral distance
        double[] pEvent = GeoCalc.Geo2Cart(sourceLat, sourceLon, -sourceDepthM);
        double[] pTarget = GeoCalc.Geo2Cart(targetLat, targetLon, ElevM);
        double distance = GeoCalc.Distance3D(pEvent, pTarget);

        double R = distance / 1000; // in kilometers

        // Compute log10(PGA_cm)
        double logpga, logpgasigma;
        if (Mag >= 4) {
            // Akkar and Bommer (BSSA,2007)
            logpga = 1.647 + 0.767 * Mag - 0.074 * pow(Mag, 2) + (-3.162 + 0.321 * Mag) * log10(sqrt(pow(R, 2) + pow(7.682, 2)));
            logpgasigma = sqrt(pow(0.557 - 0.049 * Mag, 2) + pow(0.189 - 0.017 * Mag, 2));
        } else {
            // Emolo et al. (JGE,2010)
            logpga = -2.024 + 0.469 * Mag - 1.442 * log10(R) + 2;
            logpgasigma = 0.444;
        }

        // Return shaking in %g
        double pga = pow(10, logpga) / 981 * 100;
        double pgaplus = pow(10, logpga + logpgasigma) / 981 * 100;
        double pgaminus = pow(10, logpga - logpgasigma) / 981 * 100;

        Shaking PGA = new Shaking();
        PGA.setShakingExpected(pga);
        PGA.setShaking84percentile(pgaplus);
        PGA.setShaking16percentile(pgaminus);

        return PGA;
    }

    public Shaking getPGV(double Mag, double sourceLat, double sourceLon, double sourceDepthM, double targetLat, double targetLon, double ElevM, String ampType, double deltaIvalue, EventParameters ParamfromQuakeML) {
        // Compute hypocentral distance
        double[] pEvent = GeoCalc.Geo2Cart(sourceLat, sourceLon, -sourceDepthM);
        double[] pTarget = GeoCalc.Geo2Cart(targetLat, targetLon, ElevM);
        double distance = GeoCalc.Distance3D(pEvent, pTarget);

        double R = distance / 1000; // in kilometers

        // Compute log10(PGV_cm)
        double logpgv, logpgvsigma;
        if (Mag >= 4) {
            // Akkar and Bommer (BSSA,2007)
            logpgv = -1.36 + 1.063 * Mag - 0.079 * pow(Mag, 2) + (-2.948 + 0.306 * Mag) * log10(sqrt(pow(R, 2) + pow(5.547, 2)));
            logpgvsigma = sqrt(pow(0.85 - 0.096 * Mag, 2) + pow(0.313 - 0.040 * Mag, 2));
        } else {
            // Emolo et al. (JGE,2010)
            logpgv = -3.943 + 0.540 * Mag - 1.458 * log10(R) + 2;
            logpgvsigma = 0.359;
        }

        // Return shaking in cm/s
        double pgv = pow(10, logpgv);
        double pgvplus = pow(10, logpgv + logpgvsigma);
        double pgvminus = pow(10, logpgv - logpgvsigma);

        Shaking PGV = new Shaking();
        PGV.setShakingExpected(pgv);
        PGV.setShaking84percentile(pgvplus);
        PGV.setShaking16percentile(pgvminus);

        return PGV;
    }
}
