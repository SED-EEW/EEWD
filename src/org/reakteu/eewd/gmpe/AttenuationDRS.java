package org.reakteu.eewd.gmpe;

import org.reakteu.eewd.data.Shaking;
import org.quakeml.xmlns.bedRt.x12.EventParameters;

public interface AttenuationDRS {

    public Shaking getDRS(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI, double period,
                          EventParameters eventParameters);

}
