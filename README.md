# Earthquake Early Warning Display (EEWD)
EEWD is a demonstration software to be used by scientists and EEW datacentres, or by stakeholders in close collaboration with scientists and EEW datacentres. 

EEWD is designed as a client application of a separate compatible Earthquake Early Warning (EEW) system. As a client application, EEWD will display the warnings on a map, together with rapid computation of related distribution of co-seismic ground motions. 

EEWD connects to the EEW system via an [activeMQ](https://activemq.apache.org) broker server, using the [STOMP](https://stomp.github.io) protocol, and supports the [quakeml-rt](https://quake.ethz.ch/quakeml/QuakeML2.0/BasicEventDescription-RT) format for incoming EEW messages.

Further information on EEWD and the EEW methods developed by the Swiss seismological Service (ETHZ) for seismic monitoring services can be found at  http://www.seismo.ethz.ch/en/research-and-teaching/fields_of_research/earthquake-early-warning/.

# Compilation
Compilation is straightforward:
```
ant
```
The `eewd.jar` Java archive package should be compiled in about four seconds in the directory `dist/`. 

# Usage
To run the project from the command line, go to the dist folder and use:
```
java -jar "dist/eewd.jar" -help
```

# Configuration
Back-up and adjust the `eewd.property` file following the enclosed instructions, another example is provided in `eewd.property.ISNET`. 
