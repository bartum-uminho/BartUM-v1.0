/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.actor;

import java.util.Properties;

/**
 * This class is an actor generator. 
 * It creates actors of the Bus type (<code>ActorBus</code>), using a configurable generation model.
 * 
 * @author ASUS
 * @author luisacabs
 * @version 1.0
 */
public class GeneratorBus extends Generator {

    private final Double speed;
    private final Double prob_stop;

    /**
    * Constructor: a new <code>GeneratorBus</code>.
    * 
    * @param prop Config file properties.
    * @param genName Name of the generator 
    */
    public GeneratorBus(Properties prop, String genName) {
        super(prop, genName);
        /** speed in m/ms: corresponds to 30km/h */
        speed = 0.008;
        prob_stop = Double.parseDouble(prop.getProperty(genName + ".Pro_Stop", "0.5"));

    }

    /**
     * Defines the values of the parameters that need to be set to
     * configure new actors.
     *
     * @return A <code>String</code> with the parameters' values separated by ":"
     */
    @Override
    public String getNewActorDescription() {
        String generator = super.getNewActorDescription();
        return generator + ":" + linesMapName + ":" + speed + ":" + prob_stop + ":" + appName + ":" + opMode;
    }
}
