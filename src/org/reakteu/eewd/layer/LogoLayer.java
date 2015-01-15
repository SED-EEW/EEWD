/*
 * Copyright (C) 2014 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reakteu.eewd.layer;

import com.bbn.openmap.layer.OMGraphicHandlerLayer;
import com.bbn.openmap.omGraphics.OMGraphicAdapter;
import com.bbn.openmap.omGraphics.OMGraphicList;
import com.bbn.openmap.omGraphics.OMRaster;
import java.awt.MediaTracker;
import javax.swing.ImageIcon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reakteu.eewd.Application;

/**
 *
 * @author Stephan Herrnkind <herrnkind@gempa.de>
 */
public class LogoLayer extends OMGraphicHandlerLayer {

    private static final Logger LOG = LogManager.getLogger(LogoLayer.class);

    protected ImageIcon icon;

    public LogoLayer() {
        Application app = Application.getInstance();

        icon = new ImageIcon(app.getProperty(Application.PropertyLogoIcon,
                                             "data/icons/reakt.png"));
    }

    @Override
    public synchronized OMGraphicList prepare() {
        OMGraphicList list = null;
        if (icon != null && icon.getImageLoadStatus() == MediaTracker.COMPLETE) {
            OMGraphicAdapter symbol = new OMRaster(getWidth() - icon.getIconWidth() - 15,
                                                   getHeight() - icon.getIconHeight() - 50,
                                                   icon);
            list = new OMGraphicList();
            list.add(symbol);
            list.generate(getProjection(), false);
        }
        return list;
    }
}
