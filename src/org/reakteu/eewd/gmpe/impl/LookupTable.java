/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.reakteu.eewd.gmpe.impl;

import java.io.File;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quakeml.xmlns.bedRt.x12.EventParameters;
import org.reakteu.eewd.Application;
import org.reakteu.eewd.data.FloatTable;
import org.reakteu.eewd.data.Shaking;
import org.reakteu.eewd.gmpe.AttenuationDRS;
import org.reakteu.eewd.gmpe.AttenuationPGA;
import org.reakteu.eewd.gmpe.AttenuationPGV;
import org.reakteu.eewd.gmpe.AttenuationPSA;
import org.reakteu.eewd.utils.GeoCalc;

/**
 *
 * @author Stephan Herrnkind herrnkind at gempa dot de
 */
public abstract class LookupTable implements AttenuationPGA, AttenuationPGV, AttenuationPSA, AttenuationDRS {

    private static final Logger LOG = LogManager.getLogger(LookupTable.class);

    public static final String PropertyDataDir = "lookupTable.dataDir";
    public static final String SuffixExpectedSI = ".inp";
    public static final String SuffixPercentile84 = "_84" + SuffixExpectedSI;
    public static final String SuffixPercentile16 = "_16" + SuffixExpectedSI;
    public static final String TableText00 = "R/M";

    private FloatTable[] pgv = null;
    private FloatTable[] pga = null;
    private final HashMap<Double, FloatTable[]> psa = new HashMap();
    private final HashMap<Double, FloatTable[]> drs = new HashMap();

    public LookupTable() {
        String dir = "";
        double[] periods;
        Application app = Application.getInstance();
        if (app != null) {
            dir = app.getProperty(PropertyDataDir, dir);
            periods = app.getPeriods();

            FloatTable[] t;
            for (int i = 0; i < periods.length; ++i) {
                // PSA
                t = readTables(dir + "/" + Shaking.Type.PSA.toString() + i);
                if (t[0] != null) {
                    psa.put(periods[i], t);
                }

                // DRS
                t = readTables(dir + "/" + Shaking.Type.DRS.toString() + i);
                if (t[0] != null) {
                    drs.put(periods[i], t);
                }
            }
        }

        pgv = readTables(dir + "/" + Shaking.Type.PGV.toString());
        pga = readTables(dir + "/" + Shaking.Type.PGA.toString());
    }

    //@Override
    public Shaking getPGA(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat,
                          double targetLon, double targetElevM,
                          String amplificationType, double amplification,
                          EventParameters eventParameters) {
        return getShaking(pga, magnitude, sourceLat, sourceLon, -sourceDepthM,
                          targetLat, targetLon, targetElevM, amplification);
    }

    //@Override
    public Shaking getPGV(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat,
                          double targetLon, double targetElevM,
                          String amplificationType, double amplification,
                          EventParameters eventParameters) {
        return getShaking(pgv, magnitude, sourceLat, sourceLon, -sourceDepthM,
                          targetLat, targetLon, targetElevM, amplification);
    }

    //@Override
    public Shaking getPSA(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat,
                          double targetLon, double targetElevM,
                          String amplificationType, double amplification,
                          double period, EventParameters eventParameters) {
        FloatTable[] t = psa.get(period);
        return t == null ? new Shaking()
               : getShaking(t, magnitude, sourceLat, sourceLon, -sourceDepthM,
                            targetLat, targetLon, targetElevM,
                            amplification);
    }

    //@Override
    public Shaking getDRS(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat,
                          double targetLon, double targetElevM,
                          String amplificationType, double amplification,
                          double period, EventParameters eventParameters) {
        FloatTable[] t = drs.get(period);
        return t == null ? new Shaking()
               : getShaking(t, magnitude, sourceLat, sourceLon, -sourceDepthM,
                            targetLat, targetLon, targetElevM,
                            amplification);
    }

    private FloatTable[] readTables(String baseName) {
        File f[] = {
            new File(baseName + SuffixExpectedSI),
            new File(baseName + SuffixPercentile84),
            new File(baseName + SuffixPercentile16)
        };

        FloatTable[] t = new FloatTable[f.length];

        for (int i = 0; i < f.length; ++i) {
            t[i] = f[i].isFile() ? FloatTable.create(f[i], TableText00) : null;
        }
        return t;
    }

    private Shaking getShaking(FloatTable[] t, double mag,
                               double lat1, double lon1, double elev1,
                               double lat2, double lon2, double elev2,
                               double ampli) {
        // Compute hypocentral distance
        double[] p1 = GeoCalc.Geo2Cart(lat1, lon1, elev1);
        double[] p2 = GeoCalc.Geo2Cart(lat2, lon2, elev2);
        double r = GeoCalc.Distance3D(p1, p2) / 1000.0; // in kilometers

        Shaking s = new Shaking();
        float f;
        if (t[0] != null) {
            f = t[0].interpolate((float) mag, (float) r);
            //if (f == f) {
            s.expectedSI = f * ampli;
            //}
        }
        if (t[1] != null) {
            f = t[1].interpolate((float) mag, (float) r);
            //if (f == f) {
            s.percentile84 = f * ampli;
            //}
        }
        if (t[2] != null) {
            f = t[2].interpolate((float) mag, (float) r);
            //if (f == f) {
            s.percentile16 = f * ampli;
            //}
        }

        return s;
    }

}
