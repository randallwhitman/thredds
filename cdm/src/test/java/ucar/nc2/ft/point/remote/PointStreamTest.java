package ucar.nc2.ft.point.remote;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.NoFactoryFoundException;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.point.FlattenedDatasetPointCollection;
import ucar.nc2.ft.point.PointTestUtil;
import ucar.unidata.test.util.TestDir;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@RunWith(Parameterized.class)
public class PointStreamTest {
    public static final String cfDocDsgExamplesDir = TestDir.cdmLocalTestDataDir + "cfDocDsgExamples/";
    public static final String pointDir = TestDir.cdmLocalTestDataDir + "point/";

    public static List<Object[]> getPointDatasets() {
        List<Object[]> result = new ArrayList<>();

        result.add(new Object[] { cfDocDsgExamplesDir + "H.1.1.ncml" });
        result.add(new Object[] { pointDir + "point.ncml" });
        result.add(new Object[] { pointDir + "pointMissing.ncml" });
        result.add(new Object[] { pointDir + "pointUnlimited.nc" });

        return result;
    }

    @Parameterized.Parameters(name = "{0}")  // Name the tests after the location.
    public static List<Object[]> getTestParameters() {
        List<Object[]> result = new ArrayList<>();

        result.addAll(getPointDatasets());

        return result;
    }


    private final String location;

    public PointStreamTest(String location) {
        this.location = location;
    }

    @Test
    public void doRoundTrip() throws IOException, NoFactoryFoundException, URISyntaxException {
        File outFile = File.createTempFile("out", ".bin");

        try (FeatureDatasetPoint fdPoint =
                (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(FeatureType.ANY_POINT, location, null)) {
            PointFeatureCollection origPointCol = new FlattenedDatasetPointCollection(fdPoint);
            PointStream.write(outFile, origPointCol);

            PointFeatureCollection roundTrippedPointCol = new PointCollectionStreamLocal(outFile);
            Assert.assertTrue(PointTestUtil.equals(origPointCol, roundTrippedPointCol));
        } finally {
            outFile.delete();
        }
    }
}
