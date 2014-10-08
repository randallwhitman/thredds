package ucar.nc2.ft.point.remote;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataScalar;
import ucar.ma2.StructureMembers;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.point.PointTestUtil;
import ucar.nc2.ft.point.SimplePointFeature;
import ucar.nc2.ft.point.SimplePointFeatureCollection;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.EarthLocationImpl;
import ucar.units.UnitException;

import java.io.File;
import java.io.IOException;

public class PointStreamTest {
    private static DateUnit timeUnit;

    @BeforeClass
    public static void setupClass() throws UnitException {
        timeUnit = new DateUnit("days since 1970-01-01");
    }

    @Test
    public void testIO() throws IOException {
        SimplePointFeatureCollection pointCollOut = new SimplePointFeatureCollection("PointStreamTest", timeUnit, "m");
        pointCollOut.add(makePointFeature(30, 40, 5000, 0, 114.9));
        pointCollOut.add(makePointFeature(40, 60, 397, 365, 98.2));
        pointCollOut.add(makePointFeature(50, 80, 2525, 730, 201.6));
        pointCollOut.add(makePointFeature(60, 100, 4119, 1096, 144.8));
        pointCollOut.add(makePointFeature(70, 120, 72, 1461, 88.7));

        File outFile = new File("C:/Users/cwardgar/Desktop/out.txt");
        PointStream.write(outFile, pointCollOut);

        PointFeatureCollection pointCollIn = new PointCollectionStreamLocal(outFile);
        Assert.assertTrue(PointTestUtil.equals(pointCollOut, pointCollIn));
    }

    private static void writeFeatureCollection(PointFeatureCollection pointFeatColl) throws IOException {
        PointFeatureIterator iter = pointFeatColl.getPointFeatureIterator(-1);
        while (iter.hasNext()) {
            PointFeature pointFeat = iter.next();
            StructureData data = pointFeat.getFeatureData();

            for (StructureMembers.Member member : data.getStructureMembers().getMembers()) {
                System.out.println(member.getName() + "\t\t" + data.getArray(member));
            }

            System.out.println();
        }
    }

    private static PointFeature makePointFeature(
            double lat, double lon, double alt, double time, double tasmax) {
        StructureDataScalar featureData = new StructureDataScalar("StationPointFeature");
        featureData.addMember("lat", "Latidue", "degrees_north", DataType.DOUBLE, false, lat);
        featureData.addMember("lon", "Longitude", "degrees_east", DataType.DOUBLE, false, lon);
        featureData.addMember("alt", "Altitude", "meters", DataType.DOUBLE, false, alt);
        featureData.addMember("obsTime", "Observation time", timeUnit.getUnitsString(), DataType.DOUBLE, false, time);
        featureData.addMember("nomTime", "Nominal time", timeUnit.getUnitsString(), DataType.DOUBLE, false, time);
        featureData.addMember("tasmax", "Max temperature", "Celsius", DataType.DOUBLE, false, tasmax);

        EarthLocation loc = new EarthLocationImpl(lat, lon, alt);
        return new SimplePointFeature(loc, time, time, timeUnit, featureData);
    }
}
