package um.simulator.map;

import java.util.List;
import java.util.Map;

/**
 * This class represents a way of a .osm map.
 * 
 * 
 * @author luisacabs   
 * @version 1.0
 */
public class OSMway {
	

    private String id;
    private boolean visible;
    private List<String> ndList;
    private final Map<String, String> tags;
    
    /**
     * Constructor: Creates a new <code>OSMway</code> object.
     * 
     * @param id    The way id.
     * @param visible   True if this way is visible.
     * @param ndlist    A list of nodes that form this way.
     * @param tags  A list of tags.
     */
    public OSMway(String id, boolean visible, List<String> ndlist, Map<String, String> tags) {
        this.id = id;
        this.visible = visible;
        this.ndList = ndlist;
        this.tags = tags;
    }

    /**
     * @return Id of the <code>OSMway</code>.
     */
    public String getId() {
        return id;
    }

    /**
     * @return If way is visible or not.
     */
    public boolean isVisible() {
        return visible;
    }
    
    /**
     * @return List of forming nodes.
     */
    public List<String> getNdList() {
        return ndList;
    }

    /**
     * @return List of associated tags.
     */
    public Map<String, String> getTags() {
        return tags;
    }

    /**
     * @param id The id of the way.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @param visible   true if visible, false if invisible.
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * @param ndList    List of forming nodes.
     */
    public void setNdList(List<String> ndList) {
        this.ndList = ndList;
    }
        
	
}
