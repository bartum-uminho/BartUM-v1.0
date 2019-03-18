package um.simulator.map;

import java.io.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class represents a map used by the actors to define their movements
 * (streets network). The maps are loaded from OSM (Open Street Maps) files.
 *
 * @author XT17 
 * @author Adriano Moreira
 * @author luisacabs   
 * @version 1.0
 *
 */
public class GlobalMap implements Serializable {

    /** true if loaded from a file */
    public boolean baseMap = false; 
    /** true if a combination of other maps */
    public boolean mergedMap = false; 
    /** true if being used by any type of Generator/Actor */
    public boolean beingUsedMap = false; 
    /** file settings.properties */
    private static Properties prop; 
    /** the default colour to use for drawing the lines */
    public String linesColour = "LIGHT_GRAY"; 
    /** list of all the points in the map */
    public ArrayList<MapPoint> points = new ArrayList<>();

    /** the list of all the lines in the map. 
     * each entry in the ArrayList contains the ordered list of the points (ids) that describe a line
     */
    public ArrayList<MapLine> lines = new ArrayList<>();

    /** the list of neighbours of each point in the map,
     * (those that are directly connected through a line segment) */
    public ArrayList<HashSet<Integer>> pointsNeighbours = new ArrayList<>();

    public ArrayList<MapPoint> busStops = new ArrayList<>();

    public ArrayList<MapPoint> trafficLights = new ArrayList<>();
    
    public ArrayList<MapPoint> tramStops = new ArrayList<>();
    
    /** 
     * Constructor: Creates an empty instance of a Global_Map. 
     */
    public GlobalMap() {
    }

    /** Constructor: Creates a <code>GlobalMap</code> from another <code>GlobalMap</code>.
     * @param map   A <code>GlobalMap</code> to copy.
     */
    public GlobalMap(GlobalMap map) {
        this.baseMap = map.baseMap;
        this.mergedMap = map.mergedMap;
        this.beingUsedMap = map.beingUsedMap;
        this.points = map.getPoints();
        this.lines = map.getLines();
        this.linesColour = map.linesColour;
        this.pointsNeighbours = map.getPointsNeighbours();
        this.busStops = map.getStops();
        this.trafficLights = map.getTrafficLights();
        this.tramStops = map.getTramStops();
    }
    
    /**
     * Constructor: Creates a <code>GlobalMap</code> directly from a file in OSM (OpenStreetMaps) format.
     * @param nameFile  A path to the map file.
     * @throws IOException if an IO error occurs
     * @throws Exception if an error occurs
     */
    public GlobalMap(String nameFile) throws IOException, Exception {
        prop = new Properties();
        String settingsFile="input/settings.properties";
        try{
            prop.load(new FileInputStream(settingsFile));
        }
        catch(IOException e){
            
            System.out.println("Error reading configuration file "+ settingsFile);
            System.exit(0);
        }
        /** Reads properties file to get origin point */
        Double originLon= Double.parseDouble(prop.getProperty("Map.originLon", "0"));
        Double originLat= Double.parseDouble(prop.getProperty("Map.originLat","0"));
        CoordinatesHelper.calculateConstants(originLon, originLat);
        if (nameFile.contains(".osm")) {
            loadOSM(nameFile);
        } else {
            System.out.println("Global_Map error: unknown file extension. Filenames must be .osm.");
            System.exit(0);
        }
        
    }

    
    /** 
     * @return All map points.
     */
    public ArrayList<MapPoint> getPoints() {
        return points;
    }
    
    /** 
     * @return All map lines.
     */
    public ArrayList<MapLine> getLines() {
        return lines;
    }
    
    /** 
     * @return All bus stops in the map.
     */
    public ArrayList<MapPoint> getStops() {
        return busStops;
    }
    
    /** 
     * @return All traffic lights.
     */
    public ArrayList<MapPoint> getTrafficLights() {
        return trafficLights;
    }
 
