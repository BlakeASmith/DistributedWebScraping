import MasterGrpc.*
import io.grpc.Channel

interface GrpcClient {
    fun requestWork(jobRequest: App.JobRequest): App.Job
    fun completeWork(job: App.JobResult): App.JobCompletion
}