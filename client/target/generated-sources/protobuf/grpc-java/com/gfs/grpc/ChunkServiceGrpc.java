package com.gfs.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class ChunkServiceGrpc {

  private ChunkServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "com.gfs.grpc.ChunkService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.gfs.grpc.ChunkData,
      com.gfs.grpc.UploadStatus> getUploadChunkMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UploadChunk",
      requestType = com.gfs.grpc.ChunkData.class,
      responseType = com.gfs.grpc.UploadStatus.class,
      methodType = io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
  public static io.grpc.MethodDescriptor<com.gfs.grpc.ChunkData,
      com.gfs.grpc.UploadStatus> getUploadChunkMethod() {
    io.grpc.MethodDescriptor<com.gfs.grpc.ChunkData, com.gfs.grpc.UploadStatus> getUploadChunkMethod;
    if ((getUploadChunkMethod = ChunkServiceGrpc.getUploadChunkMethod) == null) {
      synchronized (ChunkServiceGrpc.class) {
        if ((getUploadChunkMethod = ChunkServiceGrpc.getUploadChunkMethod) == null) {
          ChunkServiceGrpc.getUploadChunkMethod = getUploadChunkMethod =
              io.grpc.MethodDescriptor.<com.gfs.grpc.ChunkData, com.gfs.grpc.UploadStatus>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UploadChunk"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.gfs.grpc.ChunkData.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.gfs.grpc.UploadStatus.getDefaultInstance()))
              .setSchemaDescriptor(new ChunkServiceMethodDescriptorSupplier("UploadChunk"))
              .build();
        }
      }
    }
    return getUploadChunkMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.gfs.grpc.ChunkRequest,
      com.gfs.grpc.ChunkData> getDownloadChunkMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DownloadChunk",
      requestType = com.gfs.grpc.ChunkRequest.class,
      responseType = com.gfs.grpc.ChunkData.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.gfs.grpc.ChunkRequest,
      com.gfs.grpc.ChunkData> getDownloadChunkMethod() {
    io.grpc.MethodDescriptor<com.gfs.grpc.ChunkRequest, com.gfs.grpc.ChunkData> getDownloadChunkMethod;
    if ((getDownloadChunkMethod = ChunkServiceGrpc.getDownloadChunkMethod) == null) {
      synchronized (ChunkServiceGrpc.class) {
        if ((getDownloadChunkMethod = ChunkServiceGrpc.getDownloadChunkMethod) == null) {
          ChunkServiceGrpc.getDownloadChunkMethod = getDownloadChunkMethod =
              io.grpc.MethodDescriptor.<com.gfs.grpc.ChunkRequest, com.gfs.grpc.ChunkData>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DownloadChunk"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.gfs.grpc.ChunkRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.gfs.grpc.ChunkData.getDefaultInstance()))
              .setSchemaDescriptor(new ChunkServiceMethodDescriptorSupplier("DownloadChunk"))
              .build();
        }
      }
    }
    return getDownloadChunkMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ChunkServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ChunkServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ChunkServiceStub>() {
        @java.lang.Override
        public ChunkServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ChunkServiceStub(channel, callOptions);
        }
      };
    return ChunkServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static ChunkServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ChunkServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ChunkServiceBlockingV2Stub>() {
        @java.lang.Override
        public ChunkServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ChunkServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return ChunkServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ChunkServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ChunkServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ChunkServiceBlockingStub>() {
        @java.lang.Override
        public ChunkServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ChunkServiceBlockingStub(channel, callOptions);
        }
      };
    return ChunkServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ChunkServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ChunkServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ChunkServiceFutureStub>() {
        @java.lang.Override
        public ChunkServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ChunkServiceFutureStub(channel, callOptions);
        }
      };
    return ChunkServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default io.grpc.stub.StreamObserver<com.gfs.grpc.ChunkData> uploadChunk(
        io.grpc.stub.StreamObserver<com.gfs.grpc.UploadStatus> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getUploadChunkMethod(), responseObserver);
    }

    /**
     */
    default void downloadChunk(com.gfs.grpc.ChunkRequest request,
        io.grpc.stub.StreamObserver<com.gfs.grpc.ChunkData> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDownloadChunkMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service ChunkService.
   */
  public static abstract class ChunkServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return ChunkServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service ChunkService.
   */
  public static final class ChunkServiceStub
      extends io.grpc.stub.AbstractAsyncStub<ChunkServiceStub> {
    private ChunkServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ChunkServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ChunkServiceStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<com.gfs.grpc.ChunkData> uploadChunk(
        io.grpc.stub.StreamObserver<com.gfs.grpc.UploadStatus> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncClientStreamingCall(
          getChannel().newCall(getUploadChunkMethod(), getCallOptions()), responseObserver);
    }

    /**
     */
    public void downloadChunk(com.gfs.grpc.ChunkRequest request,
        io.grpc.stub.StreamObserver<com.gfs.grpc.ChunkData> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getDownloadChunkMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service ChunkService.
   */
  public static final class ChunkServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<ChunkServiceBlockingV2Stub> {
    private ChunkServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ChunkServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ChunkServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<com.gfs.grpc.ChunkData, com.gfs.grpc.UploadStatus>
        uploadChunk() {
      return io.grpc.stub.ClientCalls.blockingClientStreamingCall(
          getChannel(), getUploadChunkMethod(), getCallOptions());
    }

    /**
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<?, com.gfs.grpc.ChunkData>
        downloadChunk(com.gfs.grpc.ChunkRequest request) {
      return io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(
          getChannel(), getDownloadChunkMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service ChunkService.
   */
  public static final class ChunkServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<ChunkServiceBlockingStub> {
    private ChunkServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ChunkServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ChunkServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public java.util.Iterator<com.gfs.grpc.ChunkData> downloadChunk(
        com.gfs.grpc.ChunkRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getDownloadChunkMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service ChunkService.
   */
  public static final class ChunkServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<ChunkServiceFutureStub> {
    private ChunkServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ChunkServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ChunkServiceFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_DOWNLOAD_CHUNK = 0;
  private static final int METHODID_UPLOAD_CHUNK = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_DOWNLOAD_CHUNK:
          serviceImpl.downloadChunk((com.gfs.grpc.ChunkRequest) request,
              (io.grpc.stub.StreamObserver<com.gfs.grpc.ChunkData>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_UPLOAD_CHUNK:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.uploadChunk(
              (io.grpc.stub.StreamObserver<com.gfs.grpc.UploadStatus>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getUploadChunkMethod(),
          io.grpc.stub.ServerCalls.asyncClientStreamingCall(
            new MethodHandlers<
              com.gfs.grpc.ChunkData,
              com.gfs.grpc.UploadStatus>(
                service, METHODID_UPLOAD_CHUNK)))
        .addMethod(
          getDownloadChunkMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.gfs.grpc.ChunkRequest,
              com.gfs.grpc.ChunkData>(
                service, METHODID_DOWNLOAD_CHUNK)))
        .build();
  }

  private static abstract class ChunkServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ChunkServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.gfs.grpc.ChunService.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ChunkService");
    }
  }

  private static final class ChunkServiceFileDescriptorSupplier
      extends ChunkServiceBaseDescriptorSupplier {
    ChunkServiceFileDescriptorSupplier() {}
  }

  private static final class ChunkServiceMethodDescriptorSupplier
      extends ChunkServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    ChunkServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (ChunkServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ChunkServiceFileDescriptorSupplier())
              .addMethod(getUploadChunkMethod())
              .addMethod(getDownloadChunkMethod())
              .build();
        }
      }
    }
    return result;
  }
}
