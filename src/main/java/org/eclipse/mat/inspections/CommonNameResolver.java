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
package org.eclipse.mat.inspections;

import java.lang.reflect.Modifier;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.extension.IClassSpecificNameResolver;
import org.eclipse.mat.snapshot.extension.Subject;
import org.eclipse.mat.snapshot.extension.Subjects;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.snapshot.model.IObjectArray;
import org.eclipse.mat.snapshot.model.IPrimitiveArray;
import org.eclipse.mat.snapshot.model.PrettyPrinter;

public class CommonNameResolver
{
    @Subject("java.lang.String")
    public static class StringResolver implements IClassSpecificNameResolver
    {
        public String resolve(IObject obj) throws SnapshotException
        {
            return PrettyPrinter.objectAsString(obj, 1024);
        }
    }

    @Subjects( { "java.lang.StringBuffer", //
                    "java.lang.StringBuilder" })
    public static class StringBufferResolver implements IClassSpecificNameResolver
    {
        public String resolve(IObject obj) throws SnapshotException
        {
            Integer count = (Integer) obj.resolveValue("count"); //$NON-NLS-1$
            if (count == null)
                return null;
            if (count == 0)
                return ""; //$NON-NLS-1$

            IPrimitiveArray charArray = (IPrimitiveArray) obj.resolveValue("value"); //$NON-NLS-1$
            if (charArray == null)
                return null;

            return PrettyPrinter.arrayAsString(charArray, 0, count, 1024);
        }
    }

    @Subject("java.lang.Thread")
    public static class ThreadResolver implements IClassSpecificNameResolver
    {
        public String resolve(IObject obj) throws SnapshotException
        {
            IObject name = (IObject) obj.resolveValue("name"); //$NON-NLS-1$
            return name != null ? name.getClassSpecificName() : null;
        }
    }

    @Subject("java.lang.ThreadGroup")
    public static class ThreadGroupResolver implements IClassSpecificNameResolver
    {
        public String resolve(IObject object) throws SnapshotException
        {
            IObject nameString = (IObject) object.resolveValue("name"); //$NON-NLS-1$
            if (nameString == null)
                return null;
            return nameString.getClassSpecificName();
        }
    }

    @Subjects( { "java.lang.Byte", //
                    "java.lang.Character", //
                    "java.lang.Short", //
                    "java.lang.Integer", //
                    "java.lang.Long", //
                    "java.lang.Float", //
                    "java.lang.Double", //
                    "java.lang.Boolean" })
    public static class ValueResolver implements IClassSpecificNameResolver
    {
        public String resolve(IObject heapObject) throws SnapshotException
        {
            return String.valueOf(heapObject.resolveValue("value")); //$NON-NLS-1$
        }
    }

    @Subject("char[]")
    public static class CharArrayResolver implements IClassSpecificNameResolver
    {
        public String resolve(IObject heapObject) throws SnapshotException
        {
            IPrimitiveArray charArray = (IPrimitiveArray) heapObject;
            return PrettyPrinter.arrayAsString(charArray, 0, charArray.getLength(), 1024);
        }
    }

    @Subject("byte[]")
    public static class ByteArrayResolver implements IClassSpecificNameResolver
    {
        public String resolve(IObject heapObject) throws SnapshotException
        {
            IPrimitiveArray arr = (IPrimitiveArray) heapObject;
            byte[] value = (byte[]) arr.getValueArray(0, Math.min(arr.getLength(), 1024));
            if (value == null)
                return null;

            // must not modify the original byte array
            StringBuilder r = new StringBuilder(value.length);
            for (int i = 0; i < value.length; i++)
            {
                // ASCII/Unicode 127 is not printable
                if (value[i] < 32 || value[i] > 126)
                    r.append('.');
                else
                    r.append((char) value[i]);
            }
            return r.toString();
        }
    }
    
    /*
     * Contributed in bug 273915
     */
    @Subject("java.net.URL")
    public static class URLResolver implements IClassSpecificNameResolver
    {
        public String resolve(IObject obj) throws SnapshotException
        {
            StringBuilder builder = new StringBuilder();
            IObject protocol = (IObject) obj.resolveValue("protocol"); //$NON-NLS-1$
            builder.append(protocol.getClassSpecificName());
            builder.append(":"); //$NON-NLS-1$
            IObject authority = (IObject) obj.resolveValue("authority"); //$NON-NLS-1$
            if(authority != null) {
                builder.append("//"); //$NON-NLS-1$
                builder.append(authority.getClassSpecificName());
            }
            IObject path = (IObject) obj.resolveValue("path"); //$NON-NLS-1$
            if(path != null) builder.append(path.getClassSpecificName());
            IObject query = (IObject) obj.resolveValue("query"); //$NON-NLS-1$
            if(query != null) {
                builder.append("?"); //$NON-NLS-1$
                builder.append(query.getClassSpecificName());
            }
            IObject ref = (IObject) obj.resolveValue("ref"); //$NON-NLS-1$
            if(ref != null) {
                builder.append("#"); //$NON-NLS-1$
                builder.append(ref.getClassSpecificName());
            }
            return builder.toString();
        }
    }
    
