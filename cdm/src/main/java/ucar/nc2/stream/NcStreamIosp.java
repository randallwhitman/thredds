/*
 * Copyright 2009-2012 University Corporation for Atmospheric Research/Unidata
 *
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

package ucar.nc2.stream;

import ucar.nc2.Structure;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.IO;
import ucar.unidata.io.RandomAccessFile;
import ucar.ma2.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * IOSP to read ncStream file (RandomAccessFile), into a NetcdfFile.
 */
public class NcStreamIosp extends AbstractIOServiceProvider {
  private static final boolean debug = false;

  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    raf.seek(0);
    if (!readAndTest(raf, NcStream.MAGIC_START)) return false; // must start with these 4 bytes
    byte[] b = new byte[4];
    raf.readFully(b);
    return test(b, NcStream.MAGIC_HEADER) || test(b, NcStream.MAGIC_DATA); // immed followed by one of these
  }

  public String getFileTypeId() {
    return "ncstream";
  }

  public String getFileTypeDescription() {
    return "netCDF streaming protocol";
  }

  public int getVersion() {
    return version;
  }


  //////////////////////////////////////////////////////////////////////
  private int version;

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    super.open(raf, ncfile, cancelTask);
    openDebug(raf, ncfile, null);
  }

  private static class DataStorage {
    int size;
    long filePos;
    Section section;
    boolean isVlen, isDeflate;
    ByteOrder bo;
    int nelems, uncompressedLen;
    NcStreamProto.StructureData sdata;

    DataStorage(int size, long filePos, NcStreamProto.Data dproto) {
      this.size = size;
      this.filePos = filePos;
      section = NcStream.decodeSection(dproto.getSection());
      nelems = (int) section.computeSize();
      bo = dproto.getBigend() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
      isVlen = dproto.getVdata();
      isDeflate = dproto.getCompress() == NcStreamProto.Compress.DEFLATE;
      if (isDeflate)
        uncompressedLen = dproto.getUncompressedSize();
    }

    @Override
    public String toString() {
      return "size=" + size +
              ", filePos=" + filePos +
              ", section=" + section +
              ", nelems=" + nelems +
              ", isVlen=" + isVlen +
              ", isDeflate=" + isDeflate;
    }
  }

  public Array readData(Variable v, Section section) throws IOException, InvalidRangeException {
    List<DataStorage> storage = (List<DataStorage>) v.getSPobject();
    ByteBuffer result = null;

    for (DataStorage dataStorage : storage) {
      if (dataStorage.isVlen)
        return readVlenData(v, section, dataStorage);

      if (dataStorage.sdata != null)
        return readStructureData((Structure) v, section, dataStorage);

      if (dataStorage.section.intersects(section)) { // LOOK WRONG
        raf.seek(dataStorage.filePos);
        byte[] data = new byte[dataStorage.size];
        raf.readFully(data);

        if (dataStorage.isDeflate) {
          ByteArrayInputStream bin = new ByteArrayInputStream(data);
          InflaterInputStream in = new InflaterInputStream(bin);
          ByteArrayOutputStream bout = new ByteArrayOutputStream(data.length * 7);
          IO.copy(in, bout);
          byte[] resultb = bout.toByteArray();
          result = ByteBuffer.wrap(resultb); // look - an extra copy !! override ByteArrayOutputStream to fix
          if (debug) System.out.printf(" uncompressedLen header=%d actual=%d%n", dataStorage.uncompressedLen , resultb.length);
          //result.order(ByteOrder.LITTLE_ENDIAN); // LOOK
        } else {
          result = ByteBuffer.wrap(data);
        }
      }
    }

    if (result == null) return null;

    return Array.factory(v.getDataType(), v.getShape(), result);
    //return dataArray.sectionNoReduce(section.getRanges());
  }

  private Array readStructureData(Structure v, Section section, DataStorage dataStorage) throws IOException, InvalidRangeException {
    ByteBuffer bb = dataStorage.sdata.getData().asReadOnlyByteBuffer();
    bb.order(dataStorage.bo);
    StructureMembers sm = v.makeStructureMembers();
    ArrayStructureBB.setOffsets(sm);
    // StructureMembers members, int[] shape, ByteBuffer bbuffer, int offset
    ArrayStructureBB all =  new ArrayStructureBB(sm, v.getShape(), bb, 0);
    return ArrayStructureBBsection.factory(all, section);
  }

  // lOOK probably desnt work
  private Array readVlenData(Variable v, Section section, DataStorage dataStorage) throws IOException, InvalidRangeException {
    raf.seek(dataStorage.filePos);
    int nelems = readVInt(raf);
    Object[] result = new Object[nelems];

    for (int elem = 0; elem < nelems; elem++) {
      int dsize = readVInt(raf);
      byte[] data = new byte[dsize];
      raf.readFully(data);
      Array dataArray = Array.factory(v.getDataType(), null, ByteBuffer.wrap(data));
      result[elem] = dataArray;
    }
    return new ArrayObject(result[0].getClass(), new int[]{nelems}, result);
    //return dataArray.section(section.getRanges());
  }

  private int readVInt(RandomAccessFile raf) throws IOException {
    byte b = (byte) raf.read();
    int i = b & 0x7F;
    for (int shift = 7; (b & 0x80) != 0; shift += 7) {
      b = (byte) raf.read();
      i |= (b & 0x7F) << shift;
    }
    return i;
  }

  private boolean readAndTest(RandomAccessFile raf, byte[] test) throws IOException {
    byte[] b = new byte[test.length];
    raf.readFully(b);
    return test(b, test);
  }

  private boolean test(byte[] bread, byte[] test) throws IOException {
    if (bread.length != test.length) return false;
    for (int i = 0; i < bread.length; i++)
      if (bread[i] != test[i]) return false;
    return true;
  }

  ///////////////////////////////////////////////////////////////////////////////
  // lower level interface for debugging
  // optionally read in all messages, return as List<NcMess>

  public void openDebug(RandomAccessFile raf, NetcdfFile ncfile, List<NcsMess> ncm) throws IOException {
    raf.seek(0);
    long pos = raf.getFilePointer();

    if (!readAndTest(raf, NcStream.MAGIC_START)) {
      if (ncm != null) {
        ncm.add(new NcsMess(pos, 0, "MAGIC_START missing - abort"));
        return;
      }
      throw new IOException("Data corrupted on " + raf.getLocation());
    }
    if (ncm != null) ncm.add(new NcsMess(pos, 4, "MAGIC_START"));

    pos = raf.getFilePointer();
    if (!readAndTest(raf, NcStream.MAGIC_HEADER)) {
      if (ncm != null) {
        ncm.add(new NcsMess(pos, 0, "MAGIC_HEADER missing - abort"));
        return;
      }
      throw new IOException("Data corrupted on " + ncfile.getLocation());
    }
    if (ncm != null) ncm.add(new NcsMess(pos, 4,  "MAGIC_HEADER"));

    // assume for the moment it always starts with one header message
    pos = raf.getFilePointer();
    int msize = readVInt(raf);
    byte[] m = new byte[msize];
    raf.readFully(m);
    NcStreamProto.Header proto = NcStreamProto.Header.parseFrom(m);
    if (ncm != null) ncm.add(new NcsMess(pos, msize, proto));
    version = proto.getVersion();

    NcStreamProto.Group root = proto.getRoot();
    NcStream.readGroup(root, ncfile, ncfile.getRootGroup());
    ncfile.finish();

    // then we have a stream of data messages with a final END or ERR
    while (!raf.isAtEndOfFile()) {
      pos = raf.getFilePointer();
      byte[] b = new byte[4];
      raf.readFully(b);
      if (test(b, NcStream.MAGIC_END)) {
        if (ncm != null) ncm.add(new NcsMess(pos, 4, "MAGIC_END"));
        break;
      }

      if (test(b, NcStream.MAGIC_ERR)) {
        int esize = readVInt(raf);
        byte[] dp = new byte[esize];
        raf.readFully(dp);
        NcStreamProto.Error error = NcStreamProto.Error.parseFrom(dp);
        if (ncm != null) ncm.add(new NcsMess(pos, esize, error.getMessage()));
        break; // assume broken now ?
      }

      if (!test(b, NcStream.MAGIC_DATA)) {
        if (ncm != null) ncm.add(new NcsMess(pos, 4, "MAGIC_DATA missing - abort"));
        break;
      }
      if (ncm != null) ncm.add(new NcsMess(pos, 4, "MAGIC_DATA"));

      // data messages
      pos = raf.getFilePointer();
      int psize = readVInt(raf);
      byte[] dp = new byte[psize];
      raf.readFully(dp);
      NcStreamProto.Data dproto = NcStreamProto.Data.parseFrom(dp);
      Variable v = ncfile.findVariable(dproto.getVarName());
      if (v == null) {
        System.out.printf(" ERR cant find var %s%n%s%n", dproto.getVarName(), dproto);
      }
      if (debug) System.out.printf(" dproto = %s for %s%n", dproto, v.getShortName());
      if (ncm != null) ncm.add(new NcsMess(pos, psize, dproto));
      List<DataStorage> storage;
      if (v != null) {
        storage = (List<DataStorage>) v.getSPobject(); // LOOK should be an in memory Rtree using section
        if (storage == null) {
          storage = new ArrayList<>();
          v.setSPobject(storage);
        }
      } else
        storage = new ArrayList<>(); // barf

      if (dproto.getDataType() == NcStreamProto.DataType.STRUCTURE) {
        pos = raf.getFilePointer();
        msize = readVInt(raf);
        m = new byte[msize];
        raf.readFully(m);
        NcStreamProto.StructureData sdata = NcStreamProto.StructureData.parseFrom(m);
        DataStorage dataStorage = new DataStorage(msize, pos, dproto);
        dataStorage.sdata = sdata;
        if (ncm != null) ncm.add(new NcsMess(dataStorage.filePos, msize, sdata));
        storage.add(dataStorage);

      } else if (dproto.getVdata()) {
        DataStorage dataStorage = new DataStorage(0, raf.getFilePointer(), dproto);
        int nelems = readVInt(raf);
        int totalSize = 0;
        for (int i = 0; i < nelems; i++) {
          int dsize = readVInt(raf);
          totalSize += dsize;
          raf.skipBytes(dsize);
        }
        dataStorage.nelems = nelems;
        dataStorage.size = totalSize;
        if (ncm != null) ncm.add(new NcsMess(dataStorage.filePos, totalSize, dataStorage));
        storage.add(dataStorage);

      } else {  // regular data
        int dsize = readVInt(raf);
        DataStorage dataStorage = new DataStorage(dsize, raf.getFilePointer(), dproto);
        if (ncm != null) ncm.add(new NcsMess(dataStorage.filePos, dsize, dataStorage));
        storage.add(dataStorage);
        raf.skipBytes(dsize);

      }
    }
  }

  public class NcsMess {
    public long filePos;
    public int len;
    public int nelems;
    public Object what;
    public DataType dataType;
    public String varName;

    public NcsMess(long filePos, int len, Object what) {
      this.filePos = filePos;
      this.len = len;
      this.what = what;
      if (what instanceof NcStreamProto.Data) {
        NcStreamProto.Data dataMess = (NcStreamProto.Data) what;
        this.dataType = NcStream.decodeDataType(dataMess.getDataType());
        this.varName = dataMess.getVarName();
        Section s = NcStream.decodeSection(dataMess.getSection());
        if (s != null)
          this.nelems = (int) s.computeSize();
      }
      else if (what instanceof DataStorage)
        this.nelems = ((DataStorage) what).nelems;
    }

    public String showDeflate() {
      if (!(what instanceof NcStreamIosp.DataStorage))
        return "Must select a NcStreamIosp.DataStorage object";

      Formatter f = new Formatter();
      try {
        NcStreamIosp.DataStorage dataStorage = (NcStreamIosp.DataStorage) what;

        raf.seek(dataStorage.filePos);
        byte[] data = new byte[dataStorage.size];
        raf.readFully(data);

        ByteArrayInputStream bin = new ByteArrayInputStream(data);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DeflaterOutputStream dout = new DeflaterOutputStream(bout);
        IO.copy(bin, dout);
        dout.close();
        int deflatedSize = bout.size();
        float ratio = ((float)data.length) / deflatedSize;
        f.format("Original size = %d bytes, deflated = %d bytes ratio = %f %n", data.length, deflatedSize, ratio);
        return f.toString();

      } catch (IOException e) {
        e.printStackTrace();
        return e.getMessage();
      }
    }

  }



}
