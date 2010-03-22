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

/**
 * This class manages huge bit fields. It is much faster than
 * {@link java.util.BitSet} and was specifically developed to be used with huge
 * bit sets in ISnapshot (e.g. needed in virtual GC traces). Out of performance
 * reasons no method does any parameter checking, i.e. only valid values are
 * expected.
 */
public final class BitField implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int[] bits;

    /**
     * Creates a bit field with the given number of bits. Size is expected to be
     * positive - out of performance reasons no checks are done!
     */
    public BitField(int size)
    {
        bits = new int[(((size) - 1) >>> 0x5) + 1];
    }

    /**
     * Sets the bit on the given index. Index is expected to be in range - out
     * of performance reasons no checks are done!
     */
    public final void set(int index)
    {
        bits[index >>> 0x5] |= (1 << (index & 0x1f));
    }

    /**
     * Clears the bit on the given index. Index is expected to be in range - out
     * of performance reasons no checks are done!
     */
    public final void clear(int index)
    {
        bits[index >>> 0x5] &= ~(1 << (index & 0x1f));
    }

    /**
     * Gets the bit on the given index. Index is expected to be in range - out
     * of performance reasons no checks are done!
     */
    public final boolean get(int index)
    {
        return (bits[index >>> 0x5] & (1 << (index & 0x1f))) != 0;
    }
}
