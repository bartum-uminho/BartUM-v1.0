package um.simulator.visualization;

/**
 * This class updates the simPanel automatically and periodically.
 * @author ajcmoreira
 * @version 1.0
 */
public class SimPanelUpdater extends Thread {
    int pace;
    
    /**
     * Constructor.
     * @param pace The time interval between updates, in milliseconds.
     */
    public SimPanelUpdater(int pace) {
        this.pace = pace;
    }
    
    /**
     * Update the GUI periodically.
     */
    @Override
    public void run() {
        while (true) {
            //update the simPanel
            if (SimScope.keepUpdating)
                SimScope.updateAll();
            try {
                //pause for a while
                Thread.sleep(pace);
            } catch (InterruptedException ex) {
                System.err.println("Error while updating the GUI: " + ex.getMessage());
            }
        }
    }
}
