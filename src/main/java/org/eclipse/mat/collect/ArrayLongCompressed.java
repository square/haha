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
package org.eclipse.mat.collect;

/**
 * This class compresses fixed-size long[] in a very fast and memory efficient
 * manner if many leading and/or trailing bits of the stored longs are not used
 * commonly. The internal data is never copied during the process of retrieving
 * or reconstructing objects of this class and allows for fast I/O writing and
 * reading of the underlying byte[]. Furthermore alomst no additinal data is
 * used beside the underlying byte[]. Thereby the memory consumption of this
 * data structure is kept at a minmum to build efficient long[] caches.
 */
public class ArrayLongCompressed
{
    private static final int BIT_LENGTH = 0x40;

    private byte[] data;
    private byte varyingBits;
    private byte trailingClearBits;

    /**
     * Create <code>LongArrayCompressed</code> from bytes formerly got from
     * {@link #toByteArray()}.
     * 
     * @param bytes
     *            bytes formerly got from {@link #toByteArray()}
     */
    public ArrayLongCompressed(byte[] bytes)
    {
        // Take data structure
        data = bytes;
        varyingBits = data[0];
        trailingClearBits = data[1];
    }

    /**
     * Create <code>LongArrayCompressed</code> from number of longs to be
     * stored, the number of leading and trailing clear bits. Everything else is
     * stored in the internal data structure.
     * 
     * @param size
     *            number of longs to be stored
     * @param leadingClearBits
     *            number of leading clear bits
     * @param trailingClearBits
     *            number of trailing clear bits
     */
    public ArrayLongCompressed(int size, int leadingClearBits, int trailingClearBits)
    {
        // Initialize data structure
        init(size, BIT_LENGTH - leadingClearBits - trailingClearBits, trailingClearBits);
    }

    /**
     * Create <code>LongArrayCompressed</code> from longs representing the data
     * to be stored in compressed form.
     * 
     * @param longs
     *            longs representing the data to be stored in compressed form
     */
    public ArrayLongCompressed(long[] longs)
    {
        // Call more general constructor
        this(longs, 0, longs.length);
    }

    /**
     * Create <code>LongArrayCompressed</code> from longs representing the data
     * to be stored in compressed form (from offset to offset+length).
     * 
     * @param longs
     *            longs representing the data to be stored in compressed form
     * @param offset
     *            offset from which on to compress the longs
     * @param length
     *            number of longs to compress from the given array
     */
    public ArrayLongCompressed(long[] longs, int offset, int length)
    {
        // Determine leading and trailing clear bits
        long mask = 0x0;
        for (int i = 0; i < length; i++)
        {
            mask |= longs[offset + i];
        }
        int leadingClearBits = 0;
        int trailingClearBits = 0;
        while (((mask & (1 << (BIT_LENGTH - leadingClearBits - 1))) == 0) && (leadingClearBits < BIT_LENGTH))
        {
            leadingClearBits++;
        }
        while (((mask & (1 << trailingClearBits)) == 0) && (trailingClearBits < (BIT_LENGTH - leadingClearBits)))
        {
            trailingClearBits++;
        }

        // Initialize data structure
        init(length, BIT_LENGTH - leadingClearBits - trailingClearBits, trailingClearBits);

        // Store data
        for (int i = 0; i < length; i++)
        {
            set(i, longs[offset + i]);
        }
    }

    private void init(int size, int varyingBits, int trailingClearBits)
    {
        // Allocate memory for header and data structure and put decompression
        // information into header
        data = new byte[2 + (int) (((((long) size) * varyingBits) - 1) / 0x8) + 1];
        this.varyingBits = data[0] = (byte) varyingBits;
        this.trailingClearBits = data[1] = (byte) trailingClearBits;
    }

    /**
     * Set value at the given index.
     * 
     * @param index
     *            index at which the value should be set
     * @param value
     *            value to be set at the given index
     */
    public void set(int index, long value)
    {
        value >>>= trailingClearBits;
        final long pos = (long) (index) * varyingBits;
        int idx = 2 + (int) (pos >>> 3);
        int off = ((int) (pos)) & 0x7;
        off += varyingBits;
        if (off > 0x8)
        {
            off -= 0x8;
            data[idx++] |= (byte) (value >>> off);
            while (off > 0x8)
            {
                off -= 0x8;
                data[idx++] = (byte) (value >>> off);
            }
        }
        data[idx] |= (byte) (value << (0x8 - off));
    }

    /**
     * Get value from the given index.
     * 
     * @param index
     *            index at which the value should be set
     * @return value found at the given index
     */
    public long get(int index)
    {
        long value = 0;
        final long pos = (long) (index) * varyingBits;
        int idx = 2 + (int) (pos >>> 3);
        int off = ((int) (pos)) & 0x7;
        if ((off + varyingBits) > 0x8)
        {
            value = ((data[idx++] << off) & 0xff) >>> off;
            off += varyingBits - 0x8;
            while (off > 0x8)
            {
                value <<= 0x8;
                value |= data[idx++] & 0xff;
                off -= 0x8;
            }
            value <<= off;
            value |= (data[idx] & 0xff) >>> (0x8 - off);
        }
        else
        {
            value = ((data[idx] << off) & 0xff) >>> (0x8 - varyingBits);
        }
        return value << trailingClearBits;
    }

    /**
     * Get bytes representing the internal data structure with which an
     * <code>LongArrayCompressed</code> can be reconstructed.
     * 
     * @return bytes representing the internal data structure with which an
     *         <code>LongArrayCompressed</code> can be reconstructed
     */
    public byte[] toByteArray()
    {
        // Return data structure
        return data;
    }
}
