package um.simulator.visualization;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JLabel;
import javax.swing.JPanel;
import um.simulator.core.SimStatus;
import um.simulator.map.GlobalMap;
import um.simulator.map.MapLine;
import um.simulator.map.MapPoint;
import um.simulator.actor.ActorStatus;

/**
 * This class represents the part of the GUI where the maps and actors are
 * displayed.
 *
 * @author ajcmoreira
 * @version 1.0
 */
public class SimPanel extends JPanel implements MouseMotionListener {

    JLabel welcomeMessage = new JLabel(); //holds the welcome message
    public boolean mapsVisible = false;
    public boolean updateSimPanel; //defines if the simPanel should be updated automatically and periodically
    public boolean drawPoints = false;
    public boolean drawCoordinates = false;
    public boolean drawActorsInfo = false;
    public boolean drawActorsLabel = false;
    public boolean drawStops = false;
    public boolean drawTraffic_lights = false;
    public boolean drawTram_Stops = false;

    public int numberOfActors = 0; //current number of actors (all types)
    public int numberOfCars = 0; //current number of Cars
    public int numberOfPedestrians = 0; //current number of Pedestrians
    public int numberOfTrams = 0; //current number of Trams
    public int numberOfBus = 0; //setter para o numero de bus's 
    public int numberOfTL=0;
    
    //local variables
    private double mapXmin, mapYmin, mapXmax, mapYmax; //maps bounding box
    private double scale; //scale factor to convert map coordinates into screen coordinates
    private final int border = 5; //white margin, in pixels, left around the map
    private int cx, cy; //used to always draw the maps centered in the panel
    private double zoomFactor = 1.0; //zoom control
    private int zx = 0, zy = 0; //zoom control
    private int xStart, yStart, dx = 0, dy = 0; //drag control
    private int cursor_x, cursor_y;
    private Iterator mi;
    private String mapId;
    private GlobalMap map;
    private MapPoint mp;
    private double x, y;
    private ArrayList<MapPoint> pointsList;
    private Iterator pi;
    private ArrayList<MapLine> linesList;
    private Iterator li;
    private MapLine ml;
    private ArrayList<Integer> line;
    private int pointId;
    private double xPrev, yPrev;
    private Iterator ai;
    private String actor_id;
    private ActorStatus actor;
    private ArrayList<MapPoint> bus_stops;
    private ArrayList<MapPoint> traffic_lights;
    private ArrayList<MapPoint> tram_stops;
    private Image image;
    private String label;

    /**
     * Constructor: just shows a welcome message.
     */
    public SimPanel() {
    }

    public void showWelcomeMessage(String welcomeMessageText) {
        welcomeMessage.setText(welcomeMessageText);
        this.add(welcomeMessage);
        welcomeMessage.setVisible(true);
    }

    /**
     * Initializes some global variables and add the mouse listeners.
     */
    public void initSimPanel() {
        setMapsLimits();
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                mouseClickedEvent(evt);
            }

