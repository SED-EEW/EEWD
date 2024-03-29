package org.reakteu.eewd.gmpe.impl;

/*
 * Implementation of the stochastic GMP model (for max delta_sigma = 60 bar)
 * of Edwards and Faeh (BSSA,2013) following
 * the parameterisation by Cauzzi et al. (GJI,2014)
 */
import org.reakteu.eewd.data.Shaking;
import org.reakteu.eewd.utils.GeoCalc;
import org.reakteu.eewd.gmpe.AttenuationPGV;
import org.reakteu.eewd.gmpe.AttenuationPGA;
import org.reakteu.eewd.gmpe.AttenuationPSA;
import org.reakteu.eewd.gmpe.AttenuationDRS;
import org.reakteu.eewd.data.EventData;
import org.quakeml.xmlns.bedRt.x12.EventParameters;

import org.apache.commons.math3.*;

import static java.lang.Math.*;


public class Swiss implements AttenuationPGA, AttenuationPGV, AttenuationPSA, AttenuationDRS {

    public static final double[][] CofsForeland = {
        {-4.8734444890499615e+00, 5.7255139238339519e+00, -3.2351171788383506e+00, 1.1753012922786823e+00, -2.2577904419461686e-01, 2.1104710708239459e-02, -7.6233728465190120e-04, 7.4754695839979135e-01, -2.8587026680357791e-01, 2.8522264972244808e-02, -1.4745926647067747e-03, -1.8388979803739185e+00, -7.7828595688719682e-01, 2.2898722461238621e-01, -1.2605940688534385e-02, 7.8494228230835739e-01, 6.3277279490167593e-01, -1.7398944574490496e-01, 1.0325971760028901e-02, -1.5205723450111922e-01, -1.2267103903416217e-01, 3.4545766610307123e-02, -2.1674727351593762e-03, 4.4371298316269785e-02},
        {-4.2446452870116236e+00, 4.8375251725164974e+00, -2.7229788021309029e+00, 1.0238294649223574e+00, -2.0150405845878450e-01, 1.9103910868256562e-02, -6.9598181556154671e-04, 6.7610216228600528e-01, -1.9743837389084248e-01, 6.8329307440026504e-03, 3.1438073658283688e-06, -1.6718093601509403e+00, -9.3890749215206670e-01, 2.6556200494508886e-01, -1.5024413301628218e-02, 6.6220109836230912e-01, 7.3425714009626997e-01, -1.9634171963751484e-01, 1.1791631195220621e-02, -1.2596193298445540e-01, -1.4279575092003813e-01, 3.8915852296683949e-02, -2.4535356105663945e-03, 4.4657334934501053e-02},
        {-4.8153134024948159e+00, 5.8380709834272624e+00, -3.3163640277754953e+00, 1.2043389351525382e+00, -2.3106248556484119e-01, 2.1578696524461732e-02, -7.7924162352029775e-04, 1.3303587078283059e+00, -4.1850409536098826e-01, 3.0878206881718231e-02, -8.6075525343879116e-04, -2.1860943403376427e+00, -8.0701948066394802e-01, 2.5918415043220511e-01, -1.5329696264634360e-02, 7.2444091248877507e-01, 7.3801989254560374e-01, -2.0453804851253163e-01, 1.2648971445691016e-02, -1.1236410724530474e-01, -1.5146665362985137e-01, 4.1640419403411601e-02, -2.6845761025432791e-03, 4.6290199994659983e-02},
        {-3.6050636356179018e+00, 4.8489370464740080e+00, -2.8195604236562724e+00, 1.0616667552996100e+00, -2.0865056492471559e-01, 1.9784974418035593e-02, -7.2180352968815589e-04, -2.1310503359021243e-01, -1.1827200590589794e-01, 3.9719562210626197e-02, -3.7413901024134378e-03, -3.0373830619200032e-01, -1.0658496914519333e+00, 2.2747272739577862e-01, -1.0841345245728856e-02, -1.4965727889034416e-01, 8.0141193490768170e-01, -1.7857688905941749e-01, 9.9689761221042669e-03, 1.8144953519809475e-02, -1.5036087805425485e-01, 3.5400860421632041e-02, -2.1409991173865811e-03, 4.7972391544560102e-02},
        {-3.5833569372994569e+00, 5.0949162813834876e+00, -2.9500327344177397e+00, 1.0957192856247777e+00, -2.1388090079028807e-01, 2.0230740476450439e-02, -7.3752437934239586e-04, -1.8496533499805928e-01, -4.2825229884086019e-01, 1.2221751051487911e-01, -9.3431429954146800e-03, -1.0974694238106695e+00, -3.0705765663313950e-01, 6.8028847174789880e-02, -9.1850084459168559e-04, 6.7668777354780463e-01, 2.5754027810917229e-01, -7.5859960826042128e-02, 3.8065156799217850e-03, -1.8795951256902838e-01, -3.6918482778321070e-02, 1.5196932862080011e-02, -9.5290080198249963e-04, 4.9149578814200331e-02},
        {-4.9250110022823801e+00, 5.4174958036084018e+00, -2.8653072958655130e+00, 1.0447296958832424e+00, -2.0367954004140640e-01, 1.9272323613215445e-02, -7.0273067369517619e-04, 2.4844568372869138e+00, -8.8865324605620155e-01, 9.4040692952708324e-02, -3.6061981132415517e-03, -3.6611476219406298e+00, -1.7252151262935150e-01, 1.6331030012351960e-01, -1.0388801800749636e-02, 1.6849065169426787e+00, 3.8507213516291405e-01, -1.5085855011692617e-01, 9.6580272999715975e-03, -3.0276841096310497e-01, -9.3881233378616619e-02, 3.2914101799115567e-02, -2.1664501339745228e-03, 4.5852823527052007e-02},
        {-5.5753559893231852e+00, 5.3898411945448155e+00, -2.7436738305681851e+00, 1.0057959721314511e+00, -1.9784324349408502e-01, 1.8827385586019690e-02, -6.8883565947308709e-04, 2.1188500352243813e+00, -7.3127525689493711e-01, 7.3111506905435150e-02, -2.6637143675470763e-03, -3.1082668808820890e+00, -2.5451352075120659e-01, 1.4802789158943858e-01, -8.3544121764943385e-03, 1.3635741562925447e+00, 4.0272005243329673e-01, -1.3229655579996685e-01, 7.7500725018520289e-03, -2.2920526363060009e-01, -9.4231418752258433e-02, 2.8159888246366907e-02, -1.7111754712627512e-03, 4.2765888827728742e-02},
        {-7.4174778523208502e+00, 7.0120862110271052e+00, -3.7266446962198794e+00, 1.3273234473295785e+00, -2.5228480376180218e-01, 2.3371344590677266e-02, -8.3718092621545967e-04, 3.1077754520606957e+00, -1.5277175237407317e+00, 2.4159713827136120e-01, -1.3272463117542283e-02, -4.7091015289919165e+00, 1.0902332572871154e+00, -1.3965599017553609e-01, 9.9120675798812924e-03, 2.2367507762628231e+00, -3.3404413471441413e-01, 2.5635024408971693e-02, -2.3304748000239810e-03, -3.7915601940678151e-01, 3.7735509725913545e-02, -1.4889851440892790e-04, 1.0135688861650937e-04, 3.9717435018986849e-02},
        {-1.0663248829944752e+01, 1.1818835541200810e+01, -6.7022241401697542e+00, 2.1520757105783792e+00, -3.6445442438643177e-01, 3.0640321209439565e-02, -1.0129872975743089e-03, -4.7403186359961885e-01, 1.5445218463265081e-01, -2.2579061519715591e-02, 5.1359165783287095e-04, -1.8136288257458135e+00, -2.9749427498946279e-01, 8.6014942352336532e-02, -2.2838345330654275e-03, 1.1350345228404799e+00, 2.2396801789156529e-01, -6.8407899012118911e-02, 2.8310535189435852e-03, -2.1752643160969490e-01, -4.6872706737001849e-02, 1.4541030922239125e-02, -7.1132952127526657e-04, 3.4410508831964517e-02},
        {-8.4138482663055054e+00, 6.3108096485113636e+00, -2.8917480174205745e+00, 8.4510808116413472e-01, -1.2869841535347737e-01, 9.4034957337319428e-03, -2.6114143967442111e-04, 2.3221538279298015e-01, 4.4448605648139150e-01, -1.5304911065720256e-01, 1.1026542729239561e-02, -2.3456452285051350e+00, -1.0422763035366422e+00, 3.3376028020556869e-01, -2.0714796378786784e-02, 1.3499649381398813e+00, 6.0703152410927852e-01, -1.8886699385866706e-01, 1.1528564633725771e-02, -2.5217400832468112e-01, -1.0699957513057776e-01, 3.3134938196294379e-02, -2.0298010271835596e-03, 3.3765358452780059e-02},
        {-6.6876411704769829e+00, 5.1783616860806241e+00, -2.7235796592366301e+00, 9.8177277988765177e-01, -1.8794348818415998e-01, 1.7484592401503837e-02, -6.2756849508528083e-04, 7.8845747760370566e-01, -2.2027353731526697e-01, 6.5613471384871553e-03, 2.5462780270476502e-04, -2.0082422170495149e+00, -7.3391502904102646e-01, 2.2380561214821715e-01, -1.2327602382274670e-02, 9.2086424816546830e-01, 5.7632180389102894e-01, -1.6128677911324510e-01, 9.3637431646426302e-03, -1.6656693325605040e-01, -1.1395562907589216e-01, 3.2082867955386923e-02, -1.9542747530217871e-03, 3.8720241865646120e-02}
    };

