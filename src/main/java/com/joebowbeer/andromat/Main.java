/*
 * Copyright 2005-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package com.joebowbeer.andromat;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;

import org.eclipse.mat.parser.internal.SnapshotFactory;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.util.ConsoleProgressListener;
import org.eclipse.mat.util.IProgressListener;

/**
 * Examines an Android heap dump.
 * 
 * This code is adapted from <a href="https://bitbucket.org/ekabanov/mat/">
 * bitbucket.org/ekabanov/mat/</a>, which is a stripped-down command line
 * version of <a href="http://www.eclipse.org/mat/">Eclipse Memory Analyzer</a>.
 * 
 * The Original Code is HAT. The Initial Developer of the Original Code is
 * Bill Foote, with contributions from others at JavaSoft/Sun.
 */
public class Main {

    private static String VERSION_STRING =
            Main.class.getPackage().getSpecificationTitle() + " " +
            Main.class.getPackage().getSpecificationVersion();

    private static void usage(String message) {
        if (message != null) {
            System.err.println("ERROR: " + message);
        }
        System.err.println("Usage:  andromat [-version] [-h|-help] <file>");
        System.err.println();
        System.err.println("\t-version          Report version number");
        System.err.println("\t-h|-help          Print this help and exit");
        System.err.println("\t<file>            The file to read");
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            usage("No arguments supplied");
        }

        for (int i = 0;; i += 2) {
            if (i > (args.length - 1)) {
                usage("Option parsing error");
            }
            if ("-version".equals(args[i])) {
                System.out.print(VERSION_STRING);
                System.exit(0);
            }
            if ("-h".equals(args[i]) || "-help".equals(args[i])) {
                usage(null);
            }
            if (i == (args.length - 1)) {
                break;
            }
        }

        String fileName = args[args.length - 1];

        IProgressListener listener = new ConsoleProgressListener(System.out);

        SnapshotFactory sf = new SnapshotFactory();
        ISnapshot snapshot = sf.openSnapshot(new File(fileName),
                new HashMap<String, String>(), listener);

        System.out.println(snapshot.getSnapshotInfo());
        System.out.println();

        String[] classNames = {
            "byte[]",
            "java.util.HashMap",
            "android.graphics.Bitmap"
        };

        for (String name : classNames) {
            Collection<IClass> classes = snapshot.getClassesByName(name, false);
            if (classes == null || classes.isEmpty()) {
                System.out.println(String.format(
                        "Cannot find class %s in heap dump", name));
                continue;
            }
            assert classes.size() == 1;
            IClass clazz = classes.iterator().next();
            int[] objIds = clazz.getObjectIds();
            long minRetainedSize = snapshot.getMinRetainedSize(objIds, listener);
            System.out.println(String.format(
                    "%s instances = %d, retained size >= %d",
                    clazz.getName(), objIds.length, minRetainedSize));
        }
    }
}
