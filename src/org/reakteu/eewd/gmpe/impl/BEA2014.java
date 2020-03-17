package org.reakteu.eewd.gmpe.impl;

/*
 * Implementation of the empirical GMP model (Rhypo;VS30)
 * of Bindi et al. (BEE,2014) following
 */
// import useful packages
import org.reakteu.eewd.data.Shaking;
import org.reakteu.eewd.utils.GeoCalc;
import org.reakteu.eewd.gmpe.AttenuationPGV;
import org.reakteu.eewd.gmpe.AttenuationPGA;
import org.reakteu.eewd.gmpe.AttenuationPSA;
import org.reakteu.eewd.gmpe.AttenuationDRS;
import org.quakeml.xmlns.bedRt.x12.EventParameters;

import static java.lang.Math.*;

public class BEA2014 implements AttenuationPGA, AttenuationPGV, AttenuationPSA, AttenuationDRS {

    public static final double[][] Cofs = {
        {4.3397, 4.46839, 4.5724, 4.55255, 4.51119, 4.49571, 4.49224, 4.51726, 4.46559, 4.46834, 4.3715, 4.34198, -1.37164, 4.14832, 4.09246, 4.08324, 4.07207, 3.77954, 3.69447, 3.45408, 3.38901, 3.06601, 2.89391, 4.27391, 3.24249},
        {-1.60402, -1.68536, -1.63863, -1.57947, -1.4471, -1.37039, -1.36679, -1.40078, -1.40973, -1.42893, -1.40655, -1.39751, 17.7584, -1.37169, -1.37736, -1.38649, -1.38735, -1.27343, -1.26477, -1.27364, -1.28283, -1.23427, -1.16461, -1.57821, -1.57556},
        {0.103401, 0.126703, 0.123954, 0.125609, 0.0846097, 0.0385358, 0.0129374, 0.00197997, 0.000488761, -0.00909559, 0.00100953, 0.00423803, 0.216704, 0.00226411, 0.008956, -0.00453151, -0.0185458, -0.0137662, -0.00337334, 0.083746, 0.086724, 0.150146, 0.162354, 0.108218, 0.0791774},
        {4.47852, 4.58063, 5.12096, 5.67511, 4.8248, 4.56965, 3.94802, 4.26816, 4.39978, 4.6039, 4.60254, 4.43045, 886.652, 3.00978, 3.15727, 3.4537, 3.3163, 3.04976, 3.65482, 4.59988, 4.95285, 4.45511, 4.62321, 4.82743, 4.38918},
        {2.63E-05, 0, 0.00072223, 0.00123904, 0.00169202, 0.00158593, 0.00105878, 0.000564819, 5.97E-05, 0, 0, 0, 0.0560598, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9.64E-05, 0},
        {0.230422, 0.205651, 0.226272, 0.167382, 0.194714, 0.289627, 0.321065, 0.336096, 0.346351, 0.353351, 0.35717, 0.384532, -0.255828, 0.466754, 0.510102, 0.567727, 0.631338, 0.650829, 0.6746, 0.563304, 0.548353, 0.54175, 0.590765, 0.217109, 0.472433},
        {-0.0665354, -0.0528102, -0.0298015, -0.0509066, -0.0784507, -0.0815499, -0.104184, -0.115261, -0.127114, -0.137776, -0.142768, -0.140916, -0.121077, -0.138065, -0.13263, -0.127244, -0.121241, -0.129005, -0.119081, -0.117803, -0.129571, -0.103699, -0.0853286, -0.0682563, -0.0725484},
        {0.363906, 0.323734, 0.311109, 0.348968, 0.448903, 0.533244, 0.596455, 0.612107, 0.600314, 0.621323, 0.589127, 0.543301, 0, 0.498126, 0.437529, 0.45811, 0.474982, 0.488244, 0.461122, 0.184126, 0.171017, 0.00930258, 0.0340584, 0.352976, 0.436952},
        {-0.286524, -0.232462, -0.195629, -0.168432, -0.194539, -0.270912, -0.323555, -0.363199, -0.430464, -0.467397, -0.531694, -0.555531, -0.457888, -0.698998, -0.757522, -0.786632, -0.791438, -0.803656, -0.780198, -0.749008, -0.744073, -0.744468, -0.693999, -0.293242, -0.508833},
        {-0.0469231, -0.0451723, -0.053205, -0.0470393, -0.0363123, -0.0386754, -0.0365771, -0.038065, -0.0285343, -0.0261626, -0.0192819, -0.0175798, 0.0223674, 0.0100027, 0.0150184, 0.0163802, 0.0263957, 0.024922, 0.0191231, 0.0116759, 0.00499277, 0.00602681, 0.0186211, -0.0472145, -0.0157195},
        {0.115063, 0.114597, 0.121653, 0.119021, 0.102481, 0.107555, 0.103236, 0.104818, 0.0955093, 0.0971983, 0.090202, 0.0860123, 0.125552, 0.0543876, 0.0458647, 0.0442236, 0.0411366, 0.038329, 0.0386966, 0.029249, 0.0335873, 0.0305081, -0.0189824, 0.110979, 0.0713859},
        {-0.06814, -0.069425, -0.0684477, -0.0719821, -0.0661686, -0.0688793, -0.0666589, -0.0667532, -0.0669749, -0.0710355, -0.0709198, -0.0684321, -0.147934, -0.06439, -0.0608828, -0.0606035, -0.0675319, -0.0632507, -0.0578195, -0.0409247, -0.0385798, -0.0365347, 0.000361328, -0.0637639, -0.055666},
        {0.154538, 0.158402, 0.169775, 0.165148, 0.145533, 0.144701, 0.156869, 0.165195, 0.164907, 0.165146, 0.181401, 0.189686, 0.259955, 0.20181, 0.211664, 0.225279, 0.238973, 0.212162, 0.208441, 0.203238, 0.205751, 0.190711, 0.183363, 0.145783, 0.193206},
        {0.290986, 0.298261, 0.302117, 0.310963, 0.310621, 0.308845, 0.313737, 0.311052, 0.310509, 0.310959, 0.306033, 0.304174, 0.397088, 0.30827, 0.30855, 0.313873, 0.318631, 0.324083, 0.33425, 0.342873, 0.347114, 0.339373, 0.326297, 0.291566, 0.295126},
        {0.18825, 0.192664, 0.205229, 0.212643, 0.216313, 0.20204, 0.199484, 0.186722, 0.180734, 0.182064, 0.176797, 0.178065, 0.189183, 0.264361, 0.208994, 0.225906, 0.246861, 0.245588, 0.24415, 0.256308, 0.26183, 0.242015, 0.22865, 0.186662, 0.178867},
        {0.329477, 0.337714, 0.346552, 0.352097, 0.343023, 0.341063, 0.350769, 0.352197, 0.351583, 0.352092, 0.355756, 0.358473, 0.474611, 0.368453, 0.374172, 0.386351, 0.398289, 0.387354, 0.393917, 0.398582, 0.403511, 0.389288, 0.374289, 0.325981, 0.352744}
    };

