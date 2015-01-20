package org.reakteu.eewd.gmpe.impl;

/*
 * Implementation of the empirical predictive model
 * of Cauzzi et al. (BEE,2014). The prediction model uses VS30 and unspecified style-of-faulting.
 */
import org.reakteu.eewd.utils.GeoCalc;
import org.reakteu.eewd.gmpe.AttenuationPGV;
import org.reakteu.eewd.gmpe.AttenuationPGA;
import org.reakteu.eewd.gmpe.AttenuationPSA;
import org.reakteu.eewd.gmpe.AttenuationDRS;
import org.quakeml.xmlns.bedRt.x12.EventParameters;
import org.reakteu.eewd.data.Shaking;

public class CEA2014 implements AttenuationPGA, AttenuationPGV, AttenuationPSA, AttenuationDRS {

    public static final double[][] Cofs = {
        {1.0000000000000000e-02, -2.1961743955816320e+00, 5.2374500609726815e-01, -6.0944766301039413e-02, -3.8019035608295697e+00, 3.5508081214117443e-01, 1.1641555587691633e+01, 2.1069852795965940e-01, 2.8251069212247770e-01, 2.8288461407896059e-01, -3.1007048160000000e-01, -7.0243768839999998e-01, 2.3191859784562389e+03, -2.4112243133960157e-02, 7.2463366648245209e-02, -5.6316575408539904e-02, 2.5892297207458592e-01, 2.2145060972417371e-01, 3.4070732016665728e-01, 2.5892297207458581e-01, 2.1622210447607693e-01, 3.3733233454858769e-01},
        {2.0000000000000000e-02, -1.8195694621090377e+00, 6.4536799177800097e-01, -7.1882830225919875e-02, -3.8644883929436840e+00, 3.5932035196947759e-01, 1.2135005851703138e+01, 1.9923768273916218e-01, 2.6575598990356558e-01, 2.5978327794044881e-01, -2.8384177970000002e-01, -6.6974162780000002e-01, 2.5288050326671250e+03, -2.7226135776813008e-02, 7.7085304455501033e-02, -5.8995943915412422e-02, 2.5927002121707099e-01, 2.2664087621536685e-01, 3.4436467686679145e-01, 2.5927002121707093e-01, 2.2083100662576255e-01, 3.4056904937067900e-01},
        {2.9999999999999999e-02, -1.2943572408433126e+00, 6.9827075451573273e-01, -7.9563793194872015e-02, -4.0443482492067222e+00, 3.7651632501506399e-01, 1.3315455567438576e+01, 1.7811526577077880e-01, 2.3159778709560647e-01, 2.1528153157553842e-01, -2.2882264220000001e-01, -6.1914165769999996e-01, 3.2284186287294810e+03, -3.3334058161165567e-02, 8.3593146639694457e-02, -6.2051020233367164e-02, 2.6103594215179410e-01, 2.3293788757694239e-01, 3.4985686010693440e-01, 2.6103594215179410e-01, 2.2621846218988231e-01, 3.4541939107500902e-01},
        {5.0000000000000003e-02, -1.7389653556781853e-01, 7.0382801477550727e-01, -8.7550772099494134e-02, -4.4155335972196896e+00, 4.1403065951785945e-01, 1.5963408501188804e+01, 1.6785310237919973e-01, 1.8633425538696122e-01, 1.3928556072989301e-01, -1.0000000000000001e-01, -5.6395019989999995e-01, 3.0552327167300569e+04, -3.8995585563579072e-02, 9.6030439677199098e-02, -7.0677914899529862e-02, 2.7025697769946511e-01, 2.4236240698397229e-01, 3.6301290653957996e-01, 2.7025697769946522e-01, 2.3377368912652097e-01, 3.5733593679207876e-01},
        {1.0000000000000001e-01, 6.7593512029824820e-01, 6.7015551143715946e-01, -8.4248713340999967e-02, -4.2989668796524665e+00, 3.9501509427567866e-01, 1.6949939918648305e+01, 1.9822568145527719e-01, 2.0041135773434327e-01, 1.3129956062931891e-01, -1.0000000000000001e-01, -5.9237242739999996e-01, 3.6597560045184233e+04, -2.8298983036441912e-02, 1.0056805314026121e-01, -7.9951133827065979e-02, 2.8648712133939630e-01, 2.4333680953070377e-01, 3.7588252628436453e-01, 2.8648712133939636e-01, 2.3432818637911351e-01, 3.7011426563300498e-01},
        {2.0000000000000001e-01, -9.6472284928588614e-02, 6.3941549737682701e-01, -6.2561780991999980e-02, -3.4154331729506939e+00, 3.0100512874157603e-01, 1.1453463159543706e+01, 2.8213934467418772e-01, 4.0589298767621884e-01, 3.6908965423490159e-01, -4.3844921050000002e-01, -8.9443589180000005e-01, 1.8982857019816554e+03, -3.6711341998801662e-03, 6.9339895033686566e-02, -6.3390516773482161e-02, 2.9325374736001419e-01, 2.1597167687593100e-01, 3.6419984287928048e-01, 2.9325374736001408e-01, 2.1124644453037747e-01, 3.6141779240570993e-01},
        {4.0000000000000002e-01, -1.0245292872263463e+00, 7.4770639806754491e-01, -5.6269250303999027e-02, -2.8852884017037828e+00, 2.5367832881659264e-01, 6.2119923952061891e+00, 1.8582347886042111e-01, 4.1073333374376420e-01, 5.2099466849612042e-01, -8.1008842839999995e-01, -8.5303308749999995e-01, 7.9455322052698364e+02, 4.0981773931694315e-04, 4.0388675364612082e-02, -3.8491684998828574e-02, 2.9877899823765147e-01, 2.0685782060711908e-01, 3.6339929517573588e-01, 2.9877899823765136e-01, 2.0517400070934894e-01, 3.6244345814895651e-01},
        {1.0000000000000000e+00, -2.4878726728670095e+00, 1.2134822214871814e+00, -8.5428000000130663e-02, -2.8543804213112907e+00, 2.5936994056903145e-01, 4.9780849514747230e+00, 1.5767219866083215e-01, 3.9033261708659733e-01, 6.9326301791846945e-01, -9.8918756510000005e-01, -8.2480446350000003e-01, 6.7861227017647923e+02, 3.4474250919355034e-02, -5.4363568289915549e-03, -8.2972422158439901e-03, 2.9631769967431854e-01, 2.3075007445443896e-01, 3.7556594094913448e-01, 2.9631769967431870e-01, 2.3026086564024689e-01, 3.7526556648016024e-01},
        {2.0000000000000000e+00, -3.4835528377726117e+00, 1.6562992065143463e+00, -1.1541476613278204e-01, -3.0442154569996256e+00, 2.6418135036296786e-01, 8.9746507855722584e+00, 1.0587451541242687e-01, 3.2566708834628233e-01, 5.3511581230257277e-01, -7.9095909980000001e-01, -6.3422771099999997e-01, 6.4112233696831072e+02, -1.6239650687487832e-02, -1.9971914576055435e-03, 8.0969196977610028e-03, 2.9064221903649096e-01, 2.2570080810256782e-01, 3.6798607890055801e-01, 2.9064221903649112e-01, 2.2558816573708279e-01, 3.6791700151946938e-01},
        {-1.0000000000000000e+00, 4.4221599463656885e-01, 5.4822393788181423e-01, -3.1947025802877768e-02, -2.8457788432226732e+00, 2.4067370474140704e-01, 6.5169666628779801e+00, 1.9192773133611185e-01, 3.7061964432402356e-01, 4.9780181892497277e-01, -6.9095802279999996e-01, -7.5968044040000005e-01, 8.8395654064777011e+02, -1.4333130272087613e-01, 1.8463316092423347e-02, 4.9897699311182977e-03, 2.3989358269567340e-01, 2.2130043462084306e-01, 3.2637832860338617e-01, 2.3989359224537798e-01, 2.1494042982279291e-01, 3.2209986645883393e-01}
    };

