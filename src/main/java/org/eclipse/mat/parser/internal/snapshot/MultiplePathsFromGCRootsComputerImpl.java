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
package org.eclipse.mat.parser.internal.snapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.collect.ArrayInt;
import org.eclipse.mat.collect.BitField;
import org.eclipse.mat.collect.QueueInt;
import org.eclipse.mat.parser.index.IIndexReader;
import org.eclipse.mat.parser.internal.Messages;
import org.eclipse.mat.parser.internal.SnapshotImpl;
import org.eclipse.mat.snapshot.IMultiplePathsFromGCRootsComputer;
import org.eclipse.mat.snapshot.MultiplePathsFromGCRootsClassRecord;
import org.eclipse.mat.snapshot.MultiplePathsFromGCRootsRecord;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.snapshot.model.NamedReference;
import org.eclipse.mat.util.IProgressListener;

public class MultiplePathsFromGCRootsComputerImpl implements IMultiplePathsFromGCRootsComputer
{

	int[] objectIds; // the initial objects
	Object[] paths; // paths for each of the objects

	SnapshotImpl snapshot; // snapshot
	IIndexReader.IOne2ManyIndex outboundIndex; // outbound references index

	private BitField excludeInstances;
	private Map<IClass, Set<String>> excludeMap;

	private boolean pathsCalculated;

	private static final int NOT_VISITED = -2;
	private static final int NO_PARENT = -1;

	public MultiplePathsFromGCRootsComputerImpl(int[] objectIds, Map<IClass, Set<String>> excludeMap, SnapshotImpl snapshot) throws SnapshotException
	{
		this.snapshot = snapshot;
		this.objectIds = objectIds;
		this.excludeMap = excludeMap;
		outboundIndex = snapshot.getIndexManager().outbound;

		if (excludeMap != null)
		{
			initExcludeInstances();
		}
	}

	private void initExcludeInstances() throws SnapshotException
	{
		excludeInstances = new BitField(snapshot.getIndexManager().o2address().size());
		for (IClass clazz : excludeMap.keySet())
		{
			int[] objects = clazz.getObjectIds();
			for (int objId : objects)
			{
				excludeInstances.set(objId);
			}
		}
	}

	private void computePaths(IProgressListener progressListener) throws SnapshotException
	{
		ArrayList<int[]> pathsList = new ArrayList<int[]>();

		// make a breadth first search for the objects, starting from the roots
		int[] parent = bfs(progressListener);

		// then get the shortest path per object
		for (int i = 0; i < objectIds.length; i++)
		{
			int[] path = getPathFromBFS(objectIds[i], parent);

			/*
			 * if there is an exclude filter, for some objects there could be no
			 * path, i.e. null is expected then
			 */
			if (path != null)
			{
				pathsList.add(path);
			}
		}

		pathsCalculated = true;
		paths = pathsList.toArray();
	}

	public MultiplePathsFromGCRootsRecord[] getPathsByGCRoot(IProgressListener progressListener) throws SnapshotException
	{
		if (!pathsCalculated)
		{
			computePaths(progressListener);
		}

		MultiplePathsFromGCRootsRecord dummy = new MultiplePathsFromGCRootsRecord(-1, -1, snapshot);
		for (int i = 0; i < paths.length; i++)
		{
			dummy.addPath((int[]) paths[i]);
		}

		return dummy.nextLevel();
	}

	public Object[] getAllPaths(IProgressListener progressListener) throws SnapshotException
	{
		if (!pathsCalculated)
		{
			computePaths(progressListener);
		}
		return paths;
	}

	public MultiplePathsFromGCRootsClassRecord[] getPathsGroupedByClass(boolean startFromTheGCRoots, IProgressListener progressListener)
			throws SnapshotException
	{
		if (!pathsCalculated)
		{
			computePaths(progressListener);
		}

		MultiplePathsFromGCRootsClassRecord dummy = new MultiplePathsFromGCRootsClassRecord(null, -1, startFromTheGCRoots, snapshot);
		for (int i = 0; i < paths.length; i++)
		{
			dummy.addPath((int[]) paths[i]);
		}

		return dummy.nextLevel();
	}

