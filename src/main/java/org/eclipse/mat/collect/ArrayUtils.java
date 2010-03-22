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

public class ArrayUtils
{

    /**
     * Sorts the keys in an increasing order. Elements key[i] and values[i] are
     * always swapped together in the corresponding arrays.
     * <p>
     * A mixture of several sorting algorithms is used:
     * <p>
     * A radix sort performs better on the numeric data we sort, but requires
     * additional storage to perform the sorting. Therefore only the
     * not-very-large parts produced by a quick sort are sorted with radix sort.
     * An insertion sort is used to sort the smallest arrays, where the the
     * overhead of the radix sort is also bigger
     */
    public static void sort(int[] keys, int[] values)
    {
        hybridsort(keys, values, 0, keys.length - 1);
    }

    /**
     * Sorts the keys in an decreasing order. Elements key[i] and values[i] are
     * always swapped together in the corresponding arrays.
     * <p>
     * A mixture of several sorting algorithms is used:
     * <p>
     * A radix sort performs better on the numeric data we sort, but requires
     * additional storage to perform the sorting. Therefore only the
     * not-very-large parts produced by a quick sort are sorted with radix sort.
     * An insertion sort is used to sort the smallest arrays, where the the
     * overhead of the radix sort is also bigger
     */
    public static void sortDesc(long[] keys, int[] values)
    {
        hybridsortDesc(keys, values, null, null, 0, keys.length - 1);
    }

    /**
     * Sorts the keys in an decreasing order. Elements key[i] and values[i] are
     * always swapped together in the corresponding arrays.
     * <p>
     * A mixture of several sorting algorithms is used:
     * <p>
     * A radix sort performs better on the numeric data we sort, but requires
     * additional storage to perform the sorting. Therefore only the
     * not-very-large parts produced by a quick sort are sorted with radix sort.
     * An insertion sort is used to sort the smallest arrays, where the the
     * overhead of the radix sort is also bigger
     * <p>
     * This version of the method allows the temporarily needed arrays for the
     * radix sort to be provided externally - tempa and tempb. This saves
     * unnecessary array creation and cleanup
     */
    public static void sortDesc(long[] a, int[] b, long[] tmpa, int[] tmpb)
    {
        hybridsortDesc(a, b, tmpa, tmpb, 0, a.length - 1);
    }

    /**
     * Sorts a range from the keys in an increasing order. Elements key[i] and
     * values[i] are always swapped together in the corresponding arrays.
     * <p>
     * A mixture of several sorting algorithms is used:
     * <p>
     * A radix sort performs better on the numeric data we sort, but requires
     * additional storage to perform the sorting. Therefore only the
     * not-very-large parts produced by a quick sort are sorted with radix sort.
     * An insertion sort is used to sort the smallest arrays, where the the
     * overhead of the radix sort is also bigger
     */
    public static void sort(int[] keys, int[] values, int offset, int length)
    {
        hybridsort(keys, values, offset, offset + length - 1);
    }

    /*
     * PRIVATE METHODS IMPLEMENTING THE SORTING
     */

    private static void swap(int keys[], int values[], int a, int b)
    {
        // swap the keys
        int tmp = keys[a];
        keys[a] = keys[b];
        keys[b] = tmp;

        // swap the values
        tmp = values[a];
        values[a] = values[b];
        values[b] = tmp;
    }

    private static void swap(long keys[], int values[], int a, int b)
    {
        // swap the keys
        long tmpKey = keys[a];
        keys[a] = keys[b];
        keys[b] = tmpKey;

        // swap the values
        int tmpValue = values[a];
        values[a] = values[b];
        values[b] = tmpValue;
    }

    private static int median(int x[], int pos1, int pos2, int pos3)
    {
        int v1 = x[pos1];
        int v2 = x[pos2];
        int v3 = x[pos3];

        if (v1 < v2)
            if (v2 <= v3)
                return pos2;
            else
                return v1 < v3 ? pos3 : pos1;

        /* else -> v1 > v2 */
        if (v1 <= v3)
            return pos1;
        else
            return v2 < v3 ? pos3 : pos2;
    }

