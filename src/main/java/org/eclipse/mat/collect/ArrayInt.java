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

public final class ArrayInt
{
    int elements[];
    int size;

    public ArrayInt()
    {
        this(10);
    }

    public ArrayInt(int initialCapacity)
    {
        elements = new int[initialCapacity];
        size = 0;
    }

    public ArrayInt(int[] initialValues)
    {
        this(initialValues.length);
        System.arraycopy(initialValues, 0, elements, 0, initialValues.length);
        size = initialValues.length;
    }

    public ArrayInt(ArrayInt template)
    {
        this(template.size);
        System.arraycopy(template.elements, 0, elements, 0, template.size);
        size = template.size;
    }

    public void add(int element)
    {
        ensureCapacity(size + 1);
        elements[size++] = element;
    }

    public void addAll(int[] elements)
    {
        ensureCapacity(size + elements.length);
        System.arraycopy(elements, 0, this.elements, size, elements.length);
        size += elements.length;
    }

    public void addAll(ArrayInt template)
    {
        ensureCapacity(size + template.size);
        System.arraycopy(template.elements, 0, elements, size, template.size);
        size += template.size;
    }

    public int set(int index, int element)
    {
        if (index < 0 || index >= size)
            throw new ArrayIndexOutOfBoundsException(index);

        int oldValue = elements[index];
        elements[index] = element;
        return oldValue;
    }

    public int get(int index)
    {
        if (index < 0 || index >= size)
            throw new ArrayIndexOutOfBoundsException(index);
        return elements[index];
    }

    public int size()
    {
        return size;
    }

    public int[] toArray()
    {
        int[] result = new int[size];
        System.arraycopy(elements, 0, result, 0, size);
        return result;
    }

    public boolean isEmpty()
    {
        return size == 0;
    }

    public IteratorInt iterator()
    {
        return new IteratorInt()
        {
            int index = 0;

            public boolean hasNext()
            {
                return index < size;
            }

            public int next()
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
            int oldData[] = elements;
            int newCapacity = (oldCapacity * 3) / 2 + 1;
            if (newCapacity < minCapacity)
                newCapacity = minCapacity;
            elements = new int[newCapacity];
            System.arraycopy(oldData, 0, elements, 0, size);
        }
    }
}