    public static final double PI2_4 = 4 * Math.PI * Math.PI;

    @Override
    public Shaking getPGA(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI,
                          EventParameters eventParameters) {

        // Returns median PGA, 16th-percentile PGA, 84th percentile PGA in m/s2
        // Mag is the magnitude from the EW message
        // ampType is VS30
        double Mw = magnitude;	// reasonable assumption

        // Compute hypocentral distance
        double[] pEvent = GeoCalc.Geo2Cart(sourceLat, sourceLon, -sourceDepthM);
        double[] pTarget = GeoCalc.Geo2Cart(targetLat, targetLon, targetElevM);
        double distance = GeoCalc.Distance3D(pEvent, pTarget);

        double Rh = distance / 1000; // in kilometers

        // end of hypocentral distance computation
        // Assume Rrup ~ Rh
        double Rrup = Rh;

        // Compute ground-motion prediction in log10 first, site amplification
        // based on VS30 is included here
        double logdrs01site = Cofs[0][1]
                            + Cofs[0][2] * Mw
                            + Cofs[0][3] * Math.pow(Mw, 2)
                            + (Cofs[0][4] + Cofs[0][5] * Mw) * Math.log10(Rrup + Cofs[0][6])
                            + Cofs[0][10] * Math.log10(amplificationProxyValueSI / Cofs[0][12]);
                        

        // Now compute plus/minus sigma bounds
        double sigma = Cofs[0][18];
        double logdrs01siteplus = logdrs01site + sigma;
        double logdrs01siteminus = logdrs01site - sigma;

        // Now in m/s2
        Shaking PGA = new Shaking();
        PGA.expectedSI = Math.pow(10, logdrs01site) * (PI2_4 / (0.01 * 0.01)) / 100;
        PGA.percentile84 = Math.pow(10, logdrs01siteplus) * (PI2_4 / (0.01 * 0.01)) / 100;
        PGA.percentile16 = Math.pow(10, logdrs01siteminus) * (PI2_4 / (0.01 * 0.01)) / 100;

        // Now should return Shaking ...
        return PGA;
    }

