/*
 * Copyright (C) 2014-2015 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reakteu.eewd.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reakteu.eewd.Application;
import org.reakteu.eewd.gmice.IntensityFromAcceleration;
import org.reakteu.eewd.gmice.IntensityFromVelocity;
import org.reakteu.eewd.gmpe.AttenuationDRS;
import org.reakteu.eewd.ipe.AttenuationInt;
import org.reakteu.eewd.gmpe.AttenuationPGA;
import org.reakteu.eewd.gmpe.AttenuationPGV;
import org.reakteu.eewd.gmpe.AttenuationPSA;
import org.reakteu.eewd.layer.ShakeMapLayer;

/**
 *
 * @author Stephan Herrnkind herrnkind at gempa dot de
 */
public class ShakingCalculator implements Runnable {

    private static final Logger LOG = LogManager.getLogger(ShakingCalculator.class);

    private final List<POI> targets;
    private final Map<String, POI> stations;
    private final ShakeMapLayer shakeMap;
    private final BlockingQueue<EventData> queue;

    private final String ampliProxyName;
    private final AttenuationPGA gmpePGAImpl;
    private final AttenuationPGV gmpePGVImpl;
    private final AttenuationPSA gmpePSAImpl;
    private final AttenuationDRS gmpeDRSImpl;
    private final AttenuationInt ipeIntImpl;
    private IntensityFromAcceleration gmicePGAImpl = null;
    private IntensityFromVelocity gmicePGVImpl = null;

    public ShakingCalculator(List<POI> targets, Map<String, POI> stations,
                             ShakeMapLayer shakeMap) {
        this.targets = targets;
        this.stations = stations;
        this.shakeMap = shakeMap;

        Application app = Application.getInstance();

        ampliProxyName = app.getProperty(Application.PropertyAmpliProxyName, "");

        // cache already loaded instances since one class may implement
        // multiple interfaces
        Map<String, Object> cache = new HashMap();
        String prefix;

        // gmpe PGA
        prefix = Application.PropertyGMPE + "." + Shaking.Type.PGA;
        gmpePGAImpl = (AttenuationPGA) loadImpl(prefix, cache, AttenuationPGA.class);

        // gmpe PGV
        prefix = Application.PropertyGMPE + "." + Shaking.Type.PGV;
        gmpePGVImpl = (AttenuationPGV) loadImpl(prefix, cache, AttenuationPGV.class);

        // gmpe PSA
        prefix = Application.PropertyGMPE + "." + Shaking.Type.PSA;
        gmpePSAImpl = (AttenuationPSA) loadImpl(prefix, cache, AttenuationPSA.class);

        // gmpe DRS
        prefix = Application.PropertyGMPE + "." + Shaking.Type.DRS;
        gmpeDRSImpl = (AttenuationDRS) loadImpl(prefix, cache, AttenuationDRS.class);

        // ipe Intensity
        prefix = Application.PropertyIPE + "." + Shaking.Type.Intensity;
        ipeIntImpl = (AttenuationInt) loadImpl(prefix, cache, AttenuationInt.class);

        // derive intensity from acceleration/velocity if gmpe intensity
        // implementation is not available
        if (ipeIntImpl == null) {
            // gmice PGA
            prefix = Application.PropertyGMICE + "." + Shaking.Type.PGA;
            gmicePGAImpl = (IntensityFromAcceleration) loadImpl(prefix, cache, IntensityFromAcceleration.class);

            // gmice PGV
            prefix = Application.PropertyGMICE + "." + Shaking.Type.PGV;
            gmicePGVImpl = (IntensityFromVelocity) loadImpl(prefix, cache, IntensityFromVelocity.class);
        }

        queue = new LinkedBlockingQueue();
        new Thread(this).start();
    }

    private Object loadImpl(String prefix, Map<String, Object> cache, Class type) {
        String className = Application.getInstance().getProperty(
                prefix + ".class", (String) null);
        if (className == null) {
            return null;
        }

        Object obj = cache.get(className);
        if (obj == null) {
            try {
                Class c = Class.forName(className);
                obj = c.newInstance();
                cache.put(className, obj);
                LOG.debug("instance of " + className + " created for " + prefix);
            } catch (ClassNotFoundException cnfe) {
                LOG.error("could not find " + prefix + " class " + className);
            } catch (InstantiationException | IllegalAccessException ex) {
                LOG.error("could not create instance of " + prefix
                          + " class " + className, ex);
            }
        } else {
            LOG.debug("using instance of " + className + " for " + prefix);
        }

        if (!type.isInstance(obj)) {
            LOG.error(prefix + " class " + className + " not an instance of "
                      + type.getName());
            return null;
        }
        return obj;
    }