    public static final double[][] CofsAlpine = {
        {-5.3146900016338510e+00, 6.2957248596285167e+00, -3.7449158709598422e+00, 1.4015937606588200e+00, -2.7368447520636996e-01, 2.5873999172113884e-02, -9.4285132304268719e-04, 1.9793031714745934e+00, -1.3341625107460098e+00, 2.4612361958270648e-01, -1.4252321442425938e-02, -2.9855888403247950e+00, 6.8626553128429968e-01, -7.0259855981400707e-02, 4.0639140462276954e-03, 1.1394868684306036e+00, -1.0046152533857015e-01, -2.6806302181269750e-02, 2.4180745526655636e-03, -1.8206533762089566e-01, -3.0855777385753575e-03, 1.0716439211209130e-02, -9.1449163395200918e-04, 5.5451188438455028e-02},
        {-6.3268919374356640e+00, 7.6374371193168145e+00, -4.4840542192168105e+00, 1.6163347691377763e+00, -3.0783355945908203e-01, 2.8674756381306541e-02, -1.0353838629839709e-03, 2.9542054860940614e+00, -1.8911702842060949e+00, 3.4760616616867224e-01, -2.0186652596393035e-02, -4.3640249870483849e+00, 1.4973021334210226e+00, -2.2261610551473582e-01, 1.3221367355124164e-02, 1.8681606584303252e+00, -5.3978044478865450e-01, 5.7069753045379416e-02, -2.6887748239240537e-03, -3.1173676163793523e-01, 7.6392598291573147e-02, -4.6066686709857040e-03, 2.5552770789878845e-05, 5.5728151642376912e-02},
        {-6.0635521289433463e+00, 7.5034515341845802e+00, -4.4235695883600217e+00, 1.6024221281402720e+00, -3.0621187714633941e-01, 2.8586182788592329e-02, -1.0339010503120224e-03, 3.2162757676678662e+00, -1.9861504765411495e+00, 3.5882859335616679e-01, -2.0643680224618672e-02, -4.5919369127648446e+00, 1.5744638582966126e+00, -2.3114419757894525e-01, 1.3534446033288712e-02, 1.8707414334811077e+00, -5.5029540350924455e-01, 5.7223341992109635e-02, -2.5841963315939615e-03, -2.9724157085212161e-01, 7.4765122666702433e-02, -4.1147788144311279e-03, -2.7409382824134699e-05, 5.8340719316871556e-02},
        {-5.3796420657365047e+00, 7.1387886048008049e+00, -4.3005589689179811e+00, 1.5751643353475868e+00, -3.0256437589760699e-01, 2.8335804758456384e-02, -1.0274385324615129e-03, 1.6311443262301084e+00, -1.3752079406220228e+00, 2.8099772632584052e-01, -1.7417707343129845e-02, -2.4396430874683248e+00, 8.3581450851919847e-01, -1.4429336556229524e-01, 1.0167658221139953e-02, 7.6226112022572412e-01, -2.0436733166418972e-01, 1.8608394354779425e-02, -1.1636115381665263e-03, -1.1084824250816581e-01, 2.0323531201526249e-02, 1.7059098660595644e-03, -2.2844264413524577e-04, 5.9219202190104556e-02},
        {-4.8778811422125639e+00, 6.6213687668748307e+00, -4.0124399538478528e+00, 1.4917415730692398e+00, -2.8959634614210583e-01, 2.7312677993628572e-02, -9.9516209998696419e-04, 1.3129022467616238e+00, -1.2870901590502473e+00, 2.7243883727478202e-01, -1.7152033117731044e-02, -2.2286766182270048e+00, 8.0719028049204145e-01, -1.4125658164357516e-01, 1.0029747326901331e-02, 8.5716670650037685e-01, -2.3988278403806193e-01, 2.0842650684712454e-02, -1.2138191699592156e-03, -1.6344655239887851e-01, 3.2661826211388030e-02, 1.0648380617364259e-03, -2.1969308509937980e-04, 5.6537962409412487e-02},
        {-6.3443108627086175e+00, 7.5780246548641008e+00, -4.3400314223019869e+00, 1.5658603142530276e+00, -2.9994690146805125e-01, 2.8093318049476162e-02, -1.0191313824126906e-03, 4.5691884559093072e+00, -2.5918067136161134e+00, 4.4965575710946964e-01, -2.5149625249011636e-02, -6.3158701963930355e+00, 2.3776398061682826e+00, -3.5953193251124316e-01, 2.0365491803892376e-02, 2.9137074587145131e+00, -9.6623669704865722e-01, 1.2292681902684389e-01, -6.2203187581537584e-03, -5.0198090682801011e-01, 1.4327857356376864e-01, -1.4843800857446575e-02, 5.8989398381297734e-04, 5.6509594297524855e-02},
        {-7.2922737267140487e+00, 8.1394230956013658e+00, -4.5686313359280861e+00, 1.6235861720999509e+00, -3.0795154771296185e-01, 2.8648584039163160e-02, -1.0340938539987886e-03, 4.3603630553299730e+00, -2.4984228046884955e+00, 4.3657588611195791e-01, -2.4495965247635264e-02, -5.9079024007466971e+00, 2.3264893363405887e+00, -3.7146411732750884e-01, 2.1714111375464726e-02, 2.6526753218817660e+00, -9.4964536734993654e-01, 1.3573477711296397e-01, -7.4528911694430877e-03, -4.3872960011328971e-01, 1.4196026466970671e-01, -1.8201244486679462e-02, 8.9338911040402270e-04, 5.4235601357194935e-02},
        {-9.1860436519238551e+00, 9.9061288508610907e+00, -5.5829006949720767e+00, 1.9269942256852819e+00, -3.5486123624739307e-01, 3.2235514051287037e-02, -1.1416692681900219e-03, 3.8883571573096782e+00, -2.3320976960214921e+00, 4.1729258991789503e-01, -2.3753415805587264e-02, -5.2714219605344503e+00, 2.2300860642815317e+00, -3.7900704456002621e-01, 2.2954256204962463e-02, 2.3058257391359573e+00, -9.0494686362004217e-01, 1.4148869384922355e-01, -8.2374183670670554e-03, -3.6892526978207252e-01, 1.3331050259504118e-01, -1.9058307908507842e-02, 1.0223123924171563e-03, 5.0281825226221488e-02},
        {-1.2179537011373055e+01, 1.3713224619308468e+01, -7.8130198501285255e+00, 2.5008895961948876e+00, -4.2342642685781068e-01, 3.5705235467867510e-02, -1.1863724218090111e-03, 6.4317628979091257e-01, -8.8348564433046584e-01, 2.0223898722340289e-01, -1.3141971042983109e-02, -2.3551700079732782e+00, 1.0550945572454022e+00, -2.1848136736074891e-01, 1.5556033242037822e-02, 1.1148030538419753e+00, -4.2866571875793208e-01, 7.7334962409954397e-02, -5.3604505043135953e-03, -1.8615368277786504e-01, 5.9527406499058785e-02, -8.8688991272957794e-03, 5.6035106071277438e-04, 4.3833288013001681e-02},
        {-7.8377014548992845e+00, 6.0718886286298712e+00, -2.8643669587657663e+00, 8.5033428942723122e-01, -1.3081971649289634e-01, 9.6747555256755657e-03, -2.7322679495276161e-04, -6.2917562743281985e-01, -6.2848195941942811e-02, 4.2491596057701309e-02, -3.4184005526814087e-03, -1.1420241146191588e+00, -2.6971084262168676e-01, 9.2647927589273912e-02, -5.2168140760696273e-03, 5.7306580322528611e-01, 3.1101099500948465e-01, -1.0199310517337591e-01, 6.6696514260538992e-03, -9.5649447259471027e-02, -7.5212511592881101e-02, 2.4005108182250285e-02, -1.6368058742462651e-03, 4.1287969905759017e-02},
        {-8.6587514514963164e+00, 7.8581770083532456e+00, -4.3760448590555843e+00, 1.5273946197406352e+00, -2.8470661638661593e-01, 2.6135730146693126e-02, -9.3313042977851107e-04, 3.1153778360318634e+00, -1.9437512823572183e+00, 3.5213587900454657e-01, -2.0160547327491181e-02, -4.5671124740347082e+00, 1.5857541868942817e+00, -2.3340517975581701e-01, 1.3453167374150247e-02, 1.9925733004925434e+00, -5.8505507113696154e-01, 6.3500948383173689e-02, -2.9092300011671602e-03, -3.2315615232297668e-01, 8.0336565978056440e-02, -5.1093250020588076e-03, 3.7954615973731321e-05, 4.9975855017753479e-02}
    };

