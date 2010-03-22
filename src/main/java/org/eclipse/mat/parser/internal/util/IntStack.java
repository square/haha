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
package org.eclipse.mat.parser.internal.util;

public class IntStack
{
    private int[] data;
    private int size;

    public IntStack()
    {
        this(16);
    }

    public IntStack(int capacity)
    {
        data = new int[capacity];
    }

    public final int pop()
    {
        return data[--size];
    }

    public final void push(int i)
    {
        if (size == data.length)
        {
            int[] newArr = new int[data.length << 1];
            System.arraycopy(data, 0, newArr, 0, data.length);
            data = newArr;
        }
        data[size++] = i;
    }

    public final int peek()
    {
        return data[size - 1];
    }

    public final int size()
    {
        return size;
    }

    public final int capacity()
    {
        return data.length;
    }

}