    @Override
    public void run() {
        Application app = Application.getInstance();
        Double controlPeriod = app.getControlPeriod();
        double[] periods = app.getPeriods();
        while (true) {
            EventData event;
            try {
                LOG.info("waiting for next event");
                event = queue.take();
                if (!queue.isEmpty()) {
                    LOG.debug("skipping event");
                    continue; // process only the most recent event
                }
            } catch (InterruptedException ex) {
                LOG.error("take interrupted");
                continue;
            }

            LOG.debug("processing event");
            // TODO: check for available Pd and derived PGA/PGV from Pd for
            // stations and for targets with a minimum station distance of
            // 'RadiusOfInfluence'
            // Algoritm:
            //  - create list of stations with Pd and derive PGA/PGV
            //  - for each target find nearest station with Pd and copy PGA/PGV
            //    value
            //  - continue with stations/targets without PGA/PGV
            //
            // make sure the same algorithm is used for one all POIs
            AttenuationPGA gmpePGA = gmpePGAImpl;
            AttenuationPGV gmpePGV = gmpePGVImpl;
            AttenuationPSA gmpePSA = gmpePSAImpl;
            AttenuationDRS gmpeDRS = gmpeDRSImpl;
            AttenuationInt gmpeInt = ipeIntImpl;

            IntensityFromAcceleration gmicePGA = gmicePGAImpl;
            IntensityFromVelocity gmicePGV = gmicePGVImpl;

            Shaking s;
            for (POI target : targets) {
                synchronized (target) {
                    target.clearValues();
                    if (gmpePGA != null) {
                        s = gmpePGA.getPGA(
                                event.magnitude, event.latitude, event.longitude,
                                event.depth, target.latitude, target.longitude,
                                target.altitude, ampliProxyName, target.amplification,
                                event.eventParameters,
                                event.ruptureLength,
                                event.ruptureStrike);
                        target.shakingValues.put(Shaking.Type.PGA, s);
                        if (gmpeInt == null && gmicePGA != null) {
                            s = gmicePGA.getIntensityFromAcceleration(s);
                            target.shakingValues.put(Shaking.Type.Intensity, s);
                        }
                    }
                    if (gmpePGV != null) {
                        s = gmpePGV.getPGV(
                                event.magnitude, event.latitude, event.longitude,
                                event.depth, target.latitude, target.longitude,
                                target.altitude, ampliProxyName, target.amplification,
                                event.eventParameters,
                                event.ruptureLength,
                                event.ruptureStrike);
                        target.shakingValues.put(Shaking.Type.PGV, s);
                        if (gmpeInt == null && gmicePGV != null) {
                            s = gmicePGV.getIntensityFromVelocity(s);
                            target.shakingValues.put(Shaking.Type.Intensity, s);
                        }
                    }
                    if (gmpePSA != null) {
                        if (controlPeriod != null) {
                            s = gmpePSA.getPSA(
                                    event.magnitude, event.latitude, event.longitude,
                                    event.depth, target.latitude, target.longitude,
                                    target.altitude, ampliProxyName, target.amplification,
                                    controlPeriod, event.eventParameters,
                                    event.ruptureLength,
                                    event.ruptureStrike);
                            target.shakingValues.put(Shaking.Type.PSA, s);
                        }
                        if (app.getSpectrumParameter() == Shaking.Type.PSA) {
                            for (double p : periods) {
                                target.spectralValues.add(gmpePSA.getPSA(
                                        event.magnitude, event.latitude, event.longitude,
                                        event.depth, target.latitude, target.longitude,
                                        target.altitude, ampliProxyName, target.amplification,
                                        p, event.eventParameters,
                                        event.ruptureLength,
                                        event.ruptureStrike));
                            }
                        }
                    }
                    if (gmpeDRS != null) {
                        if (controlPeriod != null) {
                            s = gmpeDRS.getDRS(
                                    event.magnitude, event.latitude, event.longitude,
                                    event.depth, target.latitude, target.longitude,
                                    target.altitude, ampliProxyName, target.amplification,
                                    controlPeriod, event.eventParameters,
                                    event.ruptureLength,
                                    event.ruptureStrike);
                            target.shakingValues.put(Shaking.Type.DRS, s);
                        }
                        if (app.getSpectrumParameter() == Shaking.Type.DRS) {
                            for (double p : periods) {
                                target.spectralValues.add(gmpeDRS.getDRS(
                                        event.magnitude, event.latitude, event.longitude,
                                        event.depth, target.latitude, target.longitude,
                                        target.altitude, ampliProxyName, target.amplification,
                                        p, event.eventParameters,
                                        event.ruptureLength,
                                        event.ruptureStrike));
                            }
                        }
                    }
                    if (gmpeInt != null) {
                        s = gmpeInt.getInt(
                                event.magnitude, event.latitude, event.longitude,
                                event.depth, target.latitude, target.longitude,
                                target.altitude, ampliProxyName, target.amplification,
                                event.eventParameters);
                        target.shakingValues.put(Shaking.Type.Intensity, s);
                    }
                }
            }

            // shake map
            Shaking.Type shakeMapParameter = app.getShakeMapParameter();
            if (shakeMap != null && shakeMapParameter != null) {
                LOG.debug("starting shake map calculation");
                long start = System.currentTimeMillis();
                boolean success = true;
                if (app.getShakeMapParameter() == Shaking.Type.PGA && gmpePGA != null) {
                    for (ShakeMapLayer.Point p : shakeMap.getPoints()) {
                        p.value = gmpePGA.getPGA(
                                event.magnitude, event.latitude, event.longitude,
                                event.depth, p.latitude, p.longitude, p.altitude,
                                ampliProxyName, p.amplification,
                                event.eventParameters,
                                event.ruptureLength,
                                event.ruptureStrike).expectedSI * Application.EarthAcceleration1;
                    }
                } else if (shakeMapParameter == Shaking.Type.PGV && gmpePGV != null) {
                    for (ShakeMapLayer.Point p : shakeMap.getPoints()) {
                        p.value = gmpePGV.getPGV(
                                event.magnitude, event.latitude, event.longitude,
                                event.depth, p.latitude, p.longitude, p.altitude,
                                ampliProxyName, p.amplification,
                                event.eventParameters,
                                event.ruptureLength,
                                event.ruptureStrike).expectedSI * 100;
                    }
                } else if (shakeMapParameter == Shaking.Type.PSA && gmpePSA != null) {
                    if (controlPeriod != null) {
                        for (ShakeMapLayer.Point p : shakeMap.getPoints()) {
                            p.value = gmpePSA.getPSA(
                                    event.magnitude, event.latitude, event.longitude,
                                    event.depth, p.latitude, p.longitude, p.altitude,
                                    ampliProxyName, p.amplification, controlPeriod,
                                    event.eventParameters,
                                    event.ruptureLength,
                                    event.ruptureStrike).expectedSI * Application.EarthAcceleration1;
                        }
                    }
                } else if (shakeMapParameter == Shaking.Type.DRS && gmpeDRS != null) {
                    if (controlPeriod != null) {
                        for (ShakeMapLayer.Point p : shakeMap.getPoints()) {
                            p.value = gmpeDRS.getDRS(
                                    event.magnitude, event.latitude, event.longitude,
                                    event.depth, p.latitude, p.longitude, p.altitude,
                                    ampliProxyName, p.amplification, controlPeriod,
                                    event.eventParameters,
                                    event.ruptureLength,
                                    event.ruptureStrike).expectedSI * 100;
                        }
                    }
                } else if (shakeMapParameter == Shaking.Type.Intensity) {
                    if (gmpeInt == null) {
                        if (gmicePGA != null && gmpePGA != null) {
                            for (ShakeMapLayer.Point p : shakeMap.getPoints()) {
                                p.value = gmicePGA.getIntensityFromAcceleration(
                                        gmpePGA.getPGA(
                                                event.magnitude, event.latitude,
                                                event.longitude, event.depth,
                                                p.latitude, p.longitude, p.altitude,
                                                ampliProxyName, p.amplification,
                                                event.eventParameters,
                                                event.ruptureLength,
                                                event.ruptureStrike)).expectedSI;
                            }
                        } else if (gmicePGV != null && gmpePGV != null) {
                            for (ShakeMapLayer.Point p : shakeMap.getPoints()) {
                                p.value = gmicePGV.getIntensityFromVelocity(
                                        gmpePGV.getPGV(
                                                event.magnitude, event.latitude,
                                                event.longitude, event.depth,
                                                p.latitude, p.longitude, p.altitude,
                                                ampliProxyName, p.amplification,
                                                event.eventParameters,
                                                event.ruptureLength,
                                                event.ruptureStrike)).expectedSI;
                            }
                        } else {
                            success = false;
                        }
                    } else {
                        for (ShakeMapLayer.Point p : shakeMap.getPoints()) {
                            p.value = gmpeInt.getInt(
                                    event.magnitude, event.latitude, event.longitude,
                                    event.depth, p.latitude, p.longitude, p.altitude,
                                    ampliProxyName, p.amplification,
                                    event.eventParameters).expectedSI;
                        }
                    }
                } else {
                    success = false;
                }

                if (success) {
                    LOG.debug(String.format("%d grid points calculated in %.3fs",
                                            shakeMap.getPoints().size(),
                                            (double) (System.currentTimeMillis() - start) / 1000.0));
                } else {
                    LOG.warn("no implementation found for "
                             + Application.PropertySMParameter + " "
                             + shakeMapParameter.toString());
                }

                shakeMap.updateImage(success);
            }
        }
    }

    public void processEvent(EventData event) {
        LOG.debug("adding new event");
        try {
            queue.put(event);
        } catch (InterruptedException ex) {
            // should never happen since queue size is Interger.MAX_VALUE
            LOG.error("put interrupted");
        }
    }
}
