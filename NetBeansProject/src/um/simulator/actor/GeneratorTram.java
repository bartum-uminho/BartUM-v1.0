package um.simulator.actor;

import java.util.Properties;

/**
 * This class is a generator of actors of type Tram.
 * It creates actors of the Tram type (<code>ActorTram</code>), using a configurable generation model.
 * 
 * @author XT17 
 * @author Adriano Moreira
 * @author luisacabs
 * @version 1.0
 */
public class GeneratorTram extends Generator {
        private final Double speed;
        private final Double prob_stop;

    /**
    * Constructor: a new <code>GeneratorTram</code>.
    * 
    * @param prop Config file properties.
    * @param genName Name of the generator 
    */
    public GeneratorTram(Properties prop, String genName) {
        super (prop, genName);
        speed=0.005;
        prob_stop = Double.parseDouble(prop.getProperty(genName + ".Pro_Stop", "0.5"));

    }

    /**
    * Defines the values of the parameters that need to be set to configure new actors.
    * @return A <code>String</code> with the parameters' values separated by ":"
    */
    @Override
    public String getNewActorDescription() {
        String generator = super.getNewActorDescription();
        return generator + ":" + linesMapName + ":" + speed + ":" + prob_stop + ":" + appName  + ":" + opMode;
    }
}
