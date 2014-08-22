package ucar.nc2.ft.point;

import ucar.nc2.ft.*;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.Iterator;

/**
 * A simple iterator that returns all of the PointFeatures in a dataset in default order, flattening nested structures
 * if necessary. Maximum read efficiency is the goal here; tasks like filtering and sorting are left to other classes.
 *
 * @see ucar.nc2.ft.point.PointIteratorFiltered
 * @see ucar.nc2.ft.point.SortingStationPointFeatureCache
 * @author cwardgar
 * @since 2014/08/22
 */
/*
 * Package-private because this is not a full implementation of PointFeatureIterator; it skips the calcBounds stuff.
 * It's not clear whether it's even appropriate to implement those methods: they make sense in the context of an
 * iterator over a single collection, but not necessarily an iterator over a dataset (which may contain several
 * collections, potentially with different coordinate systems).
 */
class PointDatasetIterator implements PointFeatureIterator {
    private final Iterator<FeatureCollection> featColIter;
    private PointFeatureIterator pointFeatIter;
    private int bufferSize = -1;
    private int count = 0;

    PointDatasetIterator(FeatureDatasetPoint fdPoint) {
        this.featColIter = fdPoint.getPointFeatureCollectionList().iterator();
    }

    @Override
    public boolean hasNext() throws IOException {
        while (pointFeatIter == null || !pointFeatIter.hasNext()) {
            if (pointFeatIter != null) {
                pointFeatIter.finish();  // Release the resources of the old iter.
                // calcBounds(pointFeatIter)
            }

            if (!featColIter.hasNext()) {
                return false;
            } else {
                pointFeatIter = getNextPointFeatureIterator();
            }
        }

        return true;
    }

    private PointFeatureIterator getNextPointFeatureIterator() throws IOException {
        FeatureCollection featCol = featColIter.next();
        assert featCol != null : "featColIter.hasNext() was called above.";
        PointFeatureCollection pointFeatCol;

        if (featCol instanceof PointFeatureCollection) {
            pointFeatCol = (PointFeatureCollection) featCol;
        } else if (featCol instanceof NestedPointFeatureCollection) {
            pointFeatCol = ((NestedPointFeatureCollection) featCol).flatten(null, (CalendarDateRange) null);
        } else {
            throw new AssertionError("getPointFeatureCollectionList() guarantees that list elems will be instances " +
                    "of PointFeatureCollection or NestedPointFeatureCollection, not " + featCol.getClass().getName());
        }

        return pointFeatCol.getPointFeatureIterator(bufferSize);
    }

    // This method could be a lot simpler if hasNext() guaranteed idempotency.
    @Override
    public PointFeature next() throws IOException {
        PointFeature pointFeat = null;
        if (pointFeatIter != null) {  // Could be null if featColIter is empty.
            pointFeat = pointFeatIter.next();
        }

        if (pointFeat != null) {
            ++count;
        }

        return pointFeat;
    }

    @Override
    public void finish() {
        if (pointFeatIter != null) {
            pointFeatIter.finish();
        }
    }

    @Override
    public void setBufferSize(int bytes) {
        this.bufferSize = bytes;
    }

    @Override
    public void setCalculateBounds(PointFeatureCollection collection) {
        throw new UnsupportedOperationException("Operation not supported by this iterator.");
    }

    @Override
    public LatLonRect getBoundingBox() {
        throw new UnsupportedOperationException("Operation not supported by this iterator.");
    }

    @Override
    public DateRange getDateRange() {
        throw new UnsupportedOperationException("Operation not supported by this iterator.");
    }

    @Override
    public CalendarDateRange getCalendarDateRange() {
        throw new UnsupportedOperationException("Operation not supported by this iterator.");
    }

    @Override
    public int getCount() {
        return count;
    }
}
