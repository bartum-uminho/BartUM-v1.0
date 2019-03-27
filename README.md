# BartUM-Simulator

## Visit our [website](http://bartum.dsi.uminho.pt/) for a complete Documentation and an User Manual.

### How to Run a simulation:

#### 1 - Choose a map
* Go to [OpenStreetMap](https://www.openstreetmap.org/) and choose the area you wish to use in your urban scenario.
* Download the osm file and save it into folder *input*.
#### 2 - Prepare the entities for simulation
 ##### BartUM has 3 types of entities:
 * GlobalCoordinator: the simulation commander
 * LocalCoordinator: an entity responsible to manage the actors
 * Visualization: an **optional** entity to provide a visual interface for the user
 
 To run a simulation you will need one, and only one *GlobalCoordinator* and at least one *LocalCoordinator*. *Visualization* is not a mandatory entity.


#### 3 - Create the configuration file
In folder *input* you will find the configuration file *settings.properties*. This file already has a sample configuration, that you can edit to your liking. Make sure to set the **GlobalCoordinator.IP** to the machine chosen to run this entity.
For more information, read the user manual in our website. (*link above*)
#### 4 - Run the simulation
Three scripts are created for each one of the entities. 
First, you should run the *GlobalCoordinator*. Open a terminal window and type:
 * sh global.sh
 
Secondly, run the *LocalCoordinator* in a different machine:
 * sh local.sh
 
Finally, if you wish a visual interface run:
 * sh visualization.sh
