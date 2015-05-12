package org.eclipse.mat.hprof;

public enum Messages {

  AbstractParser_Error_IllegalType("Illegal Type:  {0}"),
  AbstractParser_Error_InvalidHPROFHeader("Invalid HPROF file header."),
  AbstractParser_Error_NotHeapDump("Not a HPROF heap dump"),
  AbstractParser_Error_UnknownHPROFVersion("Unknown HPROF Version ({0})"),
  AbstractParser_Error_UnsupportedHPROFVersion("Unsupported HPROF Version {0}"),
  HprofIndexBuilder_ExtractingObjects("Extracting objects from {0}"),
  HprofIndexBuilder_Parsing("Parsing {0}"),
  HprofIndexBuilder_Scanning("Scanning {0}"),
  HprofIndexBuilder_Writing("Writing {0}"),
  HprofParserHandlerImpl_Error_ExpectedClassSegment(
      "Error: Found instance segment but expected class segment (see FAQ): 0x{0}"),
  HprofParserHandlerImpl_Error_MultipleClassInstancesExist(
      "multiple class instances exist for {0}"),
  HprofParserHandlerImpl_HeapContainsObjects("Heap {0} contains {1,number} objects"),
  HprofRandomAccessParser_Error_DumpIncomplete("need to create dummy class. dump incomplete"),
  HprofRandomAccessParser_Error_DuplicateClass("Duplicate class: {0}"),
  HprofRandomAccessParser_Error_IllegalDumpSegment("Illegal dump segment {0}"),
  HprofRandomAccessParser_Error_MissingClass("missing fake class {0}"),
  HprofRandomAccessParser_Error_MissingFakeClass("missing fake class"),
  Pass1Parser_Error_IllegalRecordLength("Illegal record length at byte {0}"),
  Pass1Parser_Error_IllegalType("Illegal primitive object array type"),
  Pass1Parser_Error_InvalidHeapDumpFile(
      "Error: Invalid heap dump file.\n Unsupported segment type {0} at position {1}"),
  Pass1Parser_Error_invalidHPROFFile(
      "(Possibly) Invalid HPROF file: Expected to read another {0,number} bytes, but only {1,number} bytes are available."),
  Pass1Parser_Error_NoHeapDumpIndexFound(
      "Parser found {0} HPROF dumps in file {1}. No heap dump index {2} found. See FAQ."),
  Pass1Parser_Error_SupportedDumps("Only 32bit and 64bit dumps are supported."),
  Pass1Parser_Error_UnresolvedName("Unresolved Name 0x"),
  Pass1Parser_Info_UsingDumpIndex(
      "Parser found {0} HPROF dumps in file {1}. Using dump index {2}. See FAQ."),
  Pass1Parser_Info_WroteThreadsTo("Wrote threads call stacks to {0}"),
  Pass1Parser_Error_WritingThreadsInformation("Error writing threads information"),
  Pass2Parser_Error_HandleMustCreateFakeClassForName("handler must create fake class for {0}"),
  Pass2Parser_Error_HandlerMustCreateFakeClassForAddress(
      "handler must create fake class for 0x{0}"),
  Pass2Parser_Error_InsufficientBytesRead("Insufficient bytes read for instance at {0}"),
  ClassSpecificNameResolverRegistry_Error_MissingObject(
      "No object to resolve class specific name for."),
  ClassSpecificNameResolverRegistry_ErrorMsg_DuringResolving("Error resolving name of {0}"),
  GCRootInfo_BusyMonitor("Busy Monitor"),
  GCRootInfo_Finalizable("Finalizable"),
  GCRootInfo_JavaLocal("Java Local"),
  GCRootInfo_JNIGlobal("JNI Global"),
  GCRootInfo_JNILocal("JNI Local"),
  GCRootInfo_NativeStack("Native Stack"),
  GCRootInfo_SystemClass("System Class"),
  GCRootInfo_Thread("Thread"),
  GCRootInfo_ThreadBlock("Thread Block"),
  GCRootInfo_Unfinalized("Unfinalized"),
  GCRootInfo_Unkown("Unknown"),
  GCRootInfo_Unreachable("Unreachable"),
  AbstractObjectImpl_Error_FieldContainsIllegalReference(
      "AbstractObjectImpl_Error_FieldContainsIllegalReference"),
  AbstractObjectImpl_Error_FieldIsNotReference("AbstractObjectImpl_Error_FieldIsNotReference"),
  BitOutputStream_Error_ArrayFull("BitOutputStream_Error_ArrayFull"),
  DominatorTree_CalculateRetainedSizes("DominatorTree_CalculateRetainedSizes"),
  DominatorTree_CalculatingDominatorTree("DominatorTree_CalculatingDominatorTree"),
  DominatorTree_ComputingDominators("DominatorTree_ComputingDominators"),
  DominatorTree_CreateDominatorsIndexFile("DominatorTree_CreateDominatorsIndexFile"),
  DominatorTree_DepthFirstSearch("DominatorTree_DepthFirstSearch"),
  DominatorTree_DominatorTreeCalculation("DominatorTree_DominatorTreeCalculation"),
  GarbageCleaner_ReIndexingClasses("GarbageCleaner_ReIndexingClasses"),
  GarbageCleaner_ReIndexingObjects("GarbageCleaner_ReIndexingObjects"),
  GarbageCleaner_ReIndexingOutboundIndex("GarbageCleaner_ReIndexingOutboundIndex"),
  GarbageCleaner_RemovingUnreachableObjects("GarbageCleaner_RemovingUnreachableObjects"),
  GarbageCleaner_SearchingForUnreachableObjects("GarbageCleaner_SearchingForUnreachableObjects"),
  GarbageCleaner_Writing("GarbageCleaner_Writing"),
  IndexReader_Error_IndexIsEmbedded("IndexReader_Error_IndexIsEmbedded"),
  MultiplePathsFromGCRootsComputerImpl_FindingPaths(
      "MultiplePathsFromGCRootsComputerImpl_FindingPaths"),
  SnapshotFactoryImpl_Error_NoParserRegistered("SnapshotFactoryImpl_Error_NoParserRegistered"),
  SnapshotFactoryImpl_Error_ReparsingHeapDump("SnapshotFactoryImpl_Error_ReparsingHeapDump"),
  SnapshotFactoryImpl_ReparsingHeapDumpAsIndexOutOfDate(
      "SnapshotFactoryImpl_ReparsingHeapDumpAsIndexOutOfDate"),
  SnapshotFactoryImpl_ReparsingHeapDumpWithOutOfDateIndex(
      "SnapshotFactoryImpl_ReparsingHeapDumpWithOutOfDateIndex"),
  SnapshotImpl_Error_DomTreeNotAvailable("SnapshotImpl_Error_DomTreeNotAvailable"),
  SnapshotImpl_Error_ObjectNotFound("SnapshotImpl_Error_ObjectNotFound"),
  SnapshotImpl_Error_ParserNotFound("SnapshotImpl_Error_ParserNotFound"),
  SnapshotImpl_Error_ReplacingNonExistentClassLoader(
      "SnapshotImpl_Error_ReplacingNonExistentClassLoader"),
  SnapshotImpl_Error_UnknownVersion("SnapshotImpl_Error_UnknownVersion"),
  SnapshotImpl_Error_UnrecognizedState("SnapshotImpl_Error_UnrecognizedState"),
  SnapshotImpl_Label("SnapshotImpl_Label"),
  SnapshotImpl_ReadingInboundReferrers("SnapshotImpl_ReadingInboundReferrers"),
  SnapshotImpl_ReadingOutboundReferrers("SnapshotImpl_ReadingOutboundReferrers"),
  SnapshotImpl_ReopeningParsedHeapDumpFile("SnapshotImpl_ReopeningParsedHeapDumpFile"),
  SnapshotImpl_RetrievingDominators("SnapshotImpl_RetrievingDominators"),
  ObjectArrayImpl_forArray("ObjectArrayImpl_forArray"),
  ObjectMarker_CalculateRetainedSize("ObjectMarker_CalculateRetainedSize"),
  PositionInputStream_mark("PositionInputStream_mark"),
  PositionInputStream_reset("PositionInputStream_reset"),
  PositionInputStream_seek("PositionInputStream_seek"),
  RetainedSizeCache_ErrorReadingRetainedSizes("RetainedSizeCache_ErrorReadingRetainedSizes"),
  RetainedSizeCache_Warning_IgnoreError("RetainedSizeCache_Warning_IgnoreError"),
  QueueInt_ZeroSizeQueue("QueueInt_ZeroSizeQueue"),;

  public final String pattern;

  Messages(String pattern) {
    this.pattern = pattern;
  }
}
