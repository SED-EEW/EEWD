/*
 * Copyright (C) 2014-2015 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reakteu.eewd.utils;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.geometry.euclidean.twod.Segment;
import org.apache.commons.math3.geometry.euclidean.twod.Line;


/**
 * Provides conversion and distance calculations on geodetic and Cartesian
 * coordinates
 *
 * @author Stephan Herrnkind herrnkind at gempa dot de
 */
public class GeoCalc {

    /**
     * earth radius at equator
     */
    public static final double SemiMajorAxis = 6378137.0;
    /**
     * mean earth radius
     */
    public static final double SemiMeanAxis = 6371000.8;
    /**
     * earth radius at poles
     */
    public static final double SemiMinorAxis = 6356752.3142;

    private static final double SemiMajorAxis2 = SemiMajorAxis * SemiMajorAxis;
    private static final double SemiMinorAxis2 = SemiMinorAxis * SemiMinorAxis;
    private static final double Eccentricity2 = 1 - (SemiMinorAxis2 / SemiMajorAxis2);
    private static final double Eccentricity = Math.sqrt(Eccentricity2);
    private static final double EP2 = (SemiMajorAxis2 - SemiMinorAxis2) / SemiMinorAxis2;

    /**
     * Converts geodetic to Cartesian coordinates
     *
     * @param latitude geodetic latitude (degree)
     * @param longitude geodetic longitude (degree)
     * @param altitude height above WGS84 ellipsoid (meter)
     * @return 3-dimensional double array holding x, y, z Cartesian coordinates
     */
    public static double[] Geo2Cart(double latitude, double longitude, double altitude) {
        double latRad = Math.toRadians(latitude);
        double lonRad = Math.toRadians(longitude);
        double sinLat = Math.sin(latRad);
        double cosLat = Math.cos(latRad);

        // ellipsoid radius at specific latitude
        double normal = SemiMajorAxis / Math.sqrt(1 - (Eccentricity2 * sinLat * sinLat));

        return new double[]{
            (normal + altitude) * cosLat * Math.cos(lonRad), // x
            (normal + altitude) * cosLat * Math.sin(lonRad), // y
            ((1 - Eccentricity2) * normal + altitude) * sinLat // z
        };
    }

    /**
     * Converts Cartesian to geodetic coordinates
     *
     * @param x (meter)
     * @param y (meter)
     * @param z (meter)
     * @return 3-dimensional double array holding latitude (degree),
     * longitude(degree) and altitude(meter)
     */
    public static double[] Cart2Geo(double x, double y, double z) {

        double p = Math.sqrt(x * x + y * y);
        double theta = Math.atan2(SemiMajorAxis * z, SemiMinorAxis * p);
        double lonRad = Math.atan2(y, x);
        double latRad = Math.atan2(z + EP2 * SemiMinorAxis * Math.pow(Math.sin(theta), 3),
                                   p - Eccentricity2 * SemiMajorAxis * Math.pow(Math.cos(theta), 3));

        // ellipsoid radius at specific latitude
        double sinLat = Math.sin(latRad);
        double normal = SemiMajorAxis / Math.sqrt(1 - (Eccentricity2 * sinLat * sinLat));

        double altitude = p / Math.cos(latRad) - normal;

        return new double[]{
            Math.toDegrees(latRad),
            Math.toDegrees(lonRad),
            altitude
        };
    }
    /** Carlo 06.03.2020 
     * Calculates the coordinates of the extremes of the Finder line segment
     * @param strike strike in deg
     * @param length length in km
     * @param x lon in deg
     * @param y lat in deg
     * @param z elevation in m
     * @return coordinates of the extremes in deg
     */
    public static double[] CentroidToExtremes(double strike, double length, double x, double y, double z) {
    		// length on one deg of distance at the centroid, in km
    		double scale = GeoCalc.Haversine (y, x, y+0.7071, x+0.7071) / 1000;
    		
    		
            double strikeRad = Math.toRadians(strike);
            double cosstrike = Math.cos(strikeRad);
            double sinstrike = Math.sin(strikeRad);
            
            //double[] centroid = GeoCalc.Geo2Cart(y, x, z);
            
            double ay = y - length / (2 * scale) * cosstrike;
            double ax = x - length / (2 * scale) * sinstrike;
            double by = y + length / (2 * scale) * cosstrike;
            double bx = x + length / (2 * scale) * sinstrike;

    	return new double[] {
    			ax,
    			ay,
    			z,
    			bx,
    			by,
    			z
    	};
    }

