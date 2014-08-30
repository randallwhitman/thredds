package ucar.nc2.ft.point;

import com.google.common.collect.Ordering;
import org.junit.Assert;
import org.junit.Test;
import ucar.nc2.ft.NoFactoryFoundException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class SortingStationPointFeatureCacheTest {
    @Test
    public void test1() throws IOException, NoFactoryFoundException, URISyntaxException {
        File testFile = new File(getClass().getResource("orthogonal.ncml").toURI());

        Comparator<StationPointFeature> reverseStationNameComparator =
                Ordering.from(SortingStationPointFeatureCache.stationNameComparator).reverse();
        SortingStationPointFeatureCache cache = new SortingStationPointFeatureCache(reverseStationNameComparator);
        cache.addAll(testFile);

        List<String> expectedStationNames = Arrays.asList("CCC", "BBB", "AAA");
        List<String> actualStationNames   = new LinkedList<>();

        for (StationPointFeature pointFeat : cache) {
            actualStationNames.add(pointFeat.getStation().getName());
        }

        Assert.assertEquals(expectedStationNames, actualStationNames);
    }
}
