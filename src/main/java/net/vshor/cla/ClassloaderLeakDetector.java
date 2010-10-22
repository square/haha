package net.vshor.cla;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.collect.ArrayInt;
import org.eclipse.mat.collect.HashMapIntObject;
import org.eclipse.mat.parser.internal.SnapshotFactory;
import org.eclipse.mat.snapshot.IPathsFromGCRootsComputer;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.PathsFromGCRootsTree;
import org.eclipse.mat.snapshot.model.GCRootInfo;
import org.eclipse.mat.snapshot.model.IClass;
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
//					System.out.println(String.format(
//							"Classloader %s IS NOT dominating: %s, but viceversa is true!", snapshot
//							.getObject(clId).getTechnicalName(), snapshot
//							.getObject(obj).getTechnicalName()));
//					printPathToGCRoot(snapshot, obj, false);
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
	
	private int xmlObjectCounter = 1;
	
	public void outputResultsAsXml(PrintStream where) throws SnapshotException {
		out = where;
		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.println("<?xml-stylesheet type=\"text/xsl\" href=\"leaks.xsl\"?>");
		out.println("<leaks>");
		for (int loader : clDominationFlags.getAllKeys()) {
			if (clDominationFlags.get(loader)) {
				IObject loaderObj = snapshot.getObject(loader);
				out.println(String.format("<leak>%n<classloader>%s</classloader>%n<id>0x%s</id>",
						loaderObj.getClazz().getName(),
						Long.toString(loaderObj.getObjectAddress(), 16)));
				printPathToGCRoot(snapshot, loader, true);
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
		
		int dom = snapshot.getImmediateDominatorId(obj);
		ArrayInt path = new ArrayInt();
		path.add(obj);
		while (dom != -1) {
			path.add(dom);
			dom = snapshot.getImmediateDominatorId(dom);
		}
		out.println("<dominator-path>");
		printObjectFromPath(path.toArray());
		out.println("</dominator-path>");
		
		IPathsFromGCRootsComputer pathsFromGCRoots = snapshot.getPathsFromGCRoots(obj, null);
		List<int[]> paths = new ArrayList<int[]>();
		
		
		int[] nextShortestPath = pathsFromGCRoots.getNextShortestPath();
		int pathCount = 0;
		int objectCount = 0;
		while (nextShortestPath != null ) {
			pathCount++;
			objectCount += nextShortestPath.length;
			
			if (pathCount > 500) break;
			
//			out.println("<path>");
//			printObjectFromPath(nextShortestPath);
//			out.println("</path>");
			if (!printAllPaths)
				break;
			paths.add(nextShortestPath);
			nextShortestPath = pathsFromGCRoots.getNextShortestPath();
		}
//		System.out.println(pathCount + " Paths found for Leak");
//		System.out.println(objectCount + " objects in paths");
		
		PathsFromGCRootsTree tree = pathsFromGCRoots.getTree(paths);
		out.println("<tree>");
		System.out.println(outputTree(tree, null, "  ") + " objects in path tree");
		out.println("</tree>");
	}
	
	private int outputTree(PathsFromGCRootsTree tree, IObject objReferencedByRoot, String identation) throws SnapshotException {
		out.println(String.format("%s<object id=\"%d\">", identation, xmlObjectCounter++));
		IObject object = snapshot.getObject(tree.getOwnId());

		outputGeneralObjectInfo(object, identation);
		if (objReferencedByRoot != null) {
			out.println(String.format("%s    <outbound-ref><![CDATA[%s]]></outbound-ref>", identation, getReferencingFieldName(object, objReferencedByRoot)));
		}
		
		int result = tree.getObjectIds().length;
		out.println(identation + "<incoming-fields>");
		for (int subtree : tree.getObjectIds()) {
			out.println(identation + "  <incoming-field>");
			result += outputTree(tree.getBranch(subtree), object, "    "+identation);
			out.println(identation + "  </incoming-field>");
		}
		out.println(identation + "</incoming-fields>");
		out.println(identation + "</object>");
		return result;
	}

	private void printObjectFromPath(int[] path) throws SnapshotException {
		StringBuilder closingTags = new StringBuilder();
		for (int index = path.length-1; index >= 0; index--) {
			IObject object = snapshot.getObject(path[index]);
			String identation = multiplyStr("  ", path.length-index);
			out.println(String.format("%s<object id=\"%d\">", identation, xmlObjectCounter++));
			
			outputGeneralObjectInfo(object, identation);
			
			closingTags.insert(0, String.format("%s</object>%n", identation));
			if (index > 0) {
				out.println(identation + "<field>");
				IObject referencingObject = snapshot.getObject(path[index-1]);
				out.println(String.format("%s  <name><![CDATA[%s]]></name>", identation, getReferencingFieldName(object, referencingObject)));
				closingTags.insert(0, String.format("%s</field>%n", identation));
			}
		}
		out.print(closingTags);
	}
	
	private void outputGeneralObjectInfo(IObject object, String identation) throws SnapshotException {
		String clazzName = object.getClazz().getName();
		String gcRootInfo = "";
		if (snapshot.isGCRoot(object.getObjectId())) {
			gcRootInfo = GCRootInfo.getTypeSetAsString(snapshot.getGCRootInfo(object.getObjectId())) + " ";
		}
		if (snapshot.isClass(object.getObjectId())) {
			clazzName = "[Class] " + ((IClass)object).getName();
		}
		String objAddr = Long.toString(object.getObjectAddress(), 16);
		out.println(String.format("%s<class><![CDATA[%s @ 0x%s]]></class>", identation, gcRootInfo + clazzName, objAddr));
		out.println(String.format("%s<id>0x%s</id>", identation.toString(), objAddr));
	}
	private String getReferencingFieldName(IObject object, IObject referencingObject) throws SnapshotException {
		for (NamedReference ref : object.getOutboundReferences()) {
			if (ref.getObjectId() == referencingObject.getObjectId()) {
				return ref.getName();
			}
		}
		return null;
	}

	private String multiplyStr(String str, int howMuch) {
		StringBuilder sb = new StringBuilder(str);
		while (--howMuch > 0) {
			sb.append(str);
		}
		return sb.toString();
	}
}