    /** 
     * @return All tram stops.
     */
    public ArrayList<MapPoint> getTramStops() {
        return tramStops;
    }
    
    /**
     * Changes the colour of the map's lines.
     * @param linesColour     A new colour.
     */
    public void setLinesColour(String linesColour) {
        this.linesColour = linesColour;
    }

    /**
     * @return  All neighbours.
     */
    public ArrayList<HashSet<Integer>> getPointsNeighbours() {
        return pointsNeighbours;
    }

    /**
     * Loads a map from a file in OpenStreetsMaps format (.osm).
     * @param nameFile  A path to the map file.
     */
    
    private void loadOSM(String nameFile) throws ParserConfigurationException, SAXException {

        ArrayList<Integer> newLine = new ArrayList<>();
        MapPoint previousNode;
        File osmfile = new File(nameFile);

        /** load and parse the OpenStreetMaps XML file */
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document document = null;
        try {
            document = docBuilder.parse(osmfile);
        } catch (IOException ex) {
            System.out.println("GlobalMap.loadOSM(): fatal ERROR loading file  " + nameFile + "!");
            System.exit(0);
        }

        /** get all the nodes from the XML document */
        HashMap<String, OSMnode> nodes = getNodes(document);

        List<OSMway> ways = getWays(document);
        /** for each way */
        for (OSMway w : ways) {
            previousNode = null;
            /** for each point in the current way */
            for (String nodeId : w.getNdList()) {
                
                MapPoint currentNode = new MapPoint();
                
                /** all points in cartesian values */
                HashMap<String, Double> xyPoint;

                Double pointLon=Double.parseDouble(nodes.get(nodeId).getLon());
                Double pointLat=Double.parseDouble(nodes.get(nodeId).getLat());
                
                xyPoint = CoordinatesHelper.toXY(pointLon, pointLat);
                currentNode.setX(xyPoint.get("x"));
                currentNode.setY(xyPoint.get("y"));
                currentNode.setTags(nodes.get(nodeId).getTags());

                /** checks if a node with the same coordinates already exists */
                int currentNodeId = getMapPointID(currentNode.getX(), currentNode.getY());
                /** it's a new node */
                if (currentNodeId == -1) { 
                    currentNode.setId(points.size());
                    points.add(currentNode);
                    pointsNeighbours.add(new HashSet<Integer>());
                    
                /** a node with the same coordinates already exists */        
                }else{ 
                    currentNode.setId(currentNodeId);
                }
                
                /** if not the first point in the current way, add previous and current points 
                 * as connected in the pointsNeighbours
                 */
                if (previousNode != null) { 
                    pointsNeighbours.get(previousNode.getId()).add(currentNode.getId());
                    pointsNeighbours.get(currentNode.getId()).add(previousNode.getId());  
                    
                }

                /** adds all point's ids from the nodes */
                newLine.add(currentNode.getId());
                previousNode = currentNode;
               
            } 
            
            /** creates a new MapLine from this way 
             * and adds it to the list of maps lines
             */
            if (newLine.size() > 1) {
                MapLine newMapLine = new MapLine();
                newMapLine.setId(lines.size());
                newMapLine.setPoints((ArrayList) newLine.clone());
                newMapLine.setTags(w.getTags());

                lines.add(newMapLine);
                newLine.clear();
            }
        }
        
        this.addBusStops(points);
        this.addTrafficLights(points);
        this.addTramStops(points);

    }

