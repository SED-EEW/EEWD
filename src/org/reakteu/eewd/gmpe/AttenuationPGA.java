/*
 * Copyright (C) 2014-2015 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reakteu.eewd.gmpe;

import org.reakteu.eewd.data.Shaking;
import org.quakeml.xmlns.bedRt.x12.EventParameters;
import org.reakteu.eewd.data.EventData;


public interface AttenuationPGA {

    public Shaking getPGA(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI,
                          EventParameters eventParameters,
                          Float ruptureLength,
                          Float ruptureStrike);

}
