/*
 * Copyright (C) 2014-2015 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reakteu.eewd.data;

import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.quakeml.xmlns.bedRt.x12.Arrival;
import org.quakeml.xmlns.bedRt.x12.Event;
import org.quakeml.xmlns.bedRt.x12.EventParameters;
import org.quakeml.xmlns.bedRt.x12.EventType;
import org.quakeml.xmlns.bedRt.x12.Magnitude;
import org.quakeml.xmlns.bedRt.x12.Origin;
import org.quakeml.xmlns.bedRt.x12.Pick;
import org.quakeml.xmlns.bedRt.x12.RealQuantity;
import org.quakeml.xmlns.bedRt.x12.TimeQuantity;
import org.quakeml.xmlns.vstypes.x01.Likelihood;
import org.quakeml.xmlns.vstypes.x01.Rupturestrike;
import org.quakeml.xmlns.vstypes.x01.Rupturelength;

/**
 *
 * @author Stephan Herrnkind herrnkind at gempa dot de
 */
public class EventData {

    private static final Logger LOG = LogManager.getLogger(EventData.class);

    public class InvalidEventDataException extends Exception {

        public InvalidEventDataException() {
        }

        public InvalidEventDataException(String message) {
            super(message);
        }
    }

    public final String eventID;
    public final boolean isFakeEvent;
    public final long time;
    public final double latitude;
    public final double latitudeUncertainty;
    public final double longitude;
    public final double longitudeUncertainty;
    public final double depth;
    public final double magnitude;
    public final Float likelihood;
    public final Float ruptureStrike;
    public final Float ruptureLength;

    public EventParameters eventParameters = null;

    public EventData(String id, long time, double latitude,
                     double longitude, double depth, double magnitude) {
        this.eventID = id;
        this.isFakeEvent = false;
        this.time = time;
        this.latitude = latitude;
        this.latitudeUncertainty = 0.0;
        this.longitude = longitude;
        this.longitudeUncertainty = 0.0;
        this.depth = depth;
        this.magnitude = magnitude;
        this.likelihood = null;
        this.ruptureStrike = null;
        this.ruptureLength = null;
    }

