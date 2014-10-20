package org.reaktEU.ewViewer.gmpe.interfaces;

import org.reaktEU.ewViewer.data.*;

public interface AttenuationInt {
	
	public Shaking getInt(double magnitude, double sourceLat, double sourceLon, double sourceDepthM, double targetLat, double targetLon, double targetElevM, String amplificationType, double amplificationProxyValueSI);

}