    @Override
    public Shaking getPGV(double Mag, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI,
                          EventParameters eventParameters) {

        // Returns median PGV, 16th-percentile PGV, 84th percentile PGV in m/s
        // Mag is the magnitude from the EW message
        // ampType is VS30
        double Mw = Mag;	// reasonable assumption

        // Compute hypocentral distance
        double[] pEvent = GeoCalc.Geo2Cart(sourceLat, sourceLon, -sourceDepthM);
        double[] pTarget = GeoCalc.Geo2Cart(targetLat, targetLon, targetElevM);
        double distance = GeoCalc.Distance3D(pEvent, pTarget);

        double Rh = distance / 1000; // in kilometers

        // end of hypocentral distance computation
        // Assume Rrup ~ Rh
        double Rrup = Rh;

        // Compute ground-motion prediction in log10 first, site amplification
        // based on VS30 is included here
        double logpgvsite = Cofs[9][1]
                        + Cofs[9][2] * Mw
                        + Cofs[9][3] * Math.pow(Mw, 2)
                        + (Cofs[9][4] + Cofs[9][5] * Mw) * Math.log10(Rrup + Cofs[9][6])
                        + Cofs[9][10] * Math.log10(amplificationProxyValueSI / Cofs[9][12]);

        // Now compute plus/minus sigma bounds
        double sigma = Cofs[9][18];
        double logpgvsiteplus = logpgvsite + sigma;
        double logpgvsiteminus = logpgvsite - sigma;

        // Now in m/s
        Shaking PGV = new Shaking();
        PGV.expectedSI = Math.pow(10, logpgvsite) / 100;
        PGV.percentile84 = Math.pow(10, logpgvsiteplus) / 100;
        PGV.percentile16 = Math.pow(10, logpgvsiteminus) / 100;

        // Now should return Shaking ...
        return PGV;
    }

