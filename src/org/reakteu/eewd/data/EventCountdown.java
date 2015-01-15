/*
 * Copyright (C) 2014 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reakteu.eewd.data;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reakteu.eewd.Application;
import org.reakteu.eewd.utils.GeoCalc;

/**
 *
 * @author Stephan Herrnkind <herrnkind@gempa.de>
 */
public class EventCountdown implements Runnable {

    private static final Logger LOG = LogManager.getLogger(EventCountdown.class);

    // allow up to 100ms difference of S-wave arrival introduced by location and
    // origin time updates before cancelling active countdown
    private static final long maxJitter = 100;

    private final long countdownMillis;
    private final double vs;

    private File sound = null;
    private Clip clip = null;
    private ScheduledExecutorService executor = null;
    private EventData event = null;
    private POI target = null;
    private long lastETA = 0;

    public EventCountdown() {
        Application app = Application.getInstance();
        String fileName = app.getProperty(Application.PropertyCountdownSound, (String) null);
        countdownMillis = (long) (app.getProperty(Application.PropertyCountdownSeconds, (Float) 0.0f) * 1000f);
        vs = app.getProperty(Application.PropertyVS, Application.DefaultVS);

        if (fileName != null && countdownMillis > 0) {
            try {
                sound = new File(fileName);
                AudioInputStream inputStream = AudioSystem.getAudioInputStream(sound);
                AudioFormat format = inputStream.getFormat();
                DataLine.Info info = new DataLine.Info(Clip.class, format);
                clip = (Clip) AudioSystem.getLine(info);
            } catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
                LOG.error("could not open countdown sound file", e);
            }
        }

    }

    synchronized public void setTarget(POI target) {
        if (this.target != target) {
            this.target = target;
            update();
        }
    }

    synchronized public void setEvent(EventData event) {
        if (this.event != event) {
            this.event = event;
            update();
        }
    }

    synchronized private void stop() {
        if (executor != null) {
            LOG.debug("shutting down executor");
            executor.shutdownNow();
            executor = null;
        }
        lastETA = 0;
        if (clip.isRunning()) {
            clip.stop();
        }
        if (clip.isOpen()) {
            clip.close();
        }
    }

    synchronized private void update() {
        if (clip == null) {
            return;
        } else if (target == null || event == null) {
            stop();
            return;
        }

        double[] pEvent = GeoCalc.Geo2Cart(event.latitude, event.longitude, -event.depth);
        double[] pTarget = GeoCalc.Geo2Cart(target.latitude, target.longitude, target.altitude);
        double d = GeoCalc.Distance3D(pEvent, pTarget);
        long eta = event.time + (long) (d / vs);

        if (lastETA > 0 && Math.abs(lastETA - eta) < maxJitter) {
            return; // no significat change in estimated time of arrival
        }

        stop();
        lastETA = eta;

        long millisUntilArrival = eta - System.currentTimeMillis();
        if (millisUntilArrival > 0) {
            long delay = millisUntilArrival - countdownMillis;
            executor = Executors.newScheduledThreadPool(1);
            if (delay > 0) {
                executor.schedule(this, delay, TimeUnit.MILLISECONDS);
            } else {
                long fastForward = -delay;
                if (countdownMillis < fastForward) {
                    LOG.warn("skipping replay of countdown sound, fast forward exceeds length");
                } else {
                    play(fastForward);
                }
            }
        }
    }

    @Override
    synchronized public void run() {
        play(0);
    }

    private void play(long fastForward) {
        try {
            LOG.info(String.format("playing countdown sound (fastForward: %.1f)", fastForward / 1000.0));
            clip.open(AudioSystem.getAudioInputStream(sound));
            clip.setMicrosecondPosition(fastForward * 1000);
            clip.start();
        } catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
            LOG.error("could not open countdown sound file", e);
        }
    }
}