    @Subject("java.lang.reflect.AccessibleObject")
    public static class AccessibleObjectResolver implements IClassSpecificNameResolver
    {
        public String resolve(IObject obj) throws SnapshotException
        {
            // Important fields
            // modifiers - not actually present, but present in all superclasses
            // clazz - not actually present, but present in all superclasses
            StringBuilder r = new StringBuilder();
            ISnapshot snapshot = obj.getSnapshot();
            IObject ref;
            Object val = obj.resolveValue("modifiers"); //$NON-NLS-1$
            if (val instanceof Integer)
            {
                r.append(Modifier.toString((Integer)val));
                if (r.length() > 0) r.append(' ');
            }
            ref = (IObject) obj.resolveValue("clazz"); //$NON-NLS-1$
            if (ref != null)
            {
                addClassName(snapshot, ref.getObjectAddress(), r);
            }
            else
            {
                return null;
            }
            return r.toString();
        }

        protected void addClassName(ISnapshot snapshot, long addr, StringBuilder r) throws SnapshotException
        {
            int id = snapshot.mapAddressToId(addr);
            IObject ox = snapshot.getObject(id);
            if (ox instanceof IClass)
            {
                IClass cls = (IClass) ox;
                r.append(cls.getName());
            }
        }
    }
    
    @Subject("java.lang.reflect.Field")
    public static class FieldResolver extends AccessibleObjectResolver
    {
        public String resolve(IObject obj) throws SnapshotException
        {
            // Important fields
            // modifiers
            // clazz
            // name
            // type
            StringBuilder r = new StringBuilder();
            ISnapshot snapshot = obj.getSnapshot();
            IObject ref;
            Object val = obj.resolveValue("modifiers"); //$NON-NLS-1$
            if (val instanceof Integer)
            {
                r.append(Modifier.toString((Integer)val));
                if (r.length() > 0) r.append(' ');
            }
            ref = (IObject) obj.resolveValue("type"); //$NON-NLS-1$
            if (ref != null)
            {
                addClassName(snapshot, ref.getObjectAddress(), r);
                r.append(' ');
            }
            ref = (IObject) obj.resolveValue("clazz"); //$NON-NLS-1$
            if (ref != null)
            {
                addClassName(snapshot, ref.getObjectAddress(), r);
                r.append('.');
            }
            ref = (IObject) obj.resolveValue("name"); //$NON-NLS-1$
            if (ref != null)
            {
                r.append(ref.getClassSpecificName());
            }
            else
            {
                // No method name so give up
                return null;
            }
            return r.toString();
        }
    }
    
    @Subject("java.lang.reflect.Method")
    public static class MethodResolver extends AccessibleObjectResolver
    {
        public String resolve(IObject obj) throws SnapshotException
        {
            // Important fields
            // modifiers
            // clazz
            // name
            // parameterTypes[]
            // exceptionTypes[]
            // returnType
            StringBuilder r = new StringBuilder();
            ISnapshot snapshot = obj.getSnapshot();
            IObject ref;
            Object val = obj.resolveValue("modifiers"); //$NON-NLS-1$
            if (val instanceof Integer)
            {
                r.append(Modifier.toString((Integer)val));
                if (r.length() > 0) r.append(' ');
            }
            ref = (IObject) obj.resolveValue("returnType"); //$NON-NLS-1$
            if (ref != null)
            {
                addClassName(snapshot, ref.getObjectAddress(), r);
                r.append(' ');
            }
            ref = (IObject) obj.resolveValue("clazz"); //$NON-NLS-1$
            if (ref != null)
            {
                addClassName(snapshot, ref.getObjectAddress(), r);
                r.append('.');
            }
            ref = (IObject) obj.resolveValue("name"); //$NON-NLS-1$
            if (ref != null)
            {
                r.append(ref.getClassSpecificName());
            }
            else
            {
                // No method name so give up
                return null;
            }
            r.append('(');
            ref = (IObject) obj.resolveValue("parameterTypes"); //$NON-NLS-1$
            if (ref instanceof IObjectArray)
            {
                IObjectArray orefa = (IObjectArray) ref;
                long refs[] = orefa.getReferenceArray();
                for (int i = 0; i < orefa.getLength(); ++i)
                {
                    if (i > 0)
                        r.append(',');
                    long addr = refs[i];
                    addClassName(snapshot, addr, r);
                }
            }
            r.append(')');
            return r.toString();
        }
    }
    
    @Subject("java.lang.reflect.Constructor")
    public static class ConstructorResolver extends AccessibleObjectResolver
    {
        public String resolve(IObject obj) throws SnapshotException
        {
            // Important fields
            // modifiers
            // clazz
            // parameterTypes[]
            // exceptionTypes[]
            StringBuilder r = new StringBuilder();
            ISnapshot snapshot = obj.getSnapshot();
            IObject ref;
            Object val = obj.resolveValue("modifiers"); //$NON-NLS-1$
            if (val instanceof Integer)
            {
                r.append(Modifier.toString((Integer)val));
                if (r.length() > 0) r.append(' ');
            }
            ref = (IObject) obj.resolveValue("clazz"); //$NON-NLS-1$
            if (ref != null)
            {
                addClassName(snapshot, ref.getObjectAddress(), r);
            }
            else
            {
                // No class name so give up
                return null;
            }
            r.append('(');
            ref = (IObject) obj.resolveValue("parameterTypes"); //$NON-NLS-1$
            if (ref instanceof IObjectArray)
            {
                IObjectArray orefa = (IObjectArray) ref;
                long refs[] = orefa.getReferenceArray();
                for (int i = 0; i < orefa.getLength(); ++i)
                {
                    if (i > 0)
                        r.append(',');
                    long addr = refs[i];
                    addClassName(snapshot, addr, r);
                }
            }
            r.append(')');
            return r.toString();
        }
    }
}
