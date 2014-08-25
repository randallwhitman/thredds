package ucar.nc2.ft.point;

import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.NoFactoryFoundException;
import ucar.nc2.ft.PointFeature;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class PointDatasetIteratorTest {
    @Test
    public void test1() throws IOException, NoFactoryFoundException, URISyntaxException {
        File testFile = new File(getClass().getResource("multiplePointCollections.ncml").toURI());

        try (FeatureDatasetPoint fdPoint = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
                FeatureType.POINT, testFile.getAbsolutePath(), null)) {
            PointDatasetIterator pointIter = new PointDatasetIterator(fdPoint);
            try {
                while (pointIter.hasNext()) {
                    PointFeature pointFeat = pointIter.next();
                    StructureData data = pointFeat.getData();

                    for (StructureMembers.Member member : data.getMembers()) {
                        Array memberData = data.getArray(member);
                        System.out.printf("%s: %s    ", member.getName(), memberData);
                    }

                    System.out.println();
                }
            } finally {
                pointIter.finish();
            }
        }
    }
}
