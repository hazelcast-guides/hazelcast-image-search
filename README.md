# Overview 

This solution features a Hazelcast Pipeline that consumes unstructured data 
(images), computes embeddings using Python, and stores them as vectors in a H
azelcast Enterprise vector collection.

The pipeline has the following high level components:
1. Directory Watcher detects the arrival of new images and creates an event 
containing the name of the new image.
2. A mapUsingPython stage in which images are retrieved and converted into 
vectors using the previously mentioned CLIP sentence transformer.
3. An Sink which stores the image vectors, along with their URLs, in
a Hazelcast vector collection.

See the blueprint below.

![BluePrint](resources/blueprint.png)

# Prerequisites

- Docker Desktop
- A Java IDE
- A Hazelcast Enterprise license key with "Advanced AI" enabled.


# 1x Setup

These steps need to be done once before starting the lab.

## 1. Download the CLIP model
The model we will be using to perform embedding is almost 500M.  To speed 
up everything that uses the model, we can download it ahead of time.

Run `docker compose run download-model`

Verify that the _models_ folder of the project has been populated.

## 2. Install your license
This docker compose bsed project is configured to read the license from 
the default docker compose property file, _.env_.

Create _.env_ (note the file name begins with a _dot_) in the project base 
directory.  Set the _HZ_LICENSEKEY_ variable to your license, as shown below.
```
HZ_LICENSEKEY=Your-License-Here
```

## 3. Build everything

```
mvn clean package
```

## 4. Make sure Docker Desktop has enough resources

I used 24G and 8 CPU.  I'm sure 8G would be enough though. 

# Walk Through

The Hazelcast instance, including the vector collection and index are 
configured in _hazelcast.yaml_

Start a Hazelcast instance, MC, the image web server and a Jupyter notebook
```
docker compose up -d
```

You can access MC at http://localhost:8080


Deploy the image loader job
```
docker compose run submit-image-loader-solution
```
This submits the pipeline defined in _image-ingest-pipeline/.../ImageIngestPipelineSolution.java_

That pipeline will watch the _www_ directory and, when files are added,
pass the filename to a _mapUsingPython_ stage that will pull the image 
from the www sever and embed it.   The python portion is in the 
_image-embedding-service_ folder.

Wait for the Pipeline to fully deploy, check the hazelcast logs with 
`docker compose logs --follow hz` and verify that something like the following 
is present.

```
hazelcast-image-search-hz-1  | 2024-07-19 15:09:01,342 [ INFO] [hz.sharp_meitner.cached.thread-5] [c.h.j.python]: [172.23.0.4]:5701 [dev] [5.5.0-SNAPSHOT] Started Python process: 203
hazelcast-image-search-hz-1  | 2024-07-19 15:09:04,053 [ INFO] [hz.sharp_meitner.cached.thread-9] [c.h.j.python]: [172.23.0.4]:5701 [dev] [5.5.0-SNAPSHOT] Python process 204 listening on port 33087
```

This shows that the Python service is fully initialized.  Continue to 
watch the logs while you perform the next step.

Copy ALL of the images in the _images_ folder into the _www_ folder.   After a 
short while, the job undeploys with a message like the one below.