    public EventData(EventParameters eventParameters, long offset,
                     Map<String, POI> stations)
            throws InvalidEventDataException {
        double result[] = new double[2];

        this.eventParameters = eventParameters;

        // get event
        assertOne(eventParameters.getEventArray(), "event");
        Event event = eventParameters.getEventArray(0);

        int idx = event.getPublicID().lastIndexOf('/') + 1;
        eventID = idx > 0 && idx < event.getPublicID().length()
                  ? event.getPublicID().substring(idx)
                  : event.getPublicID();

        isFakeEvent = event.getTypeArray().length == 1
                      && event.getTypeArray(0).equals(EventType.NOT_EXISTING);

        // get preferred origin
        assertOne(event.getPreferredOriginIDArray(), "preferred origin");
        String preferredOriginID = event.getPreferredOriginIDArray(0);
        Origin origin = null;
        for (Origin o : eventParameters.getOriginArray()) {
            if (o.getPublicID().equals(preferredOriginID)) {
                origin = o;
                break;
            }
        }
        if (origin == null) {
            throw new InvalidEventDataException("preferred origin with id '"
                                                + preferredOriginID + "' not found");
        }

        // read origin information
        time = getTimeQuantity(origin.getTimeArray(), "time")
               + (offset > 0 ? offset : 0);
        getRealQuantityUncertainty(result, origin.getLatitudeArray(), "latitude");
        latitude = result[0];
        latitudeUncertainty = result[1];
        getRealQuantityUncertainty(result, origin.getLongitudeArray(), "longitude");
        longitude = result[0];
        longitudeUncertainty = result[1];
        depth = getRealQuantity(origin.getDepthArray(), "depth");

        // get preferred magnitude
        assertOne(event.getPreferredMagnitudeIDArray(), "preferred magnitude");
        String preferredMagnitudeID = event.getPreferredMagnitudeIDArray(0);
        Magnitude mag = null;
        for (Magnitude m : eventParameters.getMagnitudeArray()) {
            if (m.getPublicID().equals(preferredMagnitudeID)) {
                mag = m;
                break;
            }
        }
        if (mag == null) {
            throw new InvalidEventDataException("preferred magnitude with id '"
                                                + preferredMagnitudeID + "' not found");
        }
        magnitude = getRealQuantity(mag.getMagArray(), "magnitude");

        // likelihood
        Float tmp = null;
        XmlObject[] objs = event.selectChildren(Likelihood.type.getName());
        if (objs.length > 0) {
            try {
                Likelihood l = Likelihood.Factory.parse(objs[0].xmlText());
                tmp = l.getFloatValue();
            } catch (XmlException ex) {
                LOG.warn("could not parse " + Likelihood.type.getShortJavaName());
            }
        }
        likelihood = tmp;

        // RuptureStrike
        Float tmpstrike = null;
        XmlObject[] objsstrike = event.selectChildren(Rupturestrike.type.getName());
        if (objsstrike.length > 0) {
            try {
                Rupturestrike lstrike = Rupturestrike.Factory.parse(objsstrike[0].xmlText());
                tmpstrike = lstrike.getFloatValue();
            } catch (XmlException ex) {
                LOG.warn("could not parse ruptureStrike");
            }
        }
        ruptureStrike = tmpstrike;

        // RuptureLength
        Float tmplength = null;
        XmlObject[] objslength = event.selectChildren(Rupturelength.type.getName());
        if (objslength.length > 0) {
            try {
                Rupturelength llength = Rupturelength.Factory.parse(objslength[0].xmlText());
                tmplength = llength.getFloatValue();
            } catch (XmlException ex) {
                LOG.warn("could not parse ruptureLength");
            }
        }
        ruptureLength = tmplength;

        // station information
        if (stations == null || stations.isEmpty()) {
            return;
        }
        for (POI station : stations.values()) {
            station.clearValues();
        }
        if (eventParameters.getPickArray().length == 0) {
            return;
        }

        for (Arrival a : origin.getArrivalArray()) {
            if (a.getPickIDArray().length != 1) {
                continue;
            }
            String pickID = a.getPickIDArray(0);
            for (Pick p : eventParameters.getPickArray()) {
                if (!p.getPublicID().equals(pickID) || p.getWaveformIDArray().length != 1) {
                    continue;
                }
                POI station = stations.get(p.getWaveformIDArray(0).getStationCode());
                if (station != null) {
                    station.triggered = true;
                }
                break;
            }
        }
    }

    private void assertOne(Object[] array, String name)
            throws InvalidEventDataException {
        if (array.length == 0) {
            throw new InvalidEventDataException("no " + name + " specifed");
        } else if (array.length > 1) {
            throw new InvalidEventDataException("more than one " + name + " specifed");
        }
    }

    private double getRealQuantity(RealQuantity[] array, String name)
            throws InvalidEventDataException {
        assertOne(array, name);
        RealQuantity quantity = array[0];
        if (quantity.getValueArray().length == 0) {
            throw new InvalidEventDataException("no " + name + " value specifed");
        } else if (quantity.getValueArray().length > 1) {
            throw new InvalidEventDataException("more than one " + name + " value specifed");
        }
        return quantity.getValueArray(0);
    }

    private void getRealQuantityUncertainty(double result[], RealQuantity[] array, String name)
            throws InvalidEventDataException {
        assertOne(array, name);
        RealQuantity quantity = array[0];
        if (quantity.getValueArray().length == 0) {
            throw new InvalidEventDataException("no " + name + " value specifed");
        } else if (quantity.getValueArray().length > 1) {
            throw new InvalidEventDataException("more than one " + name + " value specifed");
        }
        result[0] = quantity.getValueArray(0);
        result[1] = 0.0;

        if (quantity.getUncertaintyArray().length == 1) {
            result[1] = quantity.getUncertaintyArray(0);
        } else if (quantity.getUpperUncertaintyArray().length == 1) {
            result[1] = quantity.getUpperUncertaintyArray(0);
        }
    }

