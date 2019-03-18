package um.simulator.reporting;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import um.simulator.actor.ActorStatus;
import um.simulator.core.SimStatus;
import um.simulator.map.GlobalMap;

/**
 * This class writes, to a set of files, all the activity in a simulation.
 * Four files are created: one with global simulation parameters, including a copy of all the settings in the configuration file, one for the maps, one for the temporal evolution of the computation load of each computer, and one for the status of the actors along the time.
 * All these three files share a common base filename based on the current date/time. The filenames are:
 * - baseFileName_Sets.txt for the simulation parameters
 * - baseFileName_Maps.txt for the maps: holds the serialized version of the SimStatus.maps;
 * - baseFileName_Load.txt for the computer loads: see below for the format being used;
 * - baseFileName_Acts.txt for the status of the actors: see below for the format being used.
 * @author ajcmoreira
 * @version 1.0
 */
public class ReportGlobal extends Thread {
    /** used to control when the recording should stop and the files closed */
    public boolean keepRecording = true; 
    /** the time interval, in milliseconds, between consecutive records in the Acts files */
    private long actsUpdatePace = 200; 
    /** the time interval, in milliseconds, between consecutive records in the Load files */
    private long loadsUpdatePace = 20000; 
    /** local variables: */
    private Properties props;
    private FileOutputStream fos;
    /** the buffer where to write the load of the computers */
    private BufferedWriter bwLoad = null; 
    /** the buffer where to write the status of the actors */
    private BufferedWriter bwActs = null; 
    private long samplingTimeStamp;
    private double machineLoad;
    private ActorStatus actorStatus;
    /** this is a counter to control the number of Loads records */
    private int updateCount = 0; 
    
    /**
     * Constructor. Creates a ReportingWriter object. Defines the filenames, open the output streams to the files, and record the maps.
     * @param simProperties The Properties object holding the configuration settings
     * @param actsUpdatePace The time interval, in milliseconds, between consecutive records in the Acts files
     * @param loadsUpdatePace The time interval, in milliseconds, between consecutive records in the Load files
     */
    public ReportGlobal(Properties simProperties, long actsUpdatePace, long loadsUpdatePace) {
        this.actsUpdatePace = actsUpdatePace;
        this.loadsUpdatePace = loadsUpdatePace;
        
        /** create the base filename as the current date/time: */
        String baseFileName = getCurrentDateTime();
        
        /** write the simulation parameters: */
        String setsFileName = baseFileName+"_Sets.txt";
        try {
            fos = new FileOutputStream(setsFileName);
            props = new Properties();
            /** write the initial simulation time: */
            props.setProperty("SimulationStartAt", String.valueOf(System.currentTimeMillis()));
            /** write all the parameters in the settings file */
            for(String propKey : simProperties.stringPropertyNames()) {
                String propValue = simProperties.getProperty(propKey);
                props.setProperty(propKey, propValue);
            }
            /** these properties are saved to a file later, whithin the run() method */
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ReportGlobal.class.getName()).log(Level.SEVERE, null, ex);
        }

        /** writes the maps */
        ObjectOutputStream oosMaps = null;
        String mapsFileName = baseFileName+"_Maps.txt";
        try {
            oosMaps = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(mapsFileName)));
            /** writes only the maps marked as being used */
            HashMap<String,GlobalMap> mapsToSend = new HashMap<>();
            for (String mapId : SimStatus.maps.keySet()) {
                if (SimStatus.maps.get(mapId).beingUsedMap) {
                   mapsToSend.put(mapId, SimStatus.maps.get(mapId));
               }
            }
            oosMaps.writeObject(mapsToSend);
            oosMaps.close();
        } catch (IOException ex) {
            Logger.getLogger(ReportGlobal.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                oosMaps.close();
            } catch (IOException ex) {
                Logger.getLogger(ReportGlobal.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        /** opens the buffer where to write the computer loads */
        String loadFileName = baseFileName+"_Load.txt";
        try {
            bwLoad = new BufferedWriter(new FileWriter(loadFileName));
        } catch (IOException ex) {
            Logger.getLogger(ReportGlobal.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        /** opens the buffer where to write the actors status */
        String actsFileName = baseFileName+"_Acts.txt";
        try {
            bwActs = new BufferedWriter(new FileWriter(actsFileName));
        } catch (IOException ex) {
            Logger.getLogger(ReportGlobal.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Writes the current loads of the computers to the Load file.
     * @param timestamp to associate to the log record.
     */
    private void logLoads(long samplingTimeStamp) {
        for (String machineId : SimStatus.machineLoadMap.keySet()) {
            machineLoad = SimStatus.machineLoadMap.get(machineId);
            try {
                bwLoad.write(samplingTimeStamp + "," + machineId + "," + machineLoad + "\n");
                bwLoad.flush();
            } catch (IOException ex) {
                Logger.getLogger(ReportGlobal.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * Writes the current status of all actors.
     * @param timestamp to associate to the log record.
     */
    private void logActorsStatus(long samplingTimeStamp) {
        for (String actorId : SimStatus.globalActors.keySet()) {
            actorStatus = SimStatus.globalActors.get(actorId);
            try {
                bwActs.write(samplingTimeStamp + "," + actorId + "," + actorStatus.getActor_time() + "," + actorStatus.getActor_x().toString() + "," + actorStatus.getActor_y().toString() + "\n");
            } catch (IOException ex) {
                Logger.getLogger(ReportGlobal.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * Performs the periodic registration, to the files, of the actors status and computers loads.
     * Different paces are used for the two types of records.
     */
    @Override
    public void run() {
        while(keepRecording) {
            samplingTimeStamp = System.currentTimeMillis();
            try {
                logActorsStatus(samplingTimeStamp);
                updateCount++;
                if (actsUpdatePace * updateCount >= loadsUpdatePace) {
                    logLoads(samplingTimeStamp);
                    updateCount = 0;
                }
                Thread.sleep(actsUpdatePace - samplingTimeStamp + System.currentTimeMillis()); 
            } catch (InterruptedException ex) {
                Logger.getLogger(ReportGlobal.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        /** if intructued to stop, closes the BufferedWritters */
        try {
            
            bwLoad.close();
            bwActs.close();
            props.setProperty("SimulationStopAt", String.valueOf(System.currentTimeMillis()));
            props.store(fos, "BartUM simulation in " + getCurrentDateTime());
            fos.close();
        } catch (IOException ex) {
            Logger.getLogger(ReportGlobal.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Stops the recording process.
     */
    public void stopWriting() {
        keepRecording = false;
    }
    
    /**
     * Generates a String based on the current date/time.
     * @return A string in the form "yyyyMMdd_HHmmss" based on the current date and time.
     */
    private String getCurrentDateTime() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
    }
}
