/*
 * Copyright (C) 2014-2015 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reakteu.eewd;

import com.bbn.openmap.gui.MapPanel;
import com.bbn.openmap.gui.OMComponentPanel;
import com.bbn.openmap.gui.WindowSupport;
import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JSplitPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Stephan Herrnkind herrnkind at gempa dot de
 */
public class MainPanel extends OMComponentPanel {

    private static final Logger LOG = LogManager.getLogger(MainPanel.class);

    protected JSplitPane slider;
    protected Component leftComponent;

    public MainPanel() {
        WindowSupport.setDefaultWindowSupportDisplayType(WindowSupport.Dlg.class);
        setLayout(new BorderLayout());
        slider = new JSplitPane();
        slider.setBorder(null);
        slider.setResizeWeight(0);
        slider.setOneTouchExpandable(true);

        super.add(slider, BorderLayout.CENTER);
    }

    @Override
    public void findAndInit(Object obj) {
        if (obj instanceof MapPanel) {
            slider.setRightComponent((Component) obj);
        }
    }

    public JSplitPane getSlider() {
        return slider;
    }

}