    private static int median(long x[], int pos1, int pos2, int pos3)
    {
        long v1 = x[pos1];
        long v2 = x[pos2];
        long v3 = x[pos3];

        if (v1 < v2)
            if (v2 <= v3)
                return pos2;
            else
                return v1 < v3 ? pos3 : pos1;

        /* else -> v1 > v2 */
        if (v1 <= v3)
            return pos1;
        else
            return v2 < v3 ? pos3 : pos2;
    }

    private static int[] split(int[] keys, int[] values, int left, int right)
    {
        // just take the median of the middle key and the two border keys
        int splittingIdx = median(keys, left, right, left + ((right - left) >> 1));
        int splittingValue = keys[splittingIdx];

        // move splitting element first
        swap(keys, values, left, splittingIdx);

        int i = left;
        int c = 0; // number of elements equal to splittingValue
        for (int j = left + 1; j <= right; j++)
        {
            if (keys[j] < splittingValue)
            {
                i++;
                swap(keys, values, i, j);

                // if there are duplicates, keep them next to each other
                if (c > 0)
                    swap(keys, values, i + c, j);
            }
            else if (keys[j] == splittingValue)
            {
                c++;
                swap(keys, values, i + c, j);
            }
        }
        swap(keys, values, left, i);

        return new int[] { i, i + c };
    }

    private static int[] splitDesc(long[] keys, int[] values, int left, int right)
    {
        // just take the median of the middle key and the two border keys
        int splittingIdx = median(keys, left, right, left + ((right - left) >> 1));
        long splittingValue = keys[splittingIdx];

        // move splitting element first
        swap(keys, values, left, splittingIdx);

        int i = left;
        int c = 0; // number of elements equal to splittingValue
        for (int j = left + 1; j <= right; j++)
        {
            if (keys[j] > splittingValue)
            {
                i++;
                swap(keys, values, i, j);

                // if there are duplicates, keep them next to each other
                if (c > 0)
                    swap(keys, values, i + c, j);
            }
            else if (keys[j] == splittingValue)
            {
                c++;
                swap(keys, values, i + c, j);
            }
        }
        swap(keys, values, left, i);

        return new int[] { i, i + c };
    }

    private static void hybridsort(int[] keys, int[] values, int left, int right)
    {
        while (right - left >= 1)
        {
            if (right - left < 5000000)
            {
                radixsort(keys, values, left, right - left + 1);
                break;
            }
            else
            {
                // split the array - the elements between i[0] and i[1] are
                // equal.
                // the elements on the left are smaller, on the right - bigger
                int[] i = split(keys, values, left, right);

                int sizeLeft = i[0] - left;
                int sizeRight = right - i[1];
                
                // Limit recursion depth by doing the smaller side first
                if (sizeLeft <= sizeRight)
                {
                    // sort all keys smaller than keys[i]
                    hybridsort(keys, values, left, i[0] - 1);
                    // then loop to do all keys bigger than keys[i]
                    left = i[1] + 1;
                }
                else
                {
                    // sort all keys bigger than keys[i]
                    hybridsort(keys, values, i[1] + 1, right);
                    // then loop to do all keys smaller than keys[i]
                    right = i[0] - 1;
                }
            }
        }
    }

    private static void hybridsortDesc(long[] keys, int[] values, long[] tmpKeys, int[] tmpValues, int left, int right)
    {
        while (right - left >= 1)
        {
            if (right - left < 5000000)
            {
                // use insert sort on the small ones
                // to avoid the loop in radix sort
                if (right - left < 12)
                {
                    for (int i = left; i <= right; i++)
                        for (int j = i; j > left && keys[j - 1] < keys[j]; j--)
                            swap(keys, values, j, j - 1);
                    return;
                }
                radixsortDesc(keys, values, tmpKeys, tmpValues, left, right - left + 1);
                break;
            }
            else
            {
                // split the array - the elements between i[0] and i[1] are
                // equal.
                // the elements on the left are bigger, on the right - smaller
                int[] i = splitDesc(keys, values, left, right);

                int sizeLeft = i[0] - left;
                int sizeRight = right - i[1];
                
                // Limit recursion depth by doing the smaller side first
                if (sizeLeft <= sizeRight)
                {
                    // sort all keys bigger than keys[i]
                    hybridsortDesc(keys, values, tmpKeys, tmpValues, left, i[0] - 1);
                    // then loop to sort all keys smaller than keys[i]
                    left = i[1] + 1;
                }
                else
                {
                    // sort all keys smaller than keys[i]
                    hybridsortDesc(keys, values, tmpKeys, tmpValues, i[1] + 1, right);
                    // then loop to sort all keys bigger than keys[i]
                    right = i[0] - 1;
                }
            }
        }
    }

