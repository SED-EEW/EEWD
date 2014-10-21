package org.reaktEU.ewViewer.gmpe.interfaces;

import org.quakeml.xmlns.bedRt.x12.EventParameters;
import org.reaktEU.ewViewer.data.*;

public interface AttenuationPGA {

	public Shaking getPGA(double magnitude, double sourceLat, double sourceLon,
	                      double sourceDepthM, double targetLat, double targetLon,
	                      double targetElevM, String amplificationType,
	                      double amplificationProxyValueSI,
	                      EventParameters eventParameters);

}
