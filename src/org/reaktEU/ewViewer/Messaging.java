/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.reaktEU.ewViewer;

import java.io.IOException;
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
import org.reaktEU.ewViewer.data.EventData;
import org.reakteu.heartbeat.HbDocument;

/**
 *
 * @author Stephan Herrnkind <herrnkind@gempa.de>
 */
public class Messaging implements Listener, Runnable {

    private static final Logger LOG = LogManager.getLogger(Messaging.class);

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
    }

    synchronized public void reportConnectionState() {
        String state = "offline";
        if (client != null && client.isConnected()) {
            state = topic + "@" + host;
            if (lastDelay > maxLatency) {
                state += ", slow connection detected";
            }
        } else if (shouldListen) {
            state = "connecting ...";
        }
        Application.getInstance().setConnectionState(state);
    }

    synchronized public void listen() {
        shouldListen = true;
        if (executor == null && keepaliveInterval > 0) {
            executor = Executors.newScheduledThreadPool(1);
            executor.schedule(this, keepaliveInterval / 2, TimeUnit.MILLISECONDS);
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
            reportConnectionState();
        } catch (IOException ioe) {
            LOG.error("could not connect to host " + host, ioe);
            client = null;
        } catch (LoginException le) {
            LOG.error("could not login to host" + host, le);
            client = null;
        }

    }

    synchronized public void close() {
        shouldListen = false;
        lastDelay = 0;
        if (client != null) {
            if (client.isConnected()) {
                client.disconnect();
            }
            client = null;
        }
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
                LOG.error(String.format("no keepalive signal received within %.1f seconds, trying to reconnect",
                                        keepaliveInterval / 1000f));
                close();
                listen();
            }
        }
    }

    @Override
    public void message(Map headers, String body) {
        long received = System.currentTimeMillis();
        LOG.debug("received message");
        LOG.trace(body);

        XmlOptions xmlOptions = new XmlOptions();
        List<XmlError> xmlErrors = null;
        if (LOG.isEnabled(org.apache.logging.log4j.Level.DEBUG)) {
            xmlErrors = new ArrayList();
            xmlOptions.setErrorListener(xmlErrors);
            xmlOptions.setLoadLineNumbers();
        }

        XmlObject obj = null;
        try {
            obj = XmlObject.Factory.parse(body, xmlOptions);
        } catch (XmlException ex) {
            LOG.error("could not parse XML data", ex);
            printXMLErrors(xmlErrors);
            return;
        }

        if (obj instanceof HbDocument) {
            HbDocument hbDoc = (HbDocument) obj;
            if (hbDoc.validate(xmlOptions)) {
                // TODO: evaluate sender?
                lastUpdate = received;
                lastDelay = received - hbDoc.getHb().getTimestamp().getTimeInMillis();
                reportConnectionState();
            } else {
                LOG.warn("received invalid heartbeat document");
                printXMLErrors(xmlErrors);
            }
        } else if (obj instanceof QuakemlDocument) {
            QuakemlDocument qmlDoc = (QuakemlDocument) obj;
            if (qmlDoc.validate(xmlOptions)) {
                lastUpdate = received;
                reportConnectionState();
                Application app = Application.getInstance();
                EventData event = app.processQML(qmlDoc.getQuakeml().getEventParameters(), 0);
                app.getEventArchive().log(received, body, event);
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
