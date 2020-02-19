package org.reakteu.eewd.gmpe.impl;

/**
* Implementation of the ground-motion model by Young et al. (1997) for subduction interface events added by Carlo Cauzzi for SED-INETER Project;
* implementation based on the OpenQuake hazardlib (http://docs.openquake.org/oq-hazardlib/stable/_modules/openquake/hazardlib/gsim/youngs_1997.html#YoungsEtAl1997SInter)
 */

import org.reakteu.eewd.data.Shaking;
import org.reakteu.eewd.utils.GeoCalc;
import org.reakteu.eewd.gmpe.AttenuationPGV;
import org.reakteu.eewd.gmpe.AttenuationPGA;
import org.reakteu.eewd.gmpe.AttenuationPSA;
import org.reakteu.eewd.gmpe.AttenuationDRS;
import org.quakeml.xmlns.bedRt.x12.EventParameters;

import static java.lang.Math.*;

public class YOU97IFC implements AttenuationPGA, AttenuationPSA, AttenuationDRS {

	// Set of coefficients for soil
	// IMT   C1        C2         C3       C4       C5
    public static final double[][] Cofs_soil = {
    		{0,0,-0.0019,-2.329,1.45,-0.1},
    		{0.075,2.4,-0.0019,-2.697,1.45,-0.1},
    		{0.1,2.516,-0.0019,-2.697,1.45,-0.1},
    		{0.2,1.549,-0.002,-2.464,1.45,-0.1},
    		{0.3,0.793,-0.002,-2.327,1.45,-0.1},
    		{0.4,0.144,-0.0035,-2.23,1.45,-0.1},
    		{0.5,-0.438,-0.0048,-2.14,1.45,-0.1},
    		{0.75,-1.704,-0.0066,-1.952,1.45,-0.1},
    		{1,-2.87,-0.0114,-1.785,1.45,-0.1},
    		{1.5,-5.101,-0.0164,-1.47,1.5,-0.1},
    		{2,-6.433,-0.0221,-1.29,1.55,-0.1},
    		{3,-6.672,-0.0235,-1.347,1.65,-0.1},
    		{4,-7.618,-0.0235,-1.272,1.65,-0.1}
    		};
    
    
    // Set of coefficients for rock
    
    public static final double[][] Cofs_rock = {
    		{0,0,0,-2.552,1.45,-0.1},
    		{0.075,1.275,0,-2.707,1.45,-0.1},
    		{0.1,1.188,-0.0011,-2.655,1.45,-0.1},
    		{0.2,0.722,-0.0027,-2.528,1.45,-0.1},
    		{0.3,0.246,-0.0036,-2.454,1.45,-0.1},
    		{0.4,-0.115,-0.0043,-2.401,1.45,-0.1},
    		{0.5,-0.4,-0.0048,-2.36,1.45,-0.1},
    		{0.75,-1.149,-0.0057,-2.286,1.45,-0.1},
    		{1,-1.736,-0.0064,-2.234,1.45,-0.1},
    		{1.5,-2.634,-0.0073,-2.16,1.5,-0.1},
    		{2,-3.328,-0.008,-2.107,1.55,-0.1},
    		{3,-4.511,-0.0089,-2.033,1.65,-0.1},
    		{4,-4.511,-0.0089,-2.033,1.65,-0.1}
    		};
    
    // Period-independent coefficients
    
    double A1_rock = 0.2418;
    double A2_rock = 1.414;
    double A3_rock = 10;
    double A4_rock = 1.7818;
    double A5_rock = 0.554;
    double A6_rock = 0.00607;
    double A7_rock = 0.3846;
    double A1_soil = -0.6687;
    double A2_soil = 1.438;
    double A3_soil = 10;
    double A4_soil = 1.097;
    double A5_soil = 0.617;
    double A6_soil = 0.00648;
    double A7_soil = 0.3643;    
    
    public static final double PI2_4 = 4 * PI * PI;
    

