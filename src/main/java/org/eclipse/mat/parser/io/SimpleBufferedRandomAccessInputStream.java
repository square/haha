/*******************************************************************************
 * Copyright (c) 2008 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.mat.parser.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class SimpleBufferedRandomAccessInputStream extends InputStream
{
    private byte[] readBuffer = new byte[32];

    private RandomAccessFile raf;
    private byte[] buffer;
    private int buf_end;
    private int buf_pos;
    private long real_pos;

    public SimpleBufferedRandomAccessInputStream(RandomAccessFile in) throws IOException
    {
        this(in, 2 * 4096);
    }

    public SimpleBufferedRandomAccessInputStream(RandomAccessFile in, int bufsize) throws IOException
    {
        this.raf = in;
        invalidate();
        buffer = new byte[bufsize];
    }

    private void invalidate() throws IOException
    {
        buf_end = 0;
        buf_pos = 0;
        real_pos = raf.getFilePointer();
    }

    public final int read() throws IOException
    {
        if (buf_pos >= buf_end)
        {
            if (fillBuffer() < 0)
                return -1;
        }
        if (buf_end == 0)
        {
            return -1;
        }
        else
        {
            return buffer[buf_pos++] & 0xff;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        if (len == 0) { return 0; }

        if (buf_pos >= buf_end)
        {
            if (fillBuffer() < 0)
                return -1;
        }

        if (buf_end == 0)
            return -1;

        int copied = 0;

        while (copied < len)
        {
            if (buf_pos >= buf_end)
            {
                if (fillBuffer() < 0)
                    return copied;
            }

            int length = Math.min(len - copied, buf_end - buf_pos);
            System.arraycopy(buffer, buf_pos, b, off + copied, length);
            buf_pos += length;
            copied += length;
        }

        return copied;
    }

    private int fillBuffer() throws IOException
    {
        int n = raf.read(buffer);
        if (n >= 0)
        {
            real_pos += n;
            buf_end = n;
            buf_pos = 0;
        }
        return n;
    }

    public boolean markSupported()
    {
        return false;
    }

    public void close() throws IOException
    {
        raf.close();
        buffer = null;
    }

    public void seek(long pos) throws IOException
    {
        int n = (int) (real_pos - pos);
        if (n >= 0 && n <= buf_end)
        {
            buf_pos = buf_end - n;
        }
        else
        {
            raf.seek(pos);
            invalidate();
        }
    }

    public long getFilePointer()
    {
        return (real_pos - buf_end + buf_pos);
    }

    public final int readInt() throws IOException
    {
        if (buf_pos + 4 < buf_end)
        {
            int a = readInt(buffer, buf_pos);
            buf_pos += 4;
            return a;
        }
        else
        {
            if (read(readBuffer, 0, 4) != 4)
                throw new IOException();
            return readInt(readBuffer, 0);
        }
    }

    public final long readLong() throws IOException
    {
        if (buf_pos + 8 < buf_end)
        {
            long a = readLong(buffer, buf_pos);
            buf_pos += 8;
            return a;
        }
        else
        {
            if (read(readBuffer, 0, 8) != 8)
                throw new IOException();
            return readLong(readBuffer, 0);
        }
    }

    public int readIntArray(int[] a) throws IOException
    {
        int offset = 0, len = a.length * 4;
        byte[] b = null;
        if (buf_pos + len < buf_end)
        {
            b = buffer;
            offset = buf_pos;
            buf_pos += len;
        }
        else
        {
            b = len > readBuffer.length ? new byte[len] : readBuffer;
            if (read(b, 0, len) != len)
                throw new IOException();
        }

        for (int ii = 0; ii < a.length; ii++)
            a[ii] = readInt(b, offset + (ii * 4));

        return a.length;
    }

    private static final int readInt(byte[] b, int offset) throws IOException
    {
        int ch1 = b[offset] & 0xff;
        int ch2 = b[offset + 1] & 0xff;
        int ch3 = b[offset + 2] & 0xff;
        int ch4 = b[offset + 3] & 0xff;
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    public int readLongArray(long[] a) throws IOException
    {
        int offset = 0, len = a.length * 8;
        byte[] b = null;
        if (buf_pos + len < buf_end)
        {
            b = buffer;
            offset = buf_pos;
            buf_pos += len;
        }
        else
        {
            b = len > readBuffer.length ? new byte[len] : readBuffer;
            if (read(b, 0, len) != len)
                throw new IOException();
        }

        for (int ii = 0; ii < a.length; ii++)
            a[ii] = readLong(b, offset + (ii * 8));

        return a.length;
    }

    private static final long readLong(byte[] b, int offset)
    {
        return (((long) b[offset] << 56) //
                        + ((long) (b[offset + 1] & 255) << 48) //
                        + ((long) (b[offset + 2] & 255) << 40) //
                        + ((long) (b[offset + 3] & 255) << 32) //
                        + ((long) (b[offset + 4] & 255) << 24) //
                        + ((b[offset + 5] & 255) << 16) //
                        + ((b[offset + 6] & 255) << 8) //
        + ((b[offset + 7] & 255) << 0));
    }

}
