/*
 * Copyright (C) 2014 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reaktEU.ewViewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reaktEU.ewViewer.data.POI;
import org.reaktEU.ewViewer.data.Shaking;

@SuppressWarnings("serial")
public class SpectrumPlot extends JPanel {

    private static final Logger LOG = LogManager.getLogger(SpectrumPlot.class);

    private static final int PrefW = 800;
    private static final int PrefH = 650;
    private static final int BorderGap = 30;

    private static final Color BackgroundColor = Color.white;
    private static final Color Ref1Color = new Color(0, 192, 0);
    private static final Color Ref2Color = new Color(192, 0, 0);
    private static final Color ShakingColor = new Color(0, 0, 255);

    private static final Color GRAPH_POINT_COLOR = new Color(150, 50, 50, 180);
    private static final Stroke DefaultStroke = new BasicStroke(2f);
    private static final Stroke PercentileStroke
                                = new BasicStroke(1.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER,
                                                  10f, new float[]{7, 10}, 0f);
    private static final int GRAPH_POINT_WIDTH = 12;

    private final double[] periods;
    private final List<Double> reference1;
    private final List<Double> reference2;
    private final boolean logScale;
    private POI target;

    public SpectrumPlot(double[] periods) {
        this.periods = periods;
        this.reference1 = readReference(Application.PropertySpecRef1);
        this.reference2 = readReference(Application.PropertySpecRef2);
        this.logScale = Application.getInstance().getProperty(Application.PropertySpecLogScale, false);
    }

    private List<Double> readReference(String key) {
        List<Double> refs = new ArrayList();

        String[] values = Application.getInstance().getProperty(key, "").split(",");
        for (String v : values) {
            if (refs.size() >= periods.length) {
                LOG.warn("more reference values than periods specified in parameter: " + key);
                break;
            }
            refs.add(Double.parseDouble(v));
        }
        if (!refs.isEmpty() && refs.size() < periods.length) {
            LOG.warn("fewer reference values than periods specified in parameter: " + key);
        }
        return refs;
    }

    public void setTarget(POI target) {
        this.target = target;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth() - 2 * BorderGap;
        int height = getHeight() - 2 * BorderGap;

        g2.translate(BorderGap, BorderGap);

        // fill background
        g2.setColor(BackgroundColor);
        g2.fillRect(0, 0, width, height);

        // create x and y axes
        g2.setColor(Color.black);
        g2.drawLine(0, height, width, height);
        g2.drawLine(0, 0, 0, height);

        if (periods.length == 0 || target == null
            || periods[periods.length - 1] <= 0) {
            return;
        }

        double factor = Application.getInstance().getSpectrumParameter() == Shaking.Type.PSA
                        ? Application.EarthAcceleration1 : 100;

        synchronized (target) {

            // determine min/max of x and y axis
            double xMin = 0;
            double xMax = 0;
            if (logScale) {
                xMin = Math.floor(Math.log10(periods[0]));
                xMax = Math.ceil(Math.log10(periods[periods.length - 1]));
            } else {
                xMin = periods[0];
                xMax = periods[periods.length - 1];
            }

            double yMin = 0;
            double yMax = 0;
            double dy = 0;
            boolean first = true;
            for (double v : reference1) {
                if (first) {
                    first = false;
                    yMin = yMax = v;
                }
                yMin = Math.min(yMin, v);
                yMax = Math.max(yMax, v);
            }
            for (double v : reference2) {
                if (first) {
                    first = false;
                    yMin = yMax = v;
                }
                yMin = Math.min(yMin, v);
                yMax = Math.max(yMax, v);
            }

            for (Shaking s : target.spectralValues) {
                if (first) {
                    first = false;
                    yMin = yMax = s.expectedSI * factor;
                } else {
                    yMin = Math.min(yMin, s.expectedSI * factor);
                }
                yMin = Math.min(yMin, s.percentile84 * factor);
                yMin = Math.min(yMin, s.percentile16 * factor);
                yMax = Math.max(yMax, s.expectedSI * factor);
                yMax = Math.max(yMax, s.percentile84 * factor);
                yMax = Math.max(yMax, s.percentile16 * factor);
            }

            if (yMax > yMin) {
                dy = (double) height / (yMax - yMin);
            }

            List<Point> expectedPoints = new ArrayList();
            List<Point> percentile84Points = new ArrayList();
            List<Point> percentile16Points = new ArrayList();
            List<Point> ref1Points = new ArrayList();
            List<Point> ref2Points = new ArrayList();
            int x, y;
            for (int i = 0; i < periods.length; i++) {

                if (logScale) {
                    x = (int) ((Math.log10(periods[i]) - xMin) / (xMax - xMin) * width);
                } else {
                    x = (int) ((periods[i] - xMin) / (xMax - xMin) * width);
                }

                if (i < target.spectralValues.size()) {
                    Shaking s = target.spectralValues.get(i);
                    y = (int) ((yMax - s.expectedSI * factor) * dy);
                    expectedPoints.add(new Point(x, y));
                    y = (int) ((yMax - s.percentile84 * factor) * dy);
                    percentile84Points.add(new Point(x, y));
                    y = (int) ((yMax - s.percentile16 * factor) * dy);
                    percentile16Points.add(new Point(x, y));
                }
                if (i < reference1.size()) {
                    y = (int) ((yMax - reference1.get(i)) * dy);
                    ref1Points.add(new Point(x, y));
                }
                if (i < reference2.size()) {
                    y = (int) ((yMax - reference2.get(i)) * dy);
                    ref2Points.add(new Point(x, y));
                }
            }

//            // create hatch marks for y axis.
//            for (int i = 0; i < Y_HATCH_CNT; i++) {
//                int x0 = BORDER_GAP;
//                int x1 = GRAPH_POINT_WIDTH + BORDER_GAP;
//                int y0 = getHeight() - (((i + 1) * (getHeight() - BORDER_GAP * 2)) / Y_HATCH_CNT + BORDER_GAP);
//                int y1 = y0;
//                g2.drawLine(x0, y0, x1, y1);
//            }
//
//            // and for x axis
//            for (int i = 0; i < scores.size() - 1; i++) {
//                int x0 = (i + 1) * (getWidth() - BORDER_GAP * 2) / (scores.size() - 1) + BORDER_GAP;
//                int x1 = x0;
//                int y0 = getHeight() - BORDER_GAP;
//                int y1 = y0 - GRAPH_POINT_WIDTH;
//                g2.drawLine(x0, y0, x1, y1);
//            }
            g2.setStroke(DefaultStroke);
            g2.setColor(Ref1Color);
            drawGraph(g2, ref1Points);
            g2.setColor(Ref2Color);
            drawGraph(g2, ref2Points);

            g2.setStroke(PercentileStroke);
            g2.setColor(ShakingColor);
            drawGraph(g2, percentile16Points);
            drawGraph(g2, percentile84Points);

            g2.setStroke(DefaultStroke);
            drawGraph(g2, expectedPoints);
//            for (int i = 0; i < graphPoints.size() - 1; i++) {
//                int x1 = graphPoints.get(i).x;
//                int y1 = graphPoints.get(i).y;
//                int x2 = graphPoints.get(i + 1).x;
//                int y2 = graphPoints.get(i + 1).y;
//                g2.drawLine(x1, y1, x2, y2);
//            }
//
//            g2.setStroke(oldStroke);
//            g2.setColor(GRAPH_POINT_COLOR);
//            for (Point graphPoint : graphPoints) {
//                int x = graphPoint.x - GRAPH_POINT_WIDTH / 2;
//                int y = graphPoint.y - GRAPH_POINT_WIDTH / 2;
//                int ovalW = GRAPH_POINT_WIDTH;
//                int ovalH = GRAPH_POINT_WIDTH;
//                g2.fillOval(x, y, ovalW, ovalH);
//            }
        }
    }

    private void drawGraph(Graphics2D g, List<Point> points) {
        for (int i = 0; i < points.size() - 1; i++) {
            int x1 = points.get(i).x;
            int y1 = points.get(i).y;
            int x2 = points.get(i + 1).x;
            int y2 = points.get(i + 1).y;
            g.drawLine(x1, y1, x2, y2);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PrefW, PrefH);
    }

}
