package um.simulator.reporting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import um.simulator.actor.ActorStatus;
import um.simulator.core.SimStatus;
import um.simulator.map.GlobalMap;

/**
 * This class is used by the SimScope to playback a previously recorded simulation.
 * It takes a base filename, opens three files (maps, status of the actors, and machine loads, and updates
 * the SimStatus object accordingly. The status of the actors is updated in the SimStatus in real time,
 * accordingly to the timestamps in the reporting files. The update rate can be increased or decreased by using
 * proper methods. The updating can also be suspended or terminated using appropriate methods.
 * @author ajcmoreira
 * @version 1.0
 */
public class ReportingPlayer extends Thread {
    private String baseFileName = null;
    private float playbackPaceFactor;
    private boolean keepPlaying = true;
    private boolean pause = false;
    
    /**
     * Constructor.
     */
    public ReportingPlayer() {

    }
    
    /**
     * This method sets the baseFileName after checking the syntax of the provided filename.
     * @param providedFileName file name
     * @return true if the providedFileName is valid, or false otherwise
     */
    public boolean setBaseFileName(String providedFileName) {
        /** checks the syntax of the provided filename: it must start with 8 digits, 
         * followed by the character underscore, followed by 6 digits, 
         * followed by the character underscore, followed by one of the words Maps, 
         * Load, or Acts, followed by the string ".txt" */
        if (!providedFileName.matches("\\d{8}_\\d{6}_(Maps|Load|Acts|Sets).txt")) {
            System.out.println("The provided filename (" + providedFileName + ") is not of a valid reporting file.");
            return false;
        }
        else {
            baseFileName = providedFileName.substring(0, 16);
            return true;
        }
    }
    
    /**
     * This method checks the existence of the three files with the simulation report, based on the baseFileName,
     * and after checking if baseFileName has been set.
     * @return true if the three files exist, or false otherwise
     */
    public boolean checkFiles() {
        /** checks if baseFileName has been set */
        if (baseFileName == null) {
            return false;
        }
        /** check the Maps file */
        File f = new File(baseFileName+"Maps.txt");
        if (!f.exists() || f.isDirectory()) {
            return false;
        }
        /** checks the Load file */
        f = new File(baseFileName+"Load.txt");
        if (!f.exists() || f.isDirectory()) {
            return false;
        }
        /** checks the Actors status file */
        f = new File(baseFileName+"Acts.txt");
        if (!f.exists() || f.isDirectory()) {
            return false;
        }
        /** checks the Parameters file */
        f = new File(baseFileName+"Sets.txt");
        if (!f.exists() || f.isDirectory()) {
            return false;
        }
        return true;
    }
    
    /**
     * This method loads the maps from the file to the SimStatus.
     */
    public void loadMaps() {
        ObjectInputStream oisMaps = null;
        try {
            oisMaps = new ObjectInputStream(new FileInputStream(baseFileName+"Maps.txt"));
            HashMap<String, GlobalMap> maps = (HashMap) oisMaps.readObject();
            SimStatus.maps = maps;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ReportingPlayer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(ReportingPlayer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                oisMaps.close();
            } catch (IOException ex) {
                Logger.getLogger(ReportingPlayer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * This method reads the files with the status of the actors and with the machine loads, and updates 
     * the SimStatus object accordingly.
     */
    @Override
    public void run() {
        BufferedReader brActs;
        String actorStatusString;
        String[] actorParts;
        String currentTimestampString;
        long previousTimestamp, currentTimestamp;
        String actor_id;
        Double actor_x;
        Double actor_y;
        ActorStatus as;
        
        /** opens the file with the Actor Status */
        try {
            brActs = new BufferedReader(new FileReader(baseFileName+"Acts.txt"));
            /** starts reading the status of the actors, until the end of the file */
            /** reads the first record*/
            if ((actorStatusString = brActs.readLine()) != null) {
                actorParts = actorStatusString.split(",");
                currentTimestampString = actorParts[0];
                currentTimestamp = Long.parseLong(currentTimestampString);
                actor_id = actorParts[1];
                actor_x = Double.parseDouble(actorParts[3]);
                actor_y = Double.parseDouble(actorParts[4]);
                as = new ActorStatus(actor_id, actor_x, actor_y);
                SimStatus.setActorStatus(as);
                previousTimestamp = currentTimestamp;
                while (((actorStatusString = brActs.readLine()) != null) && keepPlaying) {
                    synchronized(this) {
                        if (pause) {
                            wait();
                        }
                    }
                    actorParts = actorStatusString.split(",");
                    currentTimestampString = actorParts[0];
                    currentTimestamp = Long.parseLong(currentTimestampString);
                    actor_id = actorParts[1];
                    actor_x = Double.parseDouble(actorParts[3]);
                    actor_y = Double.parseDouble(actorParts[4]);
                    as = new ActorStatus(actor_id, actor_x, actor_y);
                    Thread.sleep((long) ((currentTimestamp - previousTimestamp) / playbackPaceFactor));
                    SimStatus.setActorStatus(as);
                    previousTimestamp = currentTimestamp;
                }
                brActs.close();
System.out.println("ReportingPlayer.run(): reached the end of the file, or paying was stopped.");
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ReportingPlayer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(ReportingPlayer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /* --- playback control --- */
    
    /**
     * This method sets the playback speed. A playbackPaceFactor = 1 means real time playback; = 2 means double
     * speed; = 1/2 means half the speed. It can be set to any value x, with x=n, n belonging to [1,2,3, ...], or
     * with x=1/n, n belonging to [1,2,3, ...].
     * @param playbackPaceFactor playback speed
     */
    public void setPlaybackPaceFactor(float playbackPaceFactor) {
        this.playbackPaceFactor = playbackPaceFactor;
    }
    
    /**
     * Return the current value of the playbackPaceFactor variable.
     * @return the current value of the playbackPaceFactor variable
     */
    public float getPlaybackPaceFactor() {
        return playbackPaceFactor;
    }
    
    /**
     * Pauses the playback function. No more records are read from the files and updated to the SimStatus object.
     */
    public void pausePlay() {
        pause = true;
    }
    
    /**
     * Resumes the playback function.
     */
    public void resumePlay() {
        pause = false;
        synchronized(this) {
            notify();
        }
    }
    
    /**
     * Get the status of the playback: paused (true) or running (false).
     * @return the status of the playback: paused (true) or running (false).
     */
    public boolean getPauseStatus() {
        return pause;
    }
    
    /**
     * Stops the playback function, for good. It cannot be resumed after being stopped.
     */
    public void stopPlaying() {
        keepPlaying = false;
    }
}
