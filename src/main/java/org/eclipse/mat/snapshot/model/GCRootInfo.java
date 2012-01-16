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

import java.io.Serializable;

import org.eclipse.mat.internal.Messages;
import org.eclipse.mat.util.MessageUtil;

/**
 * Describes a garbage collection root.
 */
abstract public class GCRootInfo implements Serializable
{
    private static final long serialVersionUID = 2L;

    /**
     * Reasons why an heap object is a garbage collection root.
     */
    public interface Type
    {
        int UNKNOWN = 1;
        /**
         * Class loaded by system class loader, e.g. java.lang.String
         */
        int SYSTEM_CLASS = 2;
        /**
         * Local variable in native code
         */
        int NATIVE_LOCAL = 4;
        /**
         * Global variable in native code
         */
        int NATIVE_STATIC = 8;
        /**
         * Started but not stopped threads
         */
        int THREAD_BLOCK = 16;
        /**
         * Everything you have called wait() or notify() on or you have
         * synchronized on
         */
        int BUSY_MONITOR = 32;
        /**
         * Local variable, i.e. method input parameters or locally created
         * objects of methods still on the stack of a thread
         */
        int JAVA_LOCAL = 64;
        /**
         * In or out parameters in native code; frequently seen as some methods
         * have native parts and the objects handled as method parameters become
         * GC roots, e.g. parameters used for file/network I/O methods or
         * reflection
         */
        int NATIVE_STACK = 128;
        int THREAD_OBJ = 256;
        /**
         * An object which is a queue awaiting its finalizer to be run
         */
        int FINALIZABLE = 512;
        /**
         * An object which has a finalize method, but has not been finalized and
         * is not yet on the finalizer queue
         */
        int UNFINALIZED = 1024;
        /**
         * An object which is unreachable from any other root, but has been 
         * marked as a root by MAT to retain objects which otherwise would not
         * be included in the analysis
         */
        int UNREACHABLE = 2048;
    }

    private final static String[] TYPE_STRING = new String[] {
        MessageUtil.format(Messages.GCRootInfo_Unkown), //
    	MessageUtil.format(Messages.GCRootInfo_SystemClass), //
    	MessageUtil.format(Messages.GCRootInfo_JNILocal), //
    	MessageUtil.format(Messages.GCRootInfo_JNIGlobal), //
    	MessageUtil.format(Messages.GCRootInfo_ThreadBlock), //
    	MessageUtil.format(Messages.GCRootInfo_BusyMonitor), //
    	MessageUtil.format(Messages.GCRootInfo_JavaLocal), //
    	MessageUtil.format(Messages.GCRootInfo_NativeStack), //
    	MessageUtil.format(Messages.GCRootInfo_Thread), //
    	MessageUtil.format(Messages.GCRootInfo_Finalizable), //
    	MessageUtil.format(Messages.GCRootInfo_Unfinalized),
    	MessageUtil.format(Messages.GCRootInfo_Unreachable) };

    protected int objectId;
    private long objectAddress;
    protected int contextId;
    private long contextAddress;
    private int type;

    public GCRootInfo(long objectAddress, long contextAddress, int type)
    {
        this.objectAddress = objectAddress;
        this.contextAddress = contextAddress;
        this.type = type;
    }

    public int getObjectId()
    {
        return objectId;
    }

    public long getObjectAddress()
    {
        return objectAddress;
    }

    public long getContextAddress()
    {
        return contextAddress;
    }

    public int getContextId()
    {
        return contextId;
    }

    public int getType()
    {
        return type;
    }

    public static String getTypeAsString(int type)
    {
        for (int i = 0; i < TYPE_STRING.length; i++)
            if (((1 << i) & type) != 0)
                return TYPE_STRING[i];

        return null;
    }

    public static String getTypeSetAsString(GCRootInfo[] roots)
    {
        int typeSet = 0;
        for (GCRootInfo info : roots)
        {
            typeSet |= info.getType();
        }

        StringBuilder buf = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < TYPE_STRING.length; i++)
        {
            if (((1 << i) & typeSet) != 0)
            {
                if (!first)
                {
                    buf.append(", "); //$NON-NLS-1$
                }
                else
                {
                    // Performance optimization - if there is only one bit set
                    // return the type string without building a new string.
                    if ((1 << i) == typeSet)
                    {
                        return TYPE_STRING[i];
                    }
                    first = false;
                }
                buf.append(TYPE_STRING[i]);
            }
        }
        return buf.toString();

    }

}
