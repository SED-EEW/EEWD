/*
 * Copyright (C) 2014 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reaktEU.ewViewer.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Stephan Herrnkind <herrnkind@gempa.de>
 */
public class EventArchive {

    public enum EventType {

        LOGGED, SCENARIO
    }

    private static final Logger LOG = LogManager.getLogger(EventArchive.class);

    private static final String EXTENSION = ".xml";
    public static final String LOG_DIR = "log";
    public static final String SCENARIO_DIR = "scenario";

    protected File logDir;
    protected File scenarioDir;

    public EventArchive(String archivePath) {
        logDir = new File(archivePath + "/" + LOG_DIR);
        scenarioDir = new File(archivePath + "/" + SCENARIO_DIR);
    }

    public List<String> getEventList(EventType type) {
        List<String> eventList = new ArrayList();
        File path = getPath(type);
        File[] listing = path.listFiles();
        if (listing == null) {
            LOG.warn(String.format("directory '%s' not found",
                                   path.getAbsolutePath()));
            return eventList;
        }

        for (File f : listing) {
            if (f.isDirectory()) {
                eventList.add(f.getName());
            }
        }

        Collections.sort(eventList);
        return eventList;
    }

    public List<EventFile> getEventSequence(EventType type, String eventID) {
        List<EventFile> sequence = new ArrayList();
        int idx = eventID.lastIndexOf('/') + 1;
        String eventDir = idx > 0 ? eventID.substring(idx) : eventID;
        File path = new File(getPath(type).getPath() + "/" + eventDir);
        File[] listing = path.listFiles();
        if (listing == null) {
            LOG.warn(String.format("directory '%s' not found",
                                   path.getAbsolutePath()));
            return sequence;
        }

        // find all xml files and extract time stamp from file name
        for (File f : listing) {
            String name = f.getName();
            if (f.isFile() && name.endsWith(EXTENSION)) {
                String baseName = name.substring(0, name.length() - EXTENSION.length());
                try {
                    sequence.add(new EventFile(Long.parseLong(baseName), f));
                } catch (NumberFormatException nfe) {
                    LOG.warn(String.format("invalid event file name found: %s",
                                           f.getAbsolutePath()));
                }
            }
        }

        if (sequence.isEmpty()) {
            return sequence;
        }

        // sort according to time stamp (ascending)
        Collections.sort(sequence, new Comparator<EventFile>() {
            @Override
            public int compare(EventFile up1, EventFile up2) {
                if (up1 == null && up2 == null) {
                    return 0;
                }
                if (up2 == null || up1.getTime() < up2.getTime()) {
                    return -1;
                }
                if (up1.getTime() > up2.getTime()) {
                    return 1;
                }
                return 0;
            }
        });

//        // normalize time stamps by subtracting time stamp of first element from
//        // all list elements
//        if (sequence.get(0).getTime() != 0) {
//            long offset = sequence.get(0).getTime();
//            for (EventFile up : sequence) {
//                up.removeOffset(offset);
//            }
//        }
        return sequence;
    }

    public boolean createScenario(String eventID) {
        File scenarioPath = new File(String.format("%s/%s",
                                                   getPath(EventType.SCENARIO).getPath(),
                                                   eventID));
        if (scenarioPath.exists()) {
            LOG.warn(String.format("scenario directory already exists: %s",
                                   scenarioPath.getAbsolutePath()));
            return false;
        }

        File loggedPath = new File(String.format("%s/%s",
                                                 getPath(EventType.LOGGED).getPath(),
                                                 eventID));
        File[] listing = loggedPath.listFiles();
        if (listing == null) {
            LOG.warn(String.format("event log directory not found: %s",
                                   loggedPath.getAbsolutePath()));
            return false;
        }

        if (!scenarioPath.mkdir()) {
            LOG.warn(String.format("could not create scenario directory: %s",
                                   scenarioPath.getAbsolutePath()));
            return false;
        }

        for (File src : listing) {
            String name = src.getName();
            if (src.isFile() && name.endsWith(EXTENSION)) {
                try {
                    File dest = new File(scenarioPath, name);
                    InputStream in = new FileInputStream(src);
                    OutputStream out = new FileOutputStream(dest);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                    in.close();
                    out.close();
                } catch (IOException ioe) {
                    LOG.warn("could not copy file", ioe);
                    return false;
                }
            }
        }
        return true;
    }

    public boolean deleteScenario(String eventID) {
        File path = new File(getPath(EventType.SCENARIO).getPath() + "/" + eventID);
        File[] listing = path.listFiles();
        if (listing == null) {
            LOG.warn(String.format("scenario directory not found: %s",
                                   path.getAbsolutePath()));
            return false;
        }

        for (File f : listing) {
            String name = f.getName();
            if (f.isFile() && name.endsWith(EXTENSION) && !f.delete()) {
                LOG.warn(String.format("could not delete file: %s",
                                       f.getAbsolutePath()));
                return false;
            }
        }
        if (!path.delete()) {
            LOG.warn(String.format("could not delete scenario directory: %s",
                                   path.getAbsolutePath()));
            return false;
        }
        return true;
    }

    public final File getPath(EventType type) {
        return type == EventType.LOGGED ? logDir : scenarioDir;
    }

}
