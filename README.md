# sample-coroutines-basics
This app was build following Google Developers Codelabs tutorials. The app demonstrates:
Use coroutines with Architecture Components.
Use coroutines with Retrofit
Best practices for testing coroutines.


# CheatSheet

### Blocking the main thread
+ On Android, it's essential to avoid blocking the main thread. The main thread is a <b>single thread that handles all updates to the UI</b>. It's also the thread that calls all click handlers and other UI callbacks. As such, it has to run smoothly to guarantee a great user experience.

+ For your app to display to the user without any visible pauses, the main thread has to </b> update the screen every 16ms or more</b>, which is about 60 frames per second. Many common tasks take longer than this, such as <b>parsing large JSON datasets, writing data to a database, or fetching data from the network</b>. Therefore, calling code like this from the main thread can cause the app to pause, stutter, or even freeze. And if you block the main thread for too long, the app may even crash and present an <b>Application Not Responding</b> dialog.

### The callback pattern
+ One pattern for performing long-running tasks without blocking the main thread is callbacks. 
+ By using callbacks, you can start long-running tasks on a <b>background thread</b>. When the task completes, the callback is called to inform you of the result on the main thread.
+ Cons of callback pattern:
  a) Code that heavily uses callbacks can become hard to read, 
  b) Callbacks don't allow the use of some language features.

### Using coroutines to remove callbacks
+ Kotlin coroutines let you convert callback-based code to <b>sequential code</b>. 
+ Coroutine code accomplishes the same result of unblocking the current thread as callback pattern with <b>less code</b>.
+ Due to its sequential style, it's easy to chain several long running tasks without creating multiple callbacks
+ It's easier to read, and can use language features such as exceptions.


### Callback pattern vs coroutines
+ Below there is a comparison of making network request and saving data to database using callbacks and coroutines

  1. Callback-based implementation:

```
fun refreshTitleWithCallbacks(titleRefreshCallback: TitleRefreshCallback) {
   BACKGROUND.submit {
       try {
           // Make network request using a blocking call
           val result = network.fetchNextTitle().execute()
           if (result.isSuccessful) {
               // Save it to database
               titleDao.insertTitle(Title(result.body()!!))
               // Inform the caller the refresh is completed
               titleRefreshCallback.onCompleted()
           } else {
               // If it's not successful, inform the callback of the error
               titleRefreshCallback.onError(
                       TitleRefreshError("Unable to refresh title", null))
           }
       } catch (cause: Throwable) {
           // If anything throws an exception, inform the caller
           titleRefreshCallback.onError(
                   TitleRefreshError("Unable to refresh title", cause))
       }
   }
}
```

  2. Coroutines implementation fo the same long runing tasks:

```
 suspend fun refreshTitle() {
        try {
            // Make network request
            val result = network.fetchNextTitle()
            // Save it to database
            titleDao.insertTitle(Title(result))
        } catch (t: Throwable) {
            // If anything throws an exception, inform the caller
            throw TitleRefreshError("Unable to refresh title", t)
        }
    }
```

### CoroutineScope
+ In Kotlin, all coroutines run inside a CoroutineScope.
+ A scope controls the <b>lifetime of coroutines through its job</b>. When you cancel the job of a scope, it cancels all coroutines started in that scope. 
+ On Android, you can use a scope to cancel all running coroutines when, for example, the user navigates away from an Activity or Fragment.
+ Scopes also allow you to specify a default dispatcher. 

### Dispatcher
+ A dispatcher controls <b>which thread runs a coroutine</b>.
+ For coroutines started by the UI, it is typically correct to start them on <b>Dispatchers.Main</b> which is the main thread on Android.
+ A coroutine started on Dispatchers.Main <b>won't block the main thread while suspended</b>. Since a ViewModel coroutine almost always updates the UI on the main thread, starting coroutines on the main thread saves you extra thread switches. 
+ A coroutine started on the Main thread <b>can switch dispatchers any time after it's started</b>. For example, it can use another dispatcher to parse a large JSON result off the main thread.
+ Because coroutines can easily switch threads at any time and pass results back to the original thread, it's a good idea to start UI-related coroutines on the Main thread.
+ Libraries like Room and Retrofit offer main-safety out of the box when using coroutines, so you don't need to manage threads to make network or database calls. This can often lead to substantially simpler code.However, blocking code like sorting a list or reading from a file still requires explicit code to create main-safety, even when using coroutines.
+ To switch between any dispatcher, coroutines uses <b>withContext</b>. Calling withContext switches to the other dispatcher just for the lambda then comes back to the dispatcher that called it with the result of that lambda.
+ By default, Kotlin coroutines provides three Dispatchers: <b>Main, IO, and Default</b>. The IO dispatcher is optimized for IO work like reading from the network or disk, while the Default dispatcher is optimized for CPU intensive tasks.

