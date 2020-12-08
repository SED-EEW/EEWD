package org.reakteu.eewd.gmpe.impl;

/**
 * Implementation of the Ground Motion Prediction Equation Model by Víctor
 * Schmidt-Díaz published in "Revista Geologica de America Central", 50: 7-37,
 * 2014 ISSN: 0256-7024:
 *
 * GROUND MOTION PREDICTION MODELS FOR CENTRAL AMERICA USING DATA FROM 1972 TO
 * 2010
 *
 * The GMPEs are for shallow or crustal earthquakes in Central America and subduction earthquakes for Costa Rica.
 * 
 * An Earthquake is considered to be Shallow is its focal deep is less than 25 km and its
 * equation for PGA and PSA is
 *
 * log (Y) = C1 + C2*Mw + C3*log(sqrt( D**2 + C4**2 )) + C5*S + C6*H (1)
 *
 * and if it is equal or more than 25 km then is considered to be a subduction
 * event and its equation for PGA and PSA is
 *
 * Log (Y) = C1 + C2*Mw + C3* log(sqrt( D**2 + 5**2 )) + C4*S + C5*H (2)
 *
 * The corresponding coeficients are in the code (Cofs matrix for eq. 1 and
 * CofsSub for eq. 2) to obtain the PSA
 *
 * This implementation is for the EWARNICA project
 */
import static java.lang.Math.*;
import org.reakteu.eewd.data.Shaking;
import org.reakteu.eewd.utils.GeoCalc;
import org.reakteu.eewd.gmpe.AttenuationPGA;
import org.reakteu.eewd.gmpe.AttenuationPSA;
import org.reakteu.eewd.gmpe.AttenuationDRS;
import org.quakeml.xmlns.bedRt.x12.EventParameters;

/**
 * @author Billy Burgoa Rosso
 */

public class SCH2014 implements AttenuationPGA, AttenuationPSA, AttenuationDRS {

    //Coefficients GMPE
    // The first row in each matrix is the coeficients for PGA
    // The remaining ones are for PSA for different periods or frequencies
    
    
    //For Shallow or crustal Events ( Depth < 25 km )
    // Each column value is:
    /* {Period, Frequency, C1, C2, C3, C4, C5, C6, SD} */
    public static final double[][] Cofs = {
        {0.000, 0.000,  0.12602, 0.49081, -1.03591, 4.22442, 0.22075, 0.11742, 0.4078},
        {0.020,50.000,  0.15454, 0.48743, -1.03269, 3.83891, 0.21489, 0.11115, 0.4093},
        {0.075,13.330,  0.65109, 0.44289, -1.06921, 2.15714, 0.21033, 0.16857, 0.4131},
        {0.100,10.000,  0.73993, 0.45503, -1.08638, 4.65588, 0.15027, 0.12146, 0.4105},
        {0.150, 6.670,  0.75961, 0.48369, -1.14080, 8.92792, 0.17034, 0.07779, 0.4194},
        {0.200, 5.000,  0.47439, 0.51668, -1.12103, 8.40521, 0.24001, 0.13820, 0.4394},
        {0.240, 4.170,  0.13594, 0.53838, -1.06081, 6.43782, 0.35122, 0.18941, 0.4533},
        {0.303, 3.300, -0.30862, 0.57998, -1.02221, 4.81306, 0.49143, 0.25633, 0.4727},
        {0.340, 2.940, -0.54820, 0.60292, -0.99302, 3.71378, 0.53045, 0.26343, 0.4850},
        {0.400, 2.500, -0.83868, 0.64157, -0.99596, 3.50109, 0.55494, 0.27397, 0.4936},
        {0.440, 2.270, -1.00207, 0.66931, -1.01606, 4.30379, 0.56379, 0.26458, 0.4934},
        {0.500, 2.000, -1.20878, 0.70917, -1.05064, 5.31951, 0.55509, 0.24432, 0.4937},
        {0.600, 1.670, -1.43766, 0.74588, -1.07315, 6.41843, 0.52324, 0.18262, 0.5041},
        {0.752, 1.330, -1.82261, 0.80402, -1.11353, 7.88318, 0.49587, 0.17859, 0.5153},
        {0.900, 1.110, -2.14054, 0.84448, -1.11862, 7.77669, 0.45466, 0.14805, 0.5293},
        {1.000, 1.000, -2.26639, 0.85062, -1.10729, 7.93455, 0.43811, 0.13807, 0.5303},
        {1.250, 0.800, -2.46703, 0.85888, -1.11210, 9.40068, 0.41800, 0.13320, 0.5210},
        {1.493, 0.670, -2.76269, 0.87686, -1.09131, 7.94650, 0.40900, 0.12784, 0.5245},
        {2.000, 0.500, -3.12790, 0.90950, -1.11692, 7.67692, 0.39708, 0.10211, 0.5324},
        {2.500, 0.400, -3.24945, 0.89781, -1.11774, 7.38462, 0.36476, 0.09728, 0.5270},
        {3.030, 0.330, -3.33051, 0.87247, -1.09664, 6.02374, 0.35850, 0.09856, 0.5288},
        {4.000, 0.250, -3.40089, 0.83189, -1.06428, 3.28831, 0.33733, 0.08206, 0.5277},
        {5.000, 0.200, -3.41634, 0.80107, -1.06854, 2.46111, 0.33726, 0.08534, 0.5403}
    };