```
Exception in ProcessorTasklet{0c07-23ae-f400-0001/Compute Embedding#0}: com.hazelcast.jet.JetException: Async operation completed exceptionally: java.util.concurrent.ExecutionException: com.hazelcast.jet.grpc.impl.StatusRuntimeExceptionJet: RESOURCE_EXHAUSTED: gRPC message exceeds maximum size 4194304: 4616029
	at com.hazelcast.jet.impl.execution.TaskletExecutionService.handleTaskletExecutionError(TaskletExecutionService.java:289)
	at com.hazelcast.jet.impl.execution.TaskletExecutionService$CooperativeWorker.runTasklet(TaskletExecutionService.java:413)
	at java.base/java.util.concurrent.CopyOnWriteArrayList.forEach(CopyOnWriteArrayList.java:891)
	at com.hazelcast.jet.impl.execution.TaskletExecutionService$CooperativeWorker.run(TaskletExecutionService.java:372)
	at java.base/java.lang.Thread.run(Thread.java:1583)
Caused by: com.hazelcast.jet.impl.execution.TaskletExecutionException: Exception in ProcessorTasklet{0c07-23ae-f400-0001/Compute Embedding#0}: com.hazelcast.jet.JetException: Async operation completed exceptionally: java.util.concurrent.ExecutionException: com.hazelcast.jet.grpc.impl.StatusRuntimeExceptionJet: RESOURCE_EXHAUSTED: gRPC message exceeds maximum size 4194304: 4616029
	at com.hazelcast.jet.impl.execution.TaskletExecutionService.handleTaskletExecutionError(TaskletExecutionService.java:288)
	... 4 more
Caused by: com.hazelcast.jet.JetException: Async operation completed exceptionally: java.util.concurrent.ExecutionException: com.hazelcast.jet.grpc.impl.StatusRuntimeExceptionJet: RESOURCE_EXHAUSTED: gRPC message exceeds maximum size 4194304: 4616029
	at com.hazelcast.jet.impl.processor.AsyncTransformUsingServiceOrderedP.tryFlushQueue(AsyncTransformUsingServiceOrderedP.java:205)
	at com.hazelcast.jet.impl.processor.AsyncTransformUsingServiceOrderedP.makeRoomInQueue(AsyncTransformUsingServiceOrderedP.java:118)
	at com.hazelcast.jet.impl.processor.AsyncTransformUsingServiceBatchedP.process(AsyncTransformUsingServiceBatchedP.java:67)
	at com.hazelcast.jet.impl.execution.ProcessorTasklet.lambda$processInbox$2f647568$2(ProcessorTasklet.java:490)
	at com.hazelcast.jet.function.RunnableEx.run(RunnableEx.java:31)
	at com.hazelcast.jet.impl.util.Util.doWithClassLoader(Util.java:586)
	at com.hazelcast.jet.impl.execution.ProcessorTasklet.processInbox(ProcessorTasklet.java:490)
	at com.hazelcast.jet.impl.execution.ProcessorTasklet.stateMachineStep(ProcessorTasklet.java:341)
	at com.hazelcast.jet.impl.execution.ProcessorTasklet.call(ProcessorTasklet.java:291)
	at com.hazelcast.jet.impl.execution.TaskletExecutionService$CooperativeWorker.runTasklet(TaskletExecutionService.java:407)
	... 3 more
Caused by: java.util.concurrent.ExecutionException: com.hazelcast.jet.grpc.impl.StatusRuntimeExceptionJet: RESOURCE_EXHAUSTED: gRPC message exceeds maximum size 4194304: 4616029
	at java.base/java.util.concurrent.CompletableFuture.reportGet(CompletableFuture.java:396)
	at java.base/java.util.concurrent.CompletableFuture.get(CompletableFuture.java:2073)
	at com.hazelcast.jet.impl.processor.AsyncTransformUsingServiceOrderedP.tryFlushQueue(AsyncTransformUsingServiceOrderedP.java:200)
	... 12 more
Caused by: com.hazelcast.jet.grpc.impl.StatusRuntimeExceptionJet: RESOURCE_EXHAUSTED: gRPC message exceeds maximum size 4194304: 4616029
	at io.grpc.Status.asRuntimeException(Status.java:537)
	at io.grpc.stub.ClientCalls$StreamObserverToCallListenerAdapter.onClose(ClientCalls.java:481)
	at io.grpc.PartialForwardingClientCallListener.onClose(PartialForwardingClientCallListener.java:39)
	at io.grpc.ForwardingClientCallListener.onClose(ForwardingClientCallListener.java:23)
	at io.grpc.ForwardingClientCallListener$SimpleForwardingClientCallListener.onClose(ForwardingClientCallListener.java:40)
	at io.grpc.census.CensusStatsModule$StatsClientInterceptor$1$1.onClose(CensusStatsModule.java:814)
	at io.grpc.PartialForwardingClientCallListener.onClose(PartialForwardingClientCallListener.java:39)
	at io.grpc.ForwardingClientCallListener.onClose(ForwardingClientCallListener.java:23)
	at io.grpc.ForwardingClientCallListener$SimpleForwardingClientCallListener.onClose(ForwardingClientCallListener.java:40)
	at io.grpc.census.CensusTracingModule$TracingClientInterceptor$1$1.onClose(CensusTracingModule.java:494)
	at io.grpc.internal.DelayedClientCall$DelayedListener$3.run(DelayedClientCall.java:489)
	at io.grpc.internal.DelayedClientCall$DelayedListener.delayOrExecute(DelayedClientCall.java:453)
	at io.grpc.internal.DelayedClientCall$DelayedListener.onClose(DelayedClientCall.java:486)
	at io.grpc.internal.ClientCallImpl.closeObserver(ClientCallImpl.java:574)
	at io.grpc.internal.ClientCallImpl.access$300(ClientCallImpl.java:72)
	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1StreamClosed.runInternal(ClientCallImpl.java:742)
	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1StreamClosed.runInContext(ClientCallImpl.java:723)
	at io.grpc.internal.ContextRunnable.run(ContextRunnable.java:37)
	at io.grpc.internal.SerializingExecutor.run(SerializingExecutor.java:133)
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)
	... 1 more

```

Even though the message says something about resource exhaustion, I'm fairly 
sure this was not caused by CPU, memory of GC problems.  Check out this 
[video](watchme.mp4) of the MC dashboard while reproducing the problem.

I think this is just caused by trying to batch too many events together in 
one invocation of the grpc service, causing the message to exceed the 
maximum message size.  We probably need some strategy to 
limit the maximum size of a batch, based on the message size.

BTW, in this case the events just contain the names of the modified files, 
not the file contents.

