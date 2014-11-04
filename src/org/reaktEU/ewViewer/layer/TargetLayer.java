/*
 * Copyright (C) 2014 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reaktEU.ewViewer.layer;

import org.reaktEU.ewViewer.data.POI;
import com.bbn.openmap.event.MapMouseListener;
import com.bbn.openmap.event.SelectMouseMode;
import com.bbn.openmap.layer.OMGraphicHandlerLayer;
import com.bbn.openmap.omGraphics.OMCircle;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.omGraphics.OMGraphicAdapter;
import com.bbn.openmap.omGraphics.OMGraphicList;
import com.bbn.openmap.omGraphics.OMRaster;
import com.bbn.openmap.omGraphics.OMText;
import com.bbn.openmap.proj.Length;
import com.bbn.openmap.util.PaletteHelper;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Stephan Herrnkind <herrnkind@gempa.de>
 */
public class TargetLayer extends OMGraphicHandlerLayer implements MapMouseListener {

    private static final Logger LOG = LogManager.getLogger(TargetLayer.class);

    protected List<POI> targets = null;
    protected ImageIcon icon = null;
    protected boolean showingInfoLine = false;

    protected boolean showNames = false;
    protected boolean showBlindZone = true;
    protected double blindZoneRadius;

    protected int size;

    protected Box paletteBox = null;
    protected JCheckBox showNamesButton = null;
    protected JCheckBox showBlindZoneButton = null;

    public static final Paint BlindZoneFillPaint = new Color(0, 128, 255, 64);
    public static final Paint BlindZoneLinePaint = new Color(0, 128, 255);

    public static final String ShowNamesProperty = "showNames";
    public static final String ShowBlindZoneProperty = "showBlindZone";
    public static final String BlindZoneRadiusProperty = "blindZoneRadius";

    public TargetLayer(List<POI> targets) {
        this.targets = targets;
        icon = new ImageIcon(getClass().getResource("/org/reaktEU/ewViewer/resources/icons/target.png"));

        setBlindZone(40.0);
    }

    /**
     *
     * @param blindZoneRadius radius in km a target can not be alerted of an
     * approaching S-wave
     */
    public void setBlindZone(double blindZoneRadius) {
        this.blindZoneRadius = blindZoneRadius;
    }

    public synchronized OMGraphicList prepare() {

        OMGraphicList list = new OMGraphicList();

        Font f = java.awt.Font.decode("SansSerif Bold");

        for (POI target : targets) {
            OMGraphicList group = new OMGraphicList();

            // symbol
            int halfHeight;
            OMGraphicAdapter symbol;
            if (icon != null) {
                halfHeight = (int) ((icon.getIconHeight() + 0.5) / 2.0);
                symbol = new OMRaster(target.latitude, target.longitude,
                                      -(int) ((icon.getIconWidth() + 0.5) / 2.0),
                                      -halfHeight, icon);
            } else {
                halfHeight = 15;
                symbol = new OMCircle(target.latitude, target.longitude,
                                      halfHeight, halfHeight);
                symbol.setLinePaint(Color.BLACK);
                symbol.setFillPaint(Color.WHITE);
            }
            symbol.setAppObject(target);
            group.add(symbol);

            // text
            if (showNames) {
                OMText text = new OMText(target.latitude, target.longitude,
                                         0, -halfHeight - 5, target.name,
                                         f, OMText.JUSTIFY_CENTER);
                group.add(text);
            }

            // blind zone
            if (showBlindZone) {
                OMCircle blindZone = new OMCircle(target.latitude,
                                                  target.longitude,
                                                  blindZoneRadius, Length.KM);
                blindZone.setLinePaint(BlindZoneLinePaint);
                blindZone.setFillPaint(BlindZoneFillPaint);
                group.add(blindZone);
            }

            list.add(group);
        }

        list.generate(getProjection(), false);
        return list;
    }

    @Override
    public MapMouseListener getMapMouseListener() {
        return this;
    }

    @Override
    public String[] getMouseModeServiceList() {
        return new String[]{
            SelectMouseMode.modeID
        };
    }

    @Override
    public boolean mousePressed(MouseEvent e) {
        return false;
    }

    @Override
    public boolean mouseReleased(MouseEvent e) {
        OMGraphicList list = getList();
        if (list == null) {
            return false;
        }

        OMGraphic obj = list.findClosest(e.getX(), e.getY(), 4);
        if (obj == null) {
            return false;
        }
        Object appObj = obj.getAppObject();
        if (appObj != null && appObj instanceof POI) {
            fireRequestInfoLine(((POI) appObj).name);
            showingInfoLine = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseEvent e) {
        return false;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public boolean mouseDragged(MouseEvent e) {
        return false;
    }

    @Override
    public boolean mouseMoved(MouseEvent e) {
        // clean up display
        if (showingInfoLine) {
            showingInfoLine = false;
            fireRequestInfoLine("");
        }
        return false;
    }

    @Override
    public void mouseMoved() {
    }

    @Override
    public java.awt.Component getGUI() {
        if (paletteBox == null) {
            paletteBox = Box.createVerticalBox();

            JPanel layerPanel = PaletteHelper.createPaletteJPanel("Station Layer Options");

            ActionListener al = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String ac = e.getActionCommand();

                    if (ac.equalsIgnoreCase(ShowNamesProperty)) {
                        JCheckBox jcb = (JCheckBox) e.getSource();
                        showNames = jcb.isSelected();
                    } else if (ac.equalsIgnoreCase(ShowBlindZoneProperty)) {
                        JCheckBox jcb = (JCheckBox) e.getSource();
                        showBlindZone = jcb.isSelected();
                    }
                }
            };

            showNamesButton = new JCheckBox("Show Station Names");
            showNamesButton.addActionListener(al);
            showNamesButton.setActionCommand(ShowNamesProperty);

            showBlindZoneButton = new JCheckBox("Show Blind Zone");
            showBlindZoneButton.addActionListener(al);
            showBlindZoneButton.setActionCommand(ShowBlindZoneProperty);

            layerPanel.add(showNamesButton);
            paletteBox.add(layerPanel);
        }
        return paletteBox;
    }
}