    /**
     * Gets all nodes from a .xml document.
     * @param xmlDocument   A .xml document.
     * @return a list of <code>OSMNodes</code> extracted from the provided XML document
     * 
     */
    public static HashMap<String, OSMnode> getNodes(Document xmlDocument) {
        HashMap<String, OSMnode> osmNodes = new HashMap<>();

        Node osmRoot = xmlDocument.getFirstChild();
        NodeList osmXMLNodes = osmRoot.getChildNodes();
        for (int i = 1; i < osmXMLNodes.getLength(); i++) {
            Node item = osmXMLNodes.item(i);
            if (item.getNodeName().equals("node")) {
                NamedNodeMap attributes = item.getAttributes();
                NodeList tagXMLNodes = item.getChildNodes();
                Map<String, String> tags = new HashMap<>();
                Map<String, String> busStops = new HashMap<>();
                Map<String, String> lights = new HashMap<>();
                Map<String, String> tramStops = new HashMap<>();
                Node namedItemAction = attributes.getNamedItem("action");

                if (namedItemAction == null || !namedItemAction.getNodeValue().equals("delete")) {
                    for (int j = 1; j < tagXMLNodes.getLength(); j++) {
                        Node tagItem = tagXMLNodes.item(j);
                        NamedNodeMap tagAttributes = tagItem.getAttributes();

                        if (tagAttributes != null) {
                            String kValue = tagAttributes.getNamedItem("k").getNodeValue();

                            switch (kValue) {
                            
                                case "highway":
                                    String ValueH = tagAttributes.getNamedItem("v").getNodeValue();
                                    if (ValueH.equals("traffic_signals")) {
                                        tags.put(kValue, ValueH);
                                        lights.put(kValue, ValueH);
                                    }
                                    break;
                                    
                                     case "name":
                                    String ValueN = tagAttributes.getNamedItem("v").getNodeValue();
                                    if (ValueN.equals("bus_stop")) {
                                        tags.put(kValue, ValueN);
                                        busStops.put(kValue, ValueN);
                                    }
                                    if (ValueN.equals("tram_stop")) {
                                        tags.put(kValue, ValueN);
                                        tramStops.put(kValue, ValueN);
                                    }
                                    break;
                            }
                        }
                    }

                    Node namedItemID = attributes.getNamedItem("id");
                    Node namedItemLat = attributes.getNamedItem("lat");
                    Node namedItemLon = attributes.getNamedItem("lon");
                    Node namedItemVersion = attributes.getNamedItem("version");

                    String id = namedItemID.getNodeValue();
                    String latitude = namedItemLat.getNodeValue();
                    String longitude = namedItemLon.getNodeValue();

                    String version = "0";
                    if (namedItemVersion != null) {
                        version = namedItemVersion.getNodeValue();
                    }
                    osmNodes.put(id, new OSMnode(id, latitude, longitude, version, tags, busStops, lights, tramStops));
                }
            }
        }
        return osmNodes;
    }


    /** 
     * Adds all existing bus stops.
     * @param points    A list of points.
     */
    public void addBusStops(ArrayList<MapPoint> points) {
        
        for (MapPoint mp : points) {
            if (mp.getTags().containsKey("name")) {
                if (mp.getTags().get("name").equals("bus_stop")) {
                    busStops.add(mp);
                }
            }
        }
    }

    /**
     * Adds all existing traffic lights.
     * @param points    A list of points.
     */
    public void addTrafficLights(ArrayList<MapPoint> points) {
        for (MapPoint mp : points) {
            if (mp.getTags().containsKey("highway")) {
                if (mp.getTags().get("highway").equals("traffic_signals")) {
                    trafficLights.add(mp);
                }
            }
        }
    }
    
    /**
     * Adds all existing tram stops.
     * @param points    A list of points.
     */
    public void addTramStops(ArrayList<MapPoint> points) {
        
        for (MapPoint mp : points) {
            
            if (mp.getTags().containsKey("name")) {
                if (mp.getTags().get("name").equals("tram_stop")) {
                    tramStops.add(mp);
                }
            }
        }
    }

     
    /**
     * Reads ways from a .xml Document.
     * @param xmlDocument   A .xml Document.
     * @return  A list of <code>OSMWay</code>'s.
     */
    
