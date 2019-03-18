/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.comm.physical;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import um.simulator.actor.Actor;
import um.simulator.actor.ActorPositionTimestamp;
import um.simulator.comm.link.LinkLayer;
import um.simulator.comm.network.NPDU;
import um.simulator.core.SimStatus;
import um.simulator.map.CoordinatesHelper;

/**
 * This class represents an abstraction of the 802.11p protocol from the WAVE standard.
 * 
 * @author Nuno Correia
 * @version 1.0
 */
public class WAVEProtocol extends PhyProtocol {

    int transmRateKbps; 
    int transmRateKBps; 
    ArrayList<PPDU> buffer;
    int RANGE; 
    double FER; 
    Random random = new Random();
    
    /**
     * Constructor: creates a 802.11p Protocol.
     *
     * @param actor     the id of the actor
     * @param link  a reference to the link layer
     * @param PHYDataRate   data rate in the physical layer
     * @param PHYTxRange data transfer range
     * @param FER   frame error rate
     * @param routingProtocol   the network layer protocol
     * @param routingProtocolQueue  size of buffer in network layer
     * @param numberOfRetransmissions   used in network protocol
     */
    
    public WAVEProtocol(Actor actor, LinkLayer link, int PHYDataRate, int PHYTxRange, double FER,
        String routingProtocol, int routingProtocolQueue, int numberOfRetransmissions) {
        super(actor,link);
        buffer = new ArrayList<>();
        this.transmRateKbps = PHYDataRate;
        this.transmRateKBps = transmRateKbps/8;
        this.RANGE = PHYTxRange;
        this.FER = FER;
        
    }

    /**
     * Adds a pdu to the buffer.
     * 
     * @param npdu  the pdu to be sent
     * @param destAddres   the destination of the pdu
     */
    @Override
    public synchronized void sendFrame(NPDU npdu, String destAddres) {
        PPDU ppdu = new PPDU(System.currentTimeMillis(), actor.getActorId(),
                destAddres, 'W', actor.getX(), actor.getY(), npdu.getAllFields());
        buffer.add(ppdu);
        SimStatus.reportLocal.reportSentMessage(actor.getActorId(),1,ppdu.getData(),-5);
    }

    /**
     * Places the ppdus into their respective destination buffer.
     */
    @Override
    public synchronized void flushFrames() {
        for (PPDU m : buffer) {
            byte[] size = m.toString().getBytes();
            /** time in milliseconds */
            int transmitionTime = size.length / (transmRateKBps); 
            if (transmitionTime > 0.5) {
                try {
                    Thread.sleep((long) transmitionTime);
                } catch (InterruptedException ex) {
                    Logger.getLogger(WAVEProtocol.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            m.setTime_stamp(System.currentTimeMillis());
            m.setActor_x(actor.getX());
            m.setActor_y(actor.getY());
            if (m.getDestination_id().equals("Broadcast")) {
                SimStatus.addBroadcastMessage(m);
            } else {
                if (!SimStatus.globalMessages.containsKey(m.getDestination_id())) {
                    SimStatus.globalMessages.put(m.getDestination_id(), new ArrayList<PPDU>());
                }
                synchronized (SimStatus.globalMessages.get(m.getDestination_id())) {
                    SimStatus.globalMessages.get(m.getDestination_id()).add(m);
                }
            }
        }
        buffer.clear();
    }

    /** 
     * Updates the physical layer 
     */
    @Override
    public void update() {
        
        flushFrames();
        
        ArrayList<PPDU> frames = new ArrayList<>();
        ArrayList<PPDU> messagesFromActor;
        synchronized (SimStatus.globalMessages.get(actor.getActorId())) {
            messagesFromActor = SimStatus.globalMessages.get(actor.getActorId());
            int i = 0;
            while (i < messagesFromActor.size()) {
                PPDU m = messagesFromActor.get(i);
                if (m.getProtocol() == 'W') {
                    double distance;
                    float errorProbability = random.nextFloat();
                    /** remove message if it's already sent or it's broadcast */
                    if (m.isSent() || m.getDestination_id().equals("Broadcast")) {
                        distance = verifyMessageReception(m);
                        if (distance == -1 && FER <= errorProbability) {
                            frames.add(m);
                            //Faz o reporting de mensagem recebida
                            SimStatus.reportLocal.reportReceivedMessage(actor.getActorId(),1,m.getData(),-5);
                            messagesFromActor.remove(i);
                        } else {
                            messagesFromActor.remove(i);
                        }
                    }
                    else {
                        if (!m.isRead()) {
                            distance = verifyMessageReception(m);
                            if (distance == -1 && FER <= errorProbability) {
                                frames.add(m);
                                SimStatus.reportLocal.reportReceivedMessage(actor.getActorId(),1,m.getData(), -5);
                            }
                            m.setRead(true);
                        }
                        i++;
                    }
                } else {
                    i++;
                }
            }
        }
        if (!frames.isEmpty()) {
            ArrayList<NPDU> frames2 = new ArrayList<>();
            for (PPDU p : frames) {
                frames2.add(new NPDU(p.getData()));
            }
            linkLayer.sendFramesUp(frames2);
        }
    }

    /**
     * Verifies if the actor sending the ppdu is in range.
     * 
     * @param message the ppdu being considered
     * @return the distance to the actor
     */
    private double verifyMessageReception(PPDU message) {
        double distance = 0;
        
        ArrayList<ActorPositionTimestamp> position_h = actor.getPositionHistory();
        ActorPositionTimestamp actual_apt;
        long next_dif, actual_dif; // saves the diferences
        long actual_aptTime, messageTime;
        messageTime = message.getTime_stamp();
        synchronized(position_h){
            actual_apt = position_h.get(0); //oldest apt
            actual_aptTime = actual_apt.getTime_stamp(); //get time
            //Search for the closest time to the message
            for (ActorPositionTimestamp next_apt : position_h) {
                next_dif = Math.abs(next_apt.getTime_stamp() - messageTime);
                actual_dif = Math.abs(actual_aptTime - messageTime);
                // compare time diferences and save the closest
                if (next_dif <= actual_dif) {
                    actual_apt = next_apt;
                    actual_aptTime = actual_apt.getTime_stamp(); //get the actual time
                }
            }
        }
        
        Math.sqrt(CoordinatesHelper.distanceSquare(actual_apt.getActor_x(), actual_apt.getActor_y(),
                message.getActor_x(), message.getActor_y()));
        if (distance < RANGE) {
            return -1;
        } else { 
            return distance;
        }
    }

}