    public static final double PI2_4 = 4 * PI * PI;

    public static final double[][] getCofs(double lat, double lon) {
        return (0.39 * lon + 44) < lat ? CofsForeland : CofsAlpine;
    }
    
   

    @Override
    public Shaking getPGA(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI,
                          EventParameters eventParameters,
                          Float ruptureLength,
                          Float ruptureStrike) {

        // Returns median PGA, 16th-percentile PGA, 84th percentile PGA in m/s2
        // Mag is the magnitude from the EW message
        // ampType in Switzerland is deltaI, i.e. intensity increments
        // GMP coefficients are region dependent, i.e. different in the Swiss Alps and in the Swiss Foreland
        double rmin = 3; // simplified cut-off distance
        double Mw = magnitude;	// reasonable assumption in CH
        double[][] cofs = getCofs(sourceLat, sourceLon);

        // Compute distance
        //double[] pEvent = GeoCalc.Geo2Cart(sourceLat, sourceLon, -sourceDepthM); deprecated
        //double[] pTarget = GeoCalc.Geo2Cart(targetLat, targetLon, targetElevM); deprectaed
        
        double[] pEvent = {sourceLat, sourceLon, -sourceDepthM};
        double[] pTarget = {targetLat, targetLon, targetElevM};
        
        
        double distance;
        
        if (ruptureLength != null) {
        	
        	double[] lExtremes = GeoCalc.CentroidToExtremes(ruptureStrike, ruptureLength, sourceLon, sourceLat, -sourceDepthM);
            double[] start = {lExtremes[1],lExtremes[0],lExtremes[2]};
            double[] end = {lExtremes[4],lExtremes[3],lExtremes[5]};
            double[] current = {pTarget[0],pTarget[1]};
            double d = GeoCalc.DistanceFromLine(start, end, current);
            distance = Math.sqrt(d * d + (sourceDepthM + targetElevM) * (sourceDepthM + targetElevM));
             
            
        } else {
        
        	distance = GeoCalc.Distance3DDegToM(pEvent, pTarget);
        }

        double Rh = distance / 1000; // in kilometers

        // end of distance computation
        
        // Assume Rrup ~ Rh for magnitude < 5.8
        double Rrup = Rh;
        // else estimate Rrup based on Cauzzi et al. (2014)
        
        if (Mw >= 5.8){
            Rrup = Rh + 7.5 * Mw - 38 - 1.38 - 0.014 * exp(Mw); // 7.5 * Mw + 38 included to avoid negative distances at points with Rh = 0 ... 
        }

        // define the distance cut-off
        double Ru = max(rmin, Rrup);

        // define the distance metric used in the attenuation formulas
        double d = log10(Ru);

        // Compute ground-motion prediction in log10 first
        double logpga = cofs[0][0] + cofs[0][1] * Mw + cofs[0][2] * pow(Mw, 2) + cofs[0][3] * pow(Mw, 3) + cofs[0][4] * pow(Mw, 4) + cofs[0][5] * pow(Mw, 5) + cofs[0][6] * pow(Mw, 6) + (cofs[0][7] + cofs[0][8] * Mw + cofs[0][9] * pow(Mw, 2) + cofs[0][10] * pow(Mw, 3)) * d + (cofs[0][11] + cofs[0][12] * Mw + cofs[0][13] * pow(Mw, 2) + cofs[0][14] * pow(Mw, 3)) * pow(d, 2) + (cofs[0][15] + cofs[0][16] * Mw + cofs[0][17] * pow(Mw, 2) + cofs[0][18] * pow(Mw, 3)) * pow(d, 3) + (cofs[0][19] + cofs[0][20] * Mw + cofs[0][21] * pow(Mw, 2) + cofs[0][22] * pow(Mw, 3)) * pow(d, 4);

        // Now add site term
        double logpgasite = logpga + (amplificationProxyValueSI / 2.58);

        // Now compute plus/minus sigma bounds
        double sigma = 0.2910;
        double logpgasiteplus = logpgasite + sigma;
        double logpgasiteminus = logpgasite - sigma;

        // Now in m/s2
        Shaking PGA = new Shaking();
        PGA.expectedSI = pow(10, logpgasite) / 100;
        PGA.percentile84 = pow(10, logpgasiteplus) / 100;
        PGA.percentile16 = pow(10, logpgasiteminus) / 100;

        // Now should return Shaking ...
        return PGA;
    }

