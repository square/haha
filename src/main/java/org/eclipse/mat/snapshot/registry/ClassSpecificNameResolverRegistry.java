/**
 * ****************************************************************************
 * Copyright (c) 2008 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * SAP AG - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.mat.snapshot.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.hprof.Messages;
import org.eclipse.mat.snapshot.extension.IClassSpecificNameResolver;
import org.eclipse.mat.snapshot.extension.Subject;
import org.eclipse.mat.snapshot.extension.Subjects;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.util.MessageUtil;

/**
 * Registry for name resolvers which resolve the names for objects of specific
 * classes (found in an snapshot), e.g. String (where the char[] is evaluated)
 * or a specific class loader (where the appropriate field holding its name and
 * thereby deployment unit is evaluated).
 */
public final class ClassSpecificNameResolverRegistry {
  // //////////////////////////////////////////////////////////////
  // Singleton
  // //////////////////////////////////////////////////////////////

  /** inner class because RegitryReader API not public */
  private static class RegistryImpl {
    private Map<String, IClassSpecificNameResolver> resolvers;

    public RegistryImpl() {
      resolvers = new HashMap<String, IClassSpecificNameResolver>();
    }

    private String doResolve(IObject object) {
      try {
        IClass clazz = object.getClazz();
        while (clazz != null) {
          IClassSpecificNameResolver resolver = resolvers.get(clazz.getName());
          if (resolver != null) {
            return resolver.resolve(object);
          }
          clazz = clazz.getSuperClass();
        }
        return null;
      } catch (RuntimeException e) {
        Logger.getLogger(ClassSpecificNameResolverRegistry.class.getName())
            .log(Level.SEVERE, MessageUtil.format(
                Messages.ClassSpecificNameResolverRegistry_ErrorMsg_DuringResolving,
                object.getTechnicalName()), e);
        return null;
      } catch (SnapshotException e) {
        Logger.getLogger(ClassSpecificNameResolverRegistry.class.getName())
            .log(Level.SEVERE, MessageUtil.format(
                Messages.ClassSpecificNameResolverRegistry_ErrorMsg_DuringResolving,
                object.getTechnicalName()), e);
        return null;
      }
    }

    public void registerResolver(IClassSpecificNameResolver resolver) {
      String[] subjects = extractSubjects(resolver);
      if (subjects != null && subjects.length > 0) {
        for (int ii = 0; ii < subjects.length; ii++)
          instance().registry.resolvers.put(subjects[ii], resolver);
      }
    }

    private String[] extractSubjects(IClassSpecificNameResolver instance) {
      Subjects subjects = instance.getClass().getAnnotation(Subjects.class);
      if (subjects != null) return subjects.value();

      Subject subject = instance.getClass().getAnnotation(Subject.class);
      return subject != null ? new String[] { subject.value() } : null;
    }
  }

  private static ClassSpecificNameResolverRegistry instance =
      new ClassSpecificNameResolverRegistry();

  static {
    instance.registry.registerResolver(
        new org.eclipse.mat.inspections.CommonNameResolver.StringResolver());
    instance.registry.registerResolver(
        new org.eclipse.mat.inspections.CommonNameResolver.StringBufferResolver());
    instance.registry.registerResolver(
        new org.eclipse.mat.inspections.CommonNameResolver.ThreadResolver());
    instance.registry.registerResolver(
        new org.eclipse.mat.inspections.CommonNameResolver.ThreadGroupResolver());
    instance.registry.registerResolver(
        new org.eclipse.mat.inspections.CommonNameResolver.ValueResolver());
    instance.registry.registerResolver(
        new org.eclipse.mat.inspections.CommonNameResolver.CharArrayResolver());
    instance.registry.registerResolver(
        new org.eclipse.mat.inspections.CommonNameResolver.ByteArrayResolver());
    instance.registry.registerResolver(
        new org.eclipse.mat.inspections.CommonNameResolver.URLResolver());
    instance.registry.registerResolver(
        new org.eclipse.mat.inspections.CommonNameResolver.AccessibleObjectResolver());
    instance.registry.registerResolver(
        new org.eclipse.mat.inspections.CommonNameResolver.FieldResolver());
    instance.registry.registerResolver(
        new org.eclipse.mat.inspections.CommonNameResolver.MethodResolver());
    instance.registry.registerResolver(
        new org.eclipse.mat.inspections.CommonNameResolver.ConstructorResolver());
    //    	instance.registry.registerResolver(new org.eclipse.mat.inspections.eclipse.EclipseNameResolver.EclipseClassLoaderResolver());
    //    	instance.registry.registerResolver(new org.eclipse.mat.inspections.eclipse.EclipseNameResolver.EclipseDefaultClassLoaderResolver());
    //    	instance.registry.registerResolver(new org.eclipse.mat.inspections.eclipse.EclipseNameResolver.StartupClassLoaderResolver());
    //    	instance.registry.registerResolver(new org.eclipse.mat.inspections.eclipse.EclipseNameResolver.RGBResolver());
    //    	instance.registry.registerResolver(new org.eclipse.mat.inspections.eclipse.EclipseNameResolver.PointResolver());
    //    	instance.registry.registerResolver(new org.eclipse.mat.inspections.eclipse.EclipseNameResolver.RectangleResolver());
    //    	instance.registry.registerResolver(new org.eclipse.mat.inspections.jetty.JettyNameResolvers.WebAppClassLoaderResolver());
    //    	instance.registry.registerResolver(new org.eclipse.mat.inspections.jetty.JettyNameResolvers.JasperLoaderResolver());
  }

  public static ClassSpecificNameResolverRegistry instance() {
    return instance;
  }

  // //////////////////////////////////////////////////////////////
  // registry methods
  // //////////////////////////////////////////////////////////////

  private RegistryImpl registry;

  private ClassSpecificNameResolverRegistry() {
    registry = new RegistryImpl();
  }

  /**
   * Resolve name of the given snapshot object or return null if it can't be
   * resolved.
   *
   * @param object snapshot object for which the name should be resolved
   * @return name of the given snapshot object or null if it can't be resolved
   */
  public static String resolve(IObject object) {
    if (object == null) {
      throw new NullPointerException(
          Messages.ClassSpecificNameResolverRegistry_Error_MissingObject.pattern);
    }

    return instance().registry.doResolve(object);
  }
}
