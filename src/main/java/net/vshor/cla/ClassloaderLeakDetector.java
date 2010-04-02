package net.vshor.cla;

import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.collect.HashMapIntObject;
import org.eclipse.mat.parser.internal.SnapshotFactory;
import org.eclipse.mat.snapshot.IPathsFromGCRootsComputer;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.snapshot.model.NamedReference;
import org.eclipse.mat.util.ConsoleProgressListener;
import org.eclipse.mat.util.IProgressListener;

public class ClassloaderLeakDetector {

	private ISnapshot snapshot;
	private PrintStream out = System.out;
	HashMapIntObject<Boolean> clDominationFlags = new HashMapIntObject<Boolean>(500);
	
	public ClassloaderLeakDetector(File f) throws Exception {
		IProgressListener listener = new ConsoleProgressListener(System.out);

		SnapshotFactory sf = new SnapshotFactory();
		snapshot = sf.openSnapshot(f, new HashMap<String, String>(), listener);
		int[] retainedSet = snapshot.getRetainedSet(snapshot.getGCRoots(),
				listener);

		for (int obj : retainedSet) {
			if (snapshot.isClass(obj))
				continue;
			int clId = snapshot.getClassOf(obj).getClassLoaderId();
			if (snapshot.getObject(clId).getClazz().getName()
					.startsWith("sun.")) {
				continue;
			}
			Boolean dominatedAllSoFar = clDominationFlags.get(clId);
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
				clDominationFlags.put(clId, clDominates);
			} else {
				// If classloader is not dominating the object and
				// it has been dominating everything it loaded so far
				// Then this classloader is no longer dominating.
				if (!clDominates && dominatedAllSoFar) {
					clDominationFlags.put(clId, Boolean.FALSE);
				}
			}
		}

	}
	
	public void outputResultsAsXml(PrintStream where) throws SnapshotException {
		out = where;
		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.println("<leaks>");
		for (int loader : clDominationFlags.getAllKeys()) {
			if (clDominationFlags.get(loader)) {
				IObject loaderObj = snapshot.getObject(loader);
				out.println(String.format("<leak>\n<classloader>%s</classloader>\n<id>%s</id>",
						loaderObj.getClazz().getName(),
						Long.toString(loaderObj.getObjectAddress(), 16)));
				printPathToGCRoot(snapshot, loader, false);
				out.println("</leak>");
			}
		}
		out.println("</leaks>");
	}
	private boolean firstObjectDominatesSecond(int clId, int obj,
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

	private void printPathToGCRoot(ISnapshot snapshot, int obj, boolean printAllPaths)
			throws SnapshotException {
		IPathsFromGCRootsComputer pathsFromGCRoots = snapshot.getPathsFromGCRoots(obj, null);
		
		int[] nextShortestPath = pathsFromGCRoots.getNextShortestPath();
		while (nextShortestPath != null) {
			out.println("<path>");
			printObjectFromPath(nextShortestPath, nextShortestPath.length-1);
			out.println("</path>");
			if (!printAllPaths)
				break;
			nextShortestPath = pathsFromGCRoots.getNextShortestPath();
		}
	}
	
	private void printObjectFromPath(int[] path, int index) throws SnapshotException {
		IObject object = snapshot.getObject(path[index]);
		String identation = multiplyStr("\t", path.length-index);
		out.println(identation + "<object>");
		out.println(String.format("%s<class>%s</class>",identation,object.getClazz().getName()));
		out.println(String.format("%s<id>0x%s</id>", identation.toString(), Long.toString(object.getObjectAddress(), 16)));
		if (index > 0) {
			out.println(identation + "<field>");
			IObject nextObj = snapshot.getObject(path[index-1]);
			for (NamedReference ref : object.getOutboundReferences()) {
				if (ref.getObjectId() == nextObj.getObjectId()) {
					out.println(String.format("%s\t<name>%s</name>", identation, ref.getName()));
					break;
				}
			}
			printObjectFromPath(path, index-1);
			out.println(identation + "</field>");
		}
		out.println(identation + "</object>\n");
	}
	
	private String multiplyStr(String str, int howMuch) {
		StringBuilder sb = new StringBuilder(str);
		while (--howMuch > 0) {
			sb.append(str);
		}
		return sb.toString();
	}
}
