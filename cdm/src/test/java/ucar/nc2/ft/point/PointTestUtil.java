package ucar.nc2.ft.point;

import com.google.common.math.DoubleMath;
import ucar.ma2.Array;
import ucar.ma2.MAMath;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.ft.PointFeature;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.Station;

import java.io.IOException;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

/**
 * @author cwardgar
 * @since 2014/08/28
 */
public class PointTestUtil {
    public static boolean equals(StationPointFeature stationPointFeat1, StationPointFeature stationPointFeat2)
            throws IOException {
        if (stationPointFeat1 == stationPointFeat2) {
            return true;
        } else if (stationPointFeat1 == null || stationPointFeat2 == null) {
            return false;
        }

        if (!equals((PointFeature) stationPointFeat1, (PointFeature) stationPointFeat2)) {
            return false;
        } else if (!equals(stationPointFeat1.getStation(), stationPointFeat2.getStation())) {
            return false;
        }

        return true;
    }

    public static boolean equals(StationFeature stationFeat1, StationFeature stationFeat2) throws IOException {
        if (stationFeat1 == stationFeat2) {
            return true;
        } else if (stationFeat1 == null || stationFeat2 == null) {
            return false;
        }

        if (!equals((Station) stationFeat1, (Station) stationFeat2)) {
            return false;
        } else if (!equals(stationFeat1.getFeatureData(), stationFeat2.getFeatureData())) {
            return false;
        }

        return true;
    }

    public static boolean equals(Station station1, Station station2) {
        if (station1 == station2) {
            return true;
        } else if (station1 == null || station2 == null) {
            return false;
        }

        if (!equals((EarthLocation) station1, (EarthLocation) station2)) {
            return false;
        } else if (!Objects.deepEquals(station1.getName(), station2.getName())) {
            return false;
        } else if (!Objects.deepEquals(station1.getWmoId(), station2.getWmoId())) {
            return false;
        } else if (!Objects.deepEquals(station1.getDescription(), station2.getDescription())) {
            return false;
        } else if (!Objects.deepEquals(station1.getNobs(), station2.getNobs())) {
            return false;
        }

        return true;
    }

    public static boolean equals(PointFeature pointFeat1, PointFeature pointFeat2) throws IOException {
        if (pointFeat1 == pointFeat2) {
            return true;
        } else if (pointFeat1 == null || pointFeat2 == null) {
            return false;
        }

        if (!equals(pointFeat1.getLocation(), pointFeat2.getLocation())) {
            return false;
        } else if (!DoubleMath.fuzzyEquals(pointFeat1.getObservationTime(), pointFeat2.getObservationTime(), 1.0e-8)) {
            return false;
        } else if (!DoubleMath.fuzzyEquals(pointFeat1.getNominalTime(), pointFeat2.getNominalTime(), 1.0e-8)) {
            return false;
        } else if (!equals(pointFeat1.getFeatureData(), pointFeat2.getFeatureData())) {
            return false;
        }
        // getObservationTimeAsDate() and getObservationTimeAsCalendarDate() derive from getObservationTime().
        // getNominalTimeAsDate() and getNominalTimeAsCalendarDate() derive from getNominalTime().
        // getDataAll() may include data that doesn't "belong" to this feature, so ignore it.
        // getData() is deprecated.

        return true;
    }

    public static boolean equals(EarthLocation loc1, EarthLocation loc2) {
        if (loc1 == loc2) {
            return true;
        } else if (loc1 == null || loc2 == null) {
            return false;
        }

        if (!DoubleMath.fuzzyEquals(loc1.getLatitude(), loc2.getLatitude(), 1.0e-8)) {
            return false;
        } else if (!DoubleMath.fuzzyEquals(loc1.getLongitude(), loc2.getLongitude(), 1.0e-8)) {
            return false;
        } else if (!DoubleMath.fuzzyEquals(loc1.getAltitude(), loc2.getAltitude(), 1.0e-8)) {
            return false;
        } else if (!Objects.deepEquals(loc1.getLatLon(), loc2.getLatLon())) {
            return false;
        } else if (!Objects.deepEquals(loc1.isMissing(), loc2.isMissing())) {
            return false;
        }

        return true;
    }

    public static boolean equals(StructureData sdata1, StructureData sdata2) {
        if (sdata1 == sdata2) {
            return true;
        } else if (sdata1 == null || sdata2 == null) {
            return false;
        }

        if (!equals(sdata1.getStructureMembers(), sdata2.getStructureMembers())) {
            return false;
        }

        for (String memberName : sdata1.getStructureMembers().getMemberNames()) {
            Array memberArray1 = sdata1.getArray(memberName);
            Array memberArray2 = sdata2.getArray(memberName);

            if (!MAMath.isEqual(memberArray1, memberArray2)) {
                return false;
            }
        }

        return true;
    }

    public static boolean equals(StructureMembers members1, StructureMembers members2) {
        if (members1 == members2) {
            return true;
        } else if (members1 == null || members2 == null) {
            return false;
        }

        if (!Objects.deepEquals(members1.getName(), members2.getName())) {
            return false;
        } else if (!equals(members1.getMembers(), members2.getMembers())) {
            return false;
        } else if (!Objects.deepEquals(members1.getStructureSize(), members2.getStructureSize())) {
          return false;
        }
        // StructureMembers.memberHash is derived from StructureMembers.members; no need to test it.

        return false;
    }

    public static boolean equals(
            List<StructureMembers.Member> membersList1, List<StructureMembers.Member> membersList2) {
        if (membersList1 == membersList2) {
            return true;
        } else if (membersList1 == null || membersList2 == null) {
            return false;
        }

        ListIterator<StructureMembers.Member> membersIter1 = membersList1.listIterator();
        ListIterator<StructureMembers.Member> membersIter2 = membersList2.listIterator();

        while (membersIter1.hasNext() && membersIter2.hasNext()) {
            StructureMembers.Member member1 = membersIter1.next();
            StructureMembers.Member member2 = membersIter2.next();

            if (!equals(member1, member2)) {
                return false;
            }
        }

        return !(membersIter1.hasNext() || membersIter2.hasNext());
    }

    public static boolean equals(StructureMembers.Member member1, StructureMembers.Member member2) {
        if (member1 == member2) {
            return true;
        } else if (member1 == null || member2 == null) {
            return false;
        }

        if (!Objects.deepEquals(member1.getName(), member2.getName())) {
            return false;
        } else if (!Objects.deepEquals(member1.getDescription(), member2.getDescription())) {
            return false;
        } else if (!Objects.deepEquals(member1.getUnitsString(), member2.getUnitsString())) {
            return false;
        } else if (!Objects.deepEquals(member1.getDataType(), member2.getDataType())) {
            return false;
        } else if (!Objects.deepEquals(member1.getSize(), member2.getSize())) {
            return false;
        } else if (!Objects.deepEquals(member1.getShape(), member2.getShape())) {
            return false;
        } else if (!equals(member1.getStructureMembers(), member2.getStructureMembers())) {
            return false;
        } else if (!Objects.deepEquals(member1.isVariableLength(), member2.isVariableLength())) {
            return false;
        } else if (!MAMath.isEqual(member1.getDataArray(), member2.getDataArray())) {
          return false;
        } else if (!Objects.deepEquals(member1.getDataObject(), member2.getDataObject())) {
            return false;
        } else if (Objects.deepEquals(member1.getDataParam(), member2.getDataParam())) {
            return false;
        }

        return true;
    }
}
