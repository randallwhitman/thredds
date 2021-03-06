/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.core.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

abstract public class DapDump
{
    //////////////////////////////////////////////////
    // Provide a simple dump of binary data
    // (Static method)

    //////////////////////////////////////////////////
    // Constants

    static int MAXLIMIT = 20000;
    //////////////////////////////////////////////////
    // Provide a simple dump of binary data

    static public void
    dumpbytes(ByteBuffer buf0, boolean skipdmr)
    {
        int savepos = buf0.position();
        int limit0 = buf0.limit();
        int skipcount = 0;
        if(limit0 > MAXLIMIT) limit0 = MAXLIMIT;
        if(limit0 >= buf0.limit()) limit0 = buf0.limit();
        if(skipdmr) {
            skipcount = buf0.getInt(); //dmr count
            skipcount &= 0xFFFFFF; // mask off the flags to get true count
            skipcount += 4; // skip the count also
        }
        byte[] bytes = new byte[(limit0 + 8) - skipcount];
        Arrays.fill(bytes, (byte) 0);
        buf0.position(savepos + skipcount);
        buf0.get(bytes, 0, limit0 - skipcount);
        buf0.position(savepos);

        ByteBuffer buf = ByteBuffer.wrap(bytes).order(buf0.order());
        dumpbytes(buf);
    }

    /**
     * Dump the contents of a buffer from 0 to position
     *
     * @param buf0 byte buffer to dump
     */
    static public void
    dumpbytes(ByteBuffer buf0)
    {
        int stop = buf0.limit();
        int size = stop + 8;
        ByteBuffer buf = ByteBuffer.allocate(size).order(buf0.order());
        Arrays.fill(buf.array(), (byte) 0);
        buf.put(buf0.array());
        buf.position(0);
        buf.limit(size);
        int i = 0;
        try {
            for(i = 0; buf.position() < stop; i++) {
                int savepos = buf.position();
                int iv = buf.getInt();
                buf.position(savepos);
                long lv = buf.getLong();
                buf.position(savepos);
                short sv = buf.getShort();
                buf.position(savepos);
                byte b = buf.get();
                long uiv = ((long) iv) & 0xFFFFFFFFL;
                int usv = ((int) sv) & 0xFFFF;
                int ib = (int) b;
                int ub = (iv & 0xFF);
                char c = (char) ub;
                String s = Character.toString(c);
                if(c == '\r') s = "\\r";
                else if(c == '\n') s = "\\n";
                else if(c < ' ') s = "?";
                System.err.printf("[%03d] %02x %03d %4d '%s'", i, ub, ub, ib, s);
                System.err.printf("\t%12d 0x%08x", iv, uiv);
                System.err.printf("\t%5d\t0x%04x", sv, usv);
                System.err.println();
                System.err.flush();
            }

        } catch (Exception e) {
            System.err.println("failure:" + e);
        } finally {
            System.err.flush();
            //new Exception().printStackTrace(System.err);
	        System.err.flush();
        }
    }

    static public void
    dumpbytestream(OutputStream stream, ByteOrder order, String tag)
    {
        if(stream instanceof ByteArrayOutputStream) {
            byte[] content = ((ByteArrayOutputStream) stream).toByteArray();
            dumpbytestream(content, order, tag);
        }
    }

    static public void
    dumpbytestream(ByteBuffer buf, ByteOrder order, String tag)
    {
        dumpbytestream(buf.array(),0,buf.position(),order,tag);
    }

    static public void
    dumpbytestream(byte[] content, ByteOrder order, String tag)
    {
        dumpbytestream(content,0,content.length,order,tag);
    }

    static public void
    dumpbytestream(byte[] content, int start, int len, ByteOrder order, String tag)
    {
        System.err.println("++++++++++ " + tag + " ++++++++++ ");
        ByteBuffer tmp = ByteBuffer.wrap(content).order(order);
        tmp.position(start);
        tmp.limit(len);
        DapDump.dumpbytes(tmp);
        System.err.println("++++++++++ " + tag + " ++++++++++ ");
        System.err.flush();
    }

}
