package um.simulator.map;

import java.io.Serializable;
import java.util.Map;

/**
 * This class represents a point of a map.
 * @author XT17 
 * @author Adriano Moreira
 * @author luisacabs   
 * @version 1.0
 * 
 */
public class MapPoint implements Serializable {

    private int pointId;
    private double pointX, pointY;
    private Map<String, String> tags;

    /**
     * @param lnId  The id of this point.
     */
    public void setId(int lnId){
        pointId=lnId;
    }

    /**
     * @param ptX   Value of <code>MapPoint</code> in the x axis.
     */
    public void setX(double ptX){
        pointX=ptX;
    }

    /**
     * @param ptY   Value of <code>MapPoint</code> in the y axis.
     */
    public void setY(double ptY){
        pointY=ptY;
    }
    
    /**
     * @param Tags  List of tags to add to the <code>MapPoint</code>.
     */
    public void setTags(Map<String, String> Tags)
     {
        tags=Tags;
     }
    
    /**
     * @return Id of the <code>MapPoint</code>.
     */
    public int getId(){
        return pointId;
    }

    /**
     * @return <code>MapPoint</code> value in the x axis.
     */
    public double getX(){
        return pointX;
    }

    /**
     * @return <code>MapPoint</code> value in the y axis.
     */
    public double getY(){
        return pointY;
    }
    
    /**
     * @return List of tags of this <code>MapPoint</code>.
     */
    public Map<String, String> getTags() {
        return tags;
    }
    
    /**
     * Makes a copy of the <code>MapPoint</code>.
     * @return  A <code>MapPoint</code>.
     */
    @Override
    public MapPoint clone(){
        MapPoint pl = new MapPoint();
        pl.setId(this.pointId);
        pl.setX(this.pointX);
        pl.setY(this.pointY);
        pl.setTags(this.tags);
        return pl;
    }

}
