package ucar.nc2.ft.point;

import com.google.common.collect.Ordering;
import org.junit.Assert;
import org.junit.Test;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataScalar;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.NoFactoryFoundException;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.units.DateUnit;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

public class SortingStationPointFeatureCacheTest {
    @Test
    public void test1() throws Exception {
        StructureData stationData = new StructureDataScalar("StationFeature");  // leave it empty.
        StationFeature stationFeat = new StationFeatureImpl("Foo", "Bar", "123", 30, 60, 5000, 4, stationData);

        DateUnit timeUnit = new DateUnit("days since 1970-01-01");
        StructureData featureData = new StructureDataScalar("StationPointFeature");  // Leave it empty.

        StationPointFeature spf1 = new SimpleStationPointFeature(stationFeat, 5, 5, timeUnit, featureData);
        StationPointFeature spf2 = new SimpleStationPointFeature(stationFeat, 10, 10, timeUnit, featureData);
        StationPointFeature spf3 = new SimpleStationPointFeature(stationFeat, 15, 15, timeUnit, featureData);
        StationPointFeature spf4 = new SimpleStationPointFeature(stationFeat, 20, 20, timeUnit, featureData);

        Comparator<StationPointFeature> revObsTimeComp = new Comparator<StationPointFeature>() {
            @Override
            public int compare(StationPointFeature left, StationPointFeature right) {
                return -Double.compare(left.getObservationTime(), right.getObservationTime());
            }
        };

        SortingStationPointFeatureCache cache = new SortingStationPointFeatureCache(revObsTimeComp);
        List<StationPointFeature> spfList = Arrays.asList(spf1, spf2, spf3, spf4);

        for (StationPointFeature stationPointFeat : spfList) {
            cache.add(stationPointFeat);
        }

        Collections.reverse(spfList);
        Assert.assertTrue(PointTestUtil.equals(spfList.iterator(), cache.iterator()));
    }

    @Test
    public void test2() throws IOException, NoFactoryFoundException, URISyntaxException {
        File testFile = new File(getClass().getResource("orthogonal.ncml").toURI());

        Comparator<StationPointFeature> reverseStationNameComparator =
                Ordering.from(SortingStationPointFeatureCache.stationNameComparator).reverse();
        SortingStationPointFeatureCache cache = new SortingStationPointFeatureCache(reverseStationNameComparator);
        cache.addAll(testFile);

        List<String> expectedStationNames = Arrays.asList("CCC", "BBB", "AAA");
        List<String> actualStationNames = new LinkedList<>();

        for (StationPointFeature pointFeat : cache) {
            actualStationNames.add(pointFeat.getStation().getName());
        }

        Assert.assertEquals(expectedStationNames, actualStationNames);
    }

    @Test
    public void test3() throws URISyntaxException, NoFactoryFoundException, IOException {
        // Sort in reverse order of station name length.
        Comparator<StationPointFeature> longestStationNameFirst = new Comparator<StationPointFeature>() {
            @Override
            public int compare(StationPointFeature o1, StationPointFeature o2) {
                return -Integer.compare(o1.getStation().getName().length(), o2.getStation().getName().length());
            }
        };
        SortingStationPointFeatureCache cache = new SortingStationPointFeatureCache(longestStationNameFirst);

        try (FeatureDatasetPoint fdInput = PointTestUtil.openPointDataset("cacheTestInput1.ncml");
                FeatureDatasetPoint fdExpected = PointTestUtil.openPointDataset("cacheTestExpected1.ncml")) {
            cache.addAll(fdInput);

            Iterator<StationPointFeature> cacheFeatsIter = cache.iterator();
            PointFeatureIterator expectedFeatsIter = new PointDatasetIterator(fdExpected);
            try {
                while (cacheFeatsIter.hasNext() && expectedFeatsIter.hasNext()) {
                    StationPointFeature cacheFeat = cacheFeatsIter.next();
                    StationPointFeature expectedFeat = (StationPointFeature) expectedFeatsIter.next();
                }
            } finally {
                expectedFeatsIter.finish();
            }
        }
    }
}
