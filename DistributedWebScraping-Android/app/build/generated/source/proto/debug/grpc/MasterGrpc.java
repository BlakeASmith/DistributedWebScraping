import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.25.0)",
    comments = "Source: app.proto")
public final class MasterGrpc {

  private MasterGrpc() {}

  public static final String SERVICE_NAME = "Master";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<App.JobRequest,
      App.Job> getRequestJobMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RequestJob",
      requestType = App.JobRequest.class,
      responseType = App.Job.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<App.JobRequest,
      App.Job> getRequestJobMethod() {
    io.grpc.MethodDescriptor<App.JobRequest, App.Job> getRequestJobMethod;
    if ((getRequestJobMethod = MasterGrpc.getRequestJobMethod) == null) {
      synchronized (MasterGrpc.class) {
        if ((getRequestJobMethod = MasterGrpc.getRequestJobMethod) == null) {
          MasterGrpc.getRequestJobMethod = getRequestJobMethod =
              io.grpc.MethodDescriptor.<App.JobRequest, App.Job>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RequestJob"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  App.JobRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  App.Job.getDefaultInstance()))
              .build();
        }
      }
    }
    return getRequestJobMethod;
  }

  private static volatile io.grpc.MethodDescriptor<App.JobResult,
      App.JobCompletion> getCompleteJobMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CompleteJob",
      requestType = App.JobResult.class,
      responseType = App.JobCompletion.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<App.JobResult,
      App.JobCompletion> getCompleteJobMethod() {
    io.grpc.MethodDescriptor<App.JobResult, App.JobCompletion> getCompleteJobMethod;
    if ((getCompleteJobMethod = MasterGrpc.getCompleteJobMethod) == null) {
      synchronized (MasterGrpc.class) {
        if ((getCompleteJobMethod = MasterGrpc.getCompleteJobMethod) == null) {
          MasterGrpc.getCompleteJobMethod = getCompleteJobMethod =
              io.grpc.MethodDescriptor.<App.JobResult, App.JobCompletion>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CompleteJob"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  App.JobResult.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  App.JobCompletion.getDefaultInstance()))
              .build();
        }
      }
    }
    return getCompleteJobMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static MasterStub newStub(io.grpc.Channel channel) {
    return new MasterStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static MasterBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new MasterBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static MasterFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new MasterFutureStub(channel);
  }

  /**
   */
  public static abstract class MasterImplBase implements io.grpc.BindableService {

    /**
     */
    public void requestJob(App.JobRequest request,
        io.grpc.stub.StreamObserver<App.Job> responseObserver) {
      asyncUnimplementedUnaryCall(getRequestJobMethod(), responseObserver);
    }

    /**
     */
    public void completeJob(App.JobResult request,
        io.grpc.stub.StreamObserver<App.JobCompletion> responseObserver) {
      asyncUnimplementedUnaryCall(getCompleteJobMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getRequestJobMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                App.JobRequest,
                App.Job>(
                  this, METHODID_REQUEST_JOB)))
          .addMethod(
            getCompleteJobMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                App.JobResult,
                App.JobCompletion>(
                  this, METHODID_COMPLETE_JOB)))
          .build();
    }
  }

  /**
   */
  public static final class MasterStub extends io.grpc.stub.AbstractStub<MasterStub> {
    private MasterStub(io.grpc.Channel channel) {
      super(channel);
    }

    private MasterStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MasterStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new MasterStub(channel, callOptions);
    }

    /**
     */
    public void requestJob(App.JobRequest request,
        io.grpc.stub.StreamObserver<App.Job> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRequestJobMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void completeJob(App.JobResult request,
        io.grpc.stub.StreamObserver<App.JobCompletion> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCompleteJobMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class MasterBlockingStub extends io.grpc.stub.AbstractStub<MasterBlockingStub> {
    private MasterBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private MasterBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MasterBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new MasterBlockingStub(channel, callOptions);
    }

    /**
     */
    public App.Job requestJob(App.JobRequest request) {
      return blockingUnaryCall(
          getChannel(), getRequestJobMethod(), getCallOptions(), request);
    }

    /**
     */
    public App.JobCompletion completeJob(App.JobResult request) {
      return blockingUnaryCall(
          getChannel(), getCompleteJobMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class MasterFutureStub extends io.grpc.stub.AbstractStub<MasterFutureStub> {
    private MasterFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private MasterFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MasterFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new MasterFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<App.Job> requestJob(
        App.JobRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getRequestJobMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<App.JobCompletion> completeJob(
        App.JobResult request) {
      return futureUnaryCall(
          getChannel().newCall(getCompleteJobMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_REQUEST_JOB = 0;
  private static final int METHODID_COMPLETE_JOB = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final MasterImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(MasterImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_REQUEST_JOB:
          serviceImpl.requestJob((App.JobRequest) request,
              (io.grpc.stub.StreamObserver<App.Job>) responseObserver);
          break;
        case METHODID_COMPLETE_JOB:
          serviceImpl.completeJob((App.JobResult) request,
              (io.grpc.stub.StreamObserver<App.JobCompletion>) responseObserver);
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

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (MasterGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .addMethod(getRequestJobMethod())
              .addMethod(getCompleteJobMethod())
              .build();
        }
      }
    }
    return result;
  }
}
