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
package org.eclipse.mat.snapshot;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class ExcludedReferencesDescriptor
{
    private int[] objectIds;
    private Set<String> fields;

    public ExcludedReferencesDescriptor(int[] objectIds, Set<String> fields)
    {
        this.fields = fields;
        this.objectIds = objectIds;
        Arrays.sort(this.objectIds);
    }

    public ExcludedReferencesDescriptor(int[] objectIds, String... fields)
    {
        this(objectIds, new HashSet<String>(Arrays.asList(fields)));
    }

    public Set<String> getFields()
    {
        return fields;
    }

    public boolean contains(int objectId)
    {
        return Arrays.binarySearch(objectIds, objectId) >= 0;
    }

    public int[] getObjectIds()
    {
        return objectIds;
    }
}
