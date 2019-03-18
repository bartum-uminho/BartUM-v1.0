package um.simulator.map;

import java.util.Map;

/**
 * This class represents a node of a .osm map.
 * 
 * @author lmnd
 * @author luisacabs   
 * @version 1.0
 */
public class OSMnode {
    

    private String id;
    private String lat;
    private String lon;
    private String version;
    private final Map<String, String> tags;
    private final Map<String, String> busStops;
    private final Map<String, String> trafficLights;
    private final Map<String, String> tramStops;

    /** 
     * Constructor: Creates a <code>OSMnode</code>.
     * 
     * @param id    The node id.
     * @param lat   The node's latitude.
     * @param lon   The node's longitude.
     * @param version  The node's version.
     * @param tags  A list of tags associated with this object.
     * @param busStops  A list of bus stops.
     * @param trafficLights A list of traffic lights.
     * @param tramStops A list of tram stops.
     */
    public OSMnode(String id, String lat, String lon, String version, Map<String, String> tags, Map<String, String> busStops, Map<String, String> trafficLights, Map<String, String> tramStops) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.version = version;
        this.tags = tags;
        this.busStops = busStops;
        this.trafficLights = trafficLights;
        this.tramStops = tramStops;
    }
    
    /**
     * @return Id of the <code>OSMnode</code>.
     */
    public String getId() {
        return id;
    }

    /**
     * @return Latitude of the <code>OSMnode</code>.
     */
    public String getLat() {
        return lat;
    }

    /**
     * @return Longitude of the <code>OSMnode</code>.
     */
    public String getLon() {
        return lon;
    }
    
    /**
     * @return Version of the <code>OSMnode</code>.
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return Associated tags of the <code>OSMnode</code>.
     */
    public Map<String, String> getTags() {
        return tags;
    }

    /**
     * @return Bus stops.
     */
    public Map<String, String> getBusStops() {
        return busStops;
    }
    
    /**
     * @return Traffic lights.
     */
    public Map<String, String> getTrafficLights() {
        return trafficLights;
    }

    /**
     * @return Tram stops.
     */
    public Map<String, String> getTramStops(){
        return tramStops;
    }
    
    /**
     * @param id The id of the node.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @param lat The latitude of the node.
     */
    public void setLat(String lat) {
        this.lat = lat;
    }
    
    /**
     * @param lon The longitude of the node.
     */
    public void setLon(String lon) {
        this.lon = lon;
    }

    /**
     * @param version Node's version.
     */
    public void setVersion(String version) {
        this.version = version;
    } 
 
}
