/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.actor;

import static java.lang.Thread.sleep;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import um.simulator.comm.CommStack;
import um.simulator.comm.physical.WAVEProtocol;
import um.simulator.comm.application.ApplicationLayer;
import um.simulator.core.SimStatus;
import static java.lang.Thread.sleep;
import static java.lang.Thread.sleep;
import static java.lang.Thread.sleep;

/**
 * This class represents an Actor of type Traffic Light.
 * This Actor, like the others, has a Communication Stack allowing it to exchange messages 
 * with the other nodes. However, differently from other actors, the ActorTrafficLight 
 * doesn't have a mobility model, staying in the same place throughout the simulation.
 * 
 * @author luisacabs
 * @version 1.0
 */
public final class ActorTrafficLight extends Actor{
    /** green light time ON (ms) */
    int green_time = 5000;
    /** yellow light time ON (ms) */
    int yellow_time = 1000;
    /** red light time ON (ms) */
    int red_time = 2000;
    /** represents the light color ON
     * 0 - green
     * 1 - yellow
     * 2 - red
     */
    int stateTL = 0;
    ActorStatus as;
    
    /** Constructor: Creates an <code>ActorTrafficLight</code> from a String 
     * containing its defining parameters.
     * 
     *  @param actorDescription a String containing the Actor's parameters
     */
    public ActorTrafficLight(String actorDescription) {
        super(actorDescription);
        appName = actorParams[11];
        opMode = actorParams[12].charAt(0);
        this.label = "green";
        as = new ActorStatus(id, x, y, "green");
        /** randomly selects the traffic light initial state/light */
        double state = Math.random();
        SimStatus.setActorStatus(as);
        if (state <= 0.33) {
            stateTL = 0;
        } else if (state > 0.33 && state <= 0.66) {
            stateTL = 1;
        } else if (state > 0.66) {
            stateTL = 2;
        }
    }
    
    /** This method changes the Traffic Light state and gets the <code>CommStack</code> running. */
    @Override
    public void run() {
        try {
            positionHistory.add(new ActorPositionTimestamp(0, x, y));
            cs = new CommStack(this, this.csParams);
            cs.start();
            while (!SimStatus.globalActors.get(id).actorDyingQ()) {
                
                switch (stateTL) {
                    case 0:
                        as.setTlState(stateTL);
                        as.setLabel("green");
                        SimStatus.setActorStatus(as);
                        try {
                            sleep(green_time);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(ActorTrafficLight.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        stateTL=1;
                        break;
                    case 1:
                        as.setTlState(stateTL);
                        as.setLabel("yellow");
                        SimStatus.setActorStatus(as);
                        try {
                            sleep(yellow_time);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(ActorTrafficLight.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        stateTL =2;
                        break;
                    case 2:
                        as.setTlState(stateTL);
                        as.setLabel("red");
                        SimStatus.setActorStatus(as);
                        try {
                            sleep(red_time);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(ActorTrafficLight.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        stateTL=0;
                        break;
                    default:
                        break;
                }
            }
            alive = false;
            cs.join();
            SimStatus.removeActor(id);
        }catch(InterruptedException ex){
            Logger.getLogger(ActorTrafficLight.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
