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
package org.eclipse.mat.parser.internal.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.dynamichelpers.IExtensionTracker;
import org.eclipse.mat.parser.internal.Messages;
import org.eclipse.mat.parser.internal.ParserPlugin;
import org.eclipse.mat.snapshot.SnapshotFormat;
import org.eclipse.mat.util.MessageUtil;
import org.eclipse.mat.util.RegistryReader;
import org.eclipse.mat.util.SimpleStringTokenizer;

public class ParserRegistry extends RegistryReader<ParserRegistry.Parser>
{
    private static final String ID = "id";//$NON-NLS-1$
    private static final String FILE_EXTENSION = "fileExtension";//$NON-NLS-1$
    private static final String NAME = "name";//$NON-NLS-1$
    private static final String DYNAMIC = "dynamic";//$NON-NLS-1$
    public static final String INDEX_BUILDER = "indexBuilder";//$NON-NLS-1$
    public static final String OBJECT_READER = "objectReader";//$NON-NLS-1$

    public class Parser
    {
        private String id;
        private IConfigurationElement configElement;
        private SnapshotFormat snapshotFormat;
        private Pattern[] pattern;

        private Parser(IConfigurationElement configElement, String id, SnapshotFormat snapshotFormat, Pattern[] pattern)
        {
            this.id = id;
            this.configElement = configElement;
            this.snapshotFormat = snapshotFormat;
            this.pattern = pattern;
        }

        private Parser(IConfigurationElement configElement, SnapshotFormat snapshotFormat, Pattern[] pattern)
        {
            this(configElement, configElement.getDeclaringExtension().getSimpleIdentifier(), snapshotFormat, pattern);
        }

        public String getId()
        {
            return id;
        }

        public String getUniqueIdentifier()
        {
            IExtension extension = configElement.getDeclaringExtension();
            return extension.getNamespaceIdentifier()+"."+id;//$NON-NLS-1$
        }

        public SnapshotFormat getSnapshotFormat()
        {
            return snapshotFormat;
        }

        @SuppressWarnings("unchecked")
        public <I> I create(Class<I> type, String attribute)
        {
            try
            {
                return (I) configElement.createExecutableExtension(attribute);
            }
            catch (CoreException e)
            {
                Logger.getLogger(getClass().getName()).log(
                                Level.SEVERE,
                                MessageUtil.format(Messages.ParserRegistry_ErrorWhileCreating, type.getSimpleName(),
                                                attribute), e);
                return null;
            }
        }
    }
    
    /**
     * This is not a real parser - but a place holder. When a real parser is required
     * then this is used to find a list of parsers dynamically.
     */
    public class DynamicParser extends Parser
    {
        public DynamicParser(IConfigurationElement configElement, SnapshotFormat snapshotFormat, Pattern[] pattern)
        {
            super(configElement, snapshotFormat, pattern);
        }
    }

    public ParserRegistry(IExtensionTracker tracker)
    {
        init(tracker, ParserPlugin.PLUGIN_ID + ".parser");//$NON-NLS-1$
    }

    @Override
    public Parser createDelegate(IConfigurationElement configElement)
    {
        String dynamic = configElement.getAttribute(DYNAMIC);
        String fileExtensions = configElement.getAttribute(FILE_EXTENSION);
        if (fileExtensions == null || fileExtensions.length() == 0)
            if (dynamic == null)
                return null;

        try
        {
            String[] extensions = SimpleStringTokenizer.split(fileExtensions, ',');
            Pattern[] patterns = new Pattern[extensions.length];
            for (int ii = 0; ii < extensions.length; ii++)
                patterns[ii] = Pattern.compile("(.*\\.)((?i)" + extensions[ii] + ")(\\.[0-9]*)?");//$NON-NLS-1$//$NON-NLS-2$

            SnapshotFormat snapshotFormat = new SnapshotFormat(configElement.getAttribute(NAME), extensions);
            if (dynamic != null)
                return new DynamicParser(configElement, snapshotFormat, patterns);
            else
                return new Parser(configElement, snapshotFormat, patterns);
        }
        catch (PatternSyntaxException e)
        {
            Logger.getLogger(getClass().getName()).log(
                            Level.SEVERE,
                            MessageUtil.format(Messages.ParserRegistry_ErrorCompilingFileNamePattern, configElement
                                            .getNamespaceIdentifier()), e);
            return null;
        }
    }

    @Override
    protected void removeDelegate(Parser delegate)
    {}

    public Parser lookupParser(String uniqueIdentifier)
    {
        for (Parser p : delegates())
            if (uniqueIdentifier.equals(p.getUniqueIdentifier()))
                return p;
        return null;
    }

    public List<Parser> matchParser(String fileName)
    {
        List<Parser> answer = new ArrayList<Parser>();
        for (Parser p : delegates())
        {
            for (Pattern regex : p.pattern)
            {
                if (regex.matcher(fileName).matches())
                {
                    answer.add(p);
                    // Only need to add a parser once
                    break;
                }
            }
        }
        return answer;
    }
    
    @Override
    public Collection<Parser> delegates()
    {
        Collection<Parser> res = super.delegates();
        Collection<Parser> res2 = new HashSet<Parser>();
        boolean foundDynamic = false;
        for (Parser p : res)
        {
            if (p instanceof DynamicParser)
            {
                foundDynamic = true;
                @SuppressWarnings("unchecked")
                Map<String, Map<String, String>> mp = (Map<String, Map<String, String>>)p.create(Map.class, DYNAMIC);
                if (mp != null)
                {
                    for (Map<String, String> m : mp.values())
                    {
                        String id = m.get(ID);
                        String name = m.get(NAME);
                        String fileExtensions = m.get(FILE_EXTENSION);
                        if (fileExtensions == null || fileExtensions.length() == 0)
                        {

                        }
                        else
                        {
                            try
                            {
                                String[] extensions = SimpleStringTokenizer.split(fileExtensions, ',');
                                Pattern[] patterns = new Pattern[extensions.length];
                                for (int ii = 0; ii < extensions.length; ii++)
                                    patterns[ii] = Pattern.compile("(.*\\.)((?i)" + extensions[ii] + ")(\\.[0-9]*)?");//$NON-NLS-1$//$NON-NLS-2$

                                SnapshotFormat snapshotFormat = new SnapshotFormat(name, extensions);
                                res2.add(new Parser(p.configElement, id, snapshotFormat, patterns));
                            }
                            catch (PatternSyntaxException e)
                            {
                                Logger.getLogger(getClass().getName()).log(
                                                Level.SEVERE,
                                                MessageUtil.format(
                                                                Messages.ParserRegistry_ErrorCompilingFileNamePattern,
                                                                p.configElement.getNamespaceIdentifier()), e);
                            }
                        }
                    }
                }
            }
            else
            {
                res2.add(p);
            }
        }
        return foundDynamic ? Collections.unmodifiableCollection(res2) : res;
    }

}
