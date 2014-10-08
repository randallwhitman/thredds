package ucar.nc2.ft.point;

import ucar.nc2.ft.*;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * An aggregate collection of all the PointFeatures in a dataset formed by flattening the nested structures within.
 * This class's {@link #getPointFeatureIterator iterator} returns features in default order, with maximum read
 * efficiency as the goal.
 *
 * @author cwardgar
 * @since 2014/10/08
 */
public class FlattenedDatasetPointCollection extends PointCollectionImpl {
    private final FeatureDatasetPoint fdPoint;

    public FlattenedDatasetPointCollection(FeatureDatasetPoint fdPoint) {
        super(fdPoint.getLocation(), DateUnit.getUnixDateUnit(), null);  // Default dateUnit and altUnits.
        this.fdPoint = fdPoint;

        List<FeatureCollection> featCols = fdPoint.getPointFeatureCollectionList();
        if (!featCols.isEmpty()) {
            FeatureCollection firstFeatCol = featCols.get(0);

            // Replace this.dateUnit, this.altUnits, and this.extras with "typical" values from firstFeatCol.
            // We can't be certain that those values are representative of ALL collections in the dataset, but it's
            // a decent bet because in practice, firstFeatCol is so often the ONLY collection.
            copyFieldsFrom(firstFeatCol);
        }
    }

    private void copyFieldsFrom(FeatureCollection featCol) {
        if (featCol instanceof PointFeatureCollection) {
            PointFeatureCollection pointFeatCol = (PointFeatureCollection) featCol;
            this.timeUnit = pointFeatCol.getTimeUnit();
            this.altUnits = pointFeatCol.getAltUnits();
            this.extras   = pointFeatCol.getExtraVariables();
        } else if (featCol instanceof NestedPointFeatureCollection) {
            NestedPointFeatureCollection nestedPointFeatCol = (NestedPointFeatureCollection) featCol;
            this.timeUnit = nestedPointFeatCol.getTimeUnit();
            this.altUnits = nestedPointFeatCol.getAltUnits();
            this.extras   = nestedPointFeatCol.getExtraVariables();
        } else {
            throw new AssertionError("getPointFeatureCollectionList() guarantees that list elems will be " +
                    "instances of PointFeatureCollection or NestedPointFeatureCollection,  not " +
                    featCol.getClass().getName());
        }
    }

    @Override
    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
        PointFeatureIterator iter = new FlattenedDatasetPointIterator(fdPoint);
        iter.setBufferSize(bufferSize);
        return iter;
    }


    public static class FlattenedDatasetPointIterator implements PointFeatureIterator {
        private final Iterator<FeatureCollection> featColIter;
        private PointFeatureIterator pointFeatIter;

        private int bufferSize = -1;
        private int count = 0;
        private boolean calcBounds = false;
        private CalendarDateRange calendarDateRange;
        private LatLonRect boundingBox;

        public FlattenedDatasetPointIterator(FeatureDatasetPoint fdPoint) {
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
            count += pointFeatIter.getCount();  // Always count obs. For parity with PointIteratorAbstract.calcBounds().

            if (calcBounds) {
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
                throw new AssertionError("getPointFeatureCollectionList() guarantees that list elems will be " +
                        "instances of PointFeatureCollection or NestedPointFeatureCollection,  not " +
                        featCol.getClass().getName());
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
}
