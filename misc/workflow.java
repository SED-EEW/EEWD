public void calculateGroundMotion(double mag, double sourceLat, double sourceLon, double sourceDepthM) {
	// Read amplitude PointList and type from configuration file
	GeoPointList<double> ampPointList = config.getAmplificationPointList(); // from RegionalAmpliProxy.csv file
	String ampType = config.getAmplificationType()
	double period = config.getPeriod()	// you have more than one tipically ...	
	
    AttenuationPGA attenuationPGA = new AttenuationPGA();
	AttenuationPGV attenuationPV = new AttenuationPGV();
	AttenuationPSA attenuationPSA = new AttenuationPSA();
	AttenuationI attenuationI = new AttenuationI();
	AmplyProxy ampProxy = new AmplyProxy();
	
	// initialize result PointLists with amplitude PointList demension
	GeoPointList<Shaking> pgaPointList = new GeoPointList<Shaking>(ampPointList);
	GeoPointList<Shaking> pgvPointList = new GeoPointList<Shaking>(ampPointList);
	GeoPointList<Shaking> psaPointList = new GeoPointList<Shaking>(ampPointList);
	GeoPointList<Shaking> intPointList = new GeoPointList<Shaking>(ampPointList);

	// calculate ground motion values for each amplitude PointList point
	// note that as the RegionalAmpliProxy.csv file is lat,lon,elev,proxy. so there is no guarantee it is really a grid; could be any set of points. thus just sequence trough it, rather than double-looping (while ... hasNext)
	for ( double targetLat = ampPointList.minLat(); targetLat <= ampPointList.maxLat(); targetLat+= ampPointList.dtLat() ) {
		for ( double targetLon = ampPointList.minLon(); targetLon <= ampPointList.maxLon(); targetLat+= ampPointList.dtLon() ) {
           
            // We assume that teh calculation of teh amplification froma the proxy is specific to the implemntation of the GMPE.  
			// calculate groud motion values
			pgaPointList.setValue(lat, lon, attenuationPGA.getPGA(mag, sourceLat, sourceLon, sourceDepthM, targetLat, targetLon, ampProxy.getElevM(targetLat, targetLon), amplificationType, ampProxy.getProxy(targetLat, targetLon)));
			pgvPointList.setValue(lat, lon, attenuationPGV.getPGV(mag, sourceLat, sourceLon, sourceDepthM, targetLat, targetLon, ampProxy.getElevM(targetLat, targetLon), amplificationType, ampProxy.getProxy(targetLat, targetLon)));
			psaPointList.setValue(lat, lon, attenuationPSA.getPSA(mag, sourceLat, sourceLon, sourceDepthM, targetLat, targetLon, ampProxy.getElevM(targetLat, targetLon), amplificationType, ampProxy.getProxy(targetLat, targetLon), period));
			intPointList.setValue(lat, lon, attenuationI.getInt(mag, sourceLat, sourceLon, sourceDepthM, targetLat, targetLon, ampProxy.getElevM(targetLat, targetLon), amplificationType, ampProxy.getProxy(targetLat, targetLon)));
		}
	}
}
