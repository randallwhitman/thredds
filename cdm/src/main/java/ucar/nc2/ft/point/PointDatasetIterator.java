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
public class PointDatasetIterator implements PointFeatureIterator {
    private final Iterator<FeatureCollection> featColIter;
    private PointFeatureIterator pointFeatIter;

    private int bufferSize = -1;
    private int count = 0;
    private boolean calcBounds = false;
    private CalendarDateRange calendarDateRange;
    private LatLonRect boundingBox;

    public PointDatasetIterator(FeatureDatasetPoint fdPoint) {
        this.featColIter = fdPoint.getPointFeatureCollectionList().iterator();
    }

    @Override
    public boolean hasNext() throws IOException {
        while (pointFeatIter == null || !pointFeatIter.hasNext()) {
            if (pointFeatIter != null) {
                pointFeatIter.finish();  // Release the resources of the old iter.
                updateBounds(pointFeatIter);
            }

            if (!featColIter.hasNext()) {
                return false;
            } else {
                pointFeatIter = getNextPointFeatureIterator();
            }
        }

        return true;
    }

    private void updateBounds(PointFeatureIterator pointFeatIter) {
        if (calcBounds) {
            count += pointFeatIter.getCount();

            if (calendarDateRange == null) {
                calendarDateRange = pointFeatIter.getCalendarDateRange();
            } else {
                calendarDateRange = calendarDateRange.extend(pointFeatIter.getCalendarDateRange());
            }

            if (boundingBox == null) {
                boundingBox = pointFeatIter.getBoundingBox();
            } else {
                boundingBox.extend(pointFeatIter.getBoundingBox());
            }
        }
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

        PointFeatureIterator pointFeatIter = pointFeatCol.getPointFeatureIterator(bufferSize);
        if (calcBounds) {
            pointFeatIter.setCalculateBounds(pointFeatCol);
        }
        return pointFeatIter;
    }

    @Override
    public PointFeature next() throws IOException {
        if (pointFeatIter == null) {  // Could be null if featColIter is empty.
            return null;
        } else {
            return pointFeatIter.next();
        }
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
        this.calcBounds = true;
    }

    @Override
    public LatLonRect getBoundingBox() {
        return boundingBox;
    }

    @Override
    public DateRange getDateRange() {
        CalendarDateRange cdr = getCalendarDateRange();
        return (cdr != null) ? cdr.toDateRange() : null;
    }

    @Override
    public CalendarDateRange getCalendarDateRange() {
        return calendarDateRange;
    }

    @Override
    public int getCount() {
        return count;
    }
}
