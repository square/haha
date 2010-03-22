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
package org.eclipse.mat.parser.model;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.collect.ArrayLong;
import org.eclipse.mat.snapshot.model.Field;
import org.eclipse.mat.snapshot.model.IPrimitiveArray;
import org.eclipse.mat.snapshot.model.NamedReference;
import org.eclipse.mat.snapshot.model.PseudoReference;

/**
 * @noextend
 */
public class PrimitiveArrayImpl extends AbstractArrayImpl implements IPrimitiveArray
{
    private static final long serialVersionUID = 2L;

    private int type;

    public PrimitiveArrayImpl(int objectId, long address, ClassImpl classInstance, int length, int type)
    {
        super(objectId, address, classInstance, length);
        this.type = type;
    }

    public int getType()
    {
        return type;
    }

    public Class<?> getComponentType()
    {
        return COMPONENT_TYPE[type];
    }

    public Object getValueAt(int index)
    {
        Object data = getValueArray(index, 1);
        return data != null ? Array.get(data, 0) : null;
    }

    public Object getValueArray()
    {
        try
        {
            return source.getHeapObjectReader().readPrimitiveArrayContent(this, 0, getLength());
        }
        catch (SnapshotException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public Object getValueArray(int offset, int length)
    {
        try
        {
            return source.getHeapObjectReader().readPrimitiveArrayContent(this, offset, length);
        }
        catch (SnapshotException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    protected Field internalGetField(String name)
    {
        return null;
    }

    @Override
    public ArrayLong getReferences()
    {
        ArrayLong references = new ArrayLong(1);
        references.add(classInstance.getObjectAddress());
        return references;
    }

    public List<NamedReference> getOutboundReferences()
    {
        List<NamedReference> references = new ArrayList<NamedReference>(1);
        references.add(new PseudoReference(source, classInstance.getObjectAddress(), "<class>"));//$NON-NLS-1$
        return references;
    }

    @Override
    protected StringBuffer appendFields(StringBuffer buf)
    {
        return super.appendFields(buf).append(";size=").append(getUsedHeapSize());//$NON-NLS-1$
    }

    @Override
    public int getUsedHeapSize()
    {
        try {
            return getSnapshot().getHeapSize(getObjectId());
        } catch (SnapshotException e) {
            return doGetUsedHeapSize(classInstance, length, type);
        }
    }

    public static int doGetUsedHeapSize(ClassImpl clazz, int length, int type)
    {
        return alignUpTo8(2 * clazz.getHeapSizePerInstance() + 4 + length * ELEMENT_SIZE[type]);
    }

}
