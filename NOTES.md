# Problems Encountered While Attempting to Encode Images via Pipeline

The goal is to use a pipeline to encode images via a pre-trained model, CLIP, which 
facilitates text/image similarity (e.g. show me "monkeys").  Here are the 
difficulties encountered along the way.

1. __`FileSources` doesn't provide file name.__  I have a bunch of image
files that I want to encode and add to a vector collection.  Assume the image 
has been vectorized and the corresponding vector loaded. When I do a similarity 
 search, I will need some way to get back to the original file so I can display 
the image. With the current implementation of `FileSources`, I couldn't find any way
to obtain the filename from pipeline code.  I made a custom
source that returns `Tuple2<String, byte[]>` so that the name could eventually 
be included in a vector document.
2. Not wanting to incur the penalty of moving image bytes into and out of python via
"mapUsingPython", which only takes `str` input, I decided to try DJL 
(https://docs.djl.ai/), which will run pre-trained models in Java via "engines" 
_using native libraries_.  This approach did show some promise but, the __second 
time__ the pipeline was deployed, I got the error below.  It appears that a class
loader from a previous job was hanging on to the native libraries.  This 
https://hazelcast.atlassian.net/browse/ESC-10 is probably related. DJL is 
actively being developed by Amazon and seems to be a very promising way 
to do inference in Java.  It would be great is we could directly support 
it.  DJL has a serving capability and I thought about building a sidecar 
based implementation that could be accessed via "mapUSingService" but 
that seems more like a product feature than a code sample. 

```
6 of 10 tasklets failed to initialize. One of the failures is attached as the cause and its summary is ai.djl.engine.EngineException: Failed to load PyTorch native library
at com.hazelcast.jet.impl.execution.TaskletExecutionService.awaitAll(TaskletExecutionService.java:235)
at com.hazelcast.jet.impl.execution.TaskletExecutionService.submitCooperativeTasklets(TaskletExecutionService.java:196)
at com.hazelcast.jet.impl.execution.TaskletExecutionService.beginExecute(TaskletExecutionService.java:154)
at com.hazelcast.jet.impl.execution.ExecutionContext.beginExecution(ExecutionContext.java:233)
at com.hazelcast.jet.impl.JobExecutionService.beginExecution0(JobExecutionService.java:568)
at com.hazelcast.jet.impl.JobExecutionService.beginExecution(JobExecutionService.java:563)
at com.hazelcast.jet.impl.operation.StartExecutionOperation.doRun(StartExecutionOperation.java:50)
at com.hazelcast.jet.impl.operation.AsyncOperation.run(AsyncOperation.java:55)
at com.hazelcast.spi.impl.operationservice.Operation.call(Operation.java:192)
at com.hazelcast.spi.impl.operationservice.impl.OperationRunnerImpl.call(OperationRunnerImpl.java:291)
at com.hazelcast.spi.impl.operationservice.impl.OperationRunnerImpl.run(OperationRunnerImpl.java:262)
at com.hazelcast.spi.impl.operationservice.impl.OperationRunnerImpl.run(OperationRunnerImpl.java:216)
at com.hazelcast.spi.impl.operationexecutor.impl.OperationExecutorImpl.run(OperationExecutorImpl.java:459)
at com.hazelcast.spi.impl.operationexecutor.impl.OperationExecutorImpl.runOrExecute(OperationExecutorImpl.java:519)
at com.hazelcast.spi.impl.operationservice.impl.Invocation.doInvokeLocal(Invocation.java:617)
at com.hazelcast.spi.impl.operationservice.impl.Invocation.doInvoke(Invocation.java:596)
at com.hazelcast.spi.impl.operationservice.impl.Invocation.invoke0(Invocation.java:557)
at com.hazelcast.spi.impl.operationservice.impl.Invocation.invoke(Invocation.java:240)
at com.hazelcast.spi.impl.operationservice.impl.InvocationBuilderImpl.invoke(InvocationBuilderImpl.java:71)
at com.hazelcast.jet.impl.MasterContext.invokeOnParticipant(MasterContext.java:479)
at com.hazelcast.jet.impl.MasterContext.invokeOnParticipants(MasterContext.java:462)
at com.hazelcast.jet.impl.MasterContext.invokeOnParticipants(MasterContext.java:433)
at com.hazelcast.jet.impl.MasterJobContext.invokeStartExecution(MasterJobContext.java:638)
at com.hazelcast.jet.impl.MasterJobContext.lambda$onInitStepCompleted$10(MasterJobContext.java:606)
at com.hazelcast.jet.impl.JobCoordinationService.lambda$submitToCoordinatorThread$60(JobCoordinationService.java:1501)
at com.hazelcast.jet.impl.JobCoordinationService.lambda$submitToCoordinatorThread$61(JobCoordinationService.java:1522)
at com.hazelcast.internal.util.executor.CompletableFutureTask.run(CompletableFutureTask.java:64)
at com.hazelcast.internal.util.executor.CachedExecutorServiceDelegate$Worker.run(CachedExecutorServiceDelegate.java:220)
at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)
at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)
at java.base/java.lang.Thread.run(Thread.java:1583)
at com.hazelcast.internal.util.executor.HazelcastManagedThread.executeRun(HazelcastManagedThread.java:76)
at com.hazelcast.internal.util.executor.PoolExecutorThreadFactory$ManagedThread.executeRun(PoolExecutorThreadFactory.java:74)
at com.hazelcast.internal.util.executor.HazelcastManagedThread.run(HazelcastManagedThread.java:111)
Caused by: com.hazelcast.jet.impl.execution.TaskletExecutionException: 6 of 10 tasklets failed to initialize. One of the failures is attached as the cause and its summary is ai.djl.engine.EngineException: Failed to load PyTorch native library
at com.hazelcast.jet.impl.execution.TaskletExecutionService.awaitAll(TaskletExecutionService.java:234)
... 33 more
Caused by: ai.djl.engine.EngineException: Failed to load PyTorch native library
at ai.djl.pytorch.engine.PtEngine.newInstance(PtEngine.java:90)
at ai.djl.pytorch.engine.PtEngineProvider.getEngine(PtEngineProvider.java:41)
at ai.djl.engine.Engine.getEngine(Engine.java:190)
at ai.djl.Model.newInstance(Model.java:99)
at ai.djl.repository.zoo.BaseModelLoader.createModel(BaseModelLoader.java:196)
at ai.djl.repository.zoo.BaseModelLoader.loadModel(BaseModelLoader.java:159)
at ai.djl.repository.zoo.Criteria.loadModel(Criteria.java:174)
at hazelcast.platform.labs.image.similarity.ImageEncoder.<init>(ImageEncoder.java:49)
at hazelcast.platform.labs.image.similarity.ImageEncoder.newInstance(ImageEncoder.java:33)
at hazelcast.platform.labs.image.similarity.ImageEncodePipeline.lambda$createPipeline$e830f62$1(ImageEncodePipeline.java:82)
at com.hazelcast.security.impl.function.SecuredFunctions$7.applyEx(SecuredFunctions.java:191)
at com.hazelcast.security.impl.function.SecuredFunctions$7.applyEx(SecuredFunctions.java:185)
at com.hazelcast.function.BiFunctionEx.apply(BiFunctionEx.java:47)
at com.hazelcast.jet.impl.processor.AbstractTransformUsingServiceP.init(AbstractTransformUsingServiceP.java:44)
at com.hazelcast.jet.core.AbstractProcessor.init(AbstractProcessor.java:82)
at com.hazelcast.jet.impl.execution.ProcessorTasklet.lambda$init$2f647568$1(ProcessorTasklet.java:279)
at com.hazelcast.jet.function.RunnableEx.run(RunnableEx.java:31)
at com.hazelcast.jet.impl.util.Util.doWithClassLoader(Util.java:586)
at com.hazelcast.jet.impl.execution.ProcessorTasklet.init(ProcessorTasklet.java:279)
at com.hazelcast.jet.function.RunnableEx.run(RunnableEx.java:31)
at com.hazelcast.jet.impl.util.Util.doWithClassLoader(Util.java:586)
at com.hazelcast.jet.impl.execution.TaskletExecutionService.lambda$submitCooperativeTasklets$6(TaskletExecutionService.java:194)
at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:572)
... 8 more
Caused by: java.lang.UnsatisfiedLinkError: Native Library /tmp/.djl.ai/pytorch/2.2.2-cpu-precxx11-linux-aarch64/libstdc++.so.6 already loaded in another classloader
at java.base/jdk.internal.loader.NativeLibraries.loadLibrary(NativeLibraries.java:167)
at java.base/jdk.internal.loader.NativeLibraries.loadLibrary(NativeLibraries.java:139)
at java.base/java.lang.ClassLoader.loadLibrary(ClassLoader.java:2418)
at java.base/java.lang.Runtime.load0(Runtime.java:852)
at java.base/java.lang.System.load(System.java:2025)
at ai.djl.pytorch.jni.LibUtils.loadNativeLibrary(LibUtils.java:379)
at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.accept(ForEachOps.java:184)
at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
at java.base/java.util.stream.SortedOps$RefSortingSink.end(SortedOps.java:395)
at java.base/java.util.stream.Sink$ChainedReference.end(Sink.java:261)
at java.base/java.util.stream.Sink$ChainedReference.end(Sink.java:261)
at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:510)
at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
at java.base/java.util.stream.ForEachOps$ForEachOp.evaluateSequential(ForEachOps.java:151)
at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.evaluateSequential(ForEachOps.java:174)
at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234)
at java.base/java.util.stream.ReferencePipeline.forEach(ReferencePipeline.java:596)
at ai.djl.pytorch.jni.LibUtils.loadLibTorch(LibUtils.java:170)
at ai.djl.pytorch.jni.LibUtils.loadLibrary(LibUtils.java:82)
at ai.djl.pytorch.engine.PtEngine.newInstance(PtEngine.java:53)
... 30 more
```
