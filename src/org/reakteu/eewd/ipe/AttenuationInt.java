package org.reakteu.eewd.ipe;

import org.reakteu.eewd.data.Shaking;
import org.quakeml.xmlns.bedRt.x12.EventParameters;

public interface AttenuationInt {

    public Shaking getInt(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI,
                          EventParameters eventParameters);

}
