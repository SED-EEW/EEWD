package org.reaktEU.ewViewer.gmice.impl;

/*
 * Implementation of GMICE of
 * Faenza and Michelini (2010), used in Switzerland
 */
// import useful packages
import org.reaktEU.ewViewer.gmice.IntensityFromVelocity;
import org.reaktEU.ewViewer.data.*;

public class Swiss implements IntensityFromVelocity {

    public Shaking getIntensityFromVelocity(Shaking PGV) {
	// returns the macroseimic intensity I estimate as double
        // PGV is the peak ground velocity (median, 84th, 16th percentile)

        // Minimum intensity possible
        double Imin = 1;

        // Conversion equation assumes PGV in cm/s
        Shaking IfromPGV = new Shaking();

        double IfromPGVmedian = (5.11 + 2.35 * Math.log10(PGV.getShakingExpected()));
        double IfromPGV84 = (5.11 + 2.35 * Math.log10(PGV.getShaking84percentile()));
        double IfromPGV16 = (5.11 + 2.35 * Math.log10(PGV.getShaking16percentile()));

        // Impose minimum intensity if necessary
        if (IfromPGVmedian < 1) {
            IfromPGVmedian = 1;
        }
        if (IfromPGV84 < 1) {
            IfromPGV84 = 1;
        }
        if (IfromPGV16 < 1) {
            IfromPGV16 = 1;
        }

        IfromPGV.setShakingExpected(IfromPGVmedian);
        IfromPGV.setShaking84percentile(IfromPGV84);
        IfromPGV.setShaking16percentile(IfromPGV16);

        return IfromPGV;
    }

}