    public static final double PI2_4 = 4 * PI * PI;

    @Override
    public Shaking getPGA(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI,
                          EventParameters eventParameters,
                          Float ruptureLength,
                          Float ruptureStrike) {

        // Returns median PGA, 16th-percentile PGA, 84th percentile PGA in m/s2
        // Mag is the magnitude from the EW message
        // ampType is VS30
        double Mw = magnitude;	// reasonable assumption for CH, other regions should perform some investigations ...

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


        double Rh = distance / 1000; // in kilometers

        // end of hypocentral distance computation
        // Compute ground-motion prediction in log10 first
        double Mref = 5.5;
        double Rref = 1;
        double Mh = 6.75;
        double Vref = 800;
        double FM;

        double FD = (Cofs[1][23] + Cofs[2][23] * (Mw - Mref)) * log10(sqrt(pow(Rh, 2) + pow(Cofs[3][23], 2)) / Rref) - Cofs[4][23] * (sqrt(pow(Rh, 2) + pow(Cofs[3][23], 2)) - Rref);

        if (Mw <= Mh) {
            FM = Cofs[5][23] * (Mw - Mh) + Cofs[6][23] * pow((Mw - Mh), 2);
        } else {
            FM = Cofs[7][23] * (Mw - Mh);
        }
        double FSOF = (Cofs[9][23] + Cofs[10][23] + Cofs[11][23]) / 3;

        double logpga = Cofs[0][23] + FD + FM + FSOF;

        // Now add site term
        double logpgasite = logpga + Cofs[8][23] * log10(amplificationProxyValueSI / Vref);

        // Now compute plus/minus sigma bounds
        double sigma = Cofs[15][23];
        double logpgasiteplus = logpgasite + sigma;
        double logpgasiteminus = logpgasite - sigma;

        // Now in m/s2
        Shaking PGA = new Shaking();
        PGA.expectedSI = pow(10, logpgasite) / 100;
        PGA.percentile84 = pow(10, logpgasiteplus) / 100;
        PGA.percentile16 = pow(10, logpgasiteminus) / 100;

        // Now should return Shaking ...
        return PGA;
    }

