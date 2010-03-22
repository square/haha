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

/**
 * Marker interface to represent object arrays in the heap dump.
 * 
 * @noimplement
 */
public interface IObjectArray extends IArray
{
    /**
     * Get an array with the object addresses. 0 indicate <code>null</code>
     * values in the array.
     */
    long[] getReferenceArray();

    /**
     * Get an array with the object addresses, beginning at <code>offset</code>
     * and <code>length</code> number of elements.
     */
    long[] getReferenceArray(int offset, int length);

}
