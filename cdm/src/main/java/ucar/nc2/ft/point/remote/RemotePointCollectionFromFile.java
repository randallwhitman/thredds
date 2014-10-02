package ucar.nc2.ft.point.remote;

import ucar.nc2.units.DateUnit;

import java.io.*;

/**
 * @author cwardgar
 * @since 2014/10/02
 */
public class RemotePointCollectionFromFile extends RemotePointCollectionAbstract {
    private final File file;

    public RemotePointCollectionFromFile(File file) {
        // timeUnit and altUnit values are temporary; we'll read the correct values from file once
        // getPointFeatureIterator() is called.
        super(file.getAbsolutePath(), DateUnit.getUnixDateUnit(), null);
        this.file = file;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new BufferedInputStream(new FileInputStream(file));
    }

    // TODO: Read the correct values for timeUnit and altUnit from file.
}
