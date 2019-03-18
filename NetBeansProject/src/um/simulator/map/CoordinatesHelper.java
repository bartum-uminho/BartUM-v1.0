package um.simulator.map;

import java.util.HashMap;

/**
 * This class represents a coordinate converter and helper. 
 * It has an angle (orientation of
 * the axis in reference to North)<br> 
 * It has a reference point with the coordinates
 * (lat0, lon0) <br> 
 * It uses the referente point and the angle to convert coordinates
 * from Lon, Lat to X, Y (meters) <br>
 * It uses the referente point and the angle to
 * convert coordinates from X, Y (meters) to Lon, Lat <br>
 * 
 *
 * @author Ivo Silva
 * @author luisacabs   
 * @version 1.0
 */
public class CoordinatesHelper{

    /** angle in degrees */
    private static Double angle = 0.0;
    /** reference Lat and Lon coordinates, (0,0) in meters */
    private static Double lat0 = 0.0;
    private static Double lon0 = 0.0;
    static double constant_x = 0;
    static double constant_y = 0;

    /**
     * Converts from (lon,lat) to (x,y).
     *
     * @param lon   A given longitude.
     * @param lat   A given latitude.
     * @return  An <code>HashMap</code> containing cartesian values.
     */
    public static synchronized HashMap<String, Double> toXY(Double lon, Double lat) {
        
        HashMap<String, Double> xyPoint = new HashMap<String, Double>();
        
        /**
         * If you wish to use a different orientation, (not aligned with NORTH), 
         * you can use the angle variable.
         * Please use the below commented code if you wish to use this variable.
         * Make sure to comment the *NORTH ORIENTATION* code. 
         **/
        
        /*
        Double a = Math.toRadians(angle);
        
        Double xL = (lon - lon0) * constant_x;
        Double yL = (lat - lat0) * constant_y;
        
        Double x = xL * Math.cos(a) + yL * Math.sin(a);
        Double y = -xL * Math.sin(a) + yL * Math.cos(a);
        
        xyPoint.put("x", x);
        xyPoint.put("y", y);
        */
        
        
        /**
         * NORTH ORIENTATION.
        **/
        
        Double x = (lon - lon0) * constant_x;
        Double y = (lat - lat0) * constant_y;
        xyPoint.put("x", x);
        xyPoint.put("y", y);

        return xyPoint;
    }

    /**
     * Converts longitude and latitude to cartisian values.
     * 
     * @param lon A given longitude.
     * @param lat A given latitude.
     */
    public static void calculateConstants(Double lon, Double lat) {
        lat0 = lat;//map origin latitude
        lon0 = lon;
        double delta = 0.1;
        double lonMin = Math.toRadians(lon - delta / 2);
        double lonMax = Math.toRadians(lon + delta / 2);
        double latMin = Math.toRadians(lat - delta / 2);
        double latMax = Math.toRadians(lat + delta / 2);

        double dist_x = geoDist(lat, lonMin, lat, lonMax);
        double dist_y = geoDist(latMin, lon, latMax, lon);

        constant_x = dist_x * 1 / delta;
        constant_y = dist_y * 1 / delta; 
    }

    /**
     * Calculates distance between two points, using geographic coordinates.
     * @param lat   A latitude for the first point.
     * @param lon   A longitude for the first point.
     * @param lat2  A latitude for the second point.
     * @param lon2  A longitude for the second point.
     * @return  Distance between the given points.
     */  
    public static double geoDist(double lat, double lon, double lat2, double lon2) {
        int r = 3813000;
        double aux1 = Math.pow(Math.sin((lat2 - lat) / 2), 2);
        double aux2 = aux1 + Math.cos(lat) * Math.cos(lat2) * Math.pow(Math.sin((lon2 - lon) / 2), 2);
        double dx = 2 * r * Math.asin(Math.sqrt(aux2));

        return dx;
    }
    /**
     * Converts from (x,y) to (lon,lat). 
     * 
     * @param x x axis point
     * @param y y axis point
     * @return point in longitude/latitude notation 
     */
    public static HashMap<String, Double> toLonLat(Double x, Double y) {
        HashMap<String, Double> lonlatPoint = new HashMap<>();
        
        /**
         * If you wish to use a different orientation, (not aligned with NORTH), 
         * you can use @param angle.
         * Please use the below commented code if you wish to use this variable.
         * Make sure to comment the *NORTH ORIENTATION* code. 
         **/
        
        /*Double a = Math.toRadians((180 + (180 - angle)));
        
        //Firstly the CY coordinates are rotated to an axes aligned with NORTH
        Double xL = x * Math.cos(a) + y * Math.sin(a);
        Double yL = -x * Math.sin(a) + y * Math.cos(a);
        Double lon = lon0 + (xL / 83557.0);
        Double lat = lat0 + (yL / 111062.0);
        lonlatPoint.put("lon", lon);
        lonlatPoint.put("lat", lat);
        */
        
        /**
         * NORTH ORIENTATION.
        **/
        //Scaling operation to convert the meters to degrees
        Double lon = lon0 + (x / 83557.0);
        Double lat = lat0 + (y / 111062.0);

        lonlatPoint.put("lon", lon);
        lonlatPoint.put("lat", lat);

        return lonlatPoint;
    }
    
    /**
     * Calculates distance between two cartesian points.
     * @param x1    A x axis position of the first point.
     * @param y1    A y axis position of the first point.
     * @param x2    A x axis position of the second point.
     * @param y2    A y axis position of the second point.
     * @return  The square distance between the given points.
     */
    public static double distanceSquare(Double x1,Double y1,Double x2,Double y2){
        double dist = Math.abs(((x1 - x2) * (x1 - x2)) + ((y1 - y2) * (y1 - y2)));
        return dist;
    }
}
