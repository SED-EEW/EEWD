/*
 * Copyright (C) 2014-2015 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reakteu.eewd.gmice;

import org.reakteu.eewd.data.Shaking;

public interface IntensityFromAcceleration {

    public Shaking getIntensityFromAcceleration(Shaking accel);

}
