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

import java.io.Serializable;
import java.util.ArrayList;

/**
 * This class simplifies the handling of growing int[] in a very fast and memory
 * efficient manner so that no slow collections must be used. However this class
 * is only fast on big int[] and not on small ones where you collect just a
 * couple of ints. The internal data is never copied during the process of
 * growing. Only with {@link #toArray} the data is copied to the result int[].
 */
public final class ArrayIntBig implements Serializable
{
    private static final long serialVersionUID = 1L;

    private ArrayList<int[]> pages;
    private int[] page;
    private int length;

    /**
     * Create an <code>IntArray</code>. Memory consumption is equal to creating
     * a new <code>ArrayList</code>.
     */
    public ArrayIntBig()
    {
        pages = new ArrayList<int[]>();
        length = 0;
    }

    /**
     * Add int to <code>IntArray</code>.
     * 
     * @param element
     *            int which should be added
     */
    public final void add(int element)
    {
        int index = (length++) & 0x3FF;
        if (index == 0)
        {
            pages.add(page = new int[0x400]);
        }
        page[index] = element;
    }

    /**
     * Add int[] to <code>IntArray</code>.
     * 
     * @param elements
     *            int[] which should be added
     */
    public final void addAll(int[] elements)
    {
        int free = (length & 0x3FF);
        int bite = free == 0 ? 0 : Math.min(elements.length, 0x400 - free);
        if (bite > 0)
        {
            System.arraycopy(elements, 0, pages.get(length >> 10), length & 0x3FF, bite);
            length += bite;
        }
        int copied = bite;
        while (copied < elements.length)
        {
            pages.add(page = new int[0x400]);
            bite = Math.min(elements.length - copied, 0x400);
            System.arraycopy(elements, copied, page, 0, bite);
            copied += bite;
            length += bite;
        }
    }

    /**
     * Get int at index from <code>IntArray</code>.
     * 
     * @param index
     *            index of int which should be returned
     * @return int at index
     * @throws IndexOutOfBoundsException
     */
    public final int get(int index) throws IndexOutOfBoundsException
    {
        if (index >= length) { throw new IndexOutOfBoundsException(); }
        return pages.get(index >> 10)[index & 0x3FF];
    }

    /**
     * Get length of <code>IntArray</code>.
     * 
     * @return length of <code>IntArray</code>
     */
    public final int length()
    {
        return length;
    }

    /**
     * Returns <tt>true</tt> if this list contains no elements.
     * 
     * @return <tt>true</tt> if this list contains no elements.
     */
    public boolean isEmpty()
    {
        return length == 0;
    }

    /**
     * Get memory consumption of <code>IntArray</code>.
     * 
     * @return memory consumption of <code>IntArray</code>
     */
    public final long consumption()
    {
        return ((long) pages.size()) << 12;
    }

    /**
     * Convert <code>IntArray</code> to int[]. This operation is the only one
     * where the internal data is copied. It is directly copied to the int[]
     * which is returned, so don't call this method more than once when done.
     * 
     * @return int[] representing the <code>IntArray</code>
     */
    public final int[] toArray()
    {
        int[] elements = new int[length];
        int bite;
        int copied = 0;
        while (copied < length)
        {
            bite = Math.min(length - copied, 0x400);
            System.arraycopy(pages.get(copied >> 10), 0, elements, copied, bite);
            copied += bite;
        }
        return elements;
    }
}
