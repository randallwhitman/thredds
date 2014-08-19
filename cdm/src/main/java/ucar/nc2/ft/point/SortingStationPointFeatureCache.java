package ucar.nc2.ft.point;

import com.google.common.base.Preconditions;
import ucar.nc2.units.DateUnit;

import java.io.IOException;
import java.util.*;

/**
 * Created by cwardgar on 2014/08/05.
 */
public class SortingStationPointFeatureCache {
    private final Comparator<StationPointFeature> comp;
    private final SortedMap<StationPointFeature, List<StationPointFeature>> inMemCache;

    private volatile StationFeatureCopyFactory stationFeatCopyFactory;

    // We're going to init stationFeatCopyFactory using the first feat that's add()ed.
    public SortingStationPointFeatureCache(Comparator<StationPointFeature> comp) {
        this.comp = Preconditions.checkNotNull(comp, "comparator == null");
        this.inMemCache = new TreeMap<>(comp);
        // stationFeatCopyFactory remains null.
    }

    public SortingStationPointFeatureCache(
            Comparator<StationPointFeature> comp, StationPointFeature proto, DateUnit dateUnit) throws IOException {
        this.comp = Preconditions.checkNotNull(comp, "comparator == null");
        this.inMemCache = new TreeMap<>(comp);

        if (proto != null && dateUnit != null) {
            this.stationFeatCopyFactory = new StationFeatureCopyFactory(proto, dateUnit);
        }
    }

    public void add(StationPointFeature feat) throws IOException {
        Preconditions.checkNotNull(feat, "feat == null");
        StationPointFeature featCopy = getStationFeatureCopyFactory(feat).deepCopy(feat);

        List<StationPointFeature> bucket = inMemCache.get(featCopy);
        if (bucket == null) {
            bucket = new LinkedList<>();
            inMemCache.put(featCopy, bucket);
        }

        bucket.add(featCopy);
    }

    // Double-check idiom for lazy initialization of instance fields. See Effective Java 2nd Ed, p. 283.
    private StationFeatureCopyFactory getStationFeatureCopyFactory(StationPointFeature proto) throws IOException {
        if (stationFeatCopyFactory == null) {
            synchronized (this) {
                if (stationFeatCopyFactory == null) {
                    stationFeatCopyFactory = createStationFeatureCopyFactory(proto);
                }
            }
        }

        assert stationFeatCopyFactory != null : "We screwed this up.";
        return stationFeatCopyFactory;
    }

    private StationFeatureCopyFactory createStationFeatureCopyFactory(StationPointFeature proto) throws IOException {
        if (!(proto instanceof PointFeatureImpl)) {
            // LOOK: As of 2014/08/19, all StationPointFeature implementations in NetCDF-Java extend PointFeatureImpl,
            // so this won't be common. Still, we should consider a better solution, such as adding getTimeUnit() to
            // PointFeature.
            throw new IllegalArgumentException("feature is not an instance of PointFeatureImpl, " +
                    "so we cannot gather DateUnit information from it. It is a " + proto.getClass().getName());
        }

        DateUnit timeUnit = ((PointFeatureImpl) proto).getTimeUnit();
        return new StationFeatureCopyFactory(proto, timeUnit);
    }
}