    //Coeficients for subduction events (depth >= 25 km)
    /* {Period, Frequency, C1, C2, C3, C4, C5, SD} */
    public static final double[][] CofsSub = {
        {0.000, 0.00,  0.4981, 0.537, -1.301, 0.3596, 0.116, 0.352},
        {0.020,50.00,  0.5250, 0.536, -1.308, 0.3580, 0.119, 0.353},
        {0.040,25.00,  0.7320, 0.524, -1.365, 0.3450, 0.127, 0.352},
        {0.075,13.33,  1.2270, 0.496, -1.465, 0.2990, 0.134, 0.355},
        {0.100,10.00,  1.2920, 0.486, -1.403, 0.2520, 0.115, 0.349},
        {0.150, 6.67,  1.1370, 0.505, -1.294, 0.2530, 0.034, 0.368},
        {0.200, 5.00,  0.7500, 0.544, -1.215, 0.3110, 0.079, 0.362},
        {0.240, 4.17,  0.3400, 0.585, -1.168, 0.4110, 0.122, 0.368},
        {0.303, 3.30, -0.0810, 0.623, -1.146, 0.5800, 0.168, 0.361},
        {0.340, 2.94, -0.2910, 0.648, -1.144, 0.6220, 0.173, 0.372},
        {0.400, 2.50, -0.6700, 0.678, -1.082, 0.6510, 0.172, 0.371},
        {0.440, 2.27, -0.8820, 0.696, -1.066, 0.6890, 0.186, 0.373},
        {0.500, 2.00, -1.1480, 0.739, -1.087, 0.6960, 0.190, 0.373},
        {0.600, 1.67, -1.4710, 0.787, -1.094, 0.6490, 0.134, 0.373},
        {0.752, 1.33, -1.9140, 0.842, -1.108, 0.6320, 0.135, 0.387},
        {0.900, 1.11, -2.3330, 0.868, -1.038, 0.6170, 0.132, 0.396},
        {1.000, 1.00, -2.5550, 0.882, -0.991, 0.5920, 0.119, 0.397},
        {1.250, 0.80, -2.9250, 0.898, -0.908, 0.5290, 0.113, 0.401},
        {1.493, 0.67, -3.3000, 0.932, -0.889, 0.5270, 0.106, 0.412},
        {2.000, 0.50, -3.7060, 0.964, -0.909, 0.5300, 0.130, 0.412},
        {2.500, 0.40, -3.9270, 0.981, -0.921, 0.4880, 0.135, 0.398},
        {3.030, 0.33, -3.9540, 0.979, -0.973, 0.4390, 0.124, 0.386},
        {4.000, 0.25, -3.9430, 0.944, -0.981, 0.3760, 0.108, 0.384},
        {5.000, 0.20, -3.9540, 0.923, -0.989, 0.3140, 0.112, 0.380}
    };

