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
import java.io.EOFException;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;

public class BitInputStream implements Flushable, Closeable
{
    public final static int DEFAULT_BUFFER_SIZE = 16 * 1024;

    private InputStream is;
    private int current;
    private byte[] buffer;
    private int fill;
    private int pos;
    private int avail;

    public BitInputStream(final InputStream is)
    {
        this.is = is;
        this.buffer = new byte[DEFAULT_BUFFER_SIZE];
    }

    public void flush()
    {
        avail = 0;
        pos = 0;
        fill = 0;
    }

    public void close() throws IOException
    {
        is.close();
        is = null;
        buffer = null;
    }

    private int read() throws IOException
    {
        if (avail == 0)
        {
            avail = is.read(buffer);
            if (avail == -1)
            {
                avail = 0;
                throw new EOFException();
            }
            else
            {
                pos = 0;
            }
        }

        avail--;
        return buffer[pos++] & 0xFF;
    }

    private int readFromCurrent(final int len) throws IOException
    {
        if (len == 0)
            return 0;

        if (fill == 0)
        {
            current = read();
            fill = 8;
        }

        return current >>> (fill -= len) & (1 << len) - 1;
    }

    public int readBit() throws IOException
    {
        return readFromCurrent(1);
    }

    public int readInt(int len) throws IOException
    {
        int i, x = 0;

        if (len <= fill)
            return readFromCurrent(len);

        len -= fill;
        x = readFromCurrent(fill);

        i = len >> 3;
        while (i-- != 0)
            x = x << 8 | read();

        len &= 7;

        return (x << len) | readFromCurrent(len);
    }

}