            @Override
            public void mousePressed(MouseEvent evt) {
                xStart = evt.getX() - dx;
                yStart = evt.getY() - dy;
            }
        });
        addMouseMotionListener(this);
    }

    /**
     * Sets the variables representing the bounding box that includes all the
     * maps. Sets the class variables mapXmin, mapYmin, mapXmax and mapYmax with
     * the limits of the map.
     */
    private void setMapsLimits() {
        mi = SimStatus.maps.keySet().iterator();
        while (mi.hasNext()) {
            mapId = (String) mi.next();
            map = SimStatus.maps.get(mapId);
            pi = map.getPoints().iterator();
            mp = (MapPoint) pi.next();
            mapXmin = mapXmax = mp.getX();
            mapYmin = mapYmax = mp.getY();
            while (pi.hasNext()) {
                mp = (MapPoint) pi.next();
                if (mp.getX() < mapXmin) {
                    mapXmin = mp.getX();
                } else if (mp.getX() > mapXmax) {
                    mapXmax = mp.getX();
                }
                if (mp.getY() < mapYmin) {
                    mapYmin = mp.getY();
                } else if (mp.getY() > mapYmax) {
                    mapYmax = mp.getY();
                }
            }
        }
    }

    /**
     * Sets the scale factor for drawing maps, points and actors. The scale
     * factor is required to adjust the maps (and actors) coordinates to the
     * screen coordinates. Sets the class variables scale, cx, and cy. These
     * last two are used to center the map in the screen.
     */
    private void setScalefactor() {
        if (((mapXmax - mapXmin) / (mapYmax - mapYmin)) <= ((double) (this.getWidth() - 2 * border) / (double) (this.getHeight() - 2 * border))) { //fit the map to the panel height (leave a border)
            scale = (this.getHeight() - 2 * border) / (mapYmax - mapYmin);
            cx = (int) (this.getWidth() - ((mapXmax - mapXmin) * scale)) / 2;
            cy = 0;
        } else { //fit the map to the panel width (leave a border)
            scale = (this.getWidth() - 2 * border) / (mapXmax - mapXmin);
            cx = 0;
            cy = (int) (this.getHeight() - ((mapYmax - mapYmin) * scale)) / 2;
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (mapsVisible) {
            setScalefactor();
            //draw the maps
            drawMaps(g);
            //draw the actors
            drawActors(g);
            //draw the zoom in, zoom out, and zoom all buttons
            drawZoomButtons(g);
            //draw the cursor coordinates
            drawCoordinates(g);

        }
    }

    /**
     * Draws the maps.
     *
     * @param g
     */
    private void drawMaps(Graphics g) {
        mi = SimStatus.maps.keySet().iterator();
        while (mi.hasNext()) {
            mapId = (String) mi.next();
            map = SimStatus.maps.get(mapId);
            if (drawPoints) {
                drawPoints(map, g);
            }
            if (drawStops) {
                drawStops(map, g);
            }
            if (drawTraffic_lights) {
                drawTraffic_lights(map, g);
            }
            if (drawTram_Stops) {
                drawTram_Stops(map, g);
            }
            drawLines(map, g);
        }
    }

    /**
     * Draws all the points of a given map (map's nodes).
     *
     * @param map
     * @param g
     */
    private void drawPoints(GlobalMap map, Graphics g) {
        pointsList = map.getPoints();
        if (!pointsList.isEmpty()) {
            pi = pointsList.iterator();
            g.setColor(Color.RED);
            while (pi.hasNext()) {
                mp = (MapPoint) pi.next();
                x = ((mp.getX() - mapXmin) * scale + border + cx) * zoomFactor + zx + dx;
                y = ((mapYmax - mp.getY()) * scale + border + cy) * zoomFactor + zy + dy;
                g.drawRect((int) x - 1, (int) y - 1, 3, 3);
            }
        } else {
            System.out.println("SimPanel.drawPoints(): pointsList is empty!");
        }
    }

    //desenhar as paragens de autocarros no mapa
    private void drawStops(GlobalMap map, Graphics g) {
        bus_stops = map.getStops();
        if (!bus_stops.isEmpty()) {
            pi = bus_stops.iterator();
            g.setColor(Color.BLACK);
            while (pi.hasNext()) {
                mp = (MapPoint) pi.next();
                x = ((mp.getX() - mapXmin) * scale + border + cx) * zoomFactor + zx + dx;
                y = ((mapYmax - mp.getY()) * scale + border + cy) * zoomFactor + zy + dy;
                // g.fillOval((int)x - 1, (int)y - 1, 7, 7);//drawOval((int)x - 1,  (int)y - 1, 5, 5);//drawRect((int)x - 1, (int)y - 1, 3, 3);
                image = Toolkit.getDefaultToolkit().getImage("img/stop.png");
                g.drawImage(image, (int) x - 4, (int) y - 4, 20, 20, this);
            }
        } else {
            System.out.println("SimPanel.drawStops(): bus_stops is empty!");
        }
    }

    //desenhar os semáforos existentes no mapa
    private void drawTraffic_lights(GlobalMap map, Graphics g) {
        traffic_lights = map.getTrafficLights();
        if (!traffic_lights.isEmpty()) {
            pi = traffic_lights.iterator();
            g.setColor(Color.blue);
            while (pi.hasNext()) {
                mp = (MapPoint) pi.next();
                x = ((mp.getX() - mapXmin) * scale + border + cx) * zoomFactor + zx + dx;
                y = ((mapYmax - mp.getY()) * scale + border + cy) * zoomFactor + zy + dy;
                //g.fillOval((int)x - 1, (int)y - 1, 7, 7);//drawOval((int)x - 1,  (int)y - 1, 5, 5);
                image = Toolkit.getDefaultToolkit().getImage("light.png");
                g.drawImage(image, (int) x - 4, (int) y - 4, 20, 20, this);
            }
        } else {
            System.out.println("SimPanel.drawTraffic_lights(): traffic_lights is empty!");
        }
    }
    //desenhar as paragens do actor Tram
      private void drawTram_Stops(GlobalMap map, Graphics g) {
        tram_stops = map.getTramStops();
        if (!tram_stops.isEmpty()) {
            pi = tram_stops.iterator();
            g.setColor(Color.BLACK);
            while (pi.hasNext()) {
                mp = (MapPoint) pi.next();
                x = ((mp.getX() - mapXmin) * scale + border + cx) * zoomFactor + zx + dx;
                y = ((mapYmax - mp.getY()) * scale + border + cy) * zoomFactor + zy + dy;
                // g.fillOval((int)x - 1, (int)y - 1, 7, 7);//drawOval((int)x - 1,  (int)y - 1, 5, 5);//drawRect((int)x - 1, (int)y - 1, 3, 3);
                image = Toolkit.getDefaultToolkit().getImage("img/tram_stop.png");
                g.drawImage(image, (int) x - 4, (int) y - 4, 20, 20, this);
            }
        } else {
            System.out.println("SimPanel.drawTram_Stops(): tram_stops is empty!");
        }
    }


    /**
     * Draws all the lines of a given map.
     *
     * @param map
     * @param g
     */
    private void drawLines(GlobalMap map, Graphics g) {
        linesList = map.getLines();
        if (!linesList.isEmpty()) {
            //retrieve the points
            pointsList = map.getPoints();
            //go through each one of the lines:
            li = linesList.iterator();
            g.setColor(stringToColor(map.linesColour));
            //g.setColor(Color.ORANGE);
            while (li.hasNext()) { //for each line:
                ml = (MapLine) li.next();
                line = ml.getPoints();
                pi = line.iterator();
                //go through each one of the points in a line
                pointId = (int) pi.next();
                mp = pointsList.get(pointId);
                xPrev = ((mp.getX() - mapXmin) * scale + border + cx) * zoomFactor + zx + dx;
                yPrev = ((mapYmax - mp.getY()) * scale + border + cy) * zoomFactor + zy + dy;
                while (pi.hasNext()) { //for each point in the line:
                    pointId = (int) pi.next();
                    mp = pointsList.get(pointId);
                    x = ((mp.getX() - mapXmin) * scale + border + cx) * zoomFactor + zx + dx;
                    y = ((mapYmax - mp.getY()) * scale + border + cy) * zoomFactor + zy + dy;
                    g.drawLine((int) xPrev, (int) yPrev, (int) x, (int) y);
                    xPrev = x;
                    yPrev = y;
                }
            }
        } else {
            System.out.println("SimPanel.drawLines(): linesList is empty!");
        }
    }

    /**
     * Draws the actors.
     *
     * @param g
     */
    private void drawActors(Graphics g) {
        numberOfActors = numberOfCars = numberOfPedestrians = numberOfTrams = numberOfBus = 0; //reset the counters (this is done here to avoid going again ober the entire list of actors just to compute this values; it would make more sense to do it in the SimScope class)
        ai = SimStatus.globalActors.keySet().iterator();
        ActorStatus actor;
        Object key;
        while (ai.hasNext()) {
            numberOfActors++;
            key = ai.next();
            actor = SimStatus.globalActors.get(key);
            actor_id= actor.getActorId();
            label=actor.getLabel();
            x = ((actor.getActor_x() - mapXmin) * scale + border + cx) * zoomFactor + zx + dx;
            y = ((mapYmax - actor.getActor_y()) * scale + border + cy) * zoomFactor + zy + dy;
            if (actor_id.startsWith("Car")) {
                numberOfCars++;
                if (SimScope.carsVisible) {
                    g.setColor(Color.red);
                    image = Toolkit.getDefaultToolkit().getImage("img/car.png");
                    g.drawImage(image, (int) x - 4, (int) y - 4, 23, 23, this);
                    //g.drawRect((int) x - 4, (int) y - 4, 8, 8);
                    if (drawActorsInfo) {
                        g.drawString(actor_id, (int) x, (int) y);
                    }
                    if(drawActorsLabel){
                         g.drawString(label, (int) x, (int) y);
                    }
                }
            } else if (actor_id.startsWith("Ped")) {
                numberOfPedestrians++;
                if (SimScope.pedestriansVisible) {
                    g.setColor(Color.green);
                    image = Toolkit.getDefaultToolkit().getImage("img/man.png");
                    g.drawImage(image, (int) x - 4, (int) y - 4, 20, 20, this);
                    // g.drawRect((int)x - 4, (int)y - 4, 8, 8);
                    if (drawActorsInfo) {
                        g.drawString(actor_id, (int) x, (int) y);
                    }
                     if(drawActorsLabel){
                         g.drawString(label, (int) x, (int) y);
                    }
                }
            } else if (actor_id.startsWith("Tram")) {
                numberOfTrams++;
                if (SimScope.tramsVisible) {
                    g.setColor(Color.blue);
                    image = Toolkit.getDefaultToolkit().getImage("img/tram.png");
                    g.drawImage(image, (int) x - 4, (int) y - 4, 20, 20, this);
                   // g.drawRect((int) x - 4, (int) y - 4, 8, 8);
                    if (drawActorsInfo) {
                        g.drawString(actor_id, (int) x, (int) y);
                    }
                     if(drawActorsLabel){
                         g.drawString(label, (int) x, (int) y);
                    }
                }
            } else if (actor_id.startsWith("Bus")) {
                numberOfBus++;
                if (SimScope.busVisible) {
                    g.setColor(Color.pink);
                    image = Toolkit.getDefaultToolkit().getImage("img/red.png");
                    g.drawImage(image, (int) x - 4, (int) y - 4, 20, 20, this);
                  //  g.drawRect((int)x - 4, (int)y - 4, 8, 8);
                    //g.drawOval((int)x - 4, (int)y - 4, 8, 8);
                    if (drawActorsInfo) {
                        g.drawString(actor_id, (int) x, (int) y);
                    }
                     if(drawActorsLabel){
                         g.drawString(label, (int) x, (int) y);
                    }
                }
            }
            else if (actor_id.startsWith("Traf")) {
                numberOfTL++;
                if (SimScope.tlVisible) {
                    g.setColor(Color.black);
                image = Toolkit.getDefaultToolkit().getImage("img/light.png");
                g.drawImage(image, (int) x - 4, (int) y - 4, 20, 20, this);
                  //  g.drawRect((int)x - 4, (int)y - 4, 8, 8);
                    //g.drawOval((int)x - 4, (int)y - 4, 8, 8);
                    if (drawActorsInfo) {
                        g.drawString(actor_id, (int) x, (int) y);
                    }
                     if(drawActorsLabel){
                         g.drawString(label, (int) x, (int) y);
                    }
                }
            }
            
            else if (SimScope.othersVisible) {
                g.setColor(Color.gray);
                g.drawRect((int) x - 4, (int) y - 4, 8, 8);
                if (drawActorsInfo) {
                    g.drawString(actor_id, (int) x, (int) y);
                }
                 if(drawActorsLabel){
                         g.drawString(label, (int) x, (int) y);
                    }
            }
        }
    }

    /**
     * Draws the zoom buttons: zoom-in, zoom-out, and reset.
     *
     * @param g
     */
    private void drawZoomButtons(Graphics g) {
        g.setColor(Color.lightGray);
        //reset
        g.drawOval(10, this.getHeight() - 75, 15, 15);
        //zoom-in
        g.drawRect(10, this.getHeight() - 50, 15, 15);
        g.drawLine(14, this.getHeight() - 42, 21, this.getHeight() - 42);
        g.drawLine(18, this.getHeight() - 46, 18, this.getHeight() - 39);
        //zoom-out
        g.drawRect(10, this.getHeight() - 25, 15, 15);
        g.drawLine(14, this.getHeight() - 17, 21, this.getHeight() - 17);
    }

    /**
     * Draws the cursor (mouse pointer) coordinates (map coordinates).
     *
     * @param g
     */
    private void drawCoordinates(Graphics g) {
        if (drawCoordinates) {
            g.drawString(((cursor_x - zx - dx) / zoomFactor - border - cx) / scale + mapXmin + ", " + -(((cursor_y - zy - dy) / zoomFactor - border - cy) / scale - mapYmax), 10, 20);
        }
    }

    /**
     * This method processes mouse clicks over the zoom buttons, and adjusts the
     * zoomFactor, zx and zy variables accordingly.
     *
     * @param evt
     */
    private void mouseClickedEvent(MouseEvent evt) {
        if (evt.getX() < 25) {
            if (evt.getX() > 10) {
                if ((evt.getY() < (this.getHeight() - 60)) && (evt.getY() > (this.getHeight() - 75))) { //reset
                    zoomFactor = 1.0;
                    zx = zy = 0;
                    dx = dy = 0;
                } else if ((evt.getY() < (this.getHeight() - 35)) && (evt.getY() > (this.getHeight() - 50))) { //zoom-in
                    zoomFactor = zoomFactor * 1.25;
                    zx = (int) ((1 - zoomFactor) * this.getWidth() / 2 + dx * zoomFactor - dx);
                    zy = (int) ((1 - zoomFactor) * this.getHeight() / 2 + dy * zoomFactor - dy);
                } else if ((evt.getY() < (this.getHeight() - 10)) && (evt.getY() > (this.getHeight() - 25))) { //zoom-out
                    zoomFactor = zoomFactor / 1.25;
                    zx = (int) ((1 - zoomFactor) * this.getWidth() / 2 + dx * zoomFactor - dx);
                    zy = (int) ((1 - zoomFactor) * this.getHeight() / 2 + dy * zoomFactor - dy);
                }
            }
        }
    }

    /**
     * When called, this method centers the maps on the SimPanel and sets the
     * zoom to 1.
     */
    public void showAll() {
        zoomFactor = 1.0;
        zx = zy = 0;
        dx = dy = 0;
    }

    /**
     * Converts a String to a Color object. This method is used to enable the
     * user to set a different color for each map in the configurations file.
     *
     * @param colorString
     * @return A Color object representing the given color in string format.
     */
    private Color stringToColor(String colorString) {
        Color c;
        try {
            Field field = Color.class.getField(colorString);
            c = (Color) field.get(null);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            c = Color.LIGHT_GRAY;
        }
        return c;
    }

    /**
     * This method translates a mouse drag into a panning of the maps in the
     * screen.
     *
     * @param e an event
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        dx = e.getX() - xStart;
        dy = e.getY() - yStart;
    }

    /**
     * This method tracks the mouse movement and updates the cursor_x and
     * cursor_y class variables accordingly. These variables are used to show
     * the maps' coordinates in the screen (see method drawCoordinates()).
     *
     * @param e an event
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        cursor_x = e.getX();
        cursor_y = e.getY();
    }

}