    public static List<OSMway> getWays(Document xmlDocument) {
        List<OSMway> osmWays = new ArrayList<>();
        Node osmRoot = xmlDocument.getFirstChild();
        NodeList osmXMLNodes = osmRoot.getChildNodes();
        for (int i = 1; i < osmXMLNodes.getLength(); i++) {
            Node item = osmXMLNodes.item(i);
            if (item.getNodeName().equals("way")) {
                NamedNodeMap attributes = item.getAttributes();
                NodeList tagXMLNodes = item.getChildNodes();
                Map<String, String> tags = new HashMap<>();
                /** nodes belonging to this way */
                ArrayList<String> ndlist = new ArrayList<>();
                Node namedItemAction = attributes.getNamedItem("action");
                if (namedItemAction == null || !namedItemAction.getNodeValue().equals("delete")) {
                    for (int j = 1; j < tagXMLNodes.getLength(); j++) {
                        Node tagItem = tagXMLNodes.item(j);
                        NamedNodeMap tagAttributes = tagItem.getAttributes();
                        if (tagItem.getNodeName().equalsIgnoreCase("tag") && tagAttributes != null) {
                            String kValue = tagAttributes.getNamedItem("k").getNodeValue();
                            switch (kValue) {
                                case "maxspeed":
                                    String vValue = tagAttributes.getNamedItem("v").getNodeValue();
                                    tags.put(kValue, vValue);
                                    break;

                                case "minspeed":
                                    String vValueM = tagAttributes.getNamedItem("v").getNodeValue();
                                    tags.put(kValue, vValueM);
                                    break;

                                case "lanes":
                                    String vValueL = tagAttributes.getNamedItem("v").getNodeValue();
                                    tags.put(kValue, vValueL);
                                    break;

                                case "oneway":
                                    String vValueO = tagAttributes.getNamedItem("v").getNodeValue();
                                    tags.put(kValue, vValueO);
                                    break;
                            }
                        } 
                        /** if it is a reference to a node */
                        else if (tagItem.getNodeName().equalsIgnoreCase("nd") && tagAttributes != null) {
                            String Ref = tagAttributes.getNamedItem("ref").getNodeValue();
                            ndlist.add(Ref);
                        }
                    }

                    Node namedItemID = attributes.getNamedItem("id");
                    String id = namedItemID.getNodeValue();

                    Node namedItemVisible = attributes.getNamedItem("visible");
                    boolean visible = true;
                    if (namedItemVisible.getNodeValue().equalsIgnoreCase("false")) {
                        visible = false;
                    } else if (namedItemVisible.getNodeValue().equalsIgnoreCase("true")) {
                        visible = true;
                    }

                    osmWays.add(new OSMway(id, visible, ndlist, tags));
                }
            }
        }
        return osmWays;
    }

    
    /**
     * Merges a <code>GlobalMap</code> into the current <code>GlobalMap</code>.
     *
     * @param mapToMerge A map to merge with the current one.
     */
    public void mergeWith(GlobalMap mapToMerge) {
        MapPoint mp;
        MapLine ml;
        HashMap<Integer, Integer> pointIdsMapping = new HashMap();
        ArrayList<Integer> newLine = new ArrayList<>();

        if (mapToMerge.points.size() <= 0) {
            /** there are points to merge */
            System.out.println("WARNING: GlobalMap.mergeWith(): the provided map is empty (no points)! Doing nothing.");
            return;
        } else { 
            int pId, newPointId;
            Iterator i1 = mapToMerge.points.iterator();
            while (i1.hasNext()) { 
                mp = (MapPoint) i1.next();
                pId = mp.getId();
                /** check if a point with the same coordinates already exists */
                newPointId = getMapPointID(mp.getX(), mp.getY());
                /** its a new node! */
                if (newPointId == -1) { 
                    mp.setId(points.size());
                    points.add(mp.clone());
                    pointIdsMapping.put(pId, points.size() - 1);
                    pointsNeighbours.add(new HashSet<Integer>());
                /** a point with the same coordinates already exists */
                } else { 
                    pointIdsMapping.put(pId, newPointId);
                }
            }
        }
        /** merge the lines and the neighbours */
        if (mapToMerge.lines.size() > 0) {
            /** go through each one of the lines, update the ids of the points, 
             and add them to the current mapToMerge */
            Iterator i2 = mapToMerge.lines.iterator();
            int newCurrentPointId;
            MapPoint previousPoint;
            while (i2.hasNext()) {
                previousPoint = null;
                ml = (MapLine) i2.next();
                ArrayList<Integer> l = ml.getPoints();
                Iterator i3 = l.iterator();
                while (i3.hasNext()) { 
                    newCurrentPointId = pointIdsMapping.get((Integer) i3.next());
                    /** update the point id */
                    newLine.add(newCurrentPointId); 
                    /** if not the first point in the current way, 
                     * add previous and current points as connected in the pointsNeighbours*/
                    if (previousPoint != null) { 
                        pointsNeighbours.get(previousPoint.getId()).add(newCurrentPointId);
                        pointsNeighbours.get(newCurrentPointId).add(previousPoint.getId());
                    }
                    previousPoint = points.get(newCurrentPointId);
                }
                MapLine newMapLine = new MapLine();
                newMapLine.setId(lines.size());
                newMapLine.setPoints((ArrayList) newLine.clone());
                lines.add(newMapLine);
                newLine.clear();
            }
        }
    }

