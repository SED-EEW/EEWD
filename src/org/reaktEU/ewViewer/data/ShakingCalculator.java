/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.reaktEU.ewViewer.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reaktEU.ewViewer.Application;
import static org.reaktEU.ewViewer.Application.PropertyEventArchive;
import org.reaktEU.ewViewer.gmice.IntensityFromAcceleration;
import org.reaktEU.ewViewer.gmice.IntensityFromVelocity;
import org.reaktEU.ewViewer.gmpe.AttenuationInt;
import org.reaktEU.ewViewer.gmpe.AttenuationPGA;
import org.reaktEU.ewViewer.gmpe.AttenuationPGV;
import org.reaktEU.ewViewer.gmpe.AttenuationPSA;

/**
 *
 * @author Stephan Herrnkind <herrnkind@gempa.de>
 */
public class ShakingCalculator implements Runnable {

    private static final Logger LOG = LogManager.getLogger(ShakingCalculator.class);

    private final List<POI> targets;
    private final List<POI> stations;
    private final ShakeMap shakeMap;
    private final BlockingQueue<EventData> queue;

    private final String ampliProxyName;
    private AttenuationPGA gmpePGAImpl;
    private AttenuationPGV gmpePGVImpl;
    private AttenuationPSA gmpePSAImpl;
    private AttenuationInt gmpeIntImpl;
    private IntensityFromAcceleration gmicePGAImpl = null;
    private IntensityFromVelocity gmicePGVImpl = null;

    public ShakingCalculator(List<POI> targets, List<POI> stations, ShakeMap shakeMap) {
        this.targets = targets;
        this.stations = stations;
        this.shakeMap = shakeMap;

        Application app = Application.getInstance();

        ampliProxyName = app.getProperty(Application.PropertyAmpliProxyName, "");

        // cache already loaded instances since one class may implement
        // multiple interfaces
        Map<String, Object> cache = new HashMap();
        String prefix;
        Object obj;

        // gmpe PGA
        prefix = Application.PropertyGMPE + "." + Shaking.Type.PGA;
        gmpePGAImpl = (AttenuationPGA) loadImpl(prefix, cache, AttenuationPGA.class);

        // gmpe PGV
        prefix = Application.PropertyGMPE + "." + Shaking.Type.PGV;
        gmpePGVImpl = (AttenuationPGV) loadImpl(prefix, cache, AttenuationPGV.class);

        // gmpe PSA
        prefix = Application.PropertyGMPE + "." + Shaking.Type.PSA;
        gmpePSAImpl = (AttenuationPSA) loadImpl(prefix, cache, AttenuationPSA.class);

        // gmpe Intensity
        prefix = Application.PropertyGMPE + "." + Shaking.Type.Intensity;
        gmpeIntImpl = (AttenuationInt) loadImpl(prefix, cache, AttenuationInt.class);

        // derive intensity from acceleration/velocity if gmpe intensity
        // implementation is not available
        if (gmpeIntImpl == null) {
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
            AttenuationInt gmpeInt = gmpeIntImpl;

            IntensityFromAcceleration gmicePGA = gmicePGAImpl;
            IntensityFromVelocity gmicePGV = gmicePGVImpl;

            Shaking s;
            for (POI target : targets) {
                if (gmpePGA != null) {
                    s = gmpePGA.getPGA(
                            event.magnitude, event.latitude, event.longitude,
                            event.depth, target.latitude, target.longitude,
                            target.altitude, ampliProxyName, target.amplification,
                            event.eventParameters);
                    target.shakingValues.put(Shaking.Type.PGA, s);
                    if (gmpeInt == null && gmicePGA != null) {
                        s = gmicePGA.getIntensityfromAcceleration(s);
                        target.shakingValues.put(Shaking.Type.Intensity, s);
                    }
                }
                if (gmpePGV != null) {
                    s = gmpePGV.getPGV(
                            event.magnitude, event.latitude, event.longitude,
                            event.depth, target.latitude, target.longitude,
                            target.altitude, ampliProxyName, target.amplification,
                            event.eventParameters);
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
                                controlPeriod, event.eventParameters);
                        target.shakingValues.put(Shaking.Type.PSA, s);
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

    synchronized public void setGmpePGA(AttenuationPGA gmpePGA) {
        this.gmpePGAImpl = gmpePGA;
    }

    synchronized public void setGmpePGV(AttenuationPGV gmpePGV) {
        this.gmpePGVImpl = gmpePGV;
    }

    synchronized public void setGmpePSA(AttenuationPSA gmpePSA) {
        this.gmpePSAImpl = gmpePSA;
    }

    synchronized public void setGmpeInt(AttenuationInt gmpeInt) {
        this.gmpeIntImpl = gmpeInt;
    }
}
