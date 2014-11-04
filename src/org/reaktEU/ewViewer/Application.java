/*
 * Copyright (C) 2014 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reaktEU.ewViewer;

import org.reaktEU.ewViewer.layer.TargetLayer;
import org.reaktEU.ewViewer.layer.StationLayer;
import org.reaktEU.ewViewer.data.POI;
import com.bbn.openmap.LayerHandler;
import java.awt.Component;

import javax.swing.JMenuBar;

import com.bbn.openmap.MapHandler;
import com.bbn.openmap.PropertyHandler;
import com.bbn.openmap.gui.FileMenu;
import com.bbn.openmap.gui.MapPanel;
import com.bbn.openmap.gui.OpenMapFrame;
import com.bbn.openmap.gui.OverlayMapPanel;
import com.bbn.openmap.gui.ToolPanel;
import com.bbn.openmap.util.ArgParser;
import com.bbn.openmap.util.Debug;
import org.reaktEU.ewViewer.data.EventArchive;
import org.reaktEU.ewViewer.data.EventData;
import org.reaktEU.ewViewer.data.QMLListener;
import org.reaktEU.ewViewer.data.EventTimeScheduler;
import org.reaktEU.ewViewer.data.EventFileScheduler;
import org.reaktEU.ewViewer.layer.EventLayer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.security.auth.login.LoginException;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import net.ser1.stomp.Client;
import net.ser1.stomp.Listener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quakeml.xmlns.bedRt.x12.EventParameters;

// TODO:
// int cores = Runtime.getRuntime().availableProcessors();
//
// javax.swing.SwingUtilities.isEventDispatchThread.
//
// System.getProperty("user.home")
public class Application implements Listener, QMLListener, ActionListener {

    private static final Logger LOG = LogManager.getLogger(Application.class);

    private static final String ActionEventBrowser = "eventBrowser";

    private static final String PropertyEventArchive = "eventArchive";

    // gui components
    private MapPanel mapPanel;
    private OpenMapFrame openMapFrame = null;
    private EventPanel eventPanel = null;
    private EventLayer eventLayer = null;
    private EventBrowser eventBrowser = null;

    // data handling
    private Client client = null;
    private EventArchive eventArchive = null;
    private EventTimeScheduler eventTimeScheduler = null;
    private EventFileScheduler eventFileScheduler = null;
    private List<POI> targets = null;
    private List<POI> stations = null;

    public Application() {
        this((PropertyHandler) null);
    }

    public Application(PropertyHandler propertyHandler) {
        // search event archive directory
        String eventDir = null;
        if (propertyHandler != null) {
            Properties p = propertyHandler.getProperties();
            eventDir = p.getProperty(PropertyEventArchive);
        }
//        if (eventDir == null) {
//            String home = System.getProperty("user.home");
//            if (home != null) {
//                eventDir = home.concat("/.eewd/events");
//            }
//        }
//        if (eventDir != null) {
//            eventArchive = new EventArchive(eventDir);
//        }
        eventArchive = new EventArchive("data/events");

        // regular updates of ongoing event
        eventTimeScheduler = new EventTimeScheduler();

        // scenario scheduler (sequence of event updates)
        eventFileScheduler = new EventFileScheduler();
        eventFileScheduler.addQMLListener(this);

        // read targets and stations // read targets and stations
        stations = readPOIs("data/stations.csv");
        targets = readPOIs("data/targets.csv");

        // configure gui components
        configureMapPanel(propertyHandler);

        // Schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                showInFrame();
            }
        });
    }

    public EventArchive getEventArchive() {
        return eventArchive;
    }

    public EventFileScheduler getEventFileScheduler() {
        return eventFileScheduler;
    }

    /**
     * A method that lets you control what gets added to the application
     * programmatically. These components are required for handling an
     * OMEventHandler, which would be added to the MapHandler. If you wanted to
     * use the standard OpenMap application, you could add these components to
     * the MapHandler, instead.
     *
     * @param propertyHandler
     */
    protected void configureMapPanel(PropertyHandler propertyHandler) {
        OverlayMapPanel overlayMapPanel = new OverlayMapPanel(propertyHandler, true);
        overlayMapPanel.create();
        this.mapPanel = overlayMapPanel;

        // initialize map components
        MapHandler mapHandler = mapPanel.getMapHandler();

        MainPanel mainPanel = new MainPanel();
        mapHandler.add(mainPanel);

        eventPanel = new EventPanel(targets);

        ToolPanel toolPanel = (ToolPanel) mapHandler.get(ToolPanel.class);
        if (toolPanel == null) {
            toolPanel = new ToolPanel();
            mapHandler.add(toolPanel);
        }

        mapHandler.add(eventPanel);
        mainPanel.getSlider().setLeftComponent(eventPanel);

        LayerHandler layerHandler = (LayerHandler) mapHandler.get(LayerHandler.class);
        if (layerHandler != null) {
            StationLayer stationLayer = new StationLayer(stations);
            stationLayer.setName("Stations");
            layerHandler.addLayer(stationLayer, 0);

            TargetLayer targetLayer = new TargetLayer(targets);
            targetLayer.setName("Targets");
            layerHandler.addLayer(targetLayer, 0);

            eventLayer = new EventLayer();
            eventLayer.setName("Event");
            layerHandler.addLayer(eventLayer, 0);
        }

        eventTimeScheduler.addUpdateListener(eventLayer);
        eventTimeScheduler.addUpdateListener(eventPanel);
    }

    protected void showInFrame() {
        openMapFrame = (OpenMapFrame) mapPanel.getMapHandler().get(OpenMapFrame.class);

        if (openMapFrame == null) {
            openMapFrame = new OpenMapFrame() {
                @Override
                public void considerForContent(Object someObj) {
                    if (someObj instanceof MainPanel) {
                        setContent((Component) someObj);
                    }

                    if (someObj instanceof MapPanel) {
                        JMenuBar menuBar = ((MapPanel) someObj).getMapMenuBar();
                        if (menuBar != null) {
                            getRootPane().setJMenuBar(menuBar);
                            addCustomMenuItems(menuBar);
                        }
                    }
                }
            };
            openMapFrame.setTitle("Earthquake Early Warning Display");
            mapPanel.getMapHandler().add(openMapFrame);
        }

        openMapFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        openMapFrame.setVisible(true);
        mapPanel.getMapBean().showLayerPalettes();
        Debug.message("basic", "OpenMap: READY");
    }

    private void addCustomMenuItems(JMenuBar menuBar) {
        JMenu fileMenu = null;
        for (int i = 0; i < menuBar.getMenuCount(); ++i) {
            if (menuBar.getMenu(i).getClass() == FileMenu.class) {
                fileMenu = menuBar.getMenu(i);
            }
        }
        if (fileMenu == null) {
            fileMenu = new JMenu("File");
            fileMenu.setMnemonic('F');
        }

        JMenuItem eventBrowserMI = new JMenuItem("Event Browser...");
        eventBrowserMI.setMnemonic('B');
        eventBrowserMI.addActionListener(this);
        eventBrowserMI.setActionCommand(ActionEventBrowser);
        eventBrowserMI.setEnabled(eventArchive != null);
        fileMenu.add(eventBrowserMI, 0);
    }

    private List<POI> readPOIs(String fileName) {
        List<POI> pois = new ArrayList();
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(fileName));
            String line;
            String[] parts;
            while ((line = br.readLine()) != null) {
                parts = line.split(",", 4);
                if (parts.length != 4) {
                    continue;
                }
                pois.add(new POI(
                        parts[3], // name
                        Double.parseDouble(parts[1]), // latitude
                        Double.parseDouble(parts[0]), // longitude
                        Double.parseDouble(parts[2]))); // altitude

            }
            br.close();
        } catch (IOException ioe) {
            LOG.error(String.format("could not read POI file '%s'", fileName), ioe);
        }
        return pois;
    }

    public void listen() {

        try {
            client = new Client("sc3vsd.ethz.ch", 61618, "gempa", "GempaIsOK");
            client.addErrorListener(this);
            client.subscribe("/topic/eewd", this);

            //c.unsubscribe("eewd", this);
            //c.disconnect();
        } catch (IOException ex) {
            LOG.error(ex);
        } catch (LoginException ex) {
            LOG.error(ex);
        }
    }

    @Override
    public void message(Map headers, String body) {
        System.out.println(body);
    }

    @Override
    public void processQML(EventParameters eventParameters, long offset) {
        if (eventParameters == null) {
            return;
        }
        try {
            EventData event = new EventData(eventParameters);
            if (offset > 0) {
                long t1 = event.time;
                long t2 = event.time + offset;
                event.time = event.time + offset;
                LOG.debug(t1 + ", " + offset + ", " + t2 + ", " + event.time);
            }
            eventTimeScheduler.setEvent(event);
            LOG.trace(event.toString());
        } catch (EventData.InvalidEventDataException ex) {
            LOG.warn(ex.getMessage());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(ActionEventBrowser)) {
            if (eventBrowser == null) {
                eventBrowser = new EventBrowser(this, openMapFrame, true);
            }
            eventBrowser.setVisible(true);
        }
    }

    /**
     * The main OpenMap application.
     *
     * @param args
     */
    static public void main(String args[]) {
        Debug.init();
        ArgParser ap = new ArgParser("EEWD");
        ap.add("properties",
               "A resource, file path or URL to properties file\n Ex: http://myhost.com/xyz.props or file:/myhome/abc.pro\n See Java Documentation for java.net.URL class for more details",
               1);

        ap.parse(args);

        String propArgs = null;
        String[] arg = ap.getArgValues("properties");
        if (arg != null) {
            propArgs = arg[0];
        }

        PropertyHandler propertyHandler = null;
        try {
            propertyHandler = new PropertyHandler.Builder().setPropertiesFile(propArgs).setPropertyPrefix("main").build();
        } catch (MalformedURLException murle) {
            LOG.warn(murle.getMessage(), murle);
        } catch (IOException ioe) {
            LOG.warn(ioe.getMessage(), ioe);
        }
        if (propertyHandler == null) {
            propertyHandler = new PropertyHandler();
        }
        Application app = new Application(propertyHandler);

        //app.listen();
    }

}
