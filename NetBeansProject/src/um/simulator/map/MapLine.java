package um.simulator.map;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

/**
 * This class represents a line of a map.
 * @author XT17 
 * @author Adriano Moreira
 * @author luisacabs   
 * @version 1.0 
 */
public class MapLine implements Serializable {

    private int lineId;
    private ArrayList<Integer> points;
    private Map<String, String> tags;
    
    /**
     * @param ln_id The id of the line.
     */
    public void setId(int ln_id) {
        lineId = ln_id;
    }
    
    /**
     * @param ptLn  List of points of the <code>MapLine</code>.  
     */
    public void setPoints(ArrayList ptLn) {
        points = ptLn;
    }
    
    /**
     * @param Tags  List of tags to add to the <code>MapLine</code>.
     */
    public void setTags(Map<String, String> Tags) {
        tags = Tags;
    }

    /**
     * @return Id of the <code>MapLine</code>.
     */
    public int getId() {
        return lineId;
    }

    /**
     * @return List of points of the <code>MapLine</code>.
     */
    public ArrayList getPoints() {
        return points;
    }
    
    /**
     * @return List of tags of the <code>MapLine</code>.
     */
    public Map<String, String> getTags() {
        return tags;
    }

}
