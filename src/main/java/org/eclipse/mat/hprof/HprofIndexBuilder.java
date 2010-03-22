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
package org.eclipse.mat.hprof;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.collect.IteratorLong;
import org.eclipse.mat.hprof.extension.IParsingEnhancer;
import org.eclipse.mat.parser.IIndexBuilder;
import org.eclipse.mat.parser.IPreliminaryIndex;
import org.eclipse.mat.parser.index.IndexWriter;
import org.eclipse.mat.parser.index.IIndexReader.IOne2LongIndex;
import org.eclipse.mat.util.IProgressListener;
import org.eclipse.mat.util.MessageUtil;
import org.eclipse.mat.util.SimpleMonitor;

public class HprofIndexBuilder implements IIndexBuilder
{
    private File file;
    private String prefix;
    private IOne2LongIndex id2position;
    private List<IParsingEnhancer> enhancers;

    public void init(File file, String prefix)
    {
        this.file = file;
        this.prefix = prefix;

        this.enhancers = new ArrayList<IParsingEnhancer>();
// there is no enhancers so far.
//        for (EnhancerRegistry.Enhancer enhancer : EnhancerRegistry.instance().delegates())
//        {
//            IParsingEnhancer parsingEnhancer = enhancer.parser();
//            if (parsingEnhancer != null)
//                this.enhancers.add(parsingEnhancer);
//        }

    }

    public void fill(IPreliminaryIndex preliminary, IProgressListener listener) throws SnapshotException, IOException
    {
        SimpleMonitor monitor = new SimpleMonitor(MessageUtil.format(Messages.HprofIndexBuilder_Parsing,
                        new Object[] { file.getAbsolutePath() }), listener, new int[] { 500, 1500 });

        listener.beginTask(MessageUtil.format(Messages.HprofIndexBuilder_Parsing, file.getName()), 3000);

        IHprofParserHandler handler = new HprofParserHandlerImpl();
        handler.beforePass1(preliminary.getSnapshotInfo());

        SimpleMonitor.Listener mon = (SimpleMonitor.Listener) monitor.nextMonitor();
        mon.beginTask(MessageUtil.format(Messages.HprofIndexBuilder_Scanning, new Object[] { file.getAbsolutePath() }),
                        (int) (file.length() / 1000));
        Pass1Parser pass1 = new Pass1Parser(handler, mon);
        pass1.read(file);

        if (listener.isCanceled())
            throw new IProgressListener.OperationCanceledException();

        mon.done();

        handler.beforePass2(listener);

        mon = (SimpleMonitor.Listener) monitor.nextMonitor();
        mon.beginTask(MessageUtil.format(Messages.HprofIndexBuilder_ExtractingObjects, new Object[] { file
                        .getAbsolutePath() }), (int) (file.length() / 1000));

        Pass2Parser pass2 = new Pass2Parser(handler, mon);
        pass2.read(file);

        if (listener.isCanceled())
            throw new IProgressListener.OperationCanceledException();

        mon.done();

        if (listener.isCanceled())
            throw new IProgressListener.OperationCanceledException();

        for (IParsingEnhancer enhancer : enhancers)
            enhancer.onParsingCompleted(handler.getSnapshotInfo());

        id2position = handler.fillIn(preliminary);
    }

    public void clean(final int[] purgedMapping, IProgressListener listener) throws IOException
    {

        // //////////////////////////////////////////////////////////////
        // object 2 hprof position
        // //////////////////////////////////////////////////////////////

        File indexFile = new File(prefix + "o2hprof.index"); //$NON-NLS-1$
        listener.subTask(MessageUtil.format(Messages.HprofIndexBuilder_Writing, new Object[] { indexFile
                        .getAbsolutePath() }));
        IOne2LongIndex newIndex = new IndexWriter.LongIndexStreamer().writeTo(indexFile, new IndexIterator(id2position,
                        purgedMapping));

        try
        {
            newIndex.close();
        }
        catch (IOException ignore)
        {}

        try
        {
            id2position.close();
        }
        catch (IOException ignore)
        {}

        id2position.delete();
        id2position = null;
    }

    public void cancel()
    {
        if (id2position != null)
        {
            try
            {
                id2position.close();
            }
            catch (IOException ignore)
            {
                // $JL-EXC$
            }
            id2position.delete();
        }
    }

    private static final class IndexIterator implements IteratorLong
    {
        private final IOne2LongIndex id2position;
        private final int[] purgedMapping;
        private int nextIndex = -1;

        private IndexIterator(IOne2LongIndex id2position, int[] purgedMapping)
        {
            this.id2position = id2position;
            this.purgedMapping = purgedMapping;
            findNext();
        }

        public boolean hasNext()
        {
            return nextIndex < purgedMapping.length;
        }

        public long next()
        {
            long answer = id2position.get(nextIndex);
            findNext();
            return answer;
        }

        protected void findNext()
        {
            nextIndex++;
            while (nextIndex < purgedMapping.length && purgedMapping[nextIndex] < 0)
                nextIndex++;
        }
    }
}
