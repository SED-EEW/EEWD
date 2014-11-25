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
import javax.security.auth.login.LoginException;
import net.ser1.stomp.Client;
import net.ser1.stomp.Listener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.quakeml.xmlns.quakemlRt.x12.QuakemlDocument;

/**
 *
 * @author Stephan Herrnkind <herrnkind@gempa.de>
 */
public class Messaging implements Listener {

    private static final Logger LOG = LogManager.getLogger(Messaging.class);

    public class ErrorHandler implements Listener {

        @Override
        public void message(Map headers, String body) {
            LOG.error("received messaging error");
            LOG.debug(body);
            reportConnectionState();
        }

    }

    // data handling
    private Client client = null;
    private String host = null;
    private String topic = null;

    public Messaging() {
        Application app = Application.getInstance();
        host = app.getProperty(Application.PropertyConHost, (String) null);
        if (host == null) {
            LOG.error("no host specified");
            return;
        }
        topic = app.getProperty(Application.PropertyConTopic, (String) null);
        if (topic == null) {
            LOG.error("no topic specified");
            return;
        }

    }

    synchronized public void reportConnectionState() {
        String state = "offline";
        if (client != null) {
            // TODO: test for heartbeat
            if (client.isConnected()) {
                state = topic + "@" + host;
            }
        }
        Application.getInstance().setConnectionState(state);
    }

    synchronized public void listen() {
        Application app = Application.getInstance();
        try {
            client = new Client(host,
                                app.getProperty(Application.PropertyConPort, 61618),
                                app.getProperty(Application.PropertyConUsername, ""),
                                app.getProperty(Application.PropertyConPassword, ""));
        } catch (IOException ioe) {
            LOG.error("could not connect to host " + host, ioe);
            return;
        } catch (LoginException le) {
            LOG.error("could not login to host" + host, le);
            return;
        }

        client.addErrorListener(new ErrorHandler());
        client.subscribe(topic, this);
    }

    @Override
    public void message(Map headers, String body) {
        LOG.info("received message");
        LOG.trace(body);

        try {

            XmlOptions xmlOptions = new XmlOptions();
            List<XmlError> xmlErrors = null;
            if (LOG.isEnabled(org.apache.logging.log4j.Level.DEBUG)) {
                xmlErrors = new ArrayList();
                xmlOptions.setErrorListener(xmlErrors);
                xmlOptions.setLoadLineNumbers();
            }
            QuakemlDocument qmlDoc = QuakemlDocument.Factory.parse(body, xmlOptions);
            if (qmlDoc.validate(xmlOptions)) {
                Application.getInstance().processQML(
                        qmlDoc.getQuakeml().getEventParameters(), 0);
            } else {
                LOG.warn("the document is not valid");
                if (xmlErrors != null) {
                    LOG.debug(String.format("number of validation errors: %s",
                                            xmlErrors.size()));
                    StringBuilder errStr = new StringBuilder();
                    errStr.append("validation errors: \n    ");
                    for (XmlError xmlError : xmlErrors) {
                        errStr.append(String.format("line %s: %s\n",
                                                    xmlError.getLine(), xmlError));
                    }
                    LOG.debug(errStr.toString());
                }
            }
        } catch (XmlException ex) {
            LOG.error(ex);
        }
    }
}
