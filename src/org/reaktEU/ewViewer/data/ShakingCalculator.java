/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.reaktEU.ewViewer.data;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private final BlockingQueue<EventData> queue;
    //private final ExecutorService executor;

    private AttenuationPGA gmpePGAImpl = null;
    private AttenuationPGV gmpePGVImpl = null;
    private AttenuationPSA gmpePSAImpl = null;
    private AttenuationInt gmpeIntImpl = null;
    private IntensityFromAcceleration gmicePGA = null;
    private IntensityFromVelocity gmicePGV = null;

    public ShakingCalculator(List<POI> targets, List<POI> stations) {
        this.targets = targets;
        this.stations = stations;

        queue = new LinkedBlockingQueue();
        new Thread(this).start();

        //executor = Executors.newFixedThreadPool(1);
        //executor.submit(this);
        org.reaktEU.ewViewer.gmpe.impl.Swiss gmpe = new org.reaktEU.ewViewer.gmpe.impl.Swiss();
        org.reaktEU.ewViewer.gmice.impl.Swiss gmice = new org.reaktEU.ewViewer.gmice.impl.Swiss();
        this.gmpePGAImpl = gmpe;
        this.gmpePGVImpl = gmpe;
        this.gmpePSAImpl = gmpe;
        this.gmicePGV = gmice;
    }

    @Override
    public void run() {
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
                return;
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
            AttenuationPGA gmpePGA = this.gmpePGAImpl;
            AttenuationPGV gmpePGV = this.gmpePGVImpl;
            AttenuationPSA gmpePSA = this.gmpePSAImpl;
            AttenuationInt gmpeInt = this.gmpeIntImpl;

            IntensityFromAcceleration gmicePGA = this.gmicePGA;
            IntensityFromVelocity gmicePGV = this.gmicePGV;

            Shaking s;
            for (POI target : targets) {
                if (gmpePGA != null) {
                    s = gmpePGA.getPGA(
                            event.magnitude, event.latitude, event.longitude,
                            event.depth, target.latitude, target.longitude,
                            target.altitude, "VS30", target.amplification,
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
                            target.altitude, "VS30", target.amplification,
                            event.eventParameters);
                    target.shakingValues.put(Shaking.Type.PGV, s);
                    if (gmpeInt == null && gmicePGV != null) {
                        s = gmicePGV.getIntensityFromVelocity(s);
                        target.shakingValues.put(Shaking.Type.Intensity, s);
                    }
                }
                if (gmpePSA != null) {
                    s = gmpePSA.getPSA(
                            event.magnitude, event.latitude, event.longitude,
                            event.depth, target.latitude, target.longitude,
                            target.altitude, "VS30", target.amplification,
                            2.0, event.eventParameters);
                    target.shakingValues.put(Shaking.Type.PSA, s);
                }
                if (gmpeInt != null) {
                    s = gmpeInt.getInt(
                            event.magnitude, event.latitude, event.longitude,
                            event.depth, target.latitude, target.longitude,
                            target.altitude, "VS30", target.amplification,
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
