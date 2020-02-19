/*
 * Copyright (C) 2014-2015 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reakteu.eewd;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.security.auth.login.LoginException;
import net.ser1.stomp.Client;
import net.ser1.stomp.Listener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.quakeml.xmlns.quakemlRt.x12.QuakemlDocument;
import org.reakteu.eewd.data.EventData;
import org.reakteu.heartbeat.HbDocument;

/**
 *
 * @author Stephan Herrnkind herrnkind at gempa dot de
 */
public class Messaging implements Listener, Runnable {

    private static final Logger LOG = LogManager.getLogger(Messaging.class);
    private static final long DAY_MILLIS = 24 * 60 * 60 * 1000;

    private static String host;
    private static int port;
    private static String topic;
    private static String user;
    private static String password;
    private static long keepaliveInterval;
    private static long maxLatency;

    private Client client = null;
    private ScheduledExecutorService executor;
    private long lastUpdate = 0;
    private long lastDelay = 0;
    private boolean shouldListen = false;
    private final DateFormat dfTime;

    public Messaging() {
        Application app = Application.getInstance();
        host = app.getProperty(Application.PropertyConHost, (String) null);
        if (host == null) {
            LOG.error("no host specified");
        }
        topic = app.getProperty(Application.PropertyConTopic, (String) null);
        if (topic == null) {
            LOG.error("no topic specified");
        }
        port = app.getProperty(Application.PropertyConPort, 61618);
        user = app.getProperty(Application.PropertyConUsername, "");
        password = app.getProperty(Application.PropertyConPassword, "");
        keepaliveInterval = (long) (app.getProperty(Application.PropertyConKeepaliveInterval, 0.f) * 1000.f);
        maxLatency = (long) (app.getProperty(Application.PropertyConMaxLatency, (Float) 1.f) * 1000.f);
        dfTime = new SimpleDateFormat("HH:mm:ss");
    }

    synchronized public void reportConnectionState() {
        String state = "offline";
        if (client != null && client.isConnected()) {
            state = "connected to " + host;
            if (lastDelay > maxLatency) {
                state += ", slow connection detected";
            }
        } else if (shouldListen) {
            state = "connecting ...";
        }
        Application.getInstance().setConnectionState(state);
    }

    synchronized public void listen() {
        if (host == null || host.isEmpty() || topic == null || topic.isEmpty()) {
            shouldListen = false;
            reportConnectionState();
            return;
        }
        shouldListen = true;
        lastUpdate = System.currentTimeMillis();
        if (executor == null && keepaliveInterval > 0) {
            executor = Executors.newScheduledThreadPool(1);
            executor.scheduleAtFixedRate(this, 1, 1, TimeUnit.SECONDS);
        }

        try {
            client = new Client(host, port, user, password);
            client.addErrorListener(new Listener() {
                @Override
                public void message(Map headers, String body) {
                    LOG.error("received messaging error");
                    LOG.debug(body);
                }
            });
            client.subscribe(topic, this);
        } catch (IOException ioe) {
            LOG.error("could not connect to host " + host, ioe);
            client = null;
        } catch (LoginException le) {
            LOG.error("could not login to host" + host, le);
            client = null;
        }
        reportConnectionState();
    }

    synchronized private void disconnect() {
        if (client != null) {
            if (client.isConnected()) {
                client.disconnect();
            }
            client = null;
        }
    }

    synchronized public void close() {
        shouldListen = false;
        lastDelay = 0;
        disconnect();
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
        reportConnectionState();
    }

    @Override
    synchronized public void run() {
        if (!shouldListen) {
            close();
        } else {
            long now = System.currentTimeMillis();
            if (now > lastUpdate + keepaliveInterval) {
                LOG.error(String.format("no message received within %.1f seconds, trying to reconnect",
                                        keepaliveInterval / 1000f));
                disconnect();
                listen();
            }
        }
    }

    @Override
    public void message(Map headers, String body) {
        Application app = Application.getInstance();
        long received = System.currentTimeMillis();

        XmlOptions xmlOptions = new XmlOptions();
        List<XmlError> xmlErrors = null;
        if (LOG.isEnabled(org.apache.logging.log4j.Level.DEBUG)) {
            xmlErrors = new ArrayList();
            xmlOptions.setErrorListener(xmlErrors);
            xmlOptions.setLoadLineNumbers();
        }

        XmlObject obj;
        try {
            obj = XmlObject.Factory.parse(body, xmlOptions);
        } catch (XmlException ex) {
            LOG.error("could not parse received message", ex);
            printXMLErrors(xmlErrors);
            return;
        }

        if (obj instanceof HbDocument) {
            LOG.debug("received heartbeat message");
            LOG.debug(body);

            HbDocument hbDoc = (HbDocument) obj;
            if (hbDoc.validate(xmlOptions)) {
                // TODO: evaluate sender?
                lastUpdate = received;
                lastDelay = received - hbDoc.getHb().getTimestamp().getTimeInMillis();
                reportConnectionState();
                if (app != null) {
                    app.updateHeartbeat("heartbeat: " + dfTime.format(received));
                }
            } else {
                LOG.warn("received invalid heartbeat document");
                printXMLErrors(xmlErrors);
            }
        } else if (obj instanceof QuakemlDocument) {
            LOG.debug("received event message");
            LOG.trace(body);
            QuakemlDocument qmlDoc = (QuakemlDocument) obj;
            if (qmlDoc.validate(xmlOptions)) {
                lastUpdate = received;
                reportConnectionState();
                if (app != null) {
                    EventData event = app.processQML(qmlDoc.getQuakeml().getEventParameters(), 0);
                    app.getEventArchive().log(received, body, event);
                }
            } else {
                LOG.warn("received invalid QuakeML document");
                printXMLErrors(xmlErrors);
            }
        } else {
            LOG.warn("received unsupported XML document");
        }
    }

    private void printXMLErrors(List<XmlError> xmlErrors) {
        if (xmlErrors == null) {
            return;
        }

        LOG.debug(String.format("number of validation errors: %s", xmlErrors.size()));
        StringBuilder errStr = new StringBuilder();
        errStr.append("validation errors: \n    ");
        for (XmlError xmlError : xmlErrors) {
            errStr.append(String.format("line %s: %s\n", xmlError.getLine(), xmlError));
        }
        LOG.debug(errStr.toString());
    }
}