    @Override
    public Shaking getPGV(double Mag, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI,
                          EventParameters eventParameters,
                          Float ruptureLength,
                          Float ruptureStrike) {

        // Returns median PGV, 16th-percentile PGV, 84th percentile PGV in m/s
        // Mag is the magnitude from the EW message
        // ampType in Switzerland is deltaI, i.e. intensity increments
        // GMP coefficients are region dependent, i.e. different in the Swiss Alps and in the Swiss Foreland
        double rmin = 3; // set cut-off distance
        double Mw = Mag;	// reasonable assumption in CH
        double[][] cofs = getCofs(sourceLat, sourceLon);

        // Compute distance
        //double[] pEvent = GeoCalc.Geo2Cart(sourceLat, sourceLon, -sourceDepthM); deprecated
        //double[] pTarget = GeoCalc.Geo2Cart(targetLat, targetLon, targetElevM); deprectaed
        
        double[] pEvent = {sourceLat, sourceLon, -sourceDepthM};
        double[] pTarget = {targetLat, targetLon, targetElevM};
        
        
        double distance;
        
        if (ruptureLength != null) {
        	
        	double[] lExtremes = GeoCalc.CentroidToExtremes(ruptureStrike, ruptureLength, sourceLon, sourceLat, -sourceDepthM);
            double[] start = {lExtremes[1],lExtremes[0],lExtremes[2]};
            double[] end = {lExtremes[4],lExtremes[3],lExtremes[5]};
            double[] current = {pTarget[0],pTarget[1]};
            double d = GeoCalc.DistanceFromLine(start, end, current);
            distance = Math.sqrt(d * d + (sourceDepthM + targetElevM) * (sourceDepthM + targetElevM));
             
            
        } else {
        
        	distance = GeoCalc.Distance3DDegToM(pEvent, pTarget);
        }

        double Rh = distance / 1000; // in kilometers

        // end of distance computation
        
        // Assume Rrup ~ Rh
        double Rrup = Rh;
        // else estimate Rrup based on Cauzzi et al. (2014)
        
        if (Mw >= 5.8){
            Rrup = Rh + 7.5 * Mw - 38 - 1.38 - 0.014 * exp(Mw); // 7.5 * Mw + 38 included to avoid negative distances at points with Rh = 0 ...
        }

        // define the distance cut-off
        double Ru = max(rmin, Rrup);

        // define the distance metric used in the attenuation formulas
        double d = log10(Ru);

        double logpgv = cofs[10][0] + cofs[10][1] * Mw + cofs[10][2] * pow(Mw, 2) + cofs[10][3] * pow(Mw, 3) + cofs[10][4] * pow(Mw, 4) + cofs[10][5] * pow(Mw, 5) + cofs[10][6] * pow(Mw, 6) + (cofs[10][7] + cofs[10][8] * Mw + cofs[10][9] * pow(Mw, 2) + cofs[10][10] * pow(Mw, 3)) * d + (cofs[10][11] + cofs[10][12] * Mw + cofs[10][13] * pow(Mw, 2) + cofs[10][14] * pow(Mw, 3)) * pow(d, 2) + (cofs[10][15] + cofs[10][16] * Mw + cofs[10][17] * pow(Mw, 2) + cofs[10][18] * pow(Mw, 3)) * pow(d, 3) + (cofs[10][19] + cofs[10][20] * Mw + cofs[10][21] * pow(Mw, 2) + cofs[10][22] * pow(Mw, 3)) * pow(d, 4);

        // Now add site term
        double logpgvsite = logpgv + (amplificationProxyValueSI / 2.35);

        // Now compute plus/minus sigma bounds
        double sigma = 0.2953;
        double logpgvsiteplus = logpgvsite + sigma;
        double logpgvsiteminus = logpgvsite - sigma;

        // Now in m/s
        Shaking PGV = new Shaking();
        PGV.expectedSI = pow(10, logpgvsite) / 100;
        PGV.percentile84 = pow(10, logpgvsiteplus) / 100;
        PGV.percentile16 = pow(10, logpgvsiteminus) / 100;

        // Now should return Shaking ...
        return PGV;
    }

