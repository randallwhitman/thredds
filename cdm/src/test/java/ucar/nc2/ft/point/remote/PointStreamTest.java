package ucar.nc2.ft.point.remote;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.ma2.StructureDataScalar;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.NoFactoryFoundException;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.point.FlattenedDatasetPointCollection;
import ucar.nc2.ft.point.PointTestUtil;
import ucar.nc2.ft.point.SimplePointFeature;
import ucar.nc2.ft.point.SimplePointFeatureCollection;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.EarthLocationImpl;
import ucar.units.UnitException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class PointStreamTest {
    private static DateUnit timeUnit;

    @BeforeClass
    public static void setupClass() throws UnitException {
        timeUnit = new DateUnit("days since 1970-01-01");
    }

    @Test
    public void testIo1() throws IOException {
        File outFile = new File("C:/Users/cwardgar/Desktop/out1.txt");

        SimplePointFeatureCollection pointCollOut = new SimplePointFeatureCollection("PointStreamTest", timeUnit, "m");
        pointCollOut.add(makePointFeature(outFile, 30, 40, 5000, 0, 114.9));
        pointCollOut.add(makePointFeature(outFile, 40, 60, 397, 365, 98.2));
        pointCollOut.add(makePointFeature(outFile, 50, 80, 2525, 730, 201.6));
        pointCollOut.add(makePointFeature(outFile, 60, 100, 4119, 1096, 144.8));
        pointCollOut.add(makePointFeature(outFile, 70, 120, 72, 1461, 88.7));

        PointStream.write(outFile, pointCollOut);

        PointFeatureCollection pointCollIn = new PointCollectionStreamLocal(outFile);
        Assert.assertTrue(PointTestUtil.equals(pointCollOut, pointCollIn));
    }

    private static PointFeature makePointFeature(
            File outFile, double lat, double lon, double alt, double time, double tasmax) {
        StructureDataScalar featureData = new StructureDataScalar(outFile.getAbsolutePath());
        featureData.addMember("lat", "Latidue", "degrees_north", DataType.DOUBLE, false, lat);
        featureData.addMember("lon", "Longitude", "degrees_east", DataType.DOUBLE, false, lon);
        featureData.addMember("alt", "Altitude", "meters", DataType.DOUBLE, false, alt);
        featureData.addMember("obsTime", "Observation time", timeUnit.getUnitsString(), DataType.DOUBLE, false, time);
        featureData.addMember("nomTime", "Nominal time", timeUnit.getUnitsString(), DataType.DOUBLE, false, time);
        featureData.addMember("tasmax", "Max temperature", "Celsius", DataType.DOUBLE, false, tasmax);

        EarthLocation loc = new EarthLocationImpl(lat, lon, alt);
        return new SimplePointFeature(loc, time, time, timeUnit, featureData);
    }

    @Test
    public void testIo2() throws NoFactoryFoundException, IOException, URISyntaxException {
        File outFile = new File("C:/Users/cwardgar/Desktop/out2.txt");

        try (FeatureDatasetPoint fdPoint = PointTestUtil.openPointDataset("pointsToFilter.ncml")) {
            PointFeatureCollection origPointCol = new FlattenedDatasetPointCollection(fdPoint);
            PointStream.write(outFile, origPointCol);

            PointFeatureCollection roundTrippedPointCol = new PointCollectionStreamLocal(outFile);
            Assert.assertTrue(PointTestUtil.equals(origPointCol, roundTrippedPointCol));
        }
    }
}
