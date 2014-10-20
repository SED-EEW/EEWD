package org.reaktEU.ewViewer.gmpe.interfaces;

import org.reaktEU.ewViewer.data.*;

public interface AttenuationPSA {
	
	public Shaking getPsa(double magnitude, double sourceLat, double sourceLon, double sourceDepthM, double targetLat, double targetLon, double targetElevM, String amplificationType, double amplificationProxyValueSI, double period);

}
