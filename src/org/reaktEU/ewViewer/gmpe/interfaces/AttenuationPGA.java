package org.reaktEU.ewViewer.gmpe;

import org.reaktEU.ewViewer.data;

public interface AttenuationPGA {
	
	public Shaking getPga(double magnitude, double sourceLat, double sourceLon, double targetLat, double targetLon, double depthM, String amplificationType, String amplificationValueSI)

}
