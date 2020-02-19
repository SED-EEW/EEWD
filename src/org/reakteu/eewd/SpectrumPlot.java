/*
 * Copyright (C) 2014-2015 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reakteu.eewd;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reakteu.eewd.data.POI;
import org.reakteu.eewd.data.Shaking;

/**
 *
 * @author Stephan Herrnkind herrnkind at gempa dot de
 */
@SuppressWarnings("serial")
public class SpectrumPlot extends JPanel {

    private static final Logger LOG = LogManager.getLogger(SpectrumPlot.class);

    private static final int PrefW = 800;
    private static final int PrefH = 650;
    private static final Insets Border = new Insets(7, 55, 45, 7);
    private static final int TickLength = 10;

    //private static final Color BackgroundColor = new Color(231, 229, 228);
    private static final Color Ref1Color = new Color(0, 192, 0);
    private static final Color Ref2Color = new Color(192, 0, 0);
    private static final Color ShakingColor = new Color(0, 0, 255);

    private static final Stroke DefaultStroke = new BasicStroke(2f);
    private static final Stroke PercentileStroke
                                = new BasicStroke(1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER,
                                                  10f, new float[]{5, 7}, 0f);
    private static final int PointRadius = 3;
    private static final int PointRadius2 = PointRadius * 2;

    private final double[] xValues;
    private final List<Double> reference1;
    private final List<Double> reference2;
    private final boolean logScale;
    private POI target;

    public SpectrumPlot(double[] periods) {
        Application app = Application.getInstance();
        if (app.isUseFrequencies()) {
            xValues = new double[periods.length];
            for (int i = 0; i < periods.length; ++i) {
                xValues[xValues.length - i - 1] = 1 / periods[i];
            }
        } else {
            xValues = periods;
        }

        this.reference1 = readReference(Application.PropertySpecRef1);
        this.reference2 = readReference(Application.PropertySpecRef2);
        this.logScale = Application.getInstance().getProperty(Application.PropertySpecLogScale, false);
        //this.setBackground(BackgroundColor);
    }

