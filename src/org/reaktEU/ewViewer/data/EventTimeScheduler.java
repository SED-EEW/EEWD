/*
 * Copyright (C) 2014 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reaktEU.ewViewer.data;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Stephan Herrnkind <herrnkind@gempa.de>
 */
public class EventTimeScheduler implements Runnable {

    private static final Logger LOG = LogManager.getLogger(EventTimeScheduler.class);
    private static final long UpdateInterval = 250;

    private long maxUpdateMillis;
    private Set<EventTimeListener> updateListeners;
    private ScheduledExecutorService executor;
    private EventData event;
    private boolean dirty;

    public EventTimeScheduler(int maxUpdateSeconds) {
        maxUpdateMillis = maxUpdateSeconds * 1000;
        updateListeners = new HashSet();

        event = null;
        dirty = false;
    }

    synchronized public void addUpdateListener(EventTimeListener listener) {
        if (listener != null) {
            updateListeners.add(listener);
        }
    }

    synchronized public void setEvent(EventData event) {
        if (this.event != event) {
            this.event = event;
            cancel();

            executor = Executors.newScheduledThreadPool(1);
            executor.scheduleAtFixedRate(this, 0, UpdateInterval, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    synchronized public void run() {
        Long offset = null;
        if (event != null) {
            offset = System.currentTimeMillis() - event.time;
            LOG.debug(event.eventID + ": " + offset);
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
