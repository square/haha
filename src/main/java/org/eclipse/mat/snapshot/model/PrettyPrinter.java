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
package org.eclipse.mat.snapshot.model;

import org.eclipse.mat.SnapshotException;

/**
 * Utility class to extract String representations of heap dump objects.
 */
public final class PrettyPrinter
{
    /**
     * Convert a <code>java.lang.String</code> object into a String.
     */
    public static String objectAsString(IObject stringObject, int limit) throws SnapshotException
    {
        Integer count = (Integer) stringObject.resolveValue("count"); //$NON-NLS-1$
        if (count == null)
            return null;
        if (count.intValue() == 0)
            return ""; //$NON-NLS-1$

        IPrimitiveArray charArray = (IPrimitiveArray) stringObject.resolveValue("value"); //$NON-NLS-1$
        if (charArray == null)
            return null;

        Integer offset = (Integer) stringObject.resolveValue("offset"); //$NON-NLS-1$
        if (offset == null)
            return null;

        return arrayAsString(charArray, offset, count, limit);
    }

    /**
     * Convert a <code>char[]</code> object into a String.
     */
    public static String arrayAsString(IPrimitiveArray charArray, int offset, int count, int limit)
    {
        if (charArray.getType() != IObject.Type.CHAR)
            return null;

        int length = charArray.getLength();

        int contentToRead = count <= limit ? count : limit;
        if (contentToRead > length - offset)
            contentToRead = length - offset;

        char[] value;
        if (offset == 0 && length == contentToRead)
            value = (char[]) charArray.getValueArray();
        else
            value = (char[]) charArray.getValueArray(offset, contentToRead);

        if (value == null)
            return null;

        StringBuilder result = new StringBuilder(value.length);
        for (int ii = 0; ii < value.length; ii++)
        {
            char val = value[ii];
            if (val >= 32 && val < 127)
                result.append(val);
            else
                result.append("\\u").append(String.format("%04x", 0xFFFF & val)); //$NON-NLS-1$//$NON-NLS-2$
        }
        if (limit < count)
            result.append("..."); //$NON-NLS-1$
        return result.toString();
    }

    private PrettyPrinter()
    {}
}
