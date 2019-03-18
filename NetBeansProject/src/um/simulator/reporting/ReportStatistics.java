/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.reporting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author luisacabs
 * @version 1.0
 */
public class ReportStatistics {
    /** total number of received messages in each layer */
    private int totalNrofReceivedMsgPhy = 0;
    private int totalNrofReceivedMsgNet = 0;
    private int totalNrofReceivedMsgApp = 0;
    /** total number of dropped messages */
    private int totalNrofDroppedTTL = 0;
    private int totalNrofDroppedBuf = 0;
    private int totalNrofDroppedDup = 0;
    private int totalNrofDupDestination = 0;
    /** total number of sent messages in each layer */
    private int totalNrofSentMsgPhy = 0;
    private int totalNrofSentMsgNet = 0;
    private int totalNrofSentMsgApp = 0;
    /** statistics */
    private double meanDelay;
    private double deliveryRateNet;
    private double deliveryRateApp;
    private List<String> reportLocals;
    BufferedWriter bw;
    boolean status;
    
    /** Constructor: creates a statistic's report. 
     * @param status    true if the reporting is on, and false if it's off
     */
    public ReportStatistics(boolean status){
        reportLocals = new ArrayList<String>();
        this.status = status;
    }
    
    /**
     * Adds concatenated counters to an ArrayList.
     * 
     * @param localMessage  a String containing a Local Coordinator's report counters.
     */
    public void addReportLocal(String localMessage){
        if(status)
            reportLocals.add(localMessage);
    }
    public void writeStatistics(){
        if (status){
            /** Adds date and time to the report file */
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            int mYear = calendar.get(Calendar.YEAR);
            int mMonth = calendar.get(Calendar.MONTH);
            int mDay = calendar.get(Calendar.DAY_OF_MONTH);
            int mHour = calendar.get(Calendar.HOUR);
            int mMinute = calendar.get(Calendar.MINUTE);
            File f = new File("reports/"
                    + mDay+"-"+mMonth+"-"+mYear+" "+mHour+":"+mMinute+" "+
                    "Statistics.txt");
            int nrOfDelays = 0;
            String msg;
            /** counters sum */
            for(int i = 0;i<reportLocals.size();i++){
                msg = reportLocals.get(i);
                String[] counters = msg.split(":");
                meanDelay += Long.parseLong(counters[0]);
                nrOfDelays += Integer.parseInt(counters[1]);
                totalNrofReceivedMsgPhy += Integer.parseInt(counters[2]);
                totalNrofReceivedMsgNet += Integer.parseInt(counters[3]);
                totalNrofReceivedMsgApp += Integer.parseInt(counters[4]);
                totalNrofDroppedTTL += Integer.parseInt(counters[5]);
                totalNrofDroppedBuf += Integer.parseInt(counters[6]);
                totalNrofDroppedDup += Integer.parseInt(counters[7]);
                totalNrofDupDestination += Integer.parseInt(counters[8]);
                totalNrofSentMsgPhy += Integer.parseInt(counters[9]);
                totalNrofSentMsgNet += Integer.parseInt(counters[10]);
                totalNrofSentMsgApp += Integer.parseInt(counters[11]);
            }
            /** statistis calculation */
            if(nrOfDelays==0)
                meanDelay = 0;
            else
                meanDelay = meanDelay/nrOfDelays;
            if(totalNrofSentMsgNet==0)
                deliveryRateNet = 0;
            else
                deliveryRateNet = totalNrofReceivedMsgNet/totalNrofSentMsgNet;
            if(totalNrofSentMsgApp==0)
                deliveryRateApp = 0;
            else
            deliveryRateApp = totalNrofReceivedMsgApp/totalNrofSentMsgApp;
            try {
                FileWriter fw = new FileWriter(f);
                bw = new BufferedWriter(fw);
                bw.write("Number of messages Received ");
                bw.write("\n\tPhysical Layer: " + totalNrofReceivedMsgPhy);
                bw.write("\n\tNetwork Layer: " + totalNrofReceivedMsgNet);
                bw.write("\n\tApplication Layer: " + totalNrofReceivedMsgApp);
                
                bw.write("\n\nNumber of messages Sent");
                bw.write("\n\tPhysical Layer: " + totalNrofSentMsgPhy);
                bw.write("\n\tNetwork Layer: " + totalNrofSentMsgNet);
                bw.write("\n\tApplication Layer: " + totalNrofSentMsgApp);
                
                bw.write("\n\nNumber of Dropped Messages ");
                bw.write("\n\tTTL exceeded: " + totalNrofDroppedTTL);
                bw.write("\n\tFull buffer: " + totalNrofDroppedBuf);
                bw.write("\tDuplicated message: " + totalNrofDroppedDup);
                bw.write("\tDuplicated message at destination: " + totalNrofDupDestination);
                
                bw.write("\n\n\nStatistics");
                bw.write("\n\tDelay mean value: " + meanDelay);
                bw.write("\n\tNetwork Layer Delivery Rate: " + deliveryRateNet);
                bw.write("\n\tApplication Layer Delivery Rate: " + deliveryRateApp);
                
                bw.close();
            } catch (IOException ex) {
                Logger.getLogger(ReportLocal.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
