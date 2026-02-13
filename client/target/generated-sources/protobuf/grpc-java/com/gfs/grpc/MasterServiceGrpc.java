package com.gfs.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class MasterServiceGrpc {

  private MasterServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "com.gfs.grpc.MasterService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.gfs.grpc.FileRequest,
      com.gfs.grpc.ChunkLocationResponse> getGetUploadLocationMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetUploadLocation",
      requestType = com.gfs.grpc.FileRequest.class,
      responseType = com.gfs.grpc.ChunkLocationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.gfs.grpc.FileRequest,
      com.gfs.grpc.ChunkLocationResponse> getGetUploadLocationMethod() {
    io.grpc.MethodDescriptor<com.gfs.grpc.FileRequest, com.gfs.grpc.ChunkLocationResponse> getGetUploadLocationMethod;
    if ((getGetUploadLocationMethod = MasterServiceGrpc.getGetUploadLocationMethod) == null) {
      synchronized (MasterServiceGrpc.class) {
        if ((getGetUploadLocationMethod = MasterServiceGrpc.getGetUploadLocationMethod) == null) {
          MasterServiceGrpc.getGetUploadLocationMethod = getGetUploadLocationMethod =
              io.grpc.MethodDescriptor.<com.gfs.grpc.FileRequest, com.gfs.grpc.ChunkLocationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetUploadLocation"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.gfs.grpc.FileRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.gfs.grpc.ChunkLocationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new MasterServiceMethodDescriptorSupplier("GetUploadLocation"))
              .build();
        }
      }
    }
    return getGetUploadLocationMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.gfs.grpc.ChunkRequest,
      com.gfs.grpc.ChunkLocationList> getGetDownloadLocationsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetDownloadLocations",
      requestType = com.gfs.grpc.ChunkRequest.class,
      responseType = com.gfs.grpc.ChunkLocationList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.gfs.grpc.ChunkRequest,
      com.gfs.grpc.ChunkLocationList> getGetDownloadLocationsMethod() {
    io.grpc.MethodDescriptor<com.gfs.grpc.ChunkRequest, com.gfs.grpc.ChunkLocationList> getGetDownloadLocationsMethod;
    if ((getGetDownloadLocationsMethod = MasterServiceGrpc.getGetDownloadLocationsMethod) == null) {
      synchronized (MasterServiceGrpc.class) {
        if ((getGetDownloadLocationsMethod = MasterServiceGrpc.getGetDownloadLocationsMethod) == null) {
          MasterServiceGrpc.getGetDownloadLocationsMethod = getGetDownloadLocationsMethod =
              io.grpc.MethodDescriptor.<com.gfs.grpc.ChunkRequest, com.gfs.grpc.ChunkLocationList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetDownloadLocations"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.gfs.grpc.ChunkRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.gfs.grpc.ChunkLocationList.getDefaultInstance()))
              .setSchemaDescriptor(new MasterServiceMethodDescriptorSupplier("GetDownloadLocations"))
              .build();
        }
      }
    }
    return getGetDownloadLocationsMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static MasterServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MasterServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MasterServiceStub>() {
        @java.lang.Override
        public MasterServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MasterServiceStub(channel, callOptions);
        }
      };
    return MasterServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static MasterServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MasterServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MasterServiceBlockingV2Stub>() {
        @java.lang.Override
        public MasterServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MasterServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return MasterServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static MasterServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MasterServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MasterServiceBlockingStub>() {
        @java.lang.Override
        public MasterServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MasterServiceBlockingStub(channel, callOptions);
        }
      };
    return MasterServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static MasterServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MasterServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MasterServiceFutureStub>() {
        @java.lang.Override
        public MasterServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MasterServiceFutureStub(channel, callOptions);
        }
      };
    return MasterServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void getUploadLocation(com.gfs.grpc.FileRequest request,
        io.grpc.stub.StreamObserver<com.gfs.grpc.ChunkLocationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetUploadLocationMethod(), responseObserver);
    }

    /**
     */
    default void getDownloadLocations(com.gfs.grpc.ChunkRequest request,
        io.grpc.stub.StreamObserver<com.gfs.grpc.ChunkLocationList> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetDownloadLocationsMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service MasterService.
   */
  public static abstract class MasterServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return MasterServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service MasterService.
   */
  public static final class MasterServiceStub
      extends io.grpc.stub.AbstractAsyncStub<MasterServiceStub> {
    private MasterServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MasterServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MasterServiceStub(channel, callOptions);
    }

    /**
     */
    public void getUploadLocation(com.gfs.grpc.FileRequest request,
        io.grpc.stub.StreamObserver<com.gfs.grpc.ChunkLocationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetUploadLocationMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getDownloadLocations(com.gfs.grpc.ChunkRequest request,
        io.grpc.stub.StreamObserver<com.gfs.grpc.ChunkLocationList> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetDownloadLocationsMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service MasterService.
   */
  public static final class MasterServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<MasterServiceBlockingV2Stub> {
    private MasterServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MasterServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MasterServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     */
    public com.gfs.grpc.ChunkLocationResponse getUploadLocation(com.gfs.grpc.FileRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetUploadLocationMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.gfs.grpc.ChunkLocationList getDownloadLocations(com.gfs.grpc.ChunkRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetDownloadLocationsMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service MasterService.
   */
  public static final class MasterServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<MasterServiceBlockingStub> {
    private MasterServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MasterServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MasterServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.gfs.grpc.ChunkLocationResponse getUploadLocation(com.gfs.grpc.FileRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetUploadLocationMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.gfs.grpc.ChunkLocationList getDownloadLocations(com.gfs.grpc.ChunkRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetDownloadLocationsMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service MasterService.
   */
  public static final class MasterServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<MasterServiceFutureStub> {
    private MasterServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MasterServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MasterServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.gfs.grpc.ChunkLocationResponse> getUploadLocation(
        com.gfs.grpc.FileRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetUploadLocationMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.gfs.grpc.ChunkLocationList> getDownloadLocations(
        com.gfs.grpc.ChunkRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetDownloadLocationsMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_UPLOAD_LOCATION = 0;
  private static final int METHODID_GET_DOWNLOAD_LOCATIONS = 1;

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
        case METHODID_GET_UPLOAD_LOCATION:
          serviceImpl.getUploadLocation((com.gfs.grpc.FileRequest) request,
              (io.grpc.stub.StreamObserver<com.gfs.grpc.ChunkLocationResponse>) responseObserver);
          break;
        case METHODID_GET_DOWNLOAD_LOCATIONS:
          serviceImpl.getDownloadLocations((com.gfs.grpc.ChunkRequest) request,
              (io.grpc.stub.StreamObserver<com.gfs.grpc.ChunkLocationList>) responseObserver);
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
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getGetUploadLocationMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.gfs.grpc.FileRequest,
              com.gfs.grpc.ChunkLocationResponse>(
                service, METHODID_GET_UPLOAD_LOCATION)))
        .addMethod(
          getGetDownloadLocationsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.gfs.grpc.ChunkRequest,
              com.gfs.grpc.ChunkLocationList>(
                service, METHODID_GET_DOWNLOAD_LOCATIONS)))
        .build();
  }

  private static abstract class MasterServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    MasterServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.gfs.grpc.ChunService.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("MasterService");
    }
  }

  private static final class MasterServiceFileDescriptorSupplier
      extends MasterServiceBaseDescriptorSupplier {
    MasterServiceFileDescriptorSupplier() {}
  }

  private static final class MasterServiceMethodDescriptorSupplier
      extends MasterServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    MasterServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (MasterServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new MasterServiceFileDescriptorSupplier())
              .addMethod(getGetUploadLocationMethod())
              .addMethod(getGetDownloadLocationsMethod())
              .build();
        }
      }
    }
    return result;
  }
}
