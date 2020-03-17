package org.reakteu.eewd.gmpe.impl;

/**
* Implementation of the ground-motion model by Zhao et al. (2006) for subduction intraslab events added by Carlo Cauzzi for SED-INETER Project;
* implementation based on the OpenQuake hazardlib (http://docs.openquake.org/oq-hazardlib/stable/_modules/openquake/hazardlib/gsim/zhao_2006.html#ZhaoEtAl2006SSlab)
 */

import org.reakteu.eewd.data.Shaking;
import org.reakteu.eewd.utils.GeoCalc;
import org.reakteu.eewd.gmpe.AttenuationPGV;
import org.reakteu.eewd.gmpe.AttenuationPGA;
import org.reakteu.eewd.gmpe.AttenuationPSA;
import org.reakteu.eewd.gmpe.AttenuationDRS;
import org.quakeml.xmlns.bedRt.x12.EventParameters;

import static java.lang.Math.*;

public class ZEA06ITS implements AttenuationPGA, AttenuationPSA, AttenuationDRS {
	
	
	// Coefficients of the GMPE
	// 	period, a, b, c, d, e, FR, CH, C1, C2, C3, C4, sigma, QS, WS, tauS, SI, SS, SSL, PS
    public static final double[][] Cofs = {
    		{0,1.101,-0.00564,0.0055,1.08,0.01412,0.251,0.293,1.111,1.344,1.355,1.42,0.604,0.1584,-0.0529,0.321,0,2.607,-0.528,0.1392},
    		{0.05,1.076,-0.00671,0.0075,1.06,0.01463,0.251,0.939,1.684,1.793,1.747,1.814,0.64,0.1932,-0.0841,0.378,0,2.764,-0.551,0.1636},
    		{0.1,1.118,-0.00787,0.009,1.083,0.01423,0.24,1.499,2.061,2.135,2.031,2.082,0.694,0.2057,-0.0877,0.42,0,2.156,-0.42,0.169},
    		{0.15,1.134,-0.00722,0.01,1.053,0.01509,0.251,1.462,1.916,2.168,2.052,2.113,0.702,0.1984,-0.0773,0.372,0,2.161,-0.431,0.1669},
    		{0.2,1.147,-0.00659,0.012,1.014,0.01462,0.26,1.28,1.669,2.085,2.001,2.03,0.692,0.1856,-0.0644,0.324,0,1.901,-0.372,0.1631},
    		{0.25,1.149,-0.0059,0.014,0.966,0.01459,0.269,1.121,1.468,1.942,1.941,1.937,0.682,0.1714,-0.0515,0.294,0,1.814,-0.36,0.1588},
    		{0.3,1.163,-0.0052,0.015,0.934,0.01458,0.259,0.852,1.172,1.683,1.808,1.77,0.67,0.1573,-0.0395,0.284,0,2.181,-0.45,0.1544},
    		{0.4,1.2,-0.00422,0.01,0.959,0.01257,0.248,0.365,0.655,1.127,1.482,1.397,0.659,0.1309,-0.0183,0.278,-0.041,2.432,-0.506,0.146},
    		{0.5,1.25,-0.00338,0.006,1.008,0.01114,0.247,-0.207,0.071,0.515,0.934,0.955,0.653,0.1078,-0.0008,0.272,-0.053,2.629,-0.554,0.1381},
    		{0.6,1.293,-0.00282,0.003,1.088,0.01019,0.233,-0.705,-0.429,-0.003,0.394,0.559,0.653,0.0878,0.0136,0.285,-0.103,2.702,-0.575,0.1307},
    		{0.7,1.336,-0.00258,0.0025,1.084,0.00979,0.22,-1.144,-0.866,-0.449,-0.111,0.188,0.652,0.0705,0.0254,0.29,-0.146,2.654,-0.572,0.1239},
    		{0.8,1.386,-0.00242,0.0022,1.088,0.00944,0.232,-1.609,-1.325,-0.928,-0.62,-0.246,0.647,0.0556,0.0352,0.299,-0.164,2.48,-0.54,0.1176},
    		{0.9,1.433,-0.00232,0.002,1.109,0.00972,0.22,-2.023,-1.732,-1.349,-1.066,-0.643,0.653,0.0426,0.0432,0.289,-0.206,2.332,-0.522,0.1116},
    		{1,1.479,-0.0022,0.002,1.115,0.01005,0.211,-2.451,-2.152,-1.776,-1.523,-1.084,0.657,0.0314,0.0498,0.286,-0.239,2.233,-0.509,0.106},
    		{1.25,1.551,-0.00207,0.002,1.083,0.01003,0.251,-3.243,-2.923,-2.542,-2.327,-1.936,0.66,0.0093,0.0612,0.277,-0.256,2.029,-0.469,0.0933},
    		{1.5,1.621,-0.00224,0.002,1.091,0.00928,0.248,-3.888,-3.548,-3.169,-2.979,-2.661,0.664,-0.0062,0.0674,0.282,-0.306,1.589,-0.379,0.0821},
    		{2,1.694,-0.00201,0.0025,1.055,0.00833,0.263,-4.783,-4.41,-4.039,-3.871,-3.64,0.669,-0.0235,0.0692,0.3,-0.321,0.966,-0.248,0.0628},
    		{2.5,1.748,-0.00187,0.0028,1.052,0.00776,0.262,-5.444,-5.049,-4.698,-4.496,-4.341,0.671,-0.0287,0.0622,0.292,-0.337,0.789,-0.221,0.0465},
    		{3,1.759,-0.00147,0.0032,1.025,0.00644,0.307,-5.839,-5.431,-5.089,-4.893,-4.758,0.667,-0.0261,0.0496,0.274,-0.331,1.037,-0.263,0.0322},
    		{4,1.826,-0.00195,0.004,1.044,0.0059,0.353,-6.598,-6.181,-5.882,-5.698,-5.588,0.647,-0.0065,0.015,0.281,-0.39,0.561,-0.169,0.0083},
    		{5,1.825,-0.00237,0.005,1.065,0.0051,0.248,-6.752,-6.347,-6.051,-5.873,-5.798,0.643,0.0246,-0.0268,0.296,-0.498,0.225,-0.12,-0.0117}
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
        // magnitude is the magnitude from the EW message
        // ampType is site term derived form VS30


        double Mw = magnitude;	// reasonable assumption
        double S = 0; // initialise site term


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
        
        double depth = sourceDepthM / 1000; 
        double Rh = distance / 1000; // in kilometers
        
        // Assume Rrup ~ Rh for magnitude < 5.8
        double Rrup = Rh;
        
        // Distance cutoff
        if (Rrup == 0) {
        	Rrup = 0.1;
        }
        
        
        // else estimate Rrup based on Cauzzi et al. (BEE2015)
        
        if (Mw >= 5.8){
            Rrup = Rh + 7.5 * Mw - 38 - 1.38 - 0.014 * exp(Mw); // 7.5 * Mw + 38 included to avoid negative distances at points with Rh = 0 ... 
        }

        
        // Site term
        if (amplificationProxyValueSI > 1100 ){
            S = Cofs[0][7];
        } else if (amplificationProxyValueSI > 600 ) {
            S = Cofs[0][8];
        } else if (amplificationProxyValueSI > 300 ) {
            S = Cofs[0][9];
        } else if (amplificationProxyValueSI > 200 ) {
            S = Cofs[0][10];
        } else {
            S = Cofs[0][11];
        }
        
        if (amplificationProxyValueSI == -1 ) {
        	S = log(-1);
        }
        
        // Some other period-independent parameters
        double P = Cofs[0][19];
        double M = 6.5;
        double hc = 15;
        double dterm = 0;
        
        if (depth > 125) {
        	depth = 125;
        }
        
        if (depth >= hc) {
        	dterm = Cofs[0][5] * (depth - hc);
        }
        
        
        
        // SOF term is not included as it is unknown in real-time
        
        // Compute ground-motion prediction in natural log first including site term and slab terms (no 17 and 18)
        double logpgasite = P * (Mw - M) + Cofs[0][13] * pow((Mw - M),2) + Cofs[0][14] + Cofs[0][1] * Mw + Cofs[0][2] * Rrup - log(Rrup + Cofs[0][3] * exp(Cofs[0][4] * Mw)) + dterm + S + Cofs[0][17] + Cofs[0][18] * log(Rrup);


        // Now compute plus/minus sigma bounds
        double sigma = sqrt(pow(Cofs[0][12],2) + pow(Cofs[0][15],2));
        double logpgasiteplus = logpgasite + sigma;
        double logpgasiteminus = logpgasite - sigma;

        // Now in m/s2
        Shaking PGA = new Shaking() ;
        PGA.expectedSI = exp(logpgasite) / 100;
        PGA.percentile84 = exp(logpgasiteplus) / 100;
        PGA.percentile16 = exp(logpgasiteminus) / 1000;

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

        
        int cnt = 0;
        
        // pick the right coefficients according to the spectral period
        if (period == 0.05) {
            cnt = 1;
        } else if (period == 0.10) {
            cnt = 2;
        } else if (period == 0.15) {
            cnt = 3;
        } else if (period == 0.20) {
            cnt = 4;
        } else if (period == 0.25) {
            cnt = 5;
        } else if (period == 0.30) {
            cnt = 6;
        } else if (period == 0.40) {
            cnt = 7;
        } else if (period == 0.50) {
            cnt = 8;
        } else if (period == 0.60) {
            cnt = 9;
        } else if (period == 0.70) {
            cnt = 10;
        } else if (period == 0.80) {
            cnt = 11;
        } else if (period == 0.90) {
            cnt = 12;
        } else if (period == 1.00) {
            cnt = 13;
        } else if (period == 1.25) {
            cnt = 14;
        } else if (period == 1.50) {
            cnt = 15;
        } else if (period == 2.00) {
            cnt = 16;
        } else if (period == 2.50) {
            cnt = 17;
        } else if (period == 3.00) {
            cnt = 18;
        } else if (period == 4.00) {
            cnt = 19;
        } else if (period == 5.00) {
            cnt = 20;
            }
        
        double Mw = magnitude;	// reasonable assumption
        double S = 0; // initialise site term


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

        double depth = sourceDepthM / 1000;
        double Rh = distance / 1000; // in kilometers
        
        // Assume Rrup ~ Rh for magnitude < 5.8
        double Rrup = Rh;
        
        // Distance cutoff
        if (Rrup == 0) {
        	Rrup = 0.1;
        }
        
        
        // else estimate Rrup based on Cauzzi et al. (BEE2015)
        
        if (Mw >= 5.8){
            Rrup = Rh + 7.5 * Mw - 38 - 1.38 - 0.014 * exp(Mw); // 7.5 * Mw + 38 included to avoid negative distances at points with Rh = 0 ... 
        }

        
        // Site term
        if (amplificationProxyValueSI > 1100 ){
            S = Cofs[cnt][7];
        } else if (amplificationProxyValueSI > 600 ) {
            S = Cofs[cnt][8];
        } else if (amplificationProxyValueSI > 300 ) {
            S = Cofs[cnt][9];
        } else if (amplificationProxyValueSI > 200 ) {
            S = Cofs[cnt][10];
        } else {
            S = Cofs[cnt][11];
        }
        
        if (amplificationProxyValueSI == -1 ) {
        	S = log(-1);
        }
        
        // Some other period-independent parameters
        double P = Cofs[cnt][19];
        double M = 6.5;
        double hc = 15;
        double dterm = 0;
        
        if (depth > 125) {
        	depth = 125;
        }
        
        if (depth >= hc) {
        	dterm = Cofs[cnt][5] * (depth - hc);
        }
        
        
        
        // SOF term is not included as it is unknown in real-time

        // PSA including site term and slab terms (no 17 and 18)
        double logpsasite = P * (Mw - M) + Cofs[cnt][13] * pow((Mw - M),2) + Cofs[cnt][14] + Cofs[cnt][1] * Mw + Cofs[cnt][2] * Rrup - log(Rrup + Cofs[cnt][3] * exp(Cofs[cnt][4] * Mw)) + dterm + S + Cofs[cnt][17] + Cofs[cnt][18] * log(Rrup);

        
        // Now compute plus/minus sigma bounds
        double sigma = sqrt(pow(Cofs[cnt][12],2) + pow(Cofs[cnt][15],2));
        double logpsasiteplus = logpsasite + sigma;
        double logpsasiteminus = logpsasite - sigma;

        // Now in m/s2
        Shaking PSA = new Shaking() ;
        PSA.expectedSI = exp(logpsasite) / 100;
        PSA.percentile84 = exp(logpsasiteplus) / 100;
        PSA.percentile16 = exp(logpsasiteminus) / 100;

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