    /**
     * Gets a point with the given cartesian values.
     * @param x Value in the x axis.
     * @param y Value in the y axis.
     * @return The point id.
     */
    public int getMapPointID(double x, double y) {
        for (MapPoint pl : points) {
            if ((pl.getX() == x) && (pl.getY() == y)) {
                return pl.getId();
            }
        }
        return -1;
    }

    /**
     * Gets an instance of a line that contais the given id.
     * @param id    The point's id.
     * @return A <code>MapLine</code>.
     */
    public MapLine getMapLine(int id) {
        for (MapLine ml : lines) {
            if ((ml.getPoints().contains(id))) {
                return ml;
            }
        }
        return null;
    }

    
    /** 
     * Prints the points of a map and its neighbours.
     * Used only for debuging.
     */
    public void printMap() {
        System.out.print("pointsList: ");
        for (MapPoint pl : points) {
            System.out.print(pl.getId() + ", ");
        }
        System.out.println("\npointsNeighbours(" + pointsNeighbours.size() + "): ");
        for (int i = 0; i < pointsNeighbours.size(); i++) {
            System.out.print(i + "-[");
            for (Integer pId : pointsNeighbours.get(i)) {
                System.out.print(pId + ", ");
            }
            System.out.println("]");
        }
    }

    /** 
     * Looks for a given tag.
     * @param key   The key parameter of the searched tag.
     * @param id    The point id.
     * @return  The value of the found key.
     */
    public String searchKeyTag(String key, int id) {
        String res = null;
        res = this.getPoints().get(id).getTags().get(key);
        return res;
    }

    /**
     * Looks for all tags, given a list of <code>OSMway</code>s.
     * @param key   The key tag.
     * @param osmWays   A list of ways.
     * @return  A list of all the corresponding id and value of each key.
     */
    public HashMap<String, String> searchKeyTagsWays(String key, List<OSMway> osmWays) {
        HashMap<String, String> res = new HashMap<String, String>();
        
        for (OSMway osmWay : osmWays) {
            if (osmWay.getTags().containsKey(key)) {
                res.put(osmWay.getId(), osmWay.getTags().get(key));
            }
        }
        return res;
    }

    /**
     * Looks for all tags, given a list of <code>OSMnode</code>s.
     * @param key   The key tag.
     * @param nodes A list of nodes.
     * @return A list of all the corresponding id and value of each key.
     * 
     */
    public HashMap<String, String> searchKeyTagsNode(String key, HashMap<String, OSMnode> nodes) {
        HashMap<String, String> res = new HashMap<String, String>();
        List<OSMnode> list = (List<OSMnode>) nodes.values();
      
        for (OSMnode node : list) {
            if (node.getTags().containsKey(key)) {
                res.put(node.getId(), node.getBusStops().get(key));
            }
        }
        return res;
    }
}
