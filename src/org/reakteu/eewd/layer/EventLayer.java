/*
 * Copyright (C) 2014-2015 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reakteu.eewd.layer;

import com.bbn.openmap.layer.OMGraphicHandlerLayer;
import com.bbn.openmap.omGraphics.OMCircle;
import com.bbn.openmap.omGraphics.OMEllipse;
import com.bbn.openmap.omGraphics.OMGraphicAdapter;
import com.bbn.openmap.omGraphics.OMGraphicList;
import com.bbn.openmap.omGraphics.OMRaster;
import com.bbn.openmap.omGraphics.OMLine;
import com.bbn.openmap.proj.Length;
import com.bbn.openmap.proj.coords.LatLonPoint;
import java.awt.AlphaComposite;
import org.reakteu.eewd.data.EventData;
import org.reakteu.eewd.data.EventTimeListener;
import org.reakteu.eewd.utils.GeoCalc;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.MediaTracker;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import javax.swing.ImageIcon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reakteu.eewd.Application;
import org.reakteu.eewd.data.POI;
import org.reakteu.eewd.data.Shaking;
import org.reakteu.eewd.utils.RomanNumber;

/**
 *
 * @author Stephan Herrnkind herrnkind at gempa dot de
 */
public class EventLayer extends OMGraphicHandlerLayer implements EventTimeListener {

    private static final Logger LOG = LogManager.getLogger(EventLayer.class);
    public static final Paint PWavePaint = new Color(222, 222, 38);
    public static final Paint SWavePaint = new Color(222, 38, 38);

    public static final Color CLocationLine = Color.WHITE;
    public static final Color CLocationFill = Color.RED;
    public static final Color CUncertaintyLine = Color.RED;
    public static final Color CUncertaintyFill = new Color(255, 128, 128, 192);

    public static final Font FInfoHeader = new Font("Arial", Font.BOLD, 16);
    public static final Font FInfoLabel = new Font("Arial", Font.BOLD, 20);
    public static final Font FInfoRemaining = new Font("Arial", Font.BOLD, 128);
    public static final Font FInfoMag = new Font("Arial", Font.BOLD, 96);

    public static final Color CInfoBGHeader = new Color(202, 237, 255, 192);
    public static final Color CInfoBGMain = new Color(44, 139, 189, 192);
    public static final Color CInfoFGHeader = new Color(0, 45, 68);
    public static final Color CInfoFGMain = new Color(232, 220, 92);
    public static final int InfoMargin = 8;

    protected ImageIcon icon;
    protected double vp;
    protected double vs;
    protected final DateFormat df;

    protected final BufferedImage[] infoImages = new BufferedImage[2];
    protected final FontMetrics fmInfoHeader;
    protected final FontMetrics fmInfoLabel;
    protected final FontMetrics fmInfoRemaining;
    protected final FontMetrics fmInfoMag;
    protected final int infoWidth;
    protected final int infoHeight;
    protected final int infoMagX;
    protected final int infoHeaderHeight;
    protected final int infoMainHeight;

    protected EventData event = null;
    protected Long originTimeOffset = null;
    protected POI target = null;
    protected final Shaking.Type preferredShaking;

