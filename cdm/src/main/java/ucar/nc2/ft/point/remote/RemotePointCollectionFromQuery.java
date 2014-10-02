package ucar.nc2.ft.point.remote;

import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonRect;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 *
 * @author cwardgar
 * @since 2014/10/02
 */
public class RemotePointCollectionFromQuery extends RemotePointCollectionAbstract implements QueryMaker {
    private final String uri;
    private final QueryMaker queryMaker;

    public RemotePointCollectionFromQuery(String uri, DateUnit timeUnit, String altUnits, QueryMaker queryMaker) {
        super(uri, timeUnit, altUnits);
        this.uri = uri;
        this.queryMaker = (queryMaker == null) ? this : queryMaker;
    }

    @Override
    public String makeQuery() {
        return PointDatasetRemote.makeQuery(null, boundingBox, dateRange); // default query
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return CdmRemote.sendQuery(uri, queryMaker.makeQuery());
    }

    // Must override default subsetting implementation for efficiency.

    @Override
    public PointFeatureCollection subset(LatLonRect boundingBox, CalendarDateRange dateRange) throws IOException {
        return new PointFeatureCollectionSubset(this, boundingBox, dateRange);
    }

    private class PointFeatureCollectionSubset extends RemotePointCollectionFromQuery {
        PointFeatureCollectionSubset(RemotePointCollectionFromQuery from,
                LatLonRect filter_bb, CalendarDateRange filter_date) throws IOException {
            // Passing null to the queryMaker param causes the default query to be used.
            // The default query will use the boundingBox and dateRange we calculate below.
            super(from.uri, from.getTimeUnit(), from.getAltUnits(), null);

            if (filter_bb == null) {
                this.boundingBox = from.boundingBox;
            } else {
                this.boundingBox = (from.boundingBox == null) ? filter_bb : from.boundingBox.intersect(filter_bb);
            }

            if (filter_date == null) {
                this.dateRange = from.dateRange;
            } else {
                this.dateRange = (from.dateRange == null) ? filter_date : from.dateRange.intersect(filter_date);
            }
        }
    }
}
