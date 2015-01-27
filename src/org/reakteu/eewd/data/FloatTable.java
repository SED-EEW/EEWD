/*
 * Copyright (C) 2014-2015 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reakteu.eewd.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Stephan Herrnkind <herrnkind@gempa.de>
 */
public class FloatTable {

    private static final Logger LOG = LogManager.getLogger(FloatTable.class);

    private final TreeMap<Float, Integer> header = new TreeMap();
    private final TreeMap<Float, float[]> values = new TreeMap();

    public static FloatTable create(File file, String text00) {
        LOG.debug("loading table " + file);

        FloatTable table = new FloatTable();
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            String[] cols;
            int li = -1;
            while ((line = br.readLine()) != null) {
                ++li;
                cols = line.trim().split("\\s+");
                if (cols.length == 0) {
                    continue;
                }
                // read header
                if (table.header.isEmpty()) {
                    if (cols.length < 2) {
                        LOG.error("invalid table header, expected at least two columns in file "
                                  + file);
                        return null;
                    } else if (cols[0].equals(text00)) {
                        LOG.error("invalid table header, expected " + text00
                                  + " in first column of file " + file);
                        return null;
                    }
                    for (int i = 1; i < cols.length; ++i) {
                        try {
                            table.header.put(Float.parseFloat(cols[i]), i - 1);
                        } catch (NumberFormatException nfe) {
                            LOG.error(String.format("invalid float value in header column %d of file %s", i, file));
                            return null;
                        }
                    }
                } else {
                    if (cols.length < table.header.size() - 1) {
                        LOG.error(String.format("insufficient number of columns in line %d of file %s", li, file));
                        return null;
                    }
                    float[] v = new float[table.header.size()];
                    int i = 0;
                    try {
                        table.values.put(Float.parseFloat(cols[i]), v);
                        for (; i < table.header.size(); ++i) {
                            v[i] = Float.parseFloat(cols[i + 1]);
                        }
                    } catch (NumberFormatException nfe) {
                        LOG.error(String.format("invalid float value in line %d, column %d of file %s",
                                                li, i + 1, file));
                        return null;
                    }
                }
            }
            LOG.debug(String.format("table %s loaded, %d columns, %d rows",
                                    file.getName(), table.header.size(), table.values.size()));
            return table;
        } catch (IOException ioe) {
            LOG.error("error loading table " + file, ioe);

        }
        return null;
    }

    public float get(float x, float y) {
        Integer idx = header.get(x);
        if (idx == null) {
            return Float.NaN;
        }
        float v[] = values.get(y);
        return v == null ? Float.NaN : v[idx];
    }

    public float interpolate(float x, float y) {
        Map.Entry<Float, Integer> xLow = header.floorEntry(x);
        Map.Entry<Float, Integer> xHigh = header.ceilingEntry(x);
        if (xLow == null && xHigh == null) {
            return Float.NaN;
        } else if (xLow == null || xLow.getValue().equals(xHigh.getValue())) {
            xLow = xHigh;
            xHigh = null;
        }

        Map.Entry<Float, float[]> yLow = values.floorEntry(y);
        Map.Entry<Float, float[]> yHigh = values.ceilingEntry(y);
        if (yLow == null && yHigh == null) {
            return Float.NaN;
        } else if (yLow == null) {
            yLow = yHigh;
            yHigh = null;
        }

        if (yHigh == null) {
            float[] v = yLow.getValue();
            if (xHigh == null) {
                return v[xLow.getValue()];
            } else {
                float ratioX = (x - xLow.getKey()) / (xHigh.getKey() - xLow.getKey());
                return v[xLow.getValue()] * (1 - ratioX) + v[xHigh.getValue()] * ratioX;
            }
        } else {
            float ratioY = (y - yLow.getKey()) / (yHigh.getKey() - yLow.getKey());
            float[] v1 = yLow.getValue();
            float[] v2 = yHigh.getValue();
            if (xHigh == null) {
                int i = xLow.getValue();
                return v1[i] * (1 - ratioY) + v2[i] * ratioY;
            } else {
                int i1 = xLow.getValue();
                int i2 = xHigh.getValue();
                float ratioX = (x - xLow.getKey()) / (xHigh.getKey() - xLow.getKey());
                float vi1 = v1[i1] * (1 - ratioY) + v2[i1] * ratioY;
                float vi2 = v1[i2] * (1 - ratioY) + v2[i2] * ratioY;
                return vi1 * (1 - ratioX) + vi2 * ratioX;
            }
        }
    }
}
