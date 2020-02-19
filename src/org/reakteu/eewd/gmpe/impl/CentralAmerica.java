/**
 * 
 */
package org.reakteu.eewd.gmpe.impl;

import org.reakteu.eewd.data.Shaking;
import org.reakteu.eewd.utils.GeoCalc;
import org.reakteu.eewd.gmpe.AttenuationPGV;
import org.reakteu.eewd.gmpe.AttenuationPGA;
import org.reakteu.eewd.gmpe.AttenuationPSA;
import org.reakteu.eewd.gmpe.AttenuationDRS;
import org.quakeml.xmlns.bedRt.x12.EventParameters;

import static java.lang.Math.*;

/**
 * GMPEs for Central America: different GMPEs are chosen based on the focal depth.
 * Added by Carlo Cauzzi for SED-INETER Project. 
 * Based on https://hazardwiki.openquake.org/resisii2010_intro. 
 *
 */
public class CentralAmerica implements AttenuationPGA, AttenuationPSA, AttenuationDRS {

	public static final double PI2_4 = 4 * Math.PI * Math.PI;

	@Override
	public Shaking getPGA(double magnitude, double sourceLat, double sourceLon,
			double sourceDepthM, double targetLat, double targetLon,
			double targetElevM, String amplificationType,
			double amplificationProxyValueSI,
			EventParameters eventParameters) {

		//compute hypodepth in km
		double depth = sourceDepthM / 1000; //depth in km


		// Initialise PSA and PGA;
		Shaking PGA = new Shaking();


		//Active shallow crustal seismicity: we take the average of Cli94 and ZEA06ASC
		Cli94 cli94 = new Cli94();
		Shaking PGA_Cli94 = cli94.getPGA(magnitude, sourceLat, sourceLon, sourceDepthM,
				targetLat, targetLon, targetElevM,
				amplificationType, amplificationProxyValueSI, null);

		ZEA06ASC zea06asc = new ZEA06ASC();
		Shaking PGA_zea06asc = zea06asc.getPGA(magnitude, sourceLat, sourceLon, sourceDepthM,
				targetLat, targetLon, targetElevM,
				amplificationType, amplificationProxyValueSI, null);

		PGA.expectedSI = 0.5 * (PGA_Cli94.expectedSI + PGA_zea06asc.expectedSI);
		PGA.percentile16 = 0.5 * (PGA_Cli94.percentile16 + PGA_zea06asc.percentile16);
		PGA.percentile84 = 0.5 * (PGA_Cli94.percentile84 + PGA_zea06asc.percentile84);


		// If depth is between 25 and 60 km, the above PSA are substituted by YOU97IFC model
		if (depth >= 25 && depth <= 60 ) {
			YOU97IFC you97ifc = new YOU97IFC();
			PGA = you97ifc.getPGA(magnitude, sourceLat, sourceLon, sourceDepthM,
					targetLat, targetLon, targetElevM,
					amplificationType, amplificationProxyValueSI, null);

			//        double accelerationToDisplacement = period * period / PI2_4;
			//        PSA.expectedSI *= accelerationToDisplacement;
			//        PSA.percentile16 *= accelerationToDisplacement;
			//        PSA.percentile84 *= accelerationToDisplacement;

			// Now in m/s
		}

		// If depth is larger than 60, the above PSA are substituted by the average of ZEA06ITS & YOU97ITS
		if (depth > 60) {

			YOU97ITS you97its = new YOU97ITS();
			Shaking PGA_you97its = you97its.getPGA(magnitude, sourceLat, sourceLon, sourceDepthM,
					targetLat, targetLon, targetElevM,
					amplificationType, amplificationProxyValueSI, null);

			ZEA06ITS zea06its = new ZEA06ITS();
			Shaking PGA_zea06its = zea06its.getPGA(magnitude, sourceLat, sourceLon, sourceDepthM,
					targetLat, targetLon, targetElevM,
					amplificationType, amplificationProxyValueSI, null);

			PGA.expectedSI = 0.5 * (PGA_you97its.expectedSI + PGA_zea06its.expectedSI);
			PGA.percentile16 = 0.5 * (PGA_you97its.percentile16 + PGA_zea06its.percentile16);
			PGA.percentile84 = 0.5 * (PGA_you97its.percentile84 + PGA_zea06its.percentile84);


			//        double accelerationToDisplacement = period * period / PI2_4;
			//        PSA.expectedSI *= accelerationToDisplacement;
			//        PSA.percentile16 *= accelerationToDisplacement;
			//        PSA.percentile84 *= accelerationToDisplacement;

			// Now in m/s
		}
		return PGA;
	}
	@Override
	public Shaking getPSA(double magnitude, double sourceLat, double sourceLon,
			double sourceDepthM, double targetLat, double targetLon,
			double targetElevM, String amplificationType,
			double amplificationProxyValueSI, double period,
			EventParameters eventML) {

		//compute hypodepth in km
		double depth = sourceDepthM / 1000; //depth in km


		// Initialise PSA and PGA;
		Shaking PSA = new Shaking();


		//Active shallow crustal seismicity: we take the average of Cli94 and ZEA06ASC
		Cli94 cli94 = new Cli94();
		Shaking PSA_Cli94 = cli94.getPSA(magnitude, sourceLat, sourceLon, sourceDepthM,
				targetLat, targetLon, targetElevM,
				amplificationType, amplificationProxyValueSI,
				period, eventML);

		ZEA06ASC zea06asc = new ZEA06ASC();
		Shaking PSA_zea06asc = zea06asc.getPSA(magnitude, sourceLat, sourceLon, sourceDepthM,
				targetLat, targetLon, targetElevM,
				amplificationType, amplificationProxyValueSI,
				period, eventML);

		PSA.expectedSI = 0.5 * (PSA_Cli94.expectedSI + PSA_zea06asc.expectedSI);
		PSA.percentile16 = 0.5 * (PSA_Cli94.percentile16 + PSA_zea06asc.percentile16);
		PSA.percentile84 = 0.5 * (PSA_Cli94.percentile84 + PSA_zea06asc.percentile84);


		// If depth is between 25 and 60 km, the above PSA are substituted by YOU97IFC model
		if (depth >= 25 && depth <= 60 ) {
			YOU97IFC you97ifc = new YOU97IFC();
			PSA = you97ifc.getPSA(magnitude, sourceLat, sourceLon, sourceDepthM,
					targetLat, targetLon, targetElevM,
					amplificationType, amplificationProxyValueSI,
					period, eventML);

			//        double accelerationToDisplacement = period * period / PI2_4;
			//        PSA.expectedSI *= accelerationToDisplacement;
			//        PSA.percentile16 *= accelerationToDisplacement;
			//        PSA.percentile84 *= accelerationToDisplacement;

			// Now in m/s
		}

		// If depth is larger than 60, the above PSA are substituted by the average of ZEA06ITS & YOU97ITS
		if (depth > 60) {

			YOU97ITS you97its = new YOU97ITS();
			Shaking PSA_you97its = you97its.getPSA(magnitude, sourceLat, sourceLon, sourceDepthM,
					targetLat, targetLon, targetElevM,
					amplificationType, amplificationProxyValueSI,
					period, eventML);

			ZEA06ITS zea06its = new ZEA06ITS();
			Shaking PSA_zea06its = zea06its.getPSA(magnitude, sourceLat, sourceLon, sourceDepthM,
					targetLat, targetLon, targetElevM,
					amplificationType, amplificationProxyValueSI,
					period, eventML);

			PSA.expectedSI = 0.5 * (PSA_you97its.expectedSI + PSA_zea06its.expectedSI);
			PSA.percentile16 = 0.5 * (PSA_you97its.percentile16 + PSA_zea06its.percentile16);
			PSA.percentile84 = 0.5 * (PSA_you97its.percentile84 + PSA_zea06its.percentile84);


			//        double accelerationToDisplacement = period * period / PI2_4;
			//        PSA.expectedSI *= accelerationToDisplacement;
			//        PSA.percentile16 *= accelerationToDisplacement;
			//        PSA.percentile84 *= accelerationToDisplacement;

			// Now in m/s
		}
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
