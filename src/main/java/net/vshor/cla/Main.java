package net.vshor.cla;

import java.io.File;
import java.util.HashMap;

import org.eclipse.mat.collect.HashMapIntObject;
import org.eclipse.mat.parser.internal.SnapshotFactory;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.util.ConsoleProgressListener;
import org.eclipse.mat.util.IProgressListener;

public class Main {

	/**
	 * @param args
	 */
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
			if (snapshot.getObject(clId).getClazz().getName().startsWith("sun.")) {
				System.out.println("Skipping: " + snapshot.getObject(clId).getClazz().getName());
				continue;
			} else {
				System.out.println("Processing: " + snapshot.getObject(clId).getClazz().getName());
			}
			Boolean dominatedAllSoFar = clObjects.get(clId);
			if (dominatedAllSoFar != null && !dominatedAllSoFar) 
				continue;
			
			int dom = snapshot.getImmediateDominatorId(obj);
			boolean clDominates = false;
			while (dom != -1 && !clDominates) {
				if (dom == clId) {
					clDominates = true;
				}
				dom = snapshot.getImmediateDominatorId(dom);
			}  
			
			if (clDominates) {
				System.out.println(String.format("Classloader %s dominated its loaded class!", snapshot.getObject(clId).getTechnicalName()));
			}
			if (dominatedAllSoFar == null) {
				clObjects.put(clId, clDominates);
			}
			else {
				System.out.println(String.format("Classloader %s IS NOT dominating: %s:", snapshot.getObject(clId).getTechnicalName(), snapshot.getObject(obj).getTechnicalName()));
				// If classloader is not dominating the object and 
				// it has been dominating everything it loaded so far
				// Then this classloader is no longer dominating.
				if (!clDominates && dominatedAllSoFar) {
					clObjects.put(clId, Boolean.FALSE);
				}
			}
		}
		
		for (int loader : clObjects.getAllKeys()) {
			//System.out.println(String.format("Classloader %s dominates all its loaded classes: %b", snapshot.getObject(loader).getTechnicalName(), clObjects.get(loader)));
		}
		
	}

}