    @Override
    public Shaking getPSA(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI, double period,
                          EventParameters eventParameters) {

        // Returns median PSA, 16th-percentile PSA, 84th percentile PSA in m/s2 for a given spectral period T
        // Mag is the magnitude from the EW message
        // ampType is VS30
        double Mw = magnitude;	// reasonable assumption

        int cnt = 0; // init
        double sigma = 0; //init

        // Compute hypocentral distance
        double[] pEvent = GeoCalc.Geo2Cart(sourceLat, sourceLon, -sourceDepthM);
        double[] pTarget = GeoCalc.Geo2Cart(targetLat, targetLon, targetElevM);
        double distance = GeoCalc.Distance3D(pEvent, pTarget);

        double Rh = distance / 1000; // in kilometers

        // end of hypocentral distance computation
        // Assume Rrup ~ Rh
        double Rrup = Rh;

        // pick the right coefficients according to the spectral period
        if (period == 0.01) {
            cnt = 0;
            sigma = Cofs[cnt][18];
        } else if (period == 0.02) {
            cnt = 1;
            sigma = Cofs[cnt][18];
        } else if (period == 0.03) {
            cnt = 2;
            sigma = Cofs[cnt][18];
        } else if (period == 0.05) {
            cnt = 3;
            sigma = Cofs[cnt][18];
        } else if (period == 0.1) {
            cnt = 4;
            sigma = Cofs[cnt][18];
        } else if (period == 0.2) {
            cnt = 5;
            sigma = Cofs[cnt][18];
        } else if (period == 0.4) {
            cnt = 6;
            sigma = Cofs[cnt][18];
        } else if (period == 1) {
            cnt = 7;
            sigma = Cofs[cnt][18];
        } else if (period == 2) {
            cnt = 8;
            sigma = Cofs[cnt][18];
        }

        double logdrssite = Cofs[cnt][1]
                          + Cofs[cnt][2] * Mw
                          + Cofs[cnt][3] * Math.pow(Mw, 2)
                          + (Cofs[cnt][4] + Cofs[cnt][5] * Mw) * Math.log10(Rrup + Cofs[cnt][6])
                          + Cofs[cnt][10] * Math.log10(amplificationProxyValueSI / Cofs[cnt][12]);
        
       

        // Now compute plus/minus sigma bounds
        double logdrssiteplus = logdrssite + sigma;
        double logdrssiteminus = logdrssite - sigma;

        // Now in m/s2
        Shaking PSA = new Shaking();
        PSA.expectedSI = Math.pow(10, logdrssite) * (PI2_4 / (period * period)) / 100;
        PSA.percentile84 = Math.pow(10, logdrssiteplus) * (PI2_4 / (period * period)) / 100;
        PSA.percentile16 = Math.pow(10, logdrssiteminus) * (PI2_4 / (period * period)) / 100;

        // Now should return Shaking ...
        return PSA;
    }

    @Override
    public Shaking getDRS(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI, double period,
                          EventParameters eventML) {

        Shaking PSA = getPSA(magnitude, sourceLat, sourceLon, sourceDepthM,
                             targetLat, targetLon, targetElevM,
                             amplificationType, amplificationProxyValueSI,
                             period, null);

        double accelerationToDisplacement = period * period / PI2_4;
        PSA.expectedSI *= accelerationToDisplacement;
        PSA.percentile16 *= accelerationToDisplacement;
        PSA.percentile84 *= accelerationToDisplacement;

        // Now in m/s
        return PSA;
    }
}
