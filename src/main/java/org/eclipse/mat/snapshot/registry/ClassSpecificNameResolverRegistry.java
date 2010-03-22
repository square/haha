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
package org.eclipse.mat.snapshot.registry;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.internal.MATPlugin;
import org.eclipse.mat.internal.Messages;
import org.eclipse.mat.snapshot.extension.IClassSpecificNameResolver;
import org.eclipse.mat.snapshot.extension.Subject;
import org.eclipse.mat.snapshot.extension.Subjects;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.util.MessageUtil;
import org.eclipse.mat.util.RegistryReader;

/**
 * Registry for name resolvers which resolve the names for objects of specific
 * classes (found in an snapshot), e.g. String (where the char[] is evaluated)
 * or a specific class loader (where the appropriate field holding its name and
 * thereby deployment unit is evaluated).
 */
public final class ClassSpecificNameResolverRegistry
{
    // //////////////////////////////////////////////////////////////
    // Singleton
    // //////////////////////////////////////////////////////////////

    /** inner class because RegitryReader API not public */
    private static class RegistryImpl extends RegistryReader<IClassSpecificNameResolver>
    {
        private Map<String, IClassSpecificNameResolver> resolvers;

        public RegistryImpl()
        {
            resolvers = new HashMap<String, IClassSpecificNameResolver>();
            init(MATPlugin.getDefault().getExtensionTracker(), MATPlugin.PLUGIN_ID + ".nameResolver"); //$NON-NLS-1$
        }

        @Override
        public IClassSpecificNameResolver createDelegate(IConfigurationElement configElement)
        {
            try
            {
                IClassSpecificNameResolver resolver = (IClassSpecificNameResolver) configElement
                                .createExecutableExtension("impl"); //$NON-NLS-1$

                String[] subjects = extractSubjects(resolver);
                if (subjects != null && subjects.length > 0)
                {
                    for (int ii = 0; ii < subjects.length; ii++)
                        resolvers.put(subjects[ii], resolver);
                }
                else
                {
                    String msg = MessageUtil.format(Messages.ClassSpecificNameResolverRegistry_ErrorMsg_MissingSubject,
                                    resolver.getClass().getName());
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, msg);
                }

                return resolver;
            }
            catch (CoreException e)
            {
                String msg = MessageUtil.format(
                                Messages.ClassSpecificNameResolverRegistry_ErrorMsg_WhileCreatingResolver,
                                configElement.getAttribute("impl")); //$NON-NLS-1$
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, msg, e);
                return null;

            }
        }

        @Override
        protected void removeDelegate(IClassSpecificNameResolver delegate)
        {
            for (Iterator<IClassSpecificNameResolver> iter = resolvers.values().iterator(); iter.hasNext();)
            {
                IClassSpecificNameResolver r = iter.next();
                if (r == delegate)
                    iter.remove();
            }
        }

        private String doResolve(IObject object)
        {
            try
            {
                IClass clazz = object.getClazz();
                while (clazz != null)
                {
                    IClassSpecificNameResolver resolver = resolvers.get(clazz.getName());
                    if (resolver != null) { return resolver.resolve(object); }
                    clazz = clazz.getSuperClass();
                }
                return null;
            }
            catch (RuntimeException e)
            {
                Logger.getLogger(ClassSpecificNameResolverRegistry.class.getName()).log(
                                Level.SEVERE,
                                MessageUtil.format(Messages.ClassSpecificNameResolverRegistry_ErrorMsg_DuringResolving,
                                                object.getTechnicalName()), e);
                return null;
            }
            catch (SnapshotException e)
            {
                Logger.getLogger(ClassSpecificNameResolverRegistry.class.getName()).log(
                                Level.SEVERE,
                                MessageUtil.format(Messages.ClassSpecificNameResolverRegistry_ErrorMsg_DuringResolving,
                                                object.getTechnicalName()), e);
                return null;
            }
        }

        private String[] extractSubjects(IClassSpecificNameResolver instance)
        {
            Subjects subjects = instance.getClass().getAnnotation(Subjects.class);
            if (subjects != null)
                return subjects.value();

            Subject subject = instance.getClass().getAnnotation(Subject.class);
            return subject != null ? new String[] { subject.value() } : null;
        }

    }

    private static ClassSpecificNameResolverRegistry instance = new ClassSpecificNameResolverRegistry();

    public static ClassSpecificNameResolverRegistry instance()
    {
        return instance;
    }

    // //////////////////////////////////////////////////////////////
    // registry methods
    // //////////////////////////////////////////////////////////////

    private RegistryImpl registry;

    private ClassSpecificNameResolverRegistry()
    {
        registry = new RegistryImpl();
    }

    /**
     * Register class specific name resolver.
     * 
     * @param className
     *            class name for which the class specific name resolver should
     *            be used
     * @param resolver
     *            class specific name resolver
     * @deprecated Use default extension mechanism: just implement interface and
     *             register location via UI
     */
    @Deprecated
    public static void registerResolver(String className, IClassSpecificNameResolver resolver)
    {
        instance().registry.resolvers.put(className, resolver);
    }

    /**
     * Resolve name of the given snapshot object or return null if it can't be
     * resolved.
     * 
     * @param object
     *            snapshot object for which the name should be resolved
     * @return name of the given snapshot object or null if it can't be resolved
     */
    public static String resolve(IObject object)
    {
        if (object == null)
            throw new NullPointerException(Messages.ClassSpecificNameResolverRegistry_Error_MissingObject);

        return instance().registry.doResolve(object);
    }

}