    public EventLayer() {
        Application app = Application.getInstance();

        icon = new ImageIcon(app.getProperty(Application.PropertyEventIcon,
                                             "data/icons/event.png"));
        vp = app.getProperty(Application.PropertyVP, Application.DefaultVP);
        vs = app.getProperty(Application.PropertyVS, Application.DefaultVS);

        df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));

        BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = tmp.createGraphics();
        fmInfoHeader = g.getFontMetrics(FInfoHeader);
        fmInfoLabel = g.getFontMetrics(FInfoLabel);
        fmInfoRemaining = g.getFontMetrics(FInfoRemaining);
        fmInfoMag = g.getFontMetrics(FInfoMag);

        infoMagX = InfoMargin + fmInfoMag.stringWidth("5.5  ");
        infoWidth = infoMagX + fmInfoMag.stringWidth("5.5") + InfoMargin;
        infoHeaderHeight = InfoMargin + 3 * fmInfoHeader.getHeight() + InfoMargin;
        infoMainHeight = InfoMargin + fmInfoLabel.getHeight() + fmInfoRemaining.getHeight()
                         + InfoMargin + fmInfoLabel.getHeight() + fmInfoMag.getAscent() + InfoMargin;
        infoHeight = infoHeaderHeight + infoMainHeight;
        infoImages[0] = new BufferedImage(infoWidth, infoHeight, BufferedImage.TYPE_INT_ARGB);
        infoImages[1] = new BufferedImage(infoWidth, infoHeight, BufferedImage.TYPE_INT_ARGB);

        preferredShaking = app.getShakeMapParameter();
    }

    public synchronized void setTarget(POI target) {
        if (target != this.target) {
            this.target = target;
            repaint();
        }
    }

    @Override
    public synchronized OMGraphicList prepare() {
        if (event == null) {
            return null;
        }

        OMGraphicList list = new OMGraphicList();

        // P and S wave propagation
        if (originTimeOffset != null && originTimeOffset > 0) {
            double hrp = vp * originTimeOffset;
            double hrs = vs * originTimeOffset;
            double rp = GeoCalc.SeismicWaveSurfaceDistance(event.depth, hrp, event.latitude);
            double rs = GeoCalc.SeismicWaveSurfaceDistance(event.depth, hrs, event.latitude);

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

        // epi center
        OMGraphicAdapter symbol;
        if (icon != null && icon.getImageLoadStatus() == MediaTracker.COMPLETE) {
            symbol = new OMRaster(event.latitude, event.longitude,
                                  -(int) ((icon.getIconWidth() + 0.5) / 2.0),
                                  -(int) ((icon.getIconHeight() + 0.5) / 2.0),
                                  icon);
        } else {
            symbol = new OMCircle(event.latitude, event.longitude, 15, 15);
            symbol.setLinePaint(CLocationLine);
            symbol.setFillPaint(CLocationFill);
        }

        list.add(symbol);
        
        // source line
        if (event.ruptureLength != null && event.ruptureStrike != null) {
            double strikeRad = Math.toRadians(event.ruptureStrike);
            double cosstrike = Math.cos(strikeRad);
            double sinstrike = Math.sin(strikeRad);
            symbol = new OMLine(event.latitude - event.ruptureLength / 220.0 * cosstrike, 
                                event.longitude - event.ruptureLength / 220.0 * sinstrike,
                                event.latitude + event.ruptureLength / 220.0 * cosstrike, 
                                event.longitude + event.ruptureLength / 220.0 * sinstrike,
                                OMLine.LINETYPE_STRAIGHT);
            symbol.setLinePaint(CLocationLine);
            //symbol.setLinePaint(Color.BLACK);
            symbol.setStroke(new BasicStroke(3));
            list.add(symbol);
        }

        if (event.latitudeUncertainty > 0.0 && event.longitudeUncertainty > 0.0) {
            symbol = new OMEllipse(new LatLonPoint.Double(event.latitude, event.longitude),
                                   event.longitudeUncertainty, event.latitudeUncertainty,
                                   Length.KM, 0);
            symbol.setLinePaint(CUncertaintyLine);
            symbol.setFillPaint(CUncertaintyFill);
            list.add(symbol);
        }

        // information layer
        String ot = String.format("%s ", df.format(event.time));
        if (originTimeOffset != null) {
            ot += String.format(" (%ds ago)", (int) (originTimeOffset / 1000.0));
        }

        String remaining = "-";
        String distance = "-";
        String shaking = "-";
        if (target != null) {
            if (originTimeOffset != null) {
                double[] pEvent = GeoCalc.Geo2Cart(event.latitude, event.longitude, -event.depth);
                double[] pTarget = GeoCalc.Geo2Cart(target.latitude, target.longitude, target.altitude);
                double d = GeoCalc.Distance3D(pEvent, pTarget);
                double eta = d / vs - originTimeOffset;
                remaining = String.format("%d", (int) (eta / 1000.0));
                distance = String.format("distance: %dkm", (int) (d / 1000.0));
            }
            Shaking s = target.shakingValues.get(preferredShaking);
            if (s != null) {
                if (preferredShaking == Shaking.Type.Intensity) {
                    shaking = RomanNumber.toString((int) (s.expectedSI + 0.5));
                } else if (preferredShaking == Shaking.Type.PGA
                           || preferredShaking == Shaking.Type.PSA) {
                    shaking = String.format("%.1f", s.expectedSI * Application.EarthAcceleration1);
                } else if (preferredShaking == Shaking.Type.PGV
                           || preferredShaking == Shaking.Type.DRS) {
                    shaking = String.format("%.1f", s.expectedSI * 100);
                }
            }
        }

        int y = 0;
        infoImages[0].setData(infoImages[1].getRaster());
        Graphics2D g = infoImages[0].createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // info header
        g.setColor(CInfoBGHeader);
        g.fillRect(0, y, infoWidth, infoHeaderHeight);
        g.setColor(CInfoFGHeader);
        g.setFont(FInfoHeader);
        g.drawString(event.eventID, InfoMargin, y += InfoMargin + fmInfoHeader.getAscent());
        g.drawString(ot, InfoMargin, y += fmInfoHeader.getHeight());
        g.drawString(distance, InfoMargin, y += fmInfoHeader.getHeight());

        // info main
        g.translate(0, infoHeaderHeight);
        y = 0;
        g.setPaint(CInfoBGMain);
        g.fillRect(0, y, infoWidth, infoMainHeight);
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(CInfoFGMain);
        g.setFont(FInfoLabel);
        g.drawString("Remaining time", InfoMargin, y += InfoMargin + fmInfoLabel.getAscent());
        g.setFont(FInfoRemaining);
        g.drawString(remaining, InfoMargin, y += fmInfoLabel.getDescent() + fmInfoRemaining.getAscent());
        g.setFont(FInfoLabel);
        g.drawString(preferredShaking.labelString(), InfoMargin,
                     y += fmInfoRemaining.getDescent() + fmInfoLabel.getAscent());
        g.drawString("Magnitude", infoMagX, y);
        g.setFont(FInfoMag);
        g.drawString(shaking, InfoMargin, y += fmInfoLabel.getDescent() + fmInfoMag.getAscent());
        g.drawString(String.format("%.1f", event.magnitude), infoMagX, y);
        g.dispose();

        OMRaster infoRaster = new OMRaster(50, 15, infoImages[0]);

        infoRaster.generate(getProjection());
        list.add(infoRaster);

        list.generate(getProjection(), false);
        return list;
    }

    @Override
    public synchronized void processEventTime(EventData event, Long originTimeOffset) {
        //LOG.info("EventLayer: " + timeOffset + "ms, " + event.toString());
        this.event = event;
        this.originTimeOffset = originTimeOffset;
        doPrepare();
        //repaint();
    }
}
