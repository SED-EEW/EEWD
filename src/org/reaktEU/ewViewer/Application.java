/*
 * Copyright (C) 2014 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reaktEU.ewViewer;

import org.reaktEU.ewViewer.layer.TargetLayer;
import org.reaktEU.ewViewer.layer.StationLayer;
import org.reaktEU.ewViewer.data.POI;
import com.bbn.openmap.LayerHandler;
import com.bbn.openmap.MapBean;
import java.awt.Component;

import javax.swing.JMenuBar;

import com.bbn.openmap.MapHandler;
import com.bbn.openmap.PropertyHandler;
import com.bbn.openmap.gui.BasicMapPanel;
import com.bbn.openmap.gui.FileMenu;
import com.bbn.openmap.gui.MapPanel;
import com.bbn.openmap.gui.OpenMapFrame;
import com.bbn.openmap.gui.OverlayMapPanel;
import com.bbn.openmap.gui.ToolPanel;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.util.ArgParser;
import com.bbn.openmap.util.Debug;
import java.awt.Point;
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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quakeml.xmlns.bedRt.x12.EventParameters;
import org.reaktEU.ewViewer.data.Shaking;
import org.reaktEU.ewViewer.data.ShakingCalculator;
import org.reaktEU.ewViewer.layer.LogoLayer;
import org.reaktEU.ewViewer.layer.ShakeMapLayer;

// TODO:
// int cores = Runtime.getRuntime().availableProcessors();
//
// javax.swing.SwingUtilities.isEventDispatchThread
public class Application implements QMLListener, ActionListener {

    private static final Logger LOG = LogManager.getLogger(Application.class);

    public static final String PropertyMapProperties = "mapProperties";
    public static final String PropertyEventArchive = "eventArchive";

    // target
    public static final String PropertyTargetFile = "targetFile";
    public static final String PropertyTargetIcon = "targetIcon";
    public static final String PropertyShowTargetName = "showTargetName";
    public static final String PropertyBlindZoneRadius = "blindZoneRadius";

    // station
    public static final String PropertyStationFile = "stationFile";
    public static final String PropertyShowStations = "showStations";
    public static final String PropertyShowStationName = "showStationName";
    public static final String PropertyShowUsedStations = "showUsedStations";
    public static final String PropertyShowStationShaking = "showStationShaking";
    public static final String PropertyShowStationAlert = "showStationAlert";

    // logo
    public static final String PropertyLogoIcon = "logoIcon";

    // shake map
    public static final String PropertySM = "shakeMap";
    public static final String PropertySMFile = PropertySM + ".file";
    public static final String PropertySMParameter = PropertySM + ".parameter";
    public static final String PropertySMMinValue = PropertySM + ".minValue";
    public static final String PropertySMMaxValue = PropertySM + ".maxValue";
    public static final String PropertySMLogScale = PropertySM + ".logScale";

    // event
    public static final String PropertyVP = "vp";
    public static final String PropertyVS = "vs";
    public static final String PropertyEventIcon = "eventIcon";
    public static final String PropertyTimeoutAfterOriginTime = "timeoutAfterOriginTime";
    public static final String PropertyAlertSound = "alertSound";
    public static final String PropertyAlertSoundLoop = "alertSoundLoop";

    // filter
    public static final String PropertyFilter = "filter";
    public static final String PropertyFilterMinMag = PropertyFilter + ".minimumMagnitude";
    public static final String PropertyFilterMinLikelihood = PropertyFilter + ".minimumLikelihood";

    // processing
    public static final String PropertyAmpliProxyName = "ampliProxyName";

    public static final String PropertyGMPE = "gmpe";
    public static final String PropertyGMICE = "gmice";

    public static final String PropertyControlPeriod = "controlPeriod";

    // spectrum plot
    public static final String PropertySpec = "spectrum";
    public static final String PropertySpecPeriods = PropertySpec + ".periods";
    public static final String PropertySpecLogScale = PropertySpec + ".logScale";
    public static final String PropertySpecParameter = PropertySpec + ".parameter";
    public static final String PropertySpecRef1 = PropertySpec + ".reference1";
    public static final String PropertySpecRef2 = PropertySpec + ".reference2";

    public static final String PropertyUseFrequencies = "useFrequencies";
    public static final String PropertyRIsHypocentral = "rIsHypocentral";

    public static final String PropertyRadiusOfInfluence = "radiusOfInfluence";
    public static final String PropertyStationDisplacementThreshold = "stationDisplacementThreshold";
    public static final String PropertyStationTauCThreshold = "stationTauCThreshold";

    // messaging
    public static final String PropertyConHost = "connection.host";
    public static final String PropertyConPort = "connection.port";
    public static final String PropertyConTopic = "connection.topic";
    public static final String PropertyConUsername = "connection.username";
    public static final String PropertyConPassword = "connection.password";

    public static final double EarthAcceleration = 9.807;
    public static final double EarthAcceleration1 = 1 / EarthAcceleration;

    public static final double DefaultVP = 5.5;
    public static final double DefaultVS = 3.3;

    private static final String ActionEventBrowser = "eventBrowser";

    private static Application instance = null;

    // gui components
    private BasicMapPanel mapPanel;
    private OpenMapFrame openMapFrame = null;
    private EventPanel eventPanel = null;
    private EventLayer eventLayer = null;
    private ShakeMapLayer shakeMapLayer = null;
    private EventBrowser eventBrowser = null;

    private Properties properties;
    private final EventArchive eventArchive;
    private final EventTimeScheduler eventTimeScheduler;
    private final EventFileScheduler eventFileScheduler;
    private final List<POI> targets;
    private final List<POI> stations;

    private final ShakingCalculator shakingCalculator;

    private final Messaging messaging;

    private Double controlPeriod = null;
    private double[] periods = null;
    private boolean useFrequencies = false;
    private Shaking.Type spectrumParameter = Shaking.Type.PSA;
    private Shaking.Type shakeMapParameter = Shaking.Type.PGA;

    private String title = null;

    private Float minMag = null;
    private Float minLikelihood = null;

    public Application(Properties props) {
        instance = this;
        properties = props;

        // set default system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            LOG.warn("could net set system default look and feel", ex);
        }

        String mapProps = properties.getProperty(PropertyMapProperties,
                                                 "file:data/openmap.properties");

        PropertyHandler mapPropertyHandler = null;
        try {
            //mapPropertyHandler = new PropertyHandler.Builder().setPropertiesFile((String) null).setPropertyPrefix("main").build();
            PropertyHandler.Builder builder = new PropertyHandler.Builder();
            builder.setPropertiesFile(mapProps);
            //builder.setPropertyPrefix("main");
            mapPropertyHandler = builder.build();
        } catch (MalformedURLException murle) {
            LOG.warn(murle.getMessage(), murle);
        } catch (IOException ioe) {
            LOG.warn(ioe.getMessage(), ioe);
        }
        if (mapPropertyHandler == null) {
            mapPropertyHandler = new PropertyHandler();
        }

        // search event archive directory
        eventArchive = new EventArchive();

        // regular updates of ongoing event
        eventTimeScheduler = new EventTimeScheduler();

        // scenario scheduler (sequence of event updates)
        eventFileScheduler = new EventFileScheduler();
        eventFileScheduler.addQMLListener(this);

        // read targets and stations // read targets and stations
        stations = readPOIs(properties.getProperty(PropertyStationFile,
                                                   "data/stations.csv"));
        targets = readPOIs(properties.getProperty(PropertyTargetFile,
                                                  "data/targets.csv"));

        shakeMapLayer = new ShakeMapLayer();
        shakeMapLayer.setName("Shake Map");

        controlPeriod = getProperty(PropertyControlPeriod, (Double) null);
        periods = getProperty(PropertySpecPeriods, (double[]) null);
        useFrequencies = getProperty(PropertyUseFrequencies, false);
        Arrays.sort(periods);

        minMag = getProperty(PropertyFilterMinMag, (Float) null);
        minLikelihood = getProperty(PropertyFilterMinLikelihood, (Float) null);

        // read spectrum parameter
        String param = properties.getProperty(Application.PropertySpecParameter);
        if (param != null) {
            spectrumParameter = Shaking.Type.valueOf(param);
            if (spectrumParameter == null) {
                LOG.warn("invalid " + Application.PropertySpecParameter + " value: " + param);
            }
        }

        // read shake map parameter
        param = properties.getProperty(Application.PropertySMParameter);
        if (param != null) {
            shakeMapParameter = Shaking.Type.valueOf(param);
            if (shakeMapParameter == null) {
                LOG.warn("invalid " + Application.PropertySMParameter + " value: " + param);
            }
        }

        shakingCalculator = new ShakingCalculator(targets, stations, shakeMapLayer);

        title = mapPropertyHandler.getProperties().getProperty("openmap.Title");
        configureMapPanel(mapPropertyHandler);

        messaging = new Messaging();

        // Schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                showInFrame();
            }
        });

        messaging.listen();
    }

    public Double getControlPeriod() {
        return controlPeriod;
    }

    public double[] getPeriods() {
        return periods;
    }

    public boolean isUseFrequencies() {
        return useFrequencies;
    }

    public Shaking.Type getSpectrumParameter() {
        return spectrumParameter;
    }

    public Shaking.Type getShakeMapParameter() {
        return shakeMapParameter;
    }

    public static final Application getInstance() {
        return instance;
    }

    public Properties getProperties() {
        return properties;
    }

    public String getProperty(String key, String def) {
        return properties.getProperty(key, def);
    }

    public final boolean getProperty(String key, boolean def) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Boolean.parseBoolean(value);
            } catch (NumberFormatException nfe) {
                LOG.warn(String.format("invalid boolean found in property: %s",
                                       key));
            }
        }
        return def;
    }

    public final int getProperty(String key, int def) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException nfe) {
                LOG.warn(String.format("invalid integer found in property: %s",
                                       key));
            }
        }
        return def;
    }

    public final double getProperty(String key, double def) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException nfe) {
                LOG.warn(String.format("invalid double found in property: %s",
                                       key));
            }
        }
        return def;
    }

    public final double[] getProperty(String key, double[] def) {
        String value = properties.getProperty(key);
        if (value != null) {
            String values[] = value.split(",");
            try {
                double[] retn = new double[values.length];
                for (int i = 0; i < values.length; ++i) {
                    retn[i] = Double.parseDouble(values[i]);
                }
                return retn;
            } catch (NumberFormatException nfe) {
                LOG.warn(String.format("invalid double found in property: %s",
                                       key));
            }
        }
        return def;
    }

    public final Double getProperty(String key, Double def) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Double.valueOf(value);
            } catch (NumberFormatException nfe) {
                LOG.warn(String.format("invalid double found in property: %s",
                                       key));
            }
        }
        return def;
    }

    public final Float getProperty(String key, Float def) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Float.valueOf(value);
            } catch (NumberFormatException nfe) {
                LOG.warn(String.format("invalid double found in property: %s",
                                       key));
            }
        }
        return def;
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
            LogoLayer logoLayer = new LogoLayer();
            logoLayer.setName("Logo");
            layerHandler.addLayer(logoLayer, 0);

            if (!shakeMapLayer.getPoints().isEmpty()) {
                layerHandler.addLayer(shakeMapLayer, 0);
            }

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

    public void setTarget(POI target) {
        if (eventLayer != null) {
            eventLayer.setTarget(target);
        }
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
            mapPanel.getMapHandler().add(openMapFrame);
            //openMapFrame.setTitle(title);
            messaging.reportConnectionState();
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
                // trim line and remove comments
                line = line.trim();
                int commentIdx = line.indexOf('#');
                if (commentIdx == 0) {
                    continue;
                } else if (commentIdx > 0) {
                    line = line.substring(0, commentIdx);
                }

                parts = line.split(",", 5);
                if (parts.length != 5) {
                    continue;
                }
                pois.add(new POI(
                        Double.parseDouble(parts[2]), // latitude
                        Double.parseDouble(parts[1]), // longitude
                        Double.parseDouble(parts[3]), // altitude
                        Double.parseDouble(parts[4]), // amplification
                        parts[0] // name
                ));

            }
            br.close();
        } catch (IOException ioe) {
            LOG.error(String.format("could not read POI file '%s'", fileName), ioe);
        }
        return pois;
    }

    @Override
    public EventData processQML(EventParameters eventParameters, long offset) {
        LOG.info("received event update");
        if (eventParameters == null) {
            return null;
        }

        EventData event;
        try {
            event = new EventData(eventParameters, offset);
        } catch (EventData.InvalidEventDataException ex) {
            LOG.warn(ex.getMessage());
            return null;
        }

        boolean disable = true;
        if (event.isFakeEvent) {
            LOG.info(String.format("event %s marked as not existing", event.eventID));
        } else if (minMag != null && event.magnitude < minMag) {
            LOG.info(String.format("ignoring event %s, magnitude of %.1f below threshold",
                                   event.eventID, event.magnitude));
        } else if (minLikelihood != null && event.likelihood == null) {
            LOG.info(String.format("ignoring event %s, likelihood not available",
                                   event.eventID));
        } else if (minLikelihood != null && event.likelihood < minLikelihood) {
            LOG.info(String.format("ignoring event %s, likelihood of %.1f below threshold",
                                   event.eventID, event.likelihood));
        } else {
            disable = false;

            // zoom out if epicenter is not visible
            zoomToPoint(event.latitude, event.longitude);

            // process shaking
            shakingCalculator.processEvent(event);
            LOG.trace(event.toString());
        }

        eventTimeScheduler.setEvent(event, disable);
        return event;
    }

    private void zoomToPoint(double latitude, double longitude) {
        MapBean mapBean = mapPanel.getMapBean();
        Projection proj = mapBean.getProjection();
        Point eventXY = (Point) proj.forward(latitude, longitude, new Point());
        if (mapBean.contains(eventXY)) {
            return;
        }

        int dx = Math.abs(eventXY.x - mapBean.getWidth() / 2);
        int dy = Math.abs(eventXY.y - mapBean.getHeight() / 2);
        float scale = proj.getScale();
        float newScale = Math.max(dx * scale / mapBean.getWidth(),
                                  dy * scale / mapBean.getHeight()) * 2.2f;
        if (newScale > proj.getScale()) {
            mapBean.setScale(newScale);
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

    public void setConnectionState(String state) {
        if (openMapFrame != null) {
            openMapFrame.setTitle(title + " [ " + state + " ]");
        }
    }

    /**
     * The main OpenMap application.
     *
     * @param args
     */
    static public void main(String args[]) {
        System.setProperty("user.timezone", "UTC");
        Locale.setDefault(Locale.ROOT);

        Debug.init();

        // read property location from command line parameter
        ArgParser ap = new ArgParser("EEWD");
        ap.add("properties",
               "A resource, file path or URL to properties file\n Ex: http://myhost.com/xyz.props or file:/myhome/abc.pro\n See Java Documentation for java.net.URL class for more details",
               1);
        ap.parse(args);

        String propURL = "file:eewd.properties";
        String[] arg = ap.getArgValues("properties");
        if (arg != null) {
            propURL = arg[0];
        }

        Properties props = null;
        try {
            URL url = new URL(propURL);
            InputStream in = url.openStream();
            props = new Properties();
            props.load(in);
        } catch (MalformedURLException mue) {
            LOG.fatal("invalid property file location", mue);
        } catch (IOException ioe) {
            LOG.fatal("could not read property file", ioe);
        }
        if (props == null) {
            System.exit(1);
        }

        Application app = new Application(props);
    }

}
