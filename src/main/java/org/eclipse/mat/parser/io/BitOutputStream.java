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

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.mat.parser.internal.Messages;

public class BitOutputStream implements Flushable, Closeable
{

    public final static int DEFAULT_BUFFER_SIZE = 16 * 1024;

    private OutputStream os;
    private int current;
    private byte[] buffer;
    private int free;
    private int pos;
    private int avail;

    final static int TEMP_BUFFER_SIZE = 128;
    private byte[] tempBuffer = new byte[TEMP_BUFFER_SIZE];

    public BitOutputStream(final OutputStream os)
    {
        this.os = os;
        this.buffer = new byte[DEFAULT_BUFFER_SIZE];
        avail = DEFAULT_BUFFER_SIZE;
        free = 8;
    }

    public void flush() throws IOException
    {
        align();

        os.write(buffer, 0, pos);
        pos = 0;
        avail = buffer.length;

        os.flush();
    }

    public void close() throws IOException
    {
        flush();
        os.close();
        os = null;
        buffer = null;
        tempBuffer = null;
    }

    private void write(final int b) throws IOException
    {
        if (avail-- == 0)
        {
            if (os == null)
            {
                avail = 0;
                throw new IOException(Messages.BitOutputStream_Error_ArrayFull);
            }

            if (buffer == null)
            {
                os.write(b);
                avail = 0;
                return;
            }
            os.write(buffer);
            avail = buffer.length - 1;
            pos = 0;
        }

        buffer[pos++] = (byte) b;
    }

    private int writeInCurrent(final int b, final int len) throws IOException
    {
        current |= (b & ((1 << len) - 1)) << (free -= len);
        if (free == 0)
        {
            write(current);
            free = 8;
            current = 0;
        }

        return len;
    }

    private int align() throws IOException
    {
        if (free != 8)
            return writeInCurrent(0, free);
        else
            return 0;
    }

    public int writeBit(final int bit) throws IOException
    {
        return writeInCurrent(bit, 1);
    }

    public int writeInt(int x, final int len) throws IOException
    {
        if (len <= free)
            return writeInCurrent(x, len);

        final int queue = (len - free) & 7, blocks = (len - free) >> 3;
        int i = blocks;

        if (queue != 0)
        {
            tempBuffer[blocks] = (byte) x;
            x >>= queue;
        }

        while (i-- != 0)
        {
            tempBuffer[i] = (byte) x;
            x >>>= 8;
        }

        writeInCurrent(x, free);
        for (i = 0; i < blocks; i++)
            write(tempBuffer[i]);

        if (queue != 0)
            writeInCurrent(tempBuffer[blocks], queue);
        return len;
    }

}
