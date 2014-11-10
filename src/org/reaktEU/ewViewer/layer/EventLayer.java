/*
 * Copyright (C) 2014 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reaktEU.ewViewer.layer;

import com.bbn.openmap.layer.OMGraphicHandlerLayer;
import com.bbn.openmap.omGraphics.OMCircle;
import com.bbn.openmap.omGraphics.OMGraphicAdapter;
import com.bbn.openmap.omGraphics.OMGraphicList;
import com.bbn.openmap.omGraphics.OMRaster;
import com.bbn.openmap.proj.Length;
import org.reaktEU.ewViewer.data.EventData;
import org.reaktEU.ewViewer.data.EventTimeListener;
import org.reaktEU.ewViewer.data.GeoCalc;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.MediaTracker;
import java.awt.Paint;
import javax.swing.ImageIcon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reaktEU.ewViewer.Application;

/**
 *
 * @author Stephan Herrnkind <herrnkind@gempa.de>
 */
public class EventLayer extends OMGraphicHandlerLayer implements EventTimeListener {

    private static final Logger LOG = LogManager.getLogger(EventLayer.class);

    protected ImageIcon icon;
    protected double vp;
    protected double vs;

    protected EventData event = null;
    protected Long originTimeOffset = null;

    public static final Paint PWavePaint = new Color(222, 222, 38);
    public static final Paint SWavePaint = new Color(222, 38, 38);

    public EventLayer() {
        Application app = Application.getInstance();

        icon = new ImageIcon(app.getProperty(Application.PropertyEventIcon,
                                             "data/icons/event.png"));
        vp = app.getProperty(Application.PropertyVP, 5.5);
        vs = app.getProperty(Application.PropertyVS, 3.3);
    }

    @Override
    public synchronized OMGraphicList prepare() {
        if (event == null) {
            return null;
        }

        OMGraphicList list = new OMGraphicList();
        if (originTimeOffset != null && originTimeOffset > 0) {
            double hrp = vp * originTimeOffset;
            double hrs = vs * originTimeOffset;
            double rp = GeoCalc.SeismicWaveSurfaceDistance(event.depth * 1000, hrp, event.latitude);
            double rs = GeoCalc.SeismicWaveSurfaceDistance(event.depth * 1000, hrs, event.latitude);
            System.out.println("hrp: " + hrp + ", rp: " + rp);

            OMCircle pWave = new OMCircle(event.latitude, event.longitude,
                                          rp, Length.METER);
            pWave.setLinePaint(Color.YELLOW);
            pWave.setStroke(new BasicStroke(3));
            list.add(pWave);

            OMCircle sWave = new OMCircle(event.latitude, event.longitude,
                                          rs, Length.METER);
            sWave.setLinePaint(Color.RED);
            sWave.setStroke(new BasicStroke(3));
            list.add(sWave);
        }

        OMGraphicAdapter symbol;
        if (icon != null && icon.getImageLoadStatus() == MediaTracker.COMPLETE) {
            symbol = new OMRaster(event.latitude, event.longitude,
                                  -(int) ((icon.getIconWidth() + 0.5) / 2.0),
                                  -(int) ((icon.getIconHeight() + 0.5) / 2.0),
                                  icon);
        } else {
            symbol = new OMCircle(event.latitude, event.longitude, 15, 15);
            symbol.setLinePaint(Color.WHITE);
            symbol.setFillPaint(Color.RED);
        }
        list.add(symbol);

        list.generate(getProjection(), false);
        return list;
    }

    @Override
    public void processEventTime(EventData event, Long originTimeOffset) {
        //LOG.info("EventLayer: " + timeOffset + "ms, " + event.toString());
        this.event = event;
        this.originTimeOffset = originTimeOffset;
        doPrepare();
        //repaint();
    }
}