### Launch
+ viewModelScope.launch will start a new coroutine in the viewModelScope. This means when the job that we passed to viewModelScope gets canceled, all coroutines in this job/scope will be cancelled, too upon destruction of the ViewModel.

### Suspend
+ Suspend is Kotlin's way of marking a function, or function type, available to coroutines.
+ When a coroutine calls a function marked suspend, instead of blocking until that function returns like a normal function call, it <b>suspends execution until the result is ready then it resumes where it left off with the result</b>. While it's suspended waiting for a result, it unblocks the thread that it's running on so other functions or coroutines can run.
![coroutines](https://user-images.githubusercontent.com/58771510/85583200-f51c0e80-b635-11ea-9d6f-4a1df952cf5b.png)
+ Exceptions in suspend functions work just like errors in regular functions. If you throw an error in a suspend function, it will be thrown to the caller. So even though they execute quite differently, you can use regular try/catch blocks to handle them.
+ Suspend functions can be called from a coroutine or another suspend function only.
+ <b>Example scenaro:</b> refreshTitle() is suspended function inside the repository meant to making network request and saving data to database. The coroutine inside the viewmodel calls refreshTitle() just like a regular function. However, since refreshTitle is a suspending function, it executes differently than a normal function. The coroutine will suspend until it is resumed by refreshTitle. While it looks just like a regular blocking function call, it will automatically wait until the network and database query are complete before resuming without blocking the main thread.

### Room & Retrofit 
+ Both Room and Retrofit make suspending functions main-safe.It's safe to call these suspend funs from Dispatchers.Main, even though they fetch from the network and write to the database.
+ Both Room and Retrofit use a custom dispatcher and do not use Dispatchers.IO. (You do not need to use withContext(Dispatchers.IO))
+ Room will run coroutines using the default query and transaction Executor that's configured.
+ Retrofit will create a new Call object under the hood, and call enqueue on it to send the request asynchronously.

### Suspend lambda
+ To make a suspend lambda, start with the suspend keyword. The function arrow and return type Unit complete the declaration.
block: suspend () -> Unit


### Testing coroutines
+ MainCoroutineScopeRule is a custom rule in this codebase that <b>configures Dispatchers.Main to use a TestCoroutineDispatcher</b> from kotlinx-coroutines-test. This allows tests to advance a virtual-clock for testing, and allows code to use Dispatchers.Main in unit tests. 
+ The MainCoroutineScopeRule lets you pause, resume, or control the execution of coroutines that are launched on the Dispatchers.Main
+ InstantTaskExecutorRule is a JUnit rule that configures LiveData to execute each task synchronously.
+ Coroutines started with launch are asynchronous code. Therefore to test that asynchronous code, you need some way to tell the test to wait until your coroutine completes. The library kotlinx-coroutines-test has the <b>runBlockingTest</b> function that blocks while it calls suspend functions. When runBlockingTest calls a suspend function or launches a new coroutine, it executes it immediately by default. You can think of it as a way to convert suspend functions and coroutines into normal function calls.
+ In addition, runBlockingTest will rethrow uncaught exceptions for you.
+ runBlockingTest should only be used from tests as it executes coroutines in a test-controlled manner, while runBlocking can be used to provide blocking interfaces to coroutines.
+ One of the features of runBlockingTest is that it won't let you <b>leak</b> coroutines after the test completes. If there are any unfinished coroutines, at the end of the test, it will fail the test.

### License
Copyright (C) 2019 Google LLC, the app is provided by Google Developers Codelabs.
