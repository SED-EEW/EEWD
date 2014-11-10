/*
 * Copyright (C) 2014 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reaktEU.ewViewer.layer;

import org.reaktEU.ewViewer.data.POI;
import com.bbn.openmap.event.MapMouseListener;
import com.bbn.openmap.event.SelectMouseMode;
import com.bbn.openmap.layer.OMGraphicHandlerLayer;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.omGraphics.OMGraphicList;
import com.bbn.openmap.omGraphics.OMPoly;
import static com.bbn.openmap.omGraphics.OMPoly.COORDMODE_ORIGIN;
import com.bbn.openmap.omGraphics.OMText;
import com.bbn.openmap.util.PaletteHelper;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reaktEU.ewViewer.Application;

/**
 *
 * @author Stephan Herrnkind <herrnkind@gempa.de>
 */
public class StationLayer extends OMGraphicHandlerLayer implements MapMouseListener {

    private static final Logger LOG = LogManager.getLogger(StationLayer.class);

    protected List<POI> stations;
    protected boolean showingInfoLine = false;
    protected boolean showNames = false;

    protected double size;
    protected int[] xs = new int[]{0, 0, 0};
    protected int[] ys = new int[]{0, 0, 0};

    protected Box paletteBox = null;
    protected JCheckBox showNamesButton = null;

    public static final String ShowNamesProperty = "showNames";

    public StationLayer(List<POI> stations) {
        Application app = Application.getInstance();
        this.stations = stations;
        setSize(12);

        showNames = app.getProperty(Application.PropertyShowStationName, false);
        //setProjectionChangePolicy(new com.bbn.openmap.layer.policy.NullProjectionChangePolicy());
    }

    /**
     *
     * @param size outer radius of triangle
     */
    public final void setSize(double size) {
        if (size < 1.0) {
            size = 1.0;
        }
        if (this.size == size) {
            return;
        }
        this.size = size;
        xs[2] = (int) (size * 1.5 / Math.sqrt(3) + 0.5);
        xs[0] = -xs[2];
        ys[0] = ys[2] = (int) (size + 0.5);
        ys[1] = (int) (-size / 2.0 + 0.5);

    }

    public synchronized OMGraphicList prepare() {

        OMGraphicList list = new OMGraphicList();
        OMGraphicList group = null;

        OMPoly poly;
        OMText text;
        Font f = java.awt.Font.decode("SansSerif Bold");

        for (POI station : stations) {
            poly = new OMPoly(station.latitude, station.longitude,
                              xs, ys, COORDMODE_ORIGIN);
            poly.setFillPaint(Color.GREEN);
            poly.setAppObject(station);
            if (showNames) {
                group = new OMGraphicList(2);
                group.add(poly);
                text = new OMText(station.latitude, station.longitude,
                                  0, ys[1] - 5, station.name,
                                  f, OMText.JUSTIFY_CENTER
                );
                group.add(text);
                list.add(group);
            } else {
                list.add(poly);
            }
        }

        list.generate(getProjection(), false);
        return list;
    }

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
                    }
                }
            };

            showNamesButton = new JCheckBox("Show Station Names");
            showNamesButton.addActionListener(al);
            showNamesButton.setActionCommand(ShowNamesProperty);

            layerPanel.add(showNamesButton);
            paletteBox.add(layerPanel);
        }
        return paletteBox;
    }
}
