/*
 * Copyright (C) 2014 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reakteu.eewd.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.quakeml.xmlns.bedRt.x12.EventParameters;
import org.quakeml.xmlns.quakemlRt.x12.QuakemlDocument;

/**
 *
 * @author Stephan Herrnkind <herrnkind@gempa.de>
 */
public class EventFile {

    private static final Logger LOG = LogManager.getLogger(EventFile.class);

    protected long time; // milliseconds since epoch or relative delay for scenarios
    protected File file;
    protected EventParameters eventParameters = null;

    public EventFile(long time, File file) {
        this.time = time;
        this.file = file;
    }

    public long getTime() {
        return time;
    }

    public EventParameters getEventParameters() {
        if (eventParameters == null && file != null) {
            try {
                XmlOptions xmlOptions = new XmlOptions();
                List<XmlError> xmlErrors = null;
                if (LOG.isEnabled(Level.DEBUG)) {
                    xmlErrors = new ArrayList();
                    xmlOptions.setErrorListener(xmlErrors);
                    xmlOptions.setLoadLineNumbers();
                }
                QuakemlDocument qmlDoc = QuakemlDocument.Factory.parse(file, xmlOptions);
                if (qmlDoc.validate(xmlOptions)) {
                    eventParameters = qmlDoc.getQuakeml().getEventParameters();
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

            } catch (XmlException | IOException ex) {
                LOG.error(ex);
            }
        }

        return eventParameters;
    }
}