    private List<Double> readReference(String key) {
        List<Double> refs = new ArrayList();

        String[] values = Application.getInstance().getProperty(key, "").split(",");
        for (String v : values) {
            if (refs.size() >= xValues.length) {
                LOG.warn("more reference values than periods specified in parameter: " + key);
                break;
            }
            refs.add(Double.parseDouble(v));
        }
        if (!refs.isEmpty() && refs.size() < xValues.length) {
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

        int width = getWidth() - Border.left - Border.right;
        int height = getHeight() - Border.top - Border.bottom;

        g2.translate(Border.left, Border.top);

        // create x and y axes
        g2.setColor(Color.black);
        g2.drawLine(0, height, width, height);
        g2.drawLine(0, 0, 0, height);

        g2.setFont(g2.getFont().deriveFont(10.0f));
        FontMetrics fm = g2.getFontMetrics();

        if (xValues.length == 0 || target == null
            || xValues[xValues.length - 1] <= 0) {
            return;
        }

        double factor;
        String yText;
        Application app = Application.getInstance();
        Shaking.Type t = app.getSpectrumParameter();
        if (t == Shaking.Type.PSA) {
            factor = Application.EarthAcceleration1;
            yText = t.toString().toUpperCase() + ", g";
        } else if (t == Shaking.Type.DRS) {
            factor = 100;
            yText = t.toString().toUpperCase() + ", cm";
        } else {
            LOG.warn("unsupported spectrum parameter: " + t.toString());
            return;
        }

        synchronized (target) {

            // determine min/max of x and y axis
            double xMin;
            double xMax;
            double dx = 0;
            if (logScale) {
                xMin = Math.log10(xValues[0]);
                xMax = Math.log10(xValues[xValues.length - 1]);
            } else {
                xMin = 0.0;
                xMax = xValues[xValues.length - 1];
            }
            if (xMax > xMin) {
                dx = (double) width / (xMax - xMin);
            }

            double yMin = 0;
            double yMax = 0;
            double dy = 0;
            boolean first = true;
            for (double v : reference1) {
                if (first) {
                    first = false;
                    /*yMin =*/ yMax = v;
                }
                /*yMin = Math.min(yMin, v);*/
                yMax = Math.max(yMax, v);
            }
            for (double v : reference2) {
                if (first) {
                    first = false;
                    /*yMin =*/ yMax = v;
                }
                /*yMin = Math.min(yMin, v);*/
                yMax = Math.max(yMax, v);
            }

            for (Shaking s : target.spectralValues) {
                if (first) {
                    first = false;
                    /*yMin =*/ yMax = s.expectedSI * factor;
                } else {
                    /*yMin = Math.min(yMin, s.expectedSI * factor);*/
                    yMax = Math.max(yMax, s.expectedSI * factor);
                }
                /*
                 yMin = Math.min(yMin, s.expectedSI * factor);
                 yMin = Math.min(yMin, s.percentile84 * factor);
                 yMin = Math.min(yMin, s.percentile16 * factor);
                 */
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
            int x, y, iV;
            boolean isFreq = app.isUseFrequencies();
            for (int i = 0; i < xValues.length; i++) {

                if (logScale) {
                    x = (int) ((Math.log10(xValues[i]) - xMin) / (xMax - xMin) * width);
                } else {
                    x = (int) ((xValues[i] - xMin) / (xMax - xMin) * width);
                }

                iV = isFreq ? xValues.length - i - 1 : i;

                if (iV < target.spectralValues.size()) {
                    Shaking s = target.spectralValues.get(iV);
                    y = (int) ((yMax - s.expectedSI * factor) * dy);
                    expectedPoints.add(new Point(x, y));
                    y = (int) ((yMax - s.percentile84 * factor) * dy);
                    percentile84Points.add(new Point(x, y));
                    y = (int) ((yMax - s.percentile16 * factor) * dy);
                    percentile16Points.add(new Point(x, y));
                }
                if (iV < reference1.size()) {
                    y = (int) ((yMax - reference1.get(iV)) * dy);
                    ref1Points.add(new Point(x, y));
                }
                if (iV < reference2.size()) {
                    y = (int) ((yMax - reference2.get(iV)) * dy);
                    ref2Points.add(new Point(x, y));
                }
            }

            // Y-axis
            String text;
            int halfAscent = fm.getAscent() / 2 - 1;
            double q = Math.log10(2 * (yMax - yMin) * 2 * fm.getHeight() / height);
            double rx = q - Math.floor(q);
            int d = rx < 0.3 ? 1 : rx > 0.7 ? 5 : 2;
            double tickStep = d * Math.pow(10, Math.floor(q - rx));
            for (double v = 0; v < yMax; v += tickStep) {
                y = height - (int) (v * dy) - 1;
                g2.drawLine(0, y, -TickLength, y);

                text = Double.toString(((int) (v * 1000.0 + 0.5)) / 1000.0);
                int w = (int) fm.getStringBounds(text, null).getWidth();
                g2.drawString(text, -TickLength - 3 - w, y + halfAscent);
            }

            // X-axis
            q = Math.log10(2 * (xMax - xMin) * 50 / width);
            rx = q - Math.floor(q);
            d = rx < 0.3 ? 1 : rx > 0.7 ? 5 : 2;
            tickStep = d * Math.pow(10, Math.floor(q - rx));
            for (double v = xMin; v < xMax; v += tickStep) {
                x = (int) ((v - xMin) * dx);
                g2.drawLine(x, height, x, height + TickLength);

                text = String.format("%.2f", logScale ? Math.pow(10, v) : v);
                //String text = Double.toString(logScale ? Math.pow(10, v) : v);//((int) (v * 1000.0 + 0.5)) / 1000.0);
                int w = (int) fm.getStringBounds(text, null).getWidth();
                g2.drawString(text, x - w / 2, height + TickLength + 3 + fm.getAscent());
            }

            // axis label
            g2.setFont(g2.getFont().deriveFont(14.0f));

            AffineTransform orig = g2.getTransform();
            int w = (int) ((height + fm.getStringBounds(yText, null).getWidth()) / 2);
            g2.translate(-Border.left + fm.getAscent() + 7, w);
            g2.rotate(-Math.PI / 2);
            g2.drawString(yText, 0, 0);
            g2.setTransform(orig);

            text = "vibration " + (isFreq ? "frequency, Hz" : "period, s");
            g2.drawString(text,
                          (int) ((width - fm.getStringBounds(text, null).getWidth()) / 2),
                          height + TickLength + 3 + 2 * fm.getHeight());

            g2.setClip(0, 0, width, height);

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
        }
    }

    private void drawGraph(Graphics2D g, List<Point> points) {
        if (points.isEmpty()) {
            return;
        }

        Point p, prevP = null;
        for (Point point : points) {
            p = point;
            if (prevP != null) {
                g.drawLine(prevP.x, prevP.y, p.x, p.y);
                g.fillOval(prevP.x - PointRadius, prevP.y - PointRadius, PointRadius2, PointRadius2);
            }
            prevP = p;
        }
        g.fillOval(prevP.x - PointRadius, prevP.y - PointRadius, PointRadius2, PointRadius2);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PrefW, PrefH);
    }

}
