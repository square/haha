/*******************************************************************************
 * Copyright (c) 2009 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.mat.parser.internal;

public class Messages 
{
    public static String AbstractObjectImpl_Error_FieldContainsIllegalReference;
    public static String AbstractObjectImpl_Error_FieldIsNotReference;
    public static String BitOutputStream_Error_ArrayFull;
    public static String ClassHistogramRecordBuilder_Error_IllegalUseOfHistogramBuilder;
    public static String DominatorTree_CalculateRetainedSizes;
    public static String DominatorTree_CalculatingDominatorTree;
    public static String DominatorTree_ComputingDominators;
    public static String DominatorTree_CreateDominatorsIndexFile;
    public static String DominatorTree_DepthFirstSearch;
    public static String DominatorTree_DominatorTreeCalculation;
    public static String Function_Error_NeedsNumberAsInput;
    public static String Function_ErrorNoFunction;
    public static String Function_unknown;
    public static String GarbageCleaner_ReIndexingClasses;
    public static String GarbageCleaner_ReIndexingObjects;
    public static String GarbageCleaner_ReIndexingOutboundIndex;
    public static String GarbageCleaner_RemovingUnreachableObjects;
    public static String GarbageCleaner_SearchingForUnreachableObjects;
    public static String GarbageCleaner_Writing;
    public static String HistogramBuilder_Error_FailedToStoreInHistogram;
    public static String IndexReader_Error_IndexIsEmbedded;
    public static String MethodCallExpression_Error_MethodNotFound;
    public static String MultiplePathsFromGCRootsComputerImpl_FindingPaths;
    public static String SnapshotFactoryImpl_Error_NoParserRegistered;
    public static String SnapshotFactoryImpl_Error_OpeningHeapDump;
    public static String SnapshotFactoryImpl_Error_ReparsingHeapDump;
    public static String SnapshotFactoryImpl_ErrorOpeningHeapDump;
    public static String SnapshotFactoryImpl_ReparsingHeapDumpAsIndexOutOfDate;
    public static String SnapshotFactoryImpl_ReparsingHeapDumpWithOutOfDateIndex;
    public static String SnapshotImpl_BuildingHistogram;
    public static String SnapshotImpl_Error_DomTreeNotAvailable;
    public static String SnapshotImpl_Error_ObjectNotFound;
    public static String SnapshotImpl_Error_ParserNotFound;
    public static String SnapshotImpl_Error_ReplacingNonExistentClassLoader;
    public static String SnapshotImpl_Error_UnknownVersion;
    public static String SnapshotImpl_Error_UnrecognizedState;
    public static String SnapshotImpl_Histogram;
    public static String SnapshotImpl_Label;
    public static String SnapshotImpl_ReadingInboundReferrers;
    public static String SnapshotImpl_ReadingOutboundReferrers;
    public static String SnapshotImpl_ReopeningParsedHeapDumpFile;
    public static String SnapshotImpl_RetrievingDominators;
    public static String ObjectArrayImpl_forArray;
    public static String ObjectMarker_CalculateRetainedSize;
    public static String Operation_Error_ArgumentOfUnknownClass;
    public static String Operation_Error_CannotCompare;
    public static String Operation_Error_NotInArgumentOfUnknownClass;
    public static String Operation_Error_NotInCannotCompare;
    public static String Operation_ErrorNoComparable;
    public static String Operation_ErrorNotNumber;
    public static String OQLQueryImpl_CheckingClass;
    public static String OQLQueryImpl_CollectingObjects;
    public static String OQLQueryImpl_Error_CannotCalculateRetainedSet;
    public static String OQLQueryImpl_Error_ClassCastExceptionOccured;
    public static String OQLQueryImpl_Error_ElementIsNotClass;
    public static String OQLQueryImpl_Error_InvalidClassNamePattern;
    public static String OQLQueryImpl_Error_MissingSnapshot;
    public static String OQLQueryImpl_Error_MustReturnObjectList;
    public static String OQLQueryImpl_Error_QueryCannotBeConverted;
    public static String OQLQueryImpl_Error_QueryMustHaveIdenticalSelectItems;
    public static String OQLQueryImpl_Error_QueryMustReturnObjects;
    public static String OQLQueryImpl_Error_ResultMustReturnObjectList;
    public static String OQLQueryImpl_Errot_IsNotClass;
    public static String OQLQueryImpl_SelectingObjects;
    public static String ParserRegistry_ErrorCompilingFileNamePattern;
    public static String ParserRegistry_ErrorWhileCreating;
    public static String PathExpression_Error_ArrayHasNoProperty;
    public static String PathExpression_Error_TypeHasNoProperty;
    public static String PathExpression_Error_UnknownElementInPath;
    public static String PositionInputStream_mark;
    public static String PositionInputStream_reset;
    public static String PositionInputStream_seek;
    public static String RetainedSizeCache_ErrorReadingRetainedSizes;
    public static String RetainedSizeCache_Warning_IgnoreError;

	public static String OQLParser_Encountered_X_at_line_X_column_X_Was_expecting_one_of_X;
	public static String OQLParser_Missing_return_statement_in_function;

	public static String QueueInt_ZeroSizeQueue;
	
    private Messages()
    {}
}
