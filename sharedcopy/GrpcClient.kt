import MasterGrpc.*
import io.grpc.Channel

interface GrpcClient {
    abstract suspend fun requestWork(jobRequest: App.JobRequest): App.Job
    abstract suspend fun completeWork(job: App.Job): App.JobCompletion
}