    public static final double PI2_4 = 4 * PI * PI;
    public static final double PI_2 = 2 * PI;

    @Override
    public Shaking getPGA(double magnitude, double sourceLat, double sourceLon,
            double sourceDepthM, double targetLat, double targetLon,
            double targetElevM, String amplificationType,
            double amplificationProxyValueSI,
            EventParameters eventParameters,
            Float ruptureLength,
            Float ruptureStrike) {
        
        // Returns PGA, 16th-percentile PGA, 84th percentile PGA in cm/s2 (?)
       
        double rmin = 10.0; // Minumum Distance
        double Mw = magnitude;
        double S = 1;
        double H = 0;

        double[] pEvent = {sourceLat, sourceLon, -sourceDepthM};
        double[] pTarget = {targetLat, targetLon, targetElevM};

        double distance;

        if (ruptureLength != null) {

            double[] lExtremes = GeoCalc.CentroidToExtremes(ruptureStrike, ruptureLength, sourceLon, sourceLat, -sourceDepthM);
            double[] start = {lExtremes[1], lExtremes[0], lExtremes[2]};
            double[] end = {lExtremes[4], lExtremes[3], lExtremes[5]};
            double[] current = {pTarget[0], pTarget[1]};
            double d = GeoCalc.DistanceFromLine(start, end, current);
            distance = Math.sqrt(d * d + (sourceDepthM + targetElevM) * (sourceDepthM + targetElevM));

        } else {

            distance = GeoCalc.Distance3DDegToM(pEvent, pTarget);
        }

        double Rh = distance / 1000; // in kilometers

        // Apply distance cutoff
        double Ru = max(rmin, Rh);

        // Site term
        if (amplificationProxyValueSI < 760) {
            //S III and IV are for soft soil
            S = 1;
            H = 0;

        } else {
            // S I is rock site
            S = 0;
            H = 0;
        }

        if (amplificationProxyValueSI == -1) {
            //S II firm or steady soil 
            // 
            S = 0;
            H = 1;
        }

        double depthKm = sourceDepthM / 1000.; // Depth in km

        //Shallow or Crustal Events
        if (depthKm < 25) {

            // Compute ground-motion prediction
            double logpgasite = Cofs[0][2] + Cofs[0][3] * Mw + Cofs[0][4] * log(Math.sqrt(Ru * Ru + Cofs[0][5] * Cofs[0][5])) + Cofs[0][6] * S + Cofs[0][7] * H;

            //Compute +/- sigma bounds
            double logpgasiteplus = logpgasite + Cofs[0][8];
            double logpgasiteminus = logpgasite - Cofs[0][8];

            // The original PGA is in cm/s*2 
            // So it is applied a basic conversion from cm to m
            Shaking PGA = new Shaking();
//            PGA.expectedSI = pow(10,logpgasite)/100;
//            PGA.percentile84 = pow(10,logpgasiteplus)/100;
//            PGA.percentile16 = pow(10,logpgasiteminus)/100;

            PGA.expectedSI = pow(10,logpgasite);
            PGA.percentile84 = pow(10,logpgasiteplus);
            PGA.percentile16 = pow(10,logpgasiteminus);

            return PGA;
        } //Subduction Events
        else {

            // Compute ground-motion prediction
            double logpgasite = CofsSub[0][2] + CofsSub[0][3] * Mw + CofsSub[0][4] * log(Math.sqrt(Ru * Ru + pow(5, 2))) + CofsSub[0][5] * S + CofsSub[0][6] * H;

            //Compute +/- sigma bounds
            double logpgasiteplus = logpgasite + CofsSub[0][7];
            double logpgasiteminus = logpgasite - CofsSub[0][7];

            // The original PGA is in cm/s*2 
            // So it is applied a basic conversion from cm to m
            // and the PGA is in m/s*2
            Shaking PGA = new Shaking();
//            PGA.expectedSI = pow(10,logpgasite)/100;
//            PGA.percentile84 = pow(10,logpgasiteplus)/100;
//            PGA.percentile16 = pow(10,logpgasiteminus)/100;

            PGA.expectedSI = pow(10,logpgasite);
            PGA.percentile84 = pow(10,logpgasiteplus);
            PGA.percentile16 = pow(10,logpgasiteminus);

            return PGA;

        }

    }

    @Override
    public Shaking getPSA(double magnitude, double sourceLat, double sourceLon,
            double sourceDepthM, double targetLat, double targetLon,
            double targetElevM, String amplificationType,
            double amplificationProxyValueSI, double period,
            EventParameters eventParameters,
            Float ruptureLength,
            Float ruptureStrike) {
        // Returns PsA, 16th-percentile PGA, 84th percentile PGA in cm/s2 (?)
        
        double rmin = 10.0; //Minimum Distance
        double Mw = magnitude;
        double S = 1; //site term 
        double H = 0; // site term
        int cnt = 1; // counter value

        double[] pEvent = {sourceLat, sourceLon, -sourceDepthM};
        double[] pTarget = {targetLat, targetLon, targetElevM};

        double distance;

        if (ruptureLength != null) {

            double[] lExtremes = GeoCalc.CentroidToExtremes(ruptureStrike, ruptureLength, sourceLon, sourceLat, -sourceDepthM);
            double[] start = {lExtremes[1], lExtremes[0], lExtremes[2]};
            double[] end = {lExtremes[4], lExtremes[3], lExtremes[5]};
            double[] current = {pTarget[0], pTarget[1]};
            double d = GeoCalc.DistanceFromLine(start, end, current);
            distance = Math.sqrt(d * d + (sourceDepthM + targetElevM) * (sourceDepthM + targetElevM));

        } else {

            distance = GeoCalc.Distance3DDegToM(pEvent, pTarget);
        }

        double Rh = distance / 1000; // in kilometers

        // Apply distance cutoff
        double Ru = max(rmin, Rh);

        // Site terms
        if (amplificationProxyValueSI < 760) {
            //S III and IV are for soft soil
            S = 1;
            H = 0;

        } else {
            // S I is rock site
            S = 0;
            H = 0;
        }

        if (amplificationProxyValueSI == -1) {
            //S II firm or steady soil 
            // 
            S = 0;
            H = 1;
        }

        double depthKm = sourceDepthM / 1000.0; //depth in km

        // pick the right coefficients according to the spectral period
        //Shallow events
        if (depthKm < 25) {

            if (period == 0.02) {
                cnt = 1;
            } else if (period == 0.075) {
                cnt = 2;
            } else if (period == 0.1) {
                cnt = 3;
            } else if (period == 0.15) {
                cnt = 4;
            } else if (period == 0.2) {
                cnt = 5;
            } else if (period == 0.24) {
                cnt = 6;
            } else if (period == 0.303) {
                cnt = 7;
            } else if (period == 0.34) {
                cnt = 8;
            } else if (period == 0.4) {
                cnt = 9;
            } else if (period == 0.44) {
                cnt = 10;
            } else if (period == 0.5) {
                cnt = 11;
            } else if (period == 0.6) {
                cnt = 12;

            } else if (period == 0.752) {
                cnt = 13;

            } else if (period == 0.9) {
                cnt = 14;

            } else if (period == 1.0) {
                cnt = 15;

            } else if (period == 1.25) {
                cnt = 16;

            } else if (period == 1.493) {
                cnt = 17;

            } else if (period == 2.0) {
                cnt = 18;

            } else if (period == 2.5) {
                cnt = 19;

            } else if (period == 3.03) {
                cnt = 20;

            } else if (period == 4.0) {
                cnt = 21;

            } else if (period == 5.0) {
                cnt = 22;

            }

            // Compute PSA
            double logpsasite = Cofs[cnt][2] + Cofs[cnt][3] * Mw + Cofs[cnt][4] * log(Math.sqrt(Ru * Ru + Cofs[cnt][5] * Cofs[cnt][5])) + Cofs[cnt][6] * S + Cofs[cnt][7] * H;

            double logpsasiteplus = logpsasite + Cofs[cnt][8];
            double logpsasiteminus = logpsasite - Cofs[cnt][8];

            Shaking PSA = new Shaking();
//            PSA divided by 100 to have in m/s**2
//            PSA.expectedSI = pow(10,logpsasite)/100;
//            PSA.percentile84 = pow(10,logpsasiteplus)/100;
//            PSA.percentile16 = pow(10,logpsasiteminus)/100;  

            PSA.expectedSI = pow(10,logpsasite);
            PSA.percentile84 = pow(10,logpsasiteplus);
            PSA.percentile16 = pow(10,logpsasiteminus);
            return PSA;
        } 
        //Depth >= 25 km
        else {

            if (period == 0.02) {
                cnt = 1;
            } else if (period == 0.04) {
                cnt = 2;
            } else if (period == 0.075) {
                cnt = 3;
            } else if (period == 0.1) {
                cnt = 4;

            } else if (period == 0.15) {
                cnt = 5;

            } else if (period == 0.2) {
                cnt = 6;

            } else if (period == 0.24) {
                cnt = 7;

            } else if (period == 0.303) {
                cnt = 8;

            } else if (period == 0.34) {
                cnt = 9;

            } else if (period == 0.4) {
                cnt = 10;

            } else if (period == 0.44) {
                cnt = 11;

            } else if (period == 0.5) {
                cnt = 12;

            } else if (period == 0.6) {
                cnt = 13;

            } else if (period == 0.752) {
                cnt = 14;

            } else if (period == 0.9) {
                cnt = 15;

            } else if (period == 1.0) {
                cnt = 16;

            } else if (period == 1.25) {
                cnt = 17;

            } else if (period == 1.493) {
                cnt = 18;

            } else if (period == 2.0) {
                cnt = 19;

            } else if (period == 2.5) {
                cnt = 20;

            } else if (period == 3.03) {
                cnt = 21;

            } else if (period == 4.0) {
                cnt = 22;

            } else if (period == 5.0) {
                cnt = 23;

            }

            // Compute PSA
            double logpsasite = CofsSub[cnt][2] + CofsSub[cnt][3] * Mw + CofsSub[cnt][4] * log(Math.sqrt(Ru * Ru + pow(5, 2))) + CofsSub[cnt][5] * S + CofsSub[cnt][6] * H;

            double logpsasiteplus = logpsasite + CofsSub[cnt][7];
            double logpsasiteminus = logpsasite - CofsSub[cnt][7];

            Shaking PSA = new Shaking();
            //PSA divided by 100 to have in m/s**2
//            PSA.expectedSI = pow(10,logpsasite)/100;
//            PSA.percentile84 = pow(10,logpsasiteplus)/100;
//            PSA.percentile16 = pow(10,logpsasiteminus)/100;

            PSA.expectedSI = pow(10,logpsasite);
            PSA.percentile84 = pow(10,logpsasiteplus);
            PSA.percentile16 = pow(10,logpsasiteminus);
            return PSA;
        }

    }

    @Override
    public Shaking getDRS(double magnitude, double sourceLat, double sourceLon,
            double sourceDepthM, double targetLat, double targetLon,
            double targetElevM, String amplificationType,
            double amplificationProxyValueSI, double period,
            EventParameters eventParameters,
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

        return PSA;
    }
}