	private boolean refersOnlyThroughExcluded(int referrerId, int referentId) throws SnapshotException
	{
		if (!excludeInstances.get(referrerId)) return false;

		IObject referrerObject = snapshot.getObject(referrerId);
		Set<String> excludeFields = excludeMap.get(referrerObject.getClazz());
		if (excludeFields == null) return true; // treat null as all fields

		long referentAddr = snapshot.mapIdToAddress(referentId);

		List<NamedReference> refs = referrerObject.getOutboundReferences();
		for (NamedReference reference : refs)
		{
			if (referentAddr == reference.getObjectAddress() && !excludeFields.contains(reference.getName()))
			{
				return false;
			}
		}
		return true;
	}

	private int[] bfs(IProgressListener progressListener) throws SnapshotException
	{
		// number objects in the heap
		final int numObjects = snapshot.getSnapshotInfo().getNumberOfObjects();
		final boolean skipReferences = excludeMap != null; // should some paths
		// be excluded?

		// used to store the parent of each object during the BFS
		int[] parent = new int[numObjects];
		Arrays.fill(parent, NOT_VISITED);

		// use boolean[numObjects] instead of SetInt, as it is faster to check
		boolean[] toBeChecked = new boolean[numObjects];

		int count = 0; // the number of distinct objects whose paths should be
		// calculated
		for (int i : objectIds)
		{
			if (!toBeChecked[i]) count++;
			toBeChecked[i] = true;
		}

		// use first-in-first-out to get the shortest paths
		QueueInt fifo = new QueueInt(numObjects / 8);

		// initially queue all GC roots
		int[] gcRoots = snapshot.getGCRoots();
		for (int root : gcRoots)
		{
			fifo.put(root);
			parent[root] = NO_PARENT;
		}

		// used for the progress listener
		int countVisitedObjects = 0;
		int reportFrequency = Math.max(10, numObjects / 100);

		progressListener.beginTask(Messages.MultiplePathsFromGCRootsComputerImpl_FindingPaths, 100);

		// loop until the queue is empty, or all necessary paths are found
		while (fifo.size() > 0 && count > 0)
		{
			int objectId = fifo.get();

			// was some of the objects of interest reached?
			if (toBeChecked[objectId])
			{
				count--; // reduce the remaining work
			}

			// queue any unprocessed referenced object
			int[] outbound = outboundIndex.get(objectId);
			for (int child : outbound)
			{
				if (parent[child] == NOT_VISITED)
				{
					if (skipReferences)
					{
						if (refersOnlyThroughExcluded(objectId, child)) continue;
					}
					parent[child] = objectId;
					fifo.put(child);
				}
			}

			countVisitedObjects++;
			if (countVisitedObjects % reportFrequency == 0)
			{
				if (progressListener.isCanceled()) throw new IProgressListener.OperationCanceledException();
				progressListener.worked(1);
			}
		}
		progressListener.done();
		return parent;
	}

	/*
	 * Returns the shortest path to an object, using the stored parent of every
	 * needed object calculated during a BFS
	 * 
	 * @param int objectId the object to which a path should be calculated
	 * 
	 * @param int[] parent an array, result of a BSF, which keeps a parent[i] is
	 * the parent of the object with index i, as calculated during the BFS
	 * 
	 * @return int[] the shortest path from a GC root. The object of interest is
	 * at index 0, the GC root at index length-1
	 */
	private int[] getPathFromBFS(int objectId, int[] parent)
	{
		// check if the object wasn't reached at all. This may happen if some
		// paths are excluded
		if (parent[objectId] == NOT_VISITED) return null;

		ArrayInt path = new ArrayInt();
		while (objectId != NO_PARENT)
		{
			path.add(objectId);
			objectId = parent[objectId];
		}

		return path.toArray();
	}
}
