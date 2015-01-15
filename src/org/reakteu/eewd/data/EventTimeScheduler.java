/*
 * Copyright (C) 2014 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reakteu.eewd.data;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
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

/**
 *
 * @author Stephan Herrnkind <herrnkind@gempa.de>
 */
public class EventTimeScheduler implements Runnable {

    private static final Logger LOG = LogManager.getLogger(EventTimeScheduler.class);
    private static final long UpdateInterval = 250;

    private final long maxUpdateMillis;
    private final Set<EventTimeListener> updateListeners;
    private final File alertSound;
    private final int alertSoundLoop;

    private ScheduledExecutorService executor;
    private EventData event;

    public EventTimeScheduler() {
        double maxUpdateSeconds = Application.getInstance().getProperty(
                Application.PropertyTimeoutAfterOriginTime, 60.0);

        maxUpdateMillis = (long) (maxUpdateSeconds * 1000);
        updateListeners = new HashSet();

        Application app = Application.getInstance();

        String fileName = app.getProperty(Application.PropertyAlertSound, (String) null);
        alertSound = fileName == null ? null : new File(fileName);
        alertSoundLoop = app.getProperty(Application.PropertyAlertSoundLoop, 1);

        executor = null;
        event = null;
    }

    synchronized public void addUpdateListener(EventTimeListener listener) {
        if (listener != null) {
            updateListeners.add(listener);
        }
    }

    synchronized public void setEvent(EventData event, boolean disable) {
        if (disable && this.event != null && this.event.eventID.equals(event.eventID)) {
            LOG.info("Canceling alert for event " + event.eventID);
            this.event = null;
            return;
        }

        boolean newEvent = this.event == null
                           || event != null && !this.event.eventID.equals(event.eventID);
        this.event = event;
        cancel();

        executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(this, 0, UpdateInterval, TimeUnit.MILLISECONDS);

        // play sound for new events
        if (newEvent && alertSound != null) {
            try {
                AudioInputStream inputStream = AudioSystem.getAudioInputStream(alertSound);
                AudioFormat format = inputStream.getFormat();
                DataLine.Info info = new DataLine.Info(Clip.class, format);
                Clip clip = (Clip) AudioSystem.getLine(info);
                clip.open(inputStream);
                clip.loop(alertSoundLoop);
            } catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
                LOG.error("could not play alert sound", e);
            }
        }

    }

    @Override
    synchronized public void run() {
        Long offset = null;
        if (event != null) {
            offset = System.currentTimeMillis() - event.time;
            LOG.trace(event.eventID + ": " + offset);
            if (offset > maxUpdateMillis) {
                offset = null;
                cancel();
            }
        }

        for (EventTimeListener l : updateListeners) {
            l.processEventTime(event, offset);
        }
    }

    synchronized private void cancel() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }
}
