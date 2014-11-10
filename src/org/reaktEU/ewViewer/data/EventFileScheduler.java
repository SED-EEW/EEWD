/*
 * Copyright (C) 2014 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reaktEU.ewViewer.data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quakeml.xmlns.bedRt.x12.EventParameters;

/**
 *
 * @author Stephan Herrnkind <herrnkind@gempa.de>
 */
public class EventFileScheduler implements Runnable {

    private static final Logger LOG = LogManager.getLogger(EventFileScheduler.class);

    protected List<EventFile> sequence;
    protected ScheduledExecutorService executor;
    protected int progress;
    protected long offset;

    protected List<QMLListener> messageListeners;

    public EventFileScheduler() {
        this.messageListeners = new ArrayList();
        this.executor = null;
        cancel();
    }

    synchronized public void addQMLListener(QMLListener qmlListener) {
        if (qmlListener != null) {
            messageListeners.add(qmlListener);
        }
    }

    synchronized public void start(List<EventFile> sequence) {
        cancel();

        if (sequence == null || sequence.isEmpty()) {
            return;
        }

        this.sequence = sequence;

        long firstTime = sequence.get(0).getTime();
        offset = System.currentTimeMillis() - firstTime;
        executor = Executors.newScheduledThreadPool(1);

        LOG.debug(String.format("starting replay: %s files, %dms offset",
                                sequence.size(), offset));

        for (EventFile ef : this.sequence) {
            executor.schedule(this, ef.getTime() - firstTime, TimeUnit.MILLISECONDS);
        }
    }

    public final synchronized void cancel() {
        sequence = null;
        progress = 0;
        offset = 0;

        if (executor != null) {
            LOG.debug("cancel");
            executor.shutdownNow();
            executor = null;
        }
    }

    @Override
    synchronized public void run() {
        if (progress >= sequence.size()) {
            return;
        }
        EventParameters eventParameters = sequence.get(progress)
                .getEventParameters();
        for (QMLListener l : messageListeners) {
            l.processQML(eventParameters, offset);
        }
        ++progress;
    }

}
