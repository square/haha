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
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.mat.parser.IIndexBuilder;
import org.eclipse.mat.parser.IObjectReader;
import org.eclipse.mat.snapshot.SnapshotFormat;

public class ParserRegistry 
{
    public static final String INDEX_BUILDER = "indexBuilder";
    public static final String OBJECT_READER = "objectReader";

    public List<Parser> parsers = new ArrayList<Parser>();
    
    private static ParserRegistry instance = new ParserRegistry();
    
    static {
    	addParser("hprof", "hprof", new String[] {"hprof", "bin"}, new org.eclipse.mat.hprof.HprofHeapObjectReader(), new org.eclipse.mat.hprof.HprofIndexBuilder());
    }
    
    public static class Parser
    {
    	private IObjectReader objectReader;
    	private IIndexBuilder indexBuilder;
    	
        private String id;
        private SnapshotFormat snapshotFormat;
        private Pattern[] patterns;

        public Parser(String id, SnapshotFormat snapshotFormat, IObjectReader objectReader, IIndexBuilder indexBuilder)
        {
            this.id = id;
            this.snapshotFormat = snapshotFormat;
            
            this.patterns = new Pattern[snapshotFormat.getFileExtensions().length];
            for (int ii = 0; ii < snapshotFormat.getFileExtensions().length; ii++) {
                patterns[ii] = Pattern.compile("(.*\\.)((?i)" + snapshotFormat.getFileExtensions()[ii] + ")(\\.[0-9]*)?");
            }
            this.objectReader = objectReader;
            this.indexBuilder = indexBuilder;
        }

        public IObjectReader getObjectReader() {
        	return this.objectReader;
        }
        
        public IIndexBuilder getIndexBuilder() {
        	return this.indexBuilder;
        }
        
        public String getId()
        {
            return id;
        }

        public String getUniqueIdentifier()
        {
            return id;
        }

        public SnapshotFormat getSnapshotFormat()
        {
            return snapshotFormat;
        }
    }
    
    private ParserRegistry() {
    	
    }

    public static void addParser(String id, String snapshotFormat, String[] extensions, IObjectReader objectReader, IIndexBuilder indexBuilder) {
    	SnapshotFormat sf = new SnapshotFormat(snapshotFormat, extensions);
    	Parser p = new Parser(id, sf, objectReader, indexBuilder);
    	instance.parsers.add(p);
    }
    
    public static Parser lookupParser(String uniqueIdentifier)
    {
        for (Parser p : instance.parsers)
            if (uniqueIdentifier.equals(p.getUniqueIdentifier()))
                return p;
        return null;
    }

    public static List<Parser> matchParser(String fileName)
    {
        List<Parser> answer = new ArrayList<Parser>();
        for (Parser p : instance.parsers)
        {
            for (Pattern regex : p.patterns)
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

	public static List<Parser> allParsers() {
		return instance.parsers;
	}
    
}
