/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft.point.remote;

import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.point.PointCollectionImpl;
import ucar.nc2.ft.point.PointIteratorEmpty;
import ucar.nc2.stream.NcStream;
import ucar.nc2.stream.NcStreamProto;
import ucar.nc2.units.DateUnit;

import java.io.IOException;
import java.io.InputStream;

/**
 * PointCollection over cdmRemote protocol
 *
 * @author caron
 * @since Jun 15, 2009
 */
public abstract class RemotePointCollectionAbstract extends PointCollectionImpl {
  public RemotePointCollectionAbstract(String name, DateUnit timeUnit, String altUnits) {
    super(name, timeUnit, altUnits);
  }

  public abstract InputStream getInputStream() throws IOException;

  @Override
  public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
    try (InputStream in = getInputStream()) {
      PointStream.MessageType mtype = PointStream.readMagic(in);

      if (mtype == PointStream.MessageType.PointFeatureCollection) {
        int len = NcStream.readVInt(in);
        byte[] data = new byte[len];
        NcStream.readFully(in, data);

        PointStreamProto.PointFeatureCollection pfc = PointStreamProto.PointFeatureCollection.parseFrom(data);
        PointFeatureIterator iter = new RemotePointFeatureIterator(in, new PointStream.ProtobufPointFeatureMaker(pfc));
        iter.setCalculateBounds(this);

        return iter;
      } else if (mtype == PointStream.MessageType.End) {
        return new PointIteratorEmpty(); // nothing in the iteration
      } else if (mtype == PointStream.MessageType.Error) {
        int len = NcStream.readVInt(in);
        byte[] data = new byte[len];
        NcStream.readFully(in, data);

        NcStreamProto.Error proto = NcStreamProto.Error.parseFrom(data);
        throw new IOException(NcStream.decodeErrorMessage(proto));
      } else {
        throw new IOException("Illegal pointstream message type= " + mtype); // maybe kill the socket ?
      }
    }
  }
}