    /**
     * Calculates the distance between Cartesian coordinates
     *
     * @param a point
     * @param b point
     * @return distance in meter
     */
    public static double Distance3D(double[] a, double[] b) {
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        double dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * CC on 11.03.2020
     * @param a first point
     * @param b second point
     * @return distance in m
     */
    public static double Distance3DDegToM(double[] a, double[] b) {
    	double scale = GeoCalc.Haversine (a[0], a[1], a[0]+0.7071, a[1]+0.7071);
    	double dlon = b[1] - a[1];
    	double dlat = b[0] - a[0];
    	double dz = b[2] - a[2];
    	double hdist = Math.sqrt(dlon * dlon + dlat * dlat) * scale; //m
    	double dist = Math.sqrt(hdist * hdist + dz * dz); //m
    	return dist;
    	
    }
        
    /** Carlo Cauzzi 09.03.2020 
     * Calculates distance from point c to line segment defined by two extreme points a and b
     * @param start fist point of the segment
     * @param end second point of the segment
     * @param current target point
     * @return distance in m
     */
    public static double DistanceFromLine(double[] start, double[] end, double[] current)  {
    	double scale = GeoCalc.Haversine (start[0], start[1], start[0]+0.7071, start[1]+0.7071);
    	Vector2D a  = new Vector2D(start[0],start[1]);
    	Vector2D b  = new Vector2D(end[0],end[1]);
    	Vector2D c  = new Vector2D(current[0],current[1]);
    	Segment segment = new Segment(a, b, new Line(a, b, 1.0e-10));
    	double d = segment.distance(c);
    	return d * scale;
    }
    
    /** Carlo 10.03.2020
     * Haversine formula 
     * @param lat1 latitude of first point
     * @param lat2 latitude of second point
     * @param lon1 longitude of first point
     * @param lon2 longitude of second point
     * @return distance in m between two lat/lon points in deg
     * 
     */
    public static double Haversine (double lat1, double lon1, double lat2, double lon2) {
    	double dLat = Math.toRadians(lat2 - lat1);
    	double dLon = Math.toRadians(lon2 - lon1);
    	lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
 
        double a = Math.pow(Math.sin(dLat / 2),2) + Math.pow(Math.sin(dLon / 2),2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return SemiMeanAxis * c;   	
    }
    /**
     * Calculates the surface distance of a seismic wave to its epicenter. The
     * earth is assumed to be a perfect sphere with a mean radius of
     * {@link #SemiMeanAxis}.
     *
     * @see #SeismicWaveSurfaceDistanceSphere(double, double, double)
     *
     * @param depth hypocenter depth (meter)
     * @param waveRadius radius of the seismic wave (meter)
     * @return epicenter distance (meter) or 0 if the wave did not reached the
     * surface yet respectively traveled around the globe
     */
    public static double SeismicWaveSurfaceDistance(double depth, double waveRadius) {
        return SeismicWaveSurfaceDistanceSphere(depth, waveRadius, SemiMeanAxis);
    }

    /**
     * Calculates the surface distance of a seismic wave to its epicenter. The
     * earth is assumed to be a perfect sphere with a radius derived from the
     * WGS84 ellipsoid a the given latitude. {@link #SemiMeanAxis}.
     *
     * @see #SeismicWaveSurfaceDistanceSphere(double, double, double)
     *
     * @param depth hypocenter depth (meter)
     * @param waveRadius radius of the seismic wave (meter)
     * @param latitude hypocenter latitude (degree) used to calculate radius of
     * the ellipsoid
     * @return epicenter distance (meter) or 0 if the wave did not reached the
     * surface yet respectively traveled around the globe
     */
    public static double SeismicWaveSurfaceDistance(double depth, double waveRadius, double latitude) {
        if (depth >= waveRadius) {
            return 0;
        }
        // ellipsoid radius at specific latitude
        double sinLat = Math.sin(Math.toRadians(latitude));
        double normal = SemiMajorAxis / Math.sqrt(1 - (Eccentricity2 * sinLat * sinLat));
        return SeismicWaveSurfaceDistanceSphere(depth, waveRadius, normal);
    }

    /**
     * Calculates the surface distance of a seismic wave to its epicenter. The
     * earth is assumed to be a perfect homogeneous sphere.
     *
     * Since this method returns only the 1-dimensional epicenter distance the
     * calculation can be performed in 2D:
     *
     * <p>
     * 1. Calculate one intercept point of the two circles representing the
     * sphere (x²+y²=r_sphere²) and the wave (x²+(y- (r_sphere-depth))²=r_wave²)
     * </p>
     * <p>
     * 2. Calculate the angle and distance of the intercept point and the
     * epicenter
     * </p>
     *
     * @param depth hypocenter depth (meter)
     * @param waveRadius radius of the seismic wave (meter)
     * @param sphereRadius radius of the earth
     * @return epicenter distance (meter) or 0 if the wave did not reached the
     * surface yet respectively traveled around the globe
     */
    public static double SeismicWaveSurfaceDistanceSphere(double depth, double waveRadius, double sphereRadius) {
        if (depth >= waveRadius || 2 * sphereRadius - depth < waveRadius) {
            return 0;
        }

        double coreDist = sphereRadius - depth;
        double sphereRadius2 = sphereRadius * sphereRadius;
        double y = (sphereRadius2 + coreDist * coreDist - waveRadius * waveRadius)
                   / (2 * coreDist);
        double x = Math.sqrt(sphereRadius2 - y * y);
        return Math.atan(x / y) * sphereRadius;
    }
}
