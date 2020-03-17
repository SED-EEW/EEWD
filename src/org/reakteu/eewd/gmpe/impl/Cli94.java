package org.reakteu.eewd.gmpe.impl;

/**
* Implementation of the ground-motion model by Climent et al. (1994) added by Carlo Cauzzi for SED-INETER Project;
* implementation based on the OpenQuake hazardlib (http://docs.openquake.org/oq-hazardlib/stable/_modules/openquake/hazardlib/gsim/climent_1994.html#ClimentEtAl1994)
 */

import org.reakteu.eewd.data.Shaking;
import org.reakteu.eewd.utils.GeoCalc;
import org.reakteu.eewd.gmpe.AttenuationPGV;
import org.reakteu.eewd.gmpe.AttenuationPGA;
import org.reakteu.eewd.gmpe.AttenuationPSA;
import org.reakteu.eewd.gmpe.AttenuationDRS;
import org.quakeml.xmlns.bedRt.x12.EventParameters;

import static java.lang.Math.*;

public class Cli94 implements AttenuationPGA, AttenuationPSA, AttenuationDRS {
    
	// Coefficients of the GMPE
	//       period  c1        c2       c3        c4         c5       SigmaB  r_SA     r_std

    public static final double[][] Cofs = {
    		{0.000,  -1.6870,  0.5530,  -0.5370,  -0.00302,  0.3270,  0.750,  1.1000,  1.0200},
            {0.025,  -7.2140,  0.5530,  -0.5370,  -0.00302,  0.3270,  0.750,  1.1000,  1.0200},
            {0.050,  -5.4870,  0.4470,  -0.5500,  -0.00246,  0.3090,  0.780,  1.1000,  1.0200},
            {0.100,  -4.7260,  0.4830,  -0.5810,  -0.00199,  0.3810,  0.800,  1.2020,  1.0200},
            {0.200,  -4.8760,  0.6420,  -0.6420,  -0.00156,  0.4700,  0.820,  1.2040,  1.0200},
            {0.500,  -5.8620,  0.9170,  -0.7260,  -0.00107,  0.5660,  0.820,  1.2100,  1.0200},
            {1.000,  -6.7440,  1.0810,  -0.7560,  -0.00077,  0.5880,  0.820,  1.2200,  1.0200},
            {2.000,  -7.3480,  1.1280,  -0.7280,  -0.00053,  0.5360,  0.790,  1.2400,  1.0200},
            {4.000,  -7.4410,  1.0070,  -0.6010,  -0.00040,  0.4960,  0.730,  1.2800,  1.0200}
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

        // Returns median PGA, 16th-percentile PGA, 84th percentile PGA in m/s2
        // Mag is the magnitude from the EW message
        // ampType is S derived form VS30

        double rmin = 6.056877878; // cut-off distance
        double Mw = magnitude;	// reasonable assumption
        double S = 1; // initialise site term


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

        // end of distance computation

       
        
        // Apply distance cutoff
        double Ru = max(rmin, Rh);
        
        
        // Site term
        if (amplificationProxyValueSI < 760 ){
            S = 1;
        } else {
        	S = 0;
        }
        
        if (amplificationProxyValueSI == -1 ) {
        	S = log(-1);
        } 

        // Compute ground-motion prediction in natural log first including site term
        double logpgasite = Cofs[0][1] + Cofs[0][2] * Mw + Cofs[0][3] * log(Ru) + Cofs[0][4] * Ru + Cofs[0][5] * S;


        // Now compute plus/minus sigma bounds
        double sigma = Cofs[0][6] / Cofs[0][8];
        double logpgasiteplus = logpgasite + sigma;
        double logpgasiteminus = logpgasite - sigma;

        // Now in m/s2, including correction from max to gm
        Shaking PGA = new Shaking() ;
        PGA.expectedSI = exp(logpgasite) / Cofs[0][7];
        PGA.percentile84 = exp(logpgasiteplus) / Cofs[0][7];
        PGA.percentile16 = exp(logpgasiteminus) / Cofs[0][7];

        // Now should return Shaking ...
        return PGA;
    }

    
    @Override
    public Shaking getPSA(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI, double period,
                          EventParameters eventParameters,
                          Float ruptureLength,
                          Float ruptureStrike) {

    	// Returns median PGA, 16th-percentile PGA, 84th percentile PGA in m/s2
        // magnitude is the magnitude from the EW message
        // ampType is site term derived form VS30

        double rmin = 6.056877878; // cut-off distance
        double Mw = magnitude;	// reasonable assumption
        double S = 1; // initialise site term

        int cnt = 0; // init
        
        double amp = 1; //init

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

        // end of distance computation
       
        
        // Apply distance cutoff
        double Ru = max(rmin, Rh);
        
        
     // Site term
        if (amplificationProxyValueSI < 760 ){
            S = 1;
        } else {
        	S = 0;
        }
        
        if (amplificationProxyValueSI == -1 ) {
        	S = log(-1);
        }
        
        
        // pick the right coefficients according to the spectral period
        if (period == 0.025) {
            cnt = 1;
 
        } else if (period == 0.05) {
            cnt = 2;
 

        } else if (period == 0.1) {
            cnt = 3;
            

        } else if (period == 0.2) {
            cnt = 4;
 
        } else if (period == 0.5) {
            cnt = 5;
 

        } else if (period == 1) {
            cnt = 6;
 
            
        } else if (period == 2) {
            cnt = 7;

            
        } else if (period == 4) {
            cnt = 8;
            
        }
        
        // Original predictions are in PSV
        double logpsvsite = Cofs[cnt][1] + Cofs[cnt][2] * Mw + Cofs[cnt][3] * log(Ru) + Cofs[cnt][4] * Ru + Cofs[cnt][5] * S;

        
        // Now compute plus/minus sigma bounds
        double sigma = Cofs[cnt][6] / Cofs[cnt][8];
        double logpsvsiteplus = logpsvsite + sigma;
        double logpsvsiteminus = logpsvsite - sigma;

        // Now in m/s2, including correction from max to gm e pseudo-relationship
        Shaking PSA = new Shaking() ;
        PSA.expectedSI = (PI_2 / period) * exp(logpsvsite) / Cofs[cnt][7];
        PSA.percentile84 = (PI_2 / period) * exp(logpsvsiteplus) / Cofs[cnt][7];
        PSA.percentile16 = (PI_2 / period) * exp(logpsvsiteminus) / Cofs[cnt][7];

        // Now should return Shaking ...
        return PSA;
    }

    @Override
    public Shaking getDRS(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI, double period,
                          EventParameters eventML,
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
