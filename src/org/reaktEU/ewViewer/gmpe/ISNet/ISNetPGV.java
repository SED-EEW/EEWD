package org.reaktEU.ewViewer.gmpe.ISNet;

/*
 * PGV GMPE used by PRESTo 0.2.8 for the ISNet network (Southern Italy):
 * Akkar and Bommer (BSSA,2007) for M >= 4, Emolo et al. (JGE,2010) for smaller magnitudes.
 */

// import useful packages

import org.gavaghan.geodesy.*; // used to compute the hypocentral distance;
import org.quakeml.xmlns.bedRt.x12.EventParameters;
import org.reaktEU.ewViewer.gmpe.interfaces.*;
import org.reaktEU.ewViewer.data.*;

import static java.lang.Math.*;

public class ISNetPGV implements AttenuationPGV
{
	// Returns median PGV, 16th-percentile PGV, 84th percentile PGV
	// Mag is the magnitude from the EW message

	public Shaking getPGV(double Mag, double sourceLat, double sourceLon, double sourceDepthM, double targetLat, double targetLon, double ElevM, String ampType, double deltaIvalue, EventParameters ParamfromQuakeML)
	{
	    // Compute hypocentral distance
	 
	    GeodeticCalculator geoCalc = new GeodeticCalculator();
	    Ellipsoid reference = Ellipsoid.WGS84;
	    GlobalPosition eq = new GlobalPosition(sourceLat, sourceLon, -sourceDepthM); // earthquake focus
	    GlobalPosition point = new GlobalPosition(targetLat, targetLon, ElevM); // target point
	    double distance = geoCalc.calculateGeodeticCurve(reference, eq, point).getEllipsoidalDistance(); // Distance between Point A and Point B
        double R = distance / 1000; // in kilometers

	    // Compute log10(PGV_cm)

		double logpgv, logpgvsigma;
		if (Mag >= 4)
        {
			// Akkar and Bommer (BSSA,2007)
            logpgv = -1.36 + 1.063 * Mag -0.079 * pow(Mag,2) + (-2.948 + 0.306 * Mag) * log10( sqrt( pow(R,2) + pow(5.547,2) ) );
			logpgvsigma = sqrt( pow(0.85 -0.096 * Mag,2) + pow(0.313 -0.040 * Mag,2) );
		}
		else
        {
			// Emolo et al. (JGE,2010)
			logpgv = -3.943 + 0.540 * Mag -1.458 * log10(R) + 2;
			logpgvsigma = 0.359;
		}

        // Return shaking in cm/s
		
		double pgv		=	pow(10, logpgv					);
        double pgvplus	=	pow(10, logpgv + logpgvsigma	);
        double pgvminus	=	pow(10, logpgv - logpgvsigma	);

		Shaking PGV = new Shaking();
		PGV.setShakingExpected(pgv);
		PGV.setShaking84percentile(pgvplus);
		PGV.setShaking16percentile(pgvminus);

		return PGV;
	}
}
