package org.reaktEU.ewViewer.gmpe;

public interface AttenuationPSA {
	
	public Shaking getPsa(double magnitude, double sourceLat, double sourceLon, double sourceDepthM, double targetLat, double targetLon, double targetElevM, String amplificationType, double amplificationProxyValueSI, double period)

}
