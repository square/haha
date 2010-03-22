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
package org.eclipse.mat.parser.internal;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.parser.IIndexBuilder;
import org.eclipse.mat.parser.internal.util.ParserRegistry;
import org.eclipse.mat.parser.internal.util.ParserRegistry.Parser;
import org.eclipse.mat.parser.model.XSnapshotInfo;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.GCRootInfo;
import org.eclipse.mat.util.IProgressListener;
import org.eclipse.mat.util.MessageUtil;
import org.eclipse.mat.util.IProgressListener.Severity;

public class SnapshotFactory
{
    private static class SnapshotEntry
    {
        private int usageCount;
        private WeakReference<ISnapshot> snapshot;

        public SnapshotEntry(int usageCount, ISnapshot snapshot)
        {
            this.usageCount = usageCount;
            this.snapshot = new WeakReference<ISnapshot>(snapshot);
        }
    }

    private Map<File, SnapshotEntry> snapshotCache = new HashMap<File, SnapshotEntry>();

    public ISnapshot openSnapshot(File file, Map<String, String> args, IProgressListener listener) throws SnapshotException
    {
        ISnapshot answer = null;

        // lookup in cache
        SnapshotEntry entry = snapshotCache.get(file);
        if (entry != null)
        {
            answer = entry.snapshot.get();

            if (answer != null)
            {
                entry.usageCount++;
                return answer;
            }
        }

        String name = file.getAbsolutePath();

        int p = name.lastIndexOf('.');
        String prefix = p >= 0 ? name.substring(0, p + 1) : name + ".";//$NON-NLS-1$

        try
        {
            File indexFile = new File(prefix + "index");//$NON-NLS-1$
            if (indexFile.exists())
            {
                // check if hprof file is newer than index file
                if (file.lastModified() < indexFile.lastModified())
                {
                    answer = SnapshotImpl.readFromFile(file, prefix, listener);
                }
                else
                {
                    String message = MessageUtil.format(Messages.SnapshotFactoryImpl_ReparsingHeapDumpAsIndexOutOfDate,
                                    file.getPath(), new Date(file.lastModified()),
                                    indexFile.getPath(), new Date(indexFile.lastModified()));
                    listener.sendUserMessage(Severity.INFO, message, null);
                    listener.subTask(Messages.SnapshotFactoryImpl_ReparsingHeapDumpWithOutOfDateIndex);
                }
            }
        }
        catch (IOException ignore_and_reparse)
        {
            String text = ignore_and_reparse.getMessage() != null ? ignore_and_reparse.getMessage()
                            : ignore_and_reparse.getClass().getName();
            String message = MessageUtil.format(Messages.SnapshotFactoryImpl_Error_ReparsingHeapDump, text);
            listener.sendUserMessage(Severity.WARNING, message, ignore_and_reparse);
            listener.subTask(message);
        }

        if (answer == null)
        {
            deleteIndexFiles(file);
            answer = parse(file, prefix, args, listener);
        }

        entry = new SnapshotEntry(1, answer);

        snapshotCache.put(file, entry);

        return answer;
    }

    public synchronized void dispose(ISnapshot snapshot)
    {

        for (Iterator<SnapshotEntry> iter = snapshotCache.values().iterator(); iter.hasNext();)
        {
            SnapshotEntry entry = iter.next();

            ISnapshot s = entry.snapshot.get();
            if (s == null)
            {
                iter.remove();
            }
            else if (s == snapshot)
            {
                entry.usageCount--;
                if (entry.usageCount == 0)
                {
                    snapshot.dispose();
                    iter.remove();
                }
                return;
            }
        }

        // just in case the snapshot is not stored anymore
        if (snapshot != null)
            snapshot.dispose();
    }

//    public IOQLQuery createQuery(String queryString) throws SnapshotException
//    {
//        return new OQLQueryImpl(queryString);
//    }

//    public List<SnapshotFormat> getSupportedFormats()
//    {
//        List<SnapshotFormat> answer = new ArrayList<SnapshotFormat>();
//
//        for (Parser parser : ParserPlugin.getDefault().getParserRegistry().delegates())
//            answer.add(parser.getSnapshotFormat());
//
//        return answer;
//    }

    // //////////////////////////////////////////////////////////////
    // Internal implementations
    // //////////////////////////////////////////////////////////////

    private final ISnapshot parse(File file, String prefix, Map<String, String> args, IProgressListener listener) throws SnapshotException
    {
        List<ParserRegistry.Parser> parsers = ParserRegistry.matchParser(file.getName());
        if (parsers.isEmpty())
            parsers.addAll(ParserRegistry.allParsers()); // try all...

        List<IOException> errors = new ArrayList<IOException>();

        for (Parser parser : parsers)
        {
            IIndexBuilder indexBuilder = parser.getIndexBuilder();

			if (indexBuilder == null)
				continue;
            
            try
            {
                indexBuilder.init(file, prefix);

                XSnapshotInfo snapshotInfo = new XSnapshotInfo();
                snapshotInfo.setPath(file.getAbsolutePath());
                snapshotInfo.setPrefix(prefix);
                snapshotInfo.setProperty("$heapFormat", parser.getId());//$NON-NLS-1$
                if (Boolean.parseBoolean(args.get("keep_unreachable_objects")))//$NON-NLS-1$
                {
                    snapshotInfo.setProperty("keep_unreachable_objects", GCRootInfo.Type.UNREACHABLE);//$NON-NLS-1$
                }
                PreliminaryIndexImpl idx = new PreliminaryIndexImpl(snapshotInfo);

                indexBuilder.fill(idx, listener);

                SnapshotImplBuilder builder = new SnapshotImplBuilder(idx.getSnapshotInfo());

                int[] purgedMapping = GarbageCleaner.clean(idx, builder, args, listener);

                indexBuilder.clean(purgedMapping, listener);

                SnapshotImpl snapshot = builder.create(parser, listener);

                snapshot.calculateDominatorTree(listener);

                return snapshot;
            }
            catch (IOException ioe)
            {
                errors.add(ioe);
                indexBuilder.cancel();
            }
            catch (Exception e)
            {
                indexBuilder.cancel();

                throw SnapshotException.rethrow(e);
            }
        }

        throw new SnapshotException(MessageUtil.format(Messages.SnapshotFactoryImpl_Error_NoParserRegistered, file
                            .getName()));
    }

    private void deleteIndexFiles(File file)
    {
        File directory = file.getParentFile();
        if (directory == null)
            directory = new File("."); //$NON-NLS-1$

        String filename = file.getName();

        int p = filename.lastIndexOf('.');
        final String fragment = p >= 0 ? filename.substring(0, p) : filename;
        final Pattern indexPattern = Pattern.compile("\\.(.*\\.)?index$");//$NON-NLS-1$
        final Pattern logPattern = Pattern.compile("\\.inbound\\.index.*\\.log$");//$NON-NLS-1$

        File[] files = directory.listFiles(new FileFilter()
        {
            public boolean accept(File f)
            {
                if (f.isDirectory())
                    return false;

                String name = f.getName();
                return name.startsWith(fragment)
                                && (indexPattern.matcher(name).matches() || logPattern.matcher(name).matches());
            }
        });

        if (files != null)
        {
            for (File f : files)
                f.delete();
        }
    }
}
