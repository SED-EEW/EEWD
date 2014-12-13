/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.reaktEU.ewViewer.utils;

import java.awt.Color;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Stephan Herrnkind <herrnkind@gempa.de>
 */
public class Gradient extends TreeMap<Double, Color> {

    public int colorAt(double position, boolean discrete) {
        Map.Entry<Double, Color> last = null;
        for (Map.Entry<Double, Color> entry : entrySet()) {
            if (entry.getKey() == position) {
                return entry.getValue().getRGB();
            } else if (entry.getKey() > position) {
                if (last != null) {
                    if (discrete) {
                        return last.getValue().getRGB();
                    } else {
                        double v1 = last.getKey();
                        double v2 = entry.getKey();
                        return blend(last.getValue(), entry.getValue(),
                                     (position - v1) / (v2 - v1));
                    }
                } else {
                    return entry.getValue().getRGB();
                }
            }

            last = entry;
        }

        if (last != null) {
            return last.getValue().getRGB();
        }

        return 0;
    }

    public static int blend(final Color c1, final Color c2, double ratio) {
        double invRatio = 1 - ratio;
        return ((int) (c1.getAlpha() * invRatio + c2.getAlpha() * ratio)) << 24
               | ((int) (c1.getRed() * invRatio + c2.getRed() * ratio)) << 16
               | ((int) (c1.getGreen() * invRatio + c2.getGreen() * ratio)) << 8
               | ((int) (c1.getBlue() * invRatio + c2.getBlue() * ratio));
    }
}