    @Override
    public Shaking getPGV(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI,
                          EventParameters eventParameters,
                          Float ruptureLength,
                          Float ruptureStrike) {

        // Returns median PGV, 16th-percentile PGA, 84th percentile PGA in m/s
        // Mag is the magnitude from the EW message
        // ampType is VS30
        double Mw = magnitude;	// reasonable assumption for CH, other regions should perform some investigations ...

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


        double Rh = distance / 1000; // in kilometers

        // end of hypocentral distance computation
        // Compute ground-motion prediction in log10 first
        double Mref = 5.5;
        double Rref = 1;
        double Mh = 6.75;
        double Vref = 800;
        double FM; //init

        double FD = (Cofs[1][24] + Cofs[2][24] * (Mw - Mref)) * log10(sqrt(pow(Rh, 2) + pow(Cofs[3][24], 2)) / Rref) - Cofs[4][24] * (sqrt(pow(Rh, 2) + pow(Cofs[3][24], 2)) - Rref);

        if (Mw <= Mh) {
            FM = Cofs[5][24] * (Mw - Mh) + Cofs[6][24] * pow((Mw - Mh), 2);
        } else {
            FM = Cofs[7][24] * (Mw - Mh);
        }

        double FSOF = (Cofs[9][24] + Cofs[10][24] + Cofs[11][24]) / 3;

        double logpgv = Cofs[0][24] + FD + FM + FSOF;

        // Now add site term
        double logpgvsite = logpgv + Cofs[8][24] * log10(amplificationProxyValueSI / Vref);

        // Now compute plus/minus sigma bounds
        double sigma = Cofs[15][24];
        double logpgvsiteplus = logpgvsite + sigma;
        double logpgvsiteminus = logpgvsite - sigma;

        // Now in m/s
        Shaking PGV = new Shaking();
        PGV.expectedSI = pow(10, logpgvsite) / 100;
        PGV.percentile84 = pow(10, logpgvsiteplus) / 100;
        PGV.percentile16 = pow(10, logpgvsiteminus) / 100;

        // Now should return Shaking ...
        return PGV;
    }

    @Override
    public Shaking getPSA(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI,
                          double period, EventParameters eventParameters,
                          Float ruptureLength,
                          Float ruptureStrike) {

        // Returns median PSA, 16th-percentile PGA, 84th percentile PGA in m/s
        // Mag is the magnitude from the EW message
        // ampType is VS30
        double Mw = magnitude;	// reasonable assumption for CH, other regions should perform some investigations ...
        int cnt = 0; // init

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


        double Rh = distance / 1000; // in kilometers

        // end of hypocentral distance computation
        // Compute ground-motion prediction in log10 first
        double Mref = 5.5;
        double Rref = 1;
        double Mh = 6.75;
        double Vref = 800;
        double FM; //init

        // pick the right coefficients according to the spectral period
        if (period == 0.01) { // using published coeffs for 0.02
            cnt = 0;
        } else if (period == 0.02) {
            cnt = 0;
        } else if (period == 0.03) { // using published coeffs for 0.04
            cnt = 1;
        } else if (period == 0.05) { // using published coeffs for 0.04
            cnt = 1;
        } else if (period == 0.1) {
            cnt = 3;
        } else if (period == 0.2) {
            cnt = 5;
        } else if (period == 0.4) {
            cnt = 9;
        } else if (period == 1) {
            cnt = 16;
        } else if (period == 2) {
            cnt = 20;
        }

        double FD = (Cofs[1][cnt] + Cofs[2][cnt] * (Mw - Mref)) * log10(sqrt(pow(Rh, 2) + pow(Cofs[3][cnt], 2)) / Rref) - Cofs[4][cnt] * (sqrt(pow(Rh, 2) + pow(Cofs[3][cnt], 2)) - Rref);

        if (Mw <= Mh) {
            FM = Cofs[5][cnt] * (Mw - Mh) + Cofs[6][cnt] * pow((Mw - Mh), 2);
        } else {
            FM = Cofs[7][cnt] * (Mw - Mh);
        }

        double FSOF = (Cofs[9][cnt] + Cofs[10][cnt] + Cofs[11][cnt]) / 3;

        double logpsa = Cofs[0][cnt] + FD + FM + FSOF;

        // Now add site term
        double logpsasite = logpsa + Cofs[8][cnt] * log10(amplificationProxyValueSI / Vref);

        // Now compute plus/minus sigma bounds
        double sigma = Cofs[15][cnt];
        double logpsasiteplus = logpsasite + sigma;
        double logpsasiteminus = logpsasite - sigma;

        // Now in m/s
        Shaking PSA = new Shaking();
        PSA.expectedSI = pow(10, logpsasite) / 100;
        PSA.percentile84 = pow(10, logpsasiteplus) / 100;
        PSA.percentile16 = pow(10, logpsasiteminus) / 100;

        // Now should return Shaking ...
        return PSA;
    }

    @Override
    public Shaking getDRS(double magnitude, double sourceLat, double sourceLon, double sourceDepthM, double targetLat, double targetLon, double targetElevM, String amplificationType, double amplificationProxyValueSI, double period, EventParameters eventML,
            Float ruptureLength,
            Float ruptureStrike) {

        Shaking PSA = getPSA(magnitude, sourceLat, sourceLon, sourceDepthM,
                             targetLat, targetLon, targetElevM,
                             amplificationType, amplificationProxyValueSI,
                             period, null, ruptureLength, ruptureStrike);

        double accelerationToDisplacement = period * period / PI2_4;
        PSA.expectedSI *= accelerationToDisplacement;
        PSA.percentile16 *= accelerationToDisplacement;
        PSA.percentile84 *= accelerationToDisplacement;

        // Now in m/s
        return PSA;
    }
}
