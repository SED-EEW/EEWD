/*
 * Copyright (C) 2014 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reaktEU.ewViewer.layer;

import com.bbn.openmap.layer.OMGraphicHandlerLayer;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.omGraphics.OMGraphicList;
import com.bbn.openmap.omGraphics.OMScalingRaster;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.util.Debug;
import java.awt.Image;
import java.awt.event.ActionListener;
import javax.swing.Timer;

/**
 *
 * @author Stephan Herrnkind <herrnkind@gempa.de>
 */
public class GroundMotionLayer extends OMGraphicHandlerLayer implements
        ActionListener {

    public static final String GROUNDMOTION = "groundmotion";
    public static final String GROUNDMOTIONLAYER = GroundMotionLayer.class.getSimpleName();

    private Timer timer = null;
    private OMScalingRaster img = null;

    /**
     * Handle an ActionEvent from the Timer.
     *
     * @param ae action event from the timer.
     */
    public void actionPerformed(java.awt.event.ActionEvent ae) {
        super.actionPerformed(ae);
        if (Debug.debugging(GROUNDMOTION)) {
            Debug.output(getName() + "| updating image via timer...");
        }
        doPrepare();
    }

    public void setImage(Image src, double ullat, double ullon,
                         double lrlat, double lrlon) {
        img = new OMScalingRaster(ullat, ullon, lrlat, lrlon, src);
    }

    protected OMGraphic createImage(Projection projection) {
        if (Debug.debugging(GROUNDMOTION)) {
            Debug.output(GROUNDMOTIONLAYER + ": creating image");
        }

        if (img != null) {
            img.generate(projection);
        }

        return img;
    }

    public synchronized OMGraphicList prepare() {

        OMGraphicList list = getList();
        if (list == null) {
            list = new OMGraphicList();
        } else {
            list.clear();
        }

        if (isCancelled()) {
            return null;
        }

        OMGraphic ras = createImage(getProjection());
        list.add(ras);

        return list;
    }

    /**
     * Get the timer being used for automatic updates. May be null if a timer is
     * not set.
     *
     * @return timer
     */
    public Timer getTimer() {
        return timer;
    }

    /**
     * If you want the layer to update itself at certain intervals, you can set
     * the timer to do that. Set it to null to disable it.
     *
     * @param t timer to set
     */
    public void setTimer(Timer t) {
        timer = t;
    }

}
