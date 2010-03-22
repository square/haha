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
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class HashMapLongObject<E> implements Serializable
{
    public interface Entry<E>
    {
        long getKey();

        E getValue();
    }

    private static final long serialVersionUID = 1L;

    private int capacity;
    private int step;
    private int limit;
    private int size;
    private boolean[] used;
    private long[] keys;
    private E[] values;

    public HashMapLongObject()
    {
        this(10);
    }

    public HashMapLongObject(int initialCapacity)
    {
        init(initialCapacity);
    }

    public E put(long key, E value)
    {
        if (size == limit)
            resize(capacity << 1);

        int hash = hash(key) % capacity;
        while (used[hash])
        {
            if (keys[hash] == key)
            {
                E oldValue = values[hash];
                values[hash] = value;
                return oldValue;
            }
            hash = (hash + step) % capacity;
        }
        used[hash] = true;
        keys[hash] = key;
        values[hash] = value;
        size++;
        return null;
    }

    private int hash(long key)
    {
        return (int) (key & Integer.MAX_VALUE);
    }

    public E remove(long key)
    {
        int hash = hash(key) % capacity;
        while (used[hash])
        {
            if (keys[hash] == key)
            {
                E oldValue = values[hash];
                used[hash] = false;
                size--;
                // Re-hash all follow-up entries anew; Do not fiddle with the
                // capacity limit (75 %) otherwise this code may loop forever
                hash = (hash + step) % capacity;
                while (used[hash])
                {
                    key = keys[hash];
                    used[hash] = false;
                    int newHash = hash(key) % capacity;
                    while (used[newHash])
                    {
                        newHash = (newHash + step) % capacity;
                    }
                    used[newHash] = true;
                    keys[newHash] = key;
                    values[newHash] = values[hash];
                    hash = (hash + step) % capacity;
                }
                return oldValue;
            }
            hash = (hash + step) % capacity;
        }
        return null;
    }

    public boolean containsKey(long key)
    {
        int hash = hash(key) % capacity;
        while (used[hash])
        {
            if (keys[hash] == key) { return true; }
            hash = (hash + step) % capacity;
        }
        return false;
    }

    public E get(long key)
    {
        int hash = hash(key) % capacity;
        while (used[hash])
        {
            if (keys[hash] == key) { return values[hash]; }
            hash = (hash + step) % capacity;
        }
        return null;
    }

    public long[] getAllKeys()
    {
        long[] array = new long[size];
        int j = 0;
        for (int i = 0; i < used.length; i++)
        {
            if (used[i])
            {
                array[j++] = keys[i];
            }
        }
        return array;
    }

    public Object[] getAllValues()
    {
        Object[] array = new Object[size];
        int index = 0;
        for (int ii = 0; ii < used.length; ii++)
        {
            if (used[ii])
                array[index++] = values[ii];
        }
        return array;
    }

    @SuppressWarnings("unchecked")
    public <T> T[] getAllValues(T[] a)
    {
        if (a.length < size)
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);

        int index = 0;
        for (int ii = 0; ii < used.length; ii++)
        {
            if (used[ii])
                a[index++] = (T) values[ii];
        }

        if (a.length > size)
            a[size] = null;
        return a;
    }

    public int size()
    {
        return size;
    }

    public boolean isEmpty()
    {
        return size() == 0;
    }

    public void clear()
    {
        size = 0;
        used = new boolean[capacity];
    }

    public IteratorLong keys()
    {
        return new IteratorLong()
        {
            int n = 0;
            int i = -1;

            public boolean hasNext()
            {
                return n < size;
            }

            public long next() throws NoSuchElementException
            {
                while (++i < used.length)
                {
                    if (used[i])
                    {
                        n++;
                        return keys[i];
                    }
                }
                throw new NoSuchElementException();
            }
        };
    }

    public Iterator<E> values()
    {
        return new Iterator<E>()
        {
            int n = 0;
            int i = -1;

            public boolean hasNext()
            {
                return n < size;
            }

            public E next() throws NoSuchElementException
            {
                while (++i < used.length)
                {
                    if (used[i])
                    {
                        n++;
                        return values[i];
                    }
                }
                throw new NoSuchElementException();
            }

            public void remove() throws UnsupportedOperationException
            {
                throw new UnsupportedOperationException();
            }
        };
    }

    public Iterator<Entry<E>> entries()
    {
        return new Iterator<Entry<E>>()
        {
            int n = 0;
            int i = -1;

            public boolean hasNext()
            {
                return n < size;
            }

            public Entry<E> next() throws NoSuchElementException
            {
                while (++i < used.length)
                {
                    if (used[i])
                    {
                        n++;
                        return new Entry<E>()
                        {
                            public long getKey()
                            {
                                return keys[i];
                            }

                            public E getValue()
                            {
                                return values[i];
                            }
                        };
                    }
                }
                throw new NoSuchElementException();
            }

            public void remove() throws UnsupportedOperationException
            {
                throw new UnsupportedOperationException();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private void init(int initialCapacity)
    {
        capacity = PrimeFinder.findNextPrime(initialCapacity);
        step = Math.max(1, PrimeFinder.findPrevPrime(initialCapacity / 3));
        limit = (int) (capacity * 0.75);
        clear();
        keys = new long[capacity];
        // This cast is ok as long as nobody assigns the field values to a field
        // of type <E>[] (values is of type Object[] and an assignment would
        // lead to a ClassCastException). This cast here is performed to extract
        // the array elements later without additional casts in the other calls.
        values = (E[]) new Object[capacity];
    }

    private void resize(int newCapacity)
    {
        int oldSize = size;
        boolean[] oldUsed = used;
        long[] oldKeys = keys;
        E[] oldValues = values;
        init(newCapacity);
        long key;
        int hash;
        for (int i = 0; i < oldUsed.length; i++)
        {
            if (oldUsed[i])
            {
                key = oldKeys[i];
                hash = hash(key) % capacity;
                while (used[hash])
                {
                    hash = (hash + step) % capacity;
                }
                used[hash] = true;
                keys[hash] = key;
                values[hash] = oldValues[i];
            }
        }
        size = oldSize;
    }
}
