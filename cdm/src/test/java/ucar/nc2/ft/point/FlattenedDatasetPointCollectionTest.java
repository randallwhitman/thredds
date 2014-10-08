package ucar.nc2.ft.point;

import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import ucar.ma2.Array;
import ucar.ma2.StructureData;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.units.DateUnit;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@RunWith(JMockit.class)  // Not sure if this is strictly necessary.
public class FlattenedDatasetPointCollectionTest {
    @Test
    public void emptyDataset() throws IOException {
        // Create a fake FeatureDatasetPoint that returns our desired FeatureCollections.
        // See http://jmockit.github.io/tutorial/StateBasedTesting.html#interfaces
        FeatureDatasetPoint fakeFdPoint = new MockUp<FeatureDatasetPoint>() {
            @Mock
            public List<FeatureCollection> getPointFeatureCollectionList() {
                return new LinkedList<>();
            }
        }.getMockInstance();

        PointFeatureCollection flattenedDatasetCol = new FlattenedDatasetPointCollection(fakeFdPoint);
        Assert.assertEquals(DateUnit.getUnixDateUnit().getUnitsString(),
                flattenedDatasetCol.getTimeUnit().getUnitsString());  // Default
        Assert.assertNull(flattenedDatasetCol.getAltUnits());  // Default

        PointFeatureIterator pointIter = flattenedDatasetCol.getPointFeatureIterator(-1);
        Assert.assertFalse(pointIter.hasNext());
    }

    @Test
    public void calcBounds() throws URISyntaxException, IOException, NoFactoryFoundException {
        try (   FeatureDatasetPoint orthogonalFdPoint       = newFeatureDatasetPoint("orthogonal.ncml");
                FeatureDatasetPoint contiguousRaggedFdPoint = newFeatureDatasetPoint("continuousRagged.ncml");
                FeatureDatasetPoint indexRaggedFdPoint      = newFeatureDatasetPoint("indexRagged.ncml")) {
            final List<FeatureCollection> featCols = new LinkedList<>();
            featCols.addAll(orthogonalFdPoint.getPointFeatureCollectionList());
            featCols.addAll(contiguousRaggedFdPoint.getPointFeatureCollectionList());
            featCols.addAll(indexRaggedFdPoint.getPointFeatureCollectionList());
            Assert.assertEquals(3, featCols.size());

            // Create a fake FeatureDatasetPoint that returns our desired FeatureCollections.
            // See http://jmockit.github.io/tutorial/StateBasedTesting.html#interfaces
            FeatureDatasetPoint fakeFdPoint = new MockUp<FeatureDatasetPoint>() {
                @Mock
                public List<FeatureCollection> getPointFeatureCollectionList() {
                    return featCols;
                }
            }.getMockInstance();

            PointFeatureCollection flattenedDatasetCol = new FlattenedDatasetPointCollection(fakeFdPoint);

            // These are the values from orthogonal.ncml, which yields the first FeatureCollection in fakeFdPoint.
            // contiguousRagged.ncml and indexRagged.ncml have different values, but we don't see them because those
            // files don't yield the first collection.
            Assert.assertEquals("d since 1970-01-01 00:00:00", flattenedDatasetCol.getTimeUnit().getUnitsString());
            Assert.assertEquals("yard", flattenedDatasetCol.getAltUnits());

            PointFeatureIterator pointIter = flattenedDatasetCol.getPointFeatureIterator(-1);
            pointIter.setCalculateBounds(null);  // Turn on bounds calculation.

            try {
                List<Float> expectedHumidityVals = Arrays.asList(1f, 2f, 3f, 10f, 20f, 30f, 100f, 200f, 300f);
                List<Float> actualHumidityVals = getMemberValsFromIter(pointIter, "humidity");
                Assert.assertEquals(expectedHumidityVals, actualHumidityVals);

                Assert.assertEquals(9, pointIter.getCount());

                Assert.assertEquals(-85, pointIter.getBoundingBox().getLatMin(), 0);
                Assert.assertEquals(+85, pointIter.getBoundingBox().getLatMax(), 0);
                Assert.assertEquals(-70, pointIter.getBoundingBox().getLonMin(), 0);
                Assert.assertEquals(+170, pointIter.getBoundingBox().getLonMax(), 0);

                CalendarDateUnit calDateUnit = CalendarDateUnit.of("standard", "days since 1970-01-01 00:00:00");
                CalendarDate expectedStart = calDateUnit.makeCalendarDate(90);
                CalendarDate expectedEnd = calDateUnit.makeCalendarDate(400);

                Assert.assertEquals(expectedStart.getMillis(), pointIter.getCalendarDateRange().getStart().getMillis());
                Assert.assertEquals(expectedEnd.getMillis(), pointIter.getCalendarDateRange().getEnd().getMillis());
            } finally {
                pointIter.finish();
            }
        }
    }

    private static FeatureDatasetPoint newFeatureDatasetPoint(String resource)
            throws IOException, NoFactoryFoundException, URISyntaxException {
        File file = new File(FlattenedDatasetPointCollectionTest.class.getResource(resource).toURI());
        return (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
                FeatureType.ANY_POINT, file.getAbsolutePath(), null);
    }

    // Iterates through pointIter, recording the values for the specified member.
    // After the iteration, bounds information will be available from the iterator, if calculation was enabled.
    private static List<Float> getMemberValsFromIter(PointFeatureIterator pointIter, String memberName)
            throws IOException {
        List<Float> memberVals = new LinkedList<>();
        try {
            while (pointIter.hasNext()) {
                PointFeature pointFeat = pointIter.next();
                StructureData data = pointFeat.getFeatureData();

                Array memberArray = data.getArray(memberName);
                Assert.assertEquals(1, memberArray.getSize());
                memberVals.add(memberArray.getFloat(0));
            }
        } finally {
            pointIter.finish();
        }

        return memberVals;
    }
}
