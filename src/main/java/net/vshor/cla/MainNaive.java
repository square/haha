package net.vshor.cla;

import java.io.File;
import java.util.HashMap;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.collect.HashMapIntObject;
import org.eclipse.mat.parser.internal.SnapshotFactory;
import org.eclipse.mat.snapshot.IPathsFromGCRootsComputer;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.util.ConsoleProgressListener;
import org.eclipse.mat.util.IProgressListener;

public class MainNaive {

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.out.println("No arguments supplied");
		}

		IProgressListener listener = new ConsoleProgressListener(System.out);

		SnapshotFactory sf = new SnapshotFactory();
		ISnapshot snapshot = sf.openSnapshot(new File(args[0]),
				new HashMap<String, String>(), listener);
		int[] retainedSet = snapshot.getRetainedSet(snapshot.getGCRoots(),
				listener);

		System.out.println(retainedSet.length);

		HashMapIntObject<Boolean> clObjects = new HashMapIntObject<Boolean>(500);

		for (int obj : retainedSet) {
			if (snapshot.isClass(obj))
				continue;
			int clId = snapshot.getClassOf(obj).getClassLoaderId();
			if (snapshot.getObject(clId).getClazz().getName()
					.startsWith("sun.")) {
				continue;
			}
			Boolean dominatedAllSoFar = clObjects.get(clId);
			if (dominatedAllSoFar != null && !dominatedAllSoFar)
				continue;

			boolean clDominates = firstObjectDominatesSecond(clId, obj, snapshot);

			if (!clDominates) {
				boolean objDominates = firstObjectDominatesSecond(obj, clId, snapshot);
				if (objDominates) {
					System.out.println(String.format(
							"Classloader %s IS NOT dominating: %s, but viceversa is true!", snapshot
							.getObject(clId).getTechnicalName(), snapshot
							.getObject(obj).getTechnicalName()));
					printPathToGCRoot(snapshot, obj, false);
					clDominates = true;
				}
			}
			
			if (dominatedAllSoFar == null) {
				clObjects.put(clId, clDominates);
			} else {
				// If classloader is not dominating the object and
				// it has been dominating everything it loaded so far
				// Then this classloader is no longer dominating.
				if (!clDominates && dominatedAllSoFar) {
					clObjects.put(clId, Boolean.FALSE);
				}
			}
		}

		for (int loader : clObjects.getAllKeys()) {
			if (clObjects.get(loader)) {
				System.out.println(String.format("Classloader %s dominates all its loaded classes: %b",
						snapshot.getObject(loader).getTechnicalName(),
						clObjects.get(loader)));
				printPathToGCRoot(snapshot, loader, true);
			}
		}

	}

	private static boolean firstObjectDominatesSecond(int clId, int obj,
			ISnapshot snapshot) throws SnapshotException {
		boolean clDominates = false;
		int dom = snapshot.getImmediateDominatorId(obj);
		while (dom != -1 && !clDominates) {
			if (dom == clId) {
				clDominates = true;
			}
			dom = snapshot.getImmediateDominatorId(dom);
		}
		return clDominates;
	}

	private static void printPathToGCRoot(ISnapshot snapshot, int obj, boolean printAllPaths)
			throws SnapshotException {
		IPathsFromGCRootsComputer pathsFromGCRoots = snapshot.getPathsFromGCRoots(obj, null);
		
		int[] nextShortestPath = pathsFromGCRoots.getNextShortestPath();
		while (nextShortestPath != null) {
			System.out.println("Shortest path: ");
			for (int path : nextShortestPath) {
				System.out.println("\t" + snapshot.getObject(path).getTechnicalName());
			}
			if (!printAllPaths)
				break;
			nextShortestPath = pathsFromGCRoots.getNextShortestPath();
		}
	}

}