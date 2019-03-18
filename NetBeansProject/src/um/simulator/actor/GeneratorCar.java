package um.simulator.actor;

import java.util.Properties;

/**
 * This class is an actor generator. 
 * It creates actors of the Car type (<code>ActorCar</code>), using a configurable generation model.
 * 
 * @author Rui Pinheiro 
 * @author Adriano Moreira
 * @author luisacabs
 * @version 1.0
 */
public class GeneratorCar extends Generator {

    private final Double speed;
    
    /**
    * Constructor: a new <code>GeneratorCar</code>.
    * 
    * @param prop Config file properties.
    * @param genName Name of the generator 
    */
    public GeneratorCar(Properties prop, String genName) {
        super(prop, genName);
        /** speed in m/ms: corresponds to 30km/h */
        speed = 0.008;
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
        
        return generator + ":" + linesMapName + ":" + speed+ ":" + appName+ ":" + opMode;
    }
}
