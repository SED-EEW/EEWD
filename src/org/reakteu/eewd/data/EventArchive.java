/*
 * Copyright (C) 2014-2015 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reakteu.eewd.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reakteu.eewd.Application;
import static org.reakteu.eewd.Application.PropertyEventArchive;

/**
 *
 * @author Stephan Herrnkind herrnkind at gempa dot de
 */
public class EventArchive {

    private static final Logger LOG = LogManager.getLogger(EventArchive.class);

    public static final String EXTENSION = ".xml";
    public static final String LOG_DIR = "log";
    public static final String SCENARIO_DIR = "scenario";
    public static final String INVALID_DIR = "invalid";
    //public static final long DAY_MILLIS = 24 * 60 * 60 * 1000;
    //public static final long MAX_ORIGIN_MILLIS_JITTER = 5 * 60 * 1000;

    protected final File logDir;
    protected final File scenarioDir;
    protected final File invalidDir;
    protected final DateFormat df;

    public EventArchive() {
        String path = Application.getInstance().getProperty(PropertyEventArchive,
                                                            "data/events");
        logDir = new File(path + "/" + LOG_DIR);
        scenarioDir = new File(path + "/" + SCENARIO_DIR);
        invalidDir = new File(path + "/" + INVALID_DIR);

        df = new SimpleDateFormat("yyyy/MM/dd");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public final File getLogDir() {
        return logDir;
    }

    public final File getScenarioDir() {
        return scenarioDir;
    }

//    public List<File> getEventFiles(File dir) {
//        List<File> eventList = new ArrayList();
//        File[] listing = dir.listFiles();
//        if (listing == null) {
//            LOG.warn(String.format("directory '%s' not found",
//                                   dir.getAbsolutePath()));
//            return eventList;
//        }
//
//        for (File f : listing) {
//            if (f.isDirectory()) {
//                eventList.add(f);
//            }
//        }
//
//        Collections.sort(eventList, new Comparator<File>() {
//            @Override
//            public int compare(File o1, File o2) {
//                return o1.getName().compareTo(o2.getName());
//            }
//
//        });
//
//        return eventList;
//    }
    public List<EventFile> getEventSequence(File dir) {
        List<EventFile> sequence = new ArrayList();
        File[] listing = dir.listFiles();
        if (listing == null) {
            LOG.warn(String.format("directory '%s' not found",
                                   dir.getAbsolutePath()));
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

    synchronized public void log(long received, String payload, EventData event) {
        File dir;
        if (event == null) {
            dir = invalidDir;
        } else {
//            // check if we have to search event in different folder
//            long millisOfDay = event.time % DAY_MILLIS;
//            Long tmpDate = null;
//            if (millisOfDay < MAX_ORIGIN_MILLIS_JITTER) {
//                tmpDate = event.time - MAX_ORIGIN_MILLIS_JITTER;
//            } else if (DAY_MILLIS - millisOfDay <= MAX_ORIGIN_MILLIS_JITTER) {
//                tmpDate = event.time + MAX_ORIGIN_MILLIS_JITTER;
//            }
            String eventID = event.eventID.replaceAll("[^a-zA-Z0-9.-]", "_");
            dir = new File(String.format("%s/%s/%s", logDir.getPath(),
                                         df.format(event.time), eventID));

//            if (tmpDate != null) {
//                File tmpDir = new File(String.format("%s/%s/%s", logDir.getPath(),
//                                                     df.format(tmpDate), eventID));
//                if (tmpDir.isDirectory() && !dir.exists()) {
//                    LOG.info("moving event from different date folder");
//                    try {
//                        File parent = tmpDir.getParentFile();
//                        if (parent != null && !parent.exists() && !parent.mkdirs()) {
//                            LOG.warn("could not create directory: " + parent.getAbsolutePath());
//                        } else {
//                            Files.move(tmpDir.toPath(), dir.toPath(), StandardCopyOption.ATOMIC_MOVE);
//                        }
//                    } catch (IOException ex) {
//                        LOG.error("could not move event folder", ex);
//                    }
//                }
//            }
        }

        if (!dir.exists() && !dir.mkdirs()) {
            LOG.warn("could not create directory: " + dir.getAbsolutePath());
            return;
        }

        if (!dir.isDirectory()) {
            LOG.warn("directory path exists but is not a directory: "
                     + dir.getAbsolutePath());
            return;
        }
        File file = new File(dir.getAbsolutePath() + String.format("/%d.xml", received));

        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file), "utf-8")) {
            osw.write(payload);
        } catch (IOException ioe) {
            LOG.error("could not write message to file: " + file.getAbsolutePath(), ioe);
        }
    }

    public boolean createScenario(File logged) {
        File scenario = new File(scenarioDir, logged.getName());
        if (scenario.exists()) {
            LOG.warn(String.format("scenario directory already exists: %s",
                                   scenario.getAbsolutePath()));
            return false;
        }

        File[] listing = logged.listFiles();
        if (listing == null) {
            LOG.warn(String.format("event log directory not found: %s",
                                   logged.getAbsolutePath()));
            return false;
        }

        if (!scenario.mkdir()) {
            LOG.warn(String.format("could not create scenario directory: %s",
                                   scenario.getAbsolutePath()));
            return false;
        }

        for (File src : listing) {
            String name = src.getName();
            if (src.isFile() && name.endsWith(EXTENSION)) {
                try {
                    File dest = new File(scenario, name);
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

    public boolean deleteScenario(File scenario) {
        File[] listing = scenario.listFiles();
        if (listing == null) {
            LOG.warn(String.format("scenario directory not found: %s",
                                   scenario.getAbsolutePath()));
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
        if (!scenario.delete()) {
            LOG.warn(String.format("could not delete scenario directory: %s",
                                   scenario.getAbsolutePath()));
            return false;
        }
        return true;
    }
}
