package org.reakteu.eewd.gmpe;

import org.reakteu.eewd.data.Shaking;
import org.quakeml.xmlns.bedRt.x12.EventParameters;

public interface AttenuationPGV {

    public Shaking getPGV(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI,
                          EventParameters eventParameters);

}
