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

import java.util.Arrays;

public final class ArrayLong
{
    long elements[];
    int size;

    public ArrayLong()
    {
        this(10);
    }

    public ArrayLong(int initialCapacity)
    {
        elements = new long[initialCapacity];
        size = 0;
    }

    public ArrayLong(long[] initialValues)
    {
        this(initialValues.length);
        System.arraycopy(initialValues, 0, elements, 0, initialValues.length);
        size = initialValues.length;
    }

    public ArrayLong(ArrayLong template)
    {
        this(template.size);
        System.arraycopy(template.elements, 0, elements, 0, template.size);
        size = template.size;
    }

    public void add(long element)
    {
        ensureCapacity(size + 1);
        elements[size++] = element;
    }

    public void addAll(long[] elements)
    {
        ensureCapacity(size + elements.length);
        System.arraycopy(elements, 0, this.elements, size, elements.length);
        size += elements.length;
    }

    public void addAll(ArrayLong template)
    {
        ensureCapacity(size + template.size);
        System.arraycopy(template.elements, 0, elements, size, template.size);
        size += template.size;
    }

    public long set(int index, long element)
    {
        if (index < 0 || index >= size)
            throw new ArrayIndexOutOfBoundsException(index);

        long oldValue = elements[index];
        elements[index] = element;
        return oldValue;
    }

    public long get(int index)
    {
        if (index < 0 || index >= size)
            throw new ArrayIndexOutOfBoundsException(index);
        return elements[index];
    }

    public int size()
    {
        return size;
    }

    public long[] toArray()
    {
        long[] result = new long[size];
        System.arraycopy(elements, 0, result, 0, size);
        return result;
    }

    public boolean isEmpty()
    {
        return size == 0;
    }

    public IteratorLong iterator()
    {
        return new IteratorLong()
        {
            int index = 0;

            public boolean hasNext()
            {
                return index < size;
            }

            public long next()
            {
                return elements[index++];
            }
        };
    }

    public void clear()
    {
        size = 0;
    }

    public long lastElement()
    {
        return elements[size - 1];
    }

    public long firstElement()
    {
        if (size == 0)
            throw new ArrayIndexOutOfBoundsException();

        return elements[0];
    }

    public void sort()
    {
        Arrays.sort(elements, 0, size);
    }

    // //////////////////////////////////////////////////////////////
    // implementation stuff
    // //////////////////////////////////////////////////////////////

    private void ensureCapacity(int minCapacity)
    {
        int oldCapacity = elements.length;
        if (minCapacity > oldCapacity)
        {
            long oldData[] = elements;
            int newCapacity = (oldCapacity * 3) / 2 + 1;
            if (newCapacity < minCapacity)
                newCapacity = minCapacity;
            elements = new long[newCapacity];
            System.arraycopy(oldData, 0, elements, 0, size);
        }
    }

}