    private long getTimeQuantity(TimeQuantity[] array, String name)
            throws InvalidEventDataException {
        assertOne(array, name);
        TimeQuantity quantity = array[0];
        if (quantity.getValueArray().length == 0) {
            throw new InvalidEventDataException("no " + name + " value specifed");
        } else if (quantity.getValueArray().length > 1) {
            throw new InvalidEventDataException("more than one " + name + " value specifed");
        }
        return quantity.getValueArray(0).getTimeInMillis();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + Objects.hashCode(this.eventID);
        hash = 43 * hash + (int) (this.time ^ (this.time >>> 32));
        hash = 43 * hash + (int) (Double.doubleToLongBits(this.latitude) ^ (Double.doubleToLongBits(this.latitude) >>> 32));
        hash = 43 * hash + (int) (Double.doubleToLongBits(this.longitude) ^ (Double.doubleToLongBits(this.longitude) >>> 32));
        hash = 43 * hash + (int) (Double.doubleToLongBits(this.depth) ^ (Double.doubleToLongBits(this.depth) >>> 32));
        hash = 43 * hash + (int) (Double.doubleToLongBits(this.magnitude) ^ (Double.doubleToLongBits(this.magnitude) >>> 32));
        hash = 43 * hash + Objects.hashCode(this.likelihood);
        hash = 43 * hash + Objects.hashCode(this.ruptureStrike);
        hash = 43 * hash + Objects.hashCode(this.ruptureLength);
        hash = 43 * hash + (int) (Double.doubleToLongBits(this.latitudeUncertainty) ^ (Double.doubleToLongBits(this.latitudeUncertainty) >>> 32));
        hash = 43 * hash + (int) (Double.doubleToLongBits(this.longitudeUncertainty) ^ (Double.doubleToLongBits(this.longitudeUncertainty) >>> 32));
        hash = 43 * hash + Objects.hashCode(this.eventParameters);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EventData other = (EventData) obj;
        if (!Objects.equals(this.eventID, other.eventID)) {
            return false;
        }
        if (this.time != other.time) {
            return false;
        }
        if (Double.doubleToLongBits(this.latitude) != Double.doubleToLongBits(other.latitude)) {
            return false;
        }
        if (Double.doubleToLongBits(this.longitude) != Double.doubleToLongBits(other.longitude)) {
            return false;
        }
        if (Double.doubleToLongBits(this.depth) != Double.doubleToLongBits(other.depth)) {
            return false;
        }
        if (Double.doubleToLongBits(this.magnitude) != Double.doubleToLongBits(other.magnitude)) {
            return false;
        }
        if (!Objects.equals(this.likelihood, other.likelihood)) {
            return false;
        }
        if (!Objects.equals(this.ruptureStrike, other.ruptureStrike)) {
            return false;
        }
        if (!Objects.equals(this.ruptureLength, other.ruptureLength)) {
            return false;
        }
        if (Double.doubleToLongBits(this.latitudeUncertainty) != Double.doubleToLongBits(other.latitudeUncertainty)) {
            return false;
        }
        if (Double.doubleToLongBits(this.longitudeUncertainty) != Double.doubleToLongBits(other.longitudeUncertainty)) {
            return false;
        }
        if (!Objects.equals(this.eventParameters, other.eventParameters)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "EventData{" + "eventID=" + eventID + ", time=" + time + ", latitude=" + latitude + ", longitude=" + longitude + ", depth=" + depth + ", magnitude=" + magnitude + ", likelihood=" + likelihood+ ", ruptureStrike=" + ruptureStrike+ ", ruptureLength=" + ruptureLength + ", latitudeUncertainty=" + latitudeUncertainty + ", longitudeUncertainty=" + longitudeUncertainty + ", eventParameters=" + eventParameters + '}';
    }
}
