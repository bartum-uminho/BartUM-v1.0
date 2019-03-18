package um.simulator.actor;

import java.util.Properties;

/**
 * This class is a generator of actors of type Random.
 * It creates actors of the Random type (<code>ActorRandom</code>), using a configurable generation model.
 * 
 * @author Rui Pinheiro 
 * @author Adriano Moreira
 * @author luisacabs
 * @version 1.0
 */
public class GeneratorRan extends Generator {
        
    private final Double speed;
    int PHYDataRate;
    int PHYTxRange;
    double FER;
    String routingProtocol;
    int routingProtocolQueue; 
    int numberOfRetransmissions;


    /**
    * Constructor: a new <code>GeneratorRan</code>.
    * 
    * @param prop Config file properties.
    * @param genName Name of the generator 
    */
    public GeneratorRan(Properties prop, String genName) {
        super(prop, genName);
        /**speed in m/ms: corresponds to 2.52 km/h */
        speed=0.0007;  
    }
    
    /**
    * This method defines the values of the parameters that need to be set to configure new actors.
    * @return A <code>String</code> with the parameters' values separated by ":"
    */
    @Override
    public String getNewActorDescription() {
        String generator = super.getNewActorDescription();
        return generator + ":" + linesMapName + ":" + speed + ":" + appName + ":" + opMode;
    } 
    
}
