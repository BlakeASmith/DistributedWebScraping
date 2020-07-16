interface MasterGrpcClient {
    suspend fun requestWork(jobRequest: App.JobRequest): App.Job
    suspend fun completeWork(job: App.JobResult): App.JobCompletion
}

fun job(id: Int, urls: List<String>, type: App.Job.JobType): App.Job =
    App.Job.newBuilder()
        .addAllUrls(urls)
        .setId(id)
        .setType(type)
        .build()

fun jobRequest(id: Int): App.JobRequest = App.JobRequest.newBuilder().setId(id).build()

fun jobResult(job: App.Job, complete: Boolean): App.JobResult = App.JobResult.newBuilder()
    .setJob(job).setComplete(complete).build()


