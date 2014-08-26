package ucar.nc2.ft.point;

import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;
import ucar.ma2.Array;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

@RunWith(JMockit.class)  // Not sure if this is strictly necessary.
public class PointDatasetIteratorTest {
    @Test
    public void test1() throws IOException, NoFactoryFoundException, URISyntaxException {
//        File testFile = new File(TestDir.cdmLocalTestDataDir + "cfDocDsgExamples/H.2.5.1.ncml");
//        File testFile = new File(getClass().getResource("multiplePointCollections.ncml").toURI());
        File testFile = new File("C:/Users/cwardgar/Desktop/H.2.5.1.nc");

        try (FeatureDatasetPoint fdPoint = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
                FeatureType.ANY_POINT, testFile.getAbsolutePath(), null)) {
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

    @Test
    public void test2() {
        // Create a fake FeatureDatasetPoint that returns our desired FeatureCollections.
        // See http://jmockit.github.io/tutorial/StateBasedTesting.html#interfaces
        FeatureDatasetPoint fakeFdPoint = new MockUp<FeatureDatasetPoint>() {
            @Mock
            public List<FeatureCollection> getPointFeatureCollectionList() {
                System.out.println("Fake!");
                return Collections.emptyList();
            }
        }.getMockInstance();

        // Create iterator and give it the fake.
        // Calc bounds and use JUnit assert statements.
    }
}