    @Override
    public Shaking getPSA(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI, double period,
                          EventParameters eventParameters,
                          Float ruptureLength,
                          Float ruptureStrike) {

        // Returns median PSA, 16th-percentile PSA, 84th percentile PSA in m/s2 for a given spectral period T
        // Mag is the magnitude from the EW message
        // ampType in Switzerland is deltaI, i.e. intensity increments
        // Coefficients are region dependent, i.e. different in the Swiss Alps and in the Swiss Foreland
        double rmin = 3; // set cut-off distance
        double Mw = magnitude;	// reasonable assumption in CH
        double[][] cofs = getCofs(sourceLat, sourceLon);

        int cnt = 0; // init
        double sigma = 0; //init
        double amp = 1; //init

        // Compute distance
        //double[] pEvent = GeoCalc.Geo2Cart(sourceLat, sourceLon, -sourceDepthM); deprecated
        //double[] pTarget = GeoCalc.Geo2Cart(targetLat, targetLon, targetElevM); deprectaed
        
        double[] pEvent = {sourceLat, sourceLon, -sourceDepthM};
        double[] pTarget = {targetLat, targetLon, targetElevM};
        
        
        double distance;
        
        if (ruptureLength != null) {
        	
        	double[] lExtremes = GeoCalc.CentroidToExtremes(ruptureStrike, ruptureLength, sourceLon, sourceLat, -sourceDepthM);
            double[] start = {lExtremes[1],lExtremes[0],lExtremes[2]};
            double[] end = {lExtremes[4],lExtremes[3],lExtremes[5]};
            double[] current = {pTarget[0],pTarget[1]};
            double d = GeoCalc.DistanceFromLine(start, end, current);
            distance = Math.sqrt(d * d + (sourceDepthM + targetElevM) * (sourceDepthM + targetElevM));
             
            
        } else {
        
        	distance = GeoCalc.Distance3DDegToM(pEvent, pTarget);
        }
        
        double Rh = distance / 1000; // in kilometers

        // end of distance computation
        
        // Assume Rrup ~ Rh
        double Rrup = Rh;
        // else estimate Rrup based on Cauzzi et al. (2014)
        
        if (Mw >= 5.8){
            Rrup = Rh + 7.5 * Mw - 38 - 1.38 - 0.014 * exp(Mw); // 7.5 * Mw + 38 included to avoid negative distances at points with Rh = 0 ...
        }

        // define the distance cut-off
        double Ru = max(rmin, Rrup);

        // define the distance metric used in the attenuation formulas
        double d = log10(Ru);

        // pick the right coefficients according to the spectral period
        if (period == 0.01) {
            cnt = 1;
            sigma = 0.3346;
            amp = 2.58;
        } else if (period == 0.02) {
            cnt = 2;
            sigma = 0.3346;
            amp = 2.57;

        } else if (period == 0.03) {
            cnt = 3;
            sigma = 0.3346;
            amp = 2.57;

        } else if (period == 0.05) {
            cnt = 4;
            sigma = 0.3348;
            amp = 2.56;

        } else if (period == 0.1) {
            cnt = 5;
            sigma = 0.2953;
            amp = 2.55;

        } else if (period == 0.2) {
            cnt = 6;
            sigma = 0.2884;
            amp = 2.52;

        } else if (period == 0.4) {
            cnt = 7;
            sigma = 0.2641;
            amp = 2.47;

        } else if (period == 1) {
            cnt = 8;
            sigma = 0.2751;
            amp = 2.29;

        } else if (period == 2) {
            cnt = 9;
            sigma = 0.2840;
            amp = 2.01;
        }

        double logpsa = cofs[cnt][0] + cofs[cnt][1] * Mw + cofs[cnt][2] * pow(Mw, 2) + cofs[cnt][3] * pow(Mw, 3) + cofs[cnt][4] * pow(Mw, 4) + cofs[cnt][5] * pow(Mw, 5) + cofs[cnt][6] * pow(Mw, 6) + (cofs[cnt][7] + cofs[cnt][8] * Mw + cofs[cnt][9] * pow(Mw, 2) + cofs[cnt][10] * pow(Mw, 3)) * d + (cofs[cnt][11] + cofs[cnt][12] * Mw + cofs[cnt][13] * pow(Mw, 2) + cofs[cnt][14] * pow(Mw, 3)) * pow(d, 2) + (cofs[cnt][15] + cofs[cnt][16] * Mw + cofs[cnt][17] * pow(Mw, 2) + cofs[cnt][18] * pow(Mw, 3)) * pow(d, 3) + (cofs[cnt][19] + cofs[cnt][20] * Mw + cofs[cnt][21] * pow(Mw, 2) + cofs[cnt][22] * pow(Mw, 3)) * pow(d, 4);

        // Now add site term
        double logpsasite = logpsa + (amplificationProxyValueSI / amp);

        // Now compute plus/minus sigma bounds
        double logpsasiteplus = logpsasite + sigma;
        double logpsasiteminus = logpsasite - sigma;

        // Now in m/s2
        Shaking PSA = new Shaking();
        PSA.expectedSI = pow(10, logpsasite) / 100;
        PSA.percentile84 = pow(10, logpsasiteplus) / 100;
        PSA.percentile16 = pow(10, logpsasiteminus) / 100;

        // Now should return Shaking ...
        return PSA;
    }

    @Override
    public Shaking getDRS(double magnitude, double sourceLat, double sourceLon,
                          double sourceDepthM, double targetLat, double targetLon,
                          double targetElevM, String amplificationType,
                          double amplificationProxyValueSI, double period,
                          EventParameters eventML,
                          Float ruptureLength,
                          Float ruptureStrike) {

        Shaking PSA = getPSA(magnitude, sourceLat, sourceLon, sourceDepthM,
                             targetLat, targetLon, targetElevM,
                             amplificationType, amplificationProxyValueSI,
                             period, null, ruptureLength, ruptureStrike);

        double accelerationToDisplacement = period * period / PI2_4;
        PSA.expectedSI *= accelerationToDisplacement;
        PSA.percentile16 *= accelerationToDisplacement;
        PSA.percentile84 *= accelerationToDisplacement;

        // Now in m/s
        return PSA;
    }
}
