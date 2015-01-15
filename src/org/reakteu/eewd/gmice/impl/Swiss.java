package org.reakteu.eewd.gmice.impl;

/*
 * Implementation of GMICE of
 * Faenza and Michelini (2010), used in Switzerland
 */
// import useful packages
import org.reakteu.eewd.data.Shaking;
import org.reakteu.eewd.gmice.IntensityFromVelocity;

public class Swiss implements IntensityFromVelocity {

    @Override
    public Shaking getIntensityFromVelocity(Shaking PGV) {
	// returns the macroseimic intensity I estimate as double
        // PGV is the peak ground velocity (median, 84th, 16th percentile)

        // Minimum intensity possible
        double Imin = 1;

        // Conversion equation assumes PGV in cm/s
        Shaking IfromPGV = new Shaking();

        double IfromPGVmedian = 5.11 + 2.35 * Math.log10(100 * PGV.expectedSI);
        double IfromPGV84 = 5.11 + 2.35 * Math.log10(100 * PGV.percentile84);
        double IfromPGV16 = 5.11 + 2.35 * Math.log10(100 * PGV.percentile16);

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

        IfromPGV.expectedSI = IfromPGVmedian;
        IfromPGV.percentile84 = IfromPGV84;
        IfromPGV.percentile16 = IfromPGV16;

        return IfromPGV;
    }

}
