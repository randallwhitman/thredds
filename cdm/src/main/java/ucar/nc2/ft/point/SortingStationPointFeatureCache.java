package ucar.nc2.ft.point;

import com.google.common.base.Preconditions;
import ucar.nc2.constants.FeatureType;

import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by cwardgar on 2014/08/05.
 */
public class SortingStationPointFeatureCache {
    private final FeatureType featType;
    private final Comparator<StationPointFeature> comp;
    private final SortedMap<StationPointFeature, List<StationPointFeature>> inMemCache;

    public SortingStationPointFeatureCache(FeatureType featType, Comparator<StationPointFeature> comp) {
        Preconditions.checkNotNull(featType, "featType == null");
        Preconditions.checkArgument(featType.isPointFeatureType(), "Expected a point feature type, not %s.", featType);
        this.featType = featType;

        this.comp = Preconditions.checkNotNull(comp, "comparator == null");
        this.inMemCache = new TreeMap<>(comp);
    }

    public void add(StationPointFeature feat) {
        Preconditions.checkNotNull(feat, "feat == null");
    }
}