    @Override
    public Shaking getPGA(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI,
                          EventParameters eventParameters) {

    	// Returns median PGA, 16th-percentile PGA, 84th percentile PGA in m/s2
        // magnitude is the magnitude from the EW message
        // ampType is site term derived form VS30

        double Mw = magnitude;	// reasonable assumption


        // Compute hypocentral distance 
        double[] pEvent = GeoCalc.Geo2Cart(sourceLat, sourceLon, -sourceDepthM);
        double[] pTarget = GeoCalc.Geo2Cart(targetLat, targetLon, targetElevM);
        double distance = GeoCalc.Distance3D(pEvent, pTarget);
        double depth = sourceDepthM / 1000; 
        double Rh = distance / 1000; // in kilometers
      
        
        // Assume Rrup ~ Rh for magnitude < 5.8
        double Rrup = Rh;
              
        
        // else estimate Rrup based on Cauzzi et al. (BEE2015)
        
        if (Mw >= 5.8){
            Rrup = Rh + 7.5 * Mw - 38 - 1.38 - 0.014 * exp(Mw); // 7.5 * Mw + 38 included to avoid negative distances at points with Rh = 0 ... 
        }

                
        // Compute ground-motion prediction in natural log first including site term and slab terms (no 17 and 18)
        double logpgasite = A1_rock + A2_rock * Mw + Cofs_rock[0][1] + Cofs_rock[0][2] * pow((A3_rock - Mw),3) + Cofs_rock[0][3] * log(Rrup + A4_rock * exp(A5_rock * Mw)) + A6_rock * depth;
        
        if (amplificationProxyValueSI < 760) {
        	
        	logpgasite = A1_soil + A2_soil * Mw + Cofs_soil[0][1] + Cofs_soil[0][2] * pow((A3_soil - Mw),3) + Cofs_soil[0][3] * log(Rrup + A4_soil * exp(A5_soil * Mw)) + A6_soil * depth;
        }

        if (amplificationProxyValueSI == -1 ) {
        	logpgasite = log(-1);
        }

        // Now compute plus/minus sigma bounds
        double sigma = Cofs_rock[0][4] + Cofs_rock[0][5] * Mw;
        if (Mw > 8) {
        	sigma = Cofs_rock[0][4] + Cofs_rock[0][5] * 8;
        }
        double logpgasiteplus = logpgasite + sigma;
        double logpgasiteminus = logpgasite - sigma;

        // Now in m/s2
        Shaking PGA = new Shaking() ;
        PGA.expectedSI = exp(logpgasite) * 9.806;
        PGA.percentile84 = exp(logpgasiteplus) * 9.806;
        PGA.percentile16 = exp(logpgasiteminus) * 9.806;

        // Now should return Shaking ...
        return PGA;
    }

    
    @Override
    public Shaking getPSA(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI, double period,
                          EventParameters eventParameters) {

        // Returns median PSA, 16th-percentile PSA, 84th percentile PSA in m/s2 for a given spectral period T
        // Mag is the magnitude from the EW message
        // ampType is S derived form VS30

        
        int cnt = 0;
        double scale = 1;
        
        // pick the right coefficients according to the spectral period
        if (period == 0.075) {
            cnt = 1;
        } else if (period == 0.10) {
            cnt = 2;
        } else if (period == 0.20) {
            cnt = 3;
        } else if (period == 0.30) {
            cnt = 4;
        } else if (period == 0.40) {
            cnt = 5;
        } else if (period == 0.50) {
            cnt = 6;
        } else if (period == 0.75) {
            cnt = 7;
        } else if (period == 1.00) {
            cnt = 8;
        } else if (period == 1.50) {
            cnt = 9;
        } else if (period == 2.00) {
            cnt = 10;
        } else if (period == 3.00) {
            cnt = 11;
        } else if (period == 4.00) {
            cnt = 12;
            scale = 1 / 0.399;
        }
        
     // Mag is the magnitude from the EW message
        double Mw = magnitude;	// reasonable assumption


        // Compute hypocentral distance 
        double[] pEvent = GeoCalc.Geo2Cart(sourceLat, sourceLon, -sourceDepthM);
        double[] pTarget = GeoCalc.Geo2Cart(targetLat, targetLon, targetElevM);
        double distance = GeoCalc.Distance3D(pEvent, pTarget);
        double depth = sourceDepthM / 1000; 
        double Rh = distance / 1000; // in kilometers
        
      
        
        // Assume Rrup ~ Rh for magnitude < 5.8
        double Rrup = Rh;
              
        
        // else estimate Rrup based on Cauzzi et al. (BEE2015)
        
        if (Mw >= 5.8){
            Rrup = Rh + 7.5 * Mw - 38 - 1.38 - 0.014 * exp(Mw); // 7.5 * Mw + 38 included to avoid negative distances at points with Rh = 0 ... 
        }

                
        // Compute ground-motion prediction in natural log first including site term and slab terms (no 17 and 18)
        double logpsasite = scale * (A1_rock + A2_rock * Mw + Cofs_rock[cnt][1] + Cofs_rock[cnt][2] * pow((A3_rock - Mw),3) + Cofs_rock[cnt][3] * log(Rrup + A4_rock * exp(A5_rock * Mw)) + A6_rock * depth);
        
        if (amplificationProxyValueSI < 760) {
        	
        	logpsasite = (A1_soil + A2_soil * Mw + Cofs_soil[cnt][1] + Cofs_soil[cnt][2] * pow((A3_soil - Mw),3) + Cofs_soil[cnt][3] * log(Rrup + A4_soil * exp(A5_soil * Mw)) + A6_soil * depth);
        }

        if (amplificationProxyValueSI == -1 ) {
        	logpsasite = log(-1);
        }

        // Now compute plus/minus sigma bounds
        double sigma = Cofs_rock[cnt][4] + Cofs_rock[cnt][5] * Mw;
        if (Mw > 8) {
        	sigma = Cofs_rock[cnt][4] + Cofs_rock[cnt][5] * 8;
        }
        double logpsasiteplus = logpsasite + sigma;
        double logpsasiteminus = logpsasite - sigma;

        // Now in m/s2
        Shaking PSA = new Shaking() ;
        PSA.expectedSI = exp(logpsasite) * 9.806;
        PSA.percentile84 = exp(logpsasiteplus) * 9.806;
        PSA.percentile16 = exp(logpsasiteminus) * 9.806;

        // Now should return Shaking ...
        return PSA;
    }

    @Override
    public Shaking getDRS(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI, double period,
                          EventParameters eventML) {

        Shaking PSA = getPSA(magnitude, sourceLat, sourceLon, sourceDepthM,
                             targetLat, targetLon, targetElevM,
                             amplificationType, amplificationProxyValueSI,
                             period, null);

        double accelerationToDisplacement = period * period / PI2_4;
        PSA.expectedSI *= accelerationToDisplacement;
        PSA.percentile16 *= accelerationToDisplacement;
        PSA.percentile84 *= accelerationToDisplacement;

        // Now in m/s
        return PSA;
    }
}
