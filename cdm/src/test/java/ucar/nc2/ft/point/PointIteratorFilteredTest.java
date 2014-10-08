package ucar.nc2.ft.point;

import org.junit.Assert;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.StructureData;
import ucar.nc2.ft.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class PointIteratorFilteredTest {
    // Regression test for refactoring PointIteratorFiltered to use PointFeatureIterator.Filter internally.
    @Test
    public void timeSpaceFilter() throws NoFactoryFoundException, IOException, URISyntaxException {
        try (FeatureDatasetPoint fdPoint = PointTestUtil.openPointDataset("pointsToFilter.ncml")) {
            double latMin = +10.0;
            double latMax = +50.0;
            double lonMin = -60.0;
            double lonMax = +10.0;
            LatLonRect filter_bb = new LatLonRect(
                    new LatLonPointImpl(latMin, lonMin), new LatLonPointImpl(latMax, lonMax));

            CalendarDateUnit calDateUnit = CalendarDateUnit.of("standard", "days since 1970-01-01 00:00:00");
            CalendarDate start = calDateUnit.makeCalendarDate(20);
            CalendarDate end = calDateUnit.makeCalendarDate(130);
            CalendarDateRange filter_date = CalendarDateRange.of(start, end);

            PointFeatureCollection flattenedDatasetCol = new FlattenedDatasetPointCollection(fdPoint);
            PointFeatureIterator pointIterOrig = flattenedDatasetCol.getPointFeatureIterator(-1);
            PointFeatureIterator pointIterFiltered = new PointIteratorFiltered(pointIterOrig, filter_bb, filter_date);

            List<String> expectedIdsOfFilteredPoints = Arrays.asList("B", "E");
            List<String> actualIdsOfFilteredPoints = new LinkedList<>();

            try {
                while (pointIterFiltered.hasNext()) {
                    pointIterFiltered.hasNext();  // Test idempotency. This call should have no effect.
                    PointFeature pointFeat = pointIterFiltered.next();
                    actualIdsOfFilteredPoints.add(getIdOfPoint(pointFeat));
                }
            } finally {
                pointIterFiltered.finish();
            }

            Assert.assertEquals(expectedIdsOfFilteredPoints, actualIdsOfFilteredPoints);
        }
    }

    private static String getIdOfPoint(PointFeature pointFeat) throws IOException {
        StructureData data = pointFeat.getFeatureData();
        Array memberArray = data.getArray("id");
        Assert.assertTrue(memberArray instanceof ArrayChar.D1);

        return ((ArrayChar.D1) memberArray).getString();
    }
}
