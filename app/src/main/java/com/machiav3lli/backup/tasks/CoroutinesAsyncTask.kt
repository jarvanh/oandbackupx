package com.machiav3lli.backup.tasks

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/* adapted from with small changes to fit our usage:
 * https://github.com/ladrahul25/CoroutineAsyncTask/blob/master/app/src/main/java/com/example/background/CoroutinesAsyncTask.kt
 */
abstract class CoroutinesAsyncTask<Params, Progress, Result> {

    enum class Status {
        PENDING,
        RUNNING,
        FINISHED
    }

    var status: Status = Status.PENDING
    abstract fun doInBackground(vararg params: Params?): Result?
    open fun onProgressUpdate(vararg values: Progress?) {}
    open fun onPostExecute(result: Result?) {}
    open fun onPreExecute() {}
    open fun onCancelled(result: Result?) {}
    private var isCancelled = false

    fun execute(vararg params: Params) {
        when (status) {
            Status.RUNNING -> throw IllegalStateException("Cannot execute task:${this.javaClass.name} the task is already running.")
            Status.FINISHED -> throw IllegalStateException("Cannot execute task: ${this.javaClass.name}"
                    + " the task has already been executed (a task can be executed only once)")
            Status.PENDING -> status = Status.RUNNING
        }

        // it can be used to setup UI - it should have access to Main Thread
        GlobalScope.launch(Dispatchers.Main) {
            onPreExecute()
        }

        // doInBackground works on background thread(default)
        GlobalScope.launch(Dispatchers.Default) {
            val result = doInBackground(*params)
            status = Status.FINISHED
            withContext(Dispatchers.Main) {
                // onPostExecute works on main thread to show output
                Timber.d("after do in back ${status.name}--$isCancelled")
                if (!isCancelled) {
                    onPostExecute(result)
                }
            }
        }
    }

    fun cancel(mayInterruptIfRunning: Boolean) {
        isCancelled = true
        status = Status.FINISHED
        GlobalScope.launch(Dispatchers.Main) {
            // onPostExecute works on main thread to show output
            Timber.d("after cancel ${status.name}--$isCancelled")
            onPostExecute(null)
        }
    }

    fun publishProgress(vararg progress: Progress) {
        //need to update main thread
        GlobalScope.launch(Dispatchers.Main) {
            if (!isCancelled) {
                onProgressUpdate(*progress)
            }
        }
    }
}