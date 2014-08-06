package ucar.nc2.ft.point;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.PointFeature;

import java.util.*;

/**
 * Created by cwardgar on 2014/08/05.
 */
public class SortingPointFeatureCache {
    private final FeatureType featType;
    private final Comparator<PointFeature> comparator;
    private final SortedMap<PointFeature, List<PointFeature>> inMemCache;

    public SortingPointFeatureCache(FeatureType featType, Comparator<PointFeature> comp) {
        this.featType = featType;
        this.comparator = comp;
        this.inMemCache = new TreeMap<>(comp);
    }
}
