package ucar.nc2.ft.point;

import org.junit.Test;
import ucar.nc2.ft.NoFactoryFoundException;
import ucar.unidata.test.util.TestDir;

import java.io.File;
import java.io.IOException;

public class SortingStationPointFeatureCacheTest {
    @Test
    public void roundTrip() throws IOException, NoFactoryFoundException {
        File testFile = new File(TestDir.cdmLocalTestDataDir + "cfDocDsgExamples/H.2.5.1.ncml");
        SortingStationPointFeatureCache cache = new SortingStationPointFeatureCache();
        cache.addAll(testFile);

        for (StationPointFeature stationFeat : cache) {

        }
    }
}