    private static void radixsort(int[] keys, int[] values, int offset, int length)
    {
        int[] tempKeys = new int[length];
        int[] tempValues = new int[length];
        countsort(keys, tempKeys, values, tempValues, offset, 0, length, 0);
        countsort(tempKeys, keys, tempValues, values, 0, offset, length, 1);
        countsort(keys, tempKeys, values, tempValues, offset, 0, length, 2);
        countsort(tempKeys, keys, tempValues, values, 0, offset, length, 3);
    }

    private static void radixsortDesc(long[] keys, int[] values, long[] tempKeys, int[] tempValues, int offset,
                    int length)
    {
        if (tempKeys == null)
            tempKeys = new long[length];
        if (tempValues == null)
            tempValues = new int[length];
        countsortDesc(keys, tempKeys, values, tempValues, offset, 0, length, 0);
        countsortDesc(tempKeys, keys, tempValues, values, 0, offset, length, 1);
        countsortDesc(keys, tempKeys, values, tempValues, offset, 0, length, 2);
        countsortDesc(tempKeys, keys, tempValues, values, 0, offset, length, 3);
        countsortDesc(keys, tempKeys, values, tempValues, offset, 0, length, 4);
        countsortDesc(tempKeys, keys, tempValues, values, 0, offset, length, 5);
        countsortDesc(keys, tempKeys, values, tempValues, offset, 0, length, 6);
        countsortDesc(tempKeys, keys, tempValues, values, 0, offset, length, 7);
    }

    private static void countsort(int[] srcKeys, int[] destKeys, int[] srcValues, int[] destValues, int srcOffset,
                    int trgOffset, int length, int sortByte)
    {
        int[] count = new int[256];
        int[] index = new int[256];

        int shiftBits = 8 * sortByte;
        int srcEnd = srcOffset + length;

        for (int i = srcOffset; i < srcEnd; i++)
            count[((srcKeys[i] >> (shiftBits)) & 0xff)]++;

        if (sortByte == 3)
        {
            // Sign byte, so sort 128..255 0..127
            /* index[128] = 0 */
            for (int i = 129; i < 256; i++)
                index[i] = index[i - 1] + count[i - 1];
            index[0] = index[255] + count[255];
            for (int i = 1; i < 128; i++)
                index[i] = index[i - 1] + count[i - 1];
        }
        else
        {
            /* index[0] = 0 */
            for (int i = 1; i < 256; i++)
                index[i] = index[i - 1] + count[i - 1];
        }

        for (int i = srcOffset; i < srcEnd; i++)
        {
            int idx = ((srcKeys[i] >> (shiftBits)) & 0xff);
            destValues[trgOffset + index[idx]] = srcValues[i];
            destKeys[trgOffset + index[idx]++] = srcKeys[i];
        }
    }

    private static void countsortDesc(long[] srcKeys, long[] destKeys, int[] srcValues, int[] destValues,
                    int srcOffset, int trgOffset, int length, int sortByte)
    {
        int[] count = new int[256];
        int[] index = new int[256];

        int shiftBits = 8 * sortByte;
        int srcEnd = srcOffset + length;

        for (int i = srcOffset; i < srcEnd; i++)
            count[(int) ((srcKeys[i] >> (shiftBits)) & 0xff)]++;

        if (sortByte == 7)
        {
            // Sign byte, so sort 127..0 255..128
            /* index[127] = 0 */
            for (int i = 126; i >= 0; i--)
                index[i] = index[i + 1] + count[i + 1];
            index[255] = index[0] + count[0];
            for (int i = 254; i >= 128; i--)
                index[i] = index[i + 1] + count[i + 1];
        }
        else
        {
            /* index[255] = 0 */
            for (int i = 254; i >= 0; i--)
                index[i] = index[i + 1] + count[i + 1];
        }

        for (int i = srcOffset; i < srcEnd; i++)
        {
            int idx = (int) ((srcKeys[i] >> (shiftBits)) & 0xff);
            destValues[trgOffset + index[idx]] = srcValues[i];
            destKeys[trgOffset + index[idx]++] = srcKeys[i];
        }
    }
}
