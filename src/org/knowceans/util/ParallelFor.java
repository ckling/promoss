/*
 * Created on Oct 31, 2009
 */
package org.knowceans.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * ParallelFor implements what in OpenMP is called a parallel for loop, i.e., a
 * loop that executes in parallel on several threads with a barrier (join) at
 * the end. Implementors simply subclass and implement the process() method and
 * make sure the loop() method is not called while a loop of one instance is
 * running. Execution can be stopped using stop(), which completes the
 * iterations currently running. After the last usage of the class, it should be
 * shut down properly using function shutdown() or the hard way using
 * System.exit();
 * 
 * @author gregor
 */
public abstract class ParallelFor {

    /**
     * Worker is one pooled thread
     */
    private class Worker implements Runnable {

        int id;

        public Worker(int threadid) {
            this.id = threadid;
        }

        public void run() {
            while (!isStopping) {
                int i = 0;
                synchronized (ParallelFor.this) {
                    i = iter++;
                }
                if (i >= niter) {
                    break;
                }
                process(i, id);
            }
            synchronized (ParallelFor.this) {
                ParallelFor.this.activeWorkers--;
                ParallelFor.this.notifyAll();
            }
        }
    }

    /**
     * number of threads = number of processors / cores
     */
    protected int nthreads;

    /**
     * pool of threads spread over processors
     */
    protected ExecutorService threadpool;

    /**
     * current iteration (synchronized access)
     */
    protected int iter = 0;

    /**
     * worker threads
     */
    // this also works with a single worker instead of one instance per thread. 
    //protected final Worker worker;
    protected final Worker[] workers;

    /**
     * stop flag
     */
    protected boolean isStopping;

    /**
     * loop iterations
     */
    protected int niter = 0;

    /**
     * active workers (for the barrier)
     */
    protected int activeWorkers = 0;

    /**
     * instantiate with as many threads as there are processors
     */
    public ParallelFor() {
        this(Runtime.getRuntime().availableProcessors());
    }

    /**
     * instantiate a parallel for implementation
     */
    public ParallelFor(int nthreads) {
        this.nthreads = nthreads;
        if (threadpool == null) {
            threadpool = Executors.newFixedThreadPool(nthreads);
        }
        //worker = new Worker();
        workers = new Worker[nthreads];
        for (int i = 0; i < nthreads; i++) {
            workers[i] = new Worker(i);
        }
    }

    /**
     * Start worker threads and loop through the iterations. Should never be
     * called while the loop instance is still running.
     * 
     * @param N
     */
    public void loop(int N) {
        isStopping = false;
        niter = N;
        iter = 0;

        // start worker threads
        for (int i = 0; i < nthreads; i++) {
            threadpool.execute(workers[i]);
            synchronized (this) {
                activeWorkers++;
            }
        }

        // wait until all worker threads are done
        while (activeWorkers > 0) {
            synchronized (this) {
                try {
                    wait(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * loops once, then shuts down
     * 
     * @param N
     */
    public void loopOnce(int N) {
        loop(N);
        shutdown();
    }

    /**
     * payload for the for loop
     * 
     * @param iteration in the for loop
     * @param thread on the machine
     */
    abstract public void process(int iteration, int thread);

    /**
     * stop loop execution
     */
    public void stop() {
        isStopping = true;
    }

    /**
     * shut down the thread pool after final usage
     */
    public void shutdown() {
        // immediately terminate threadpool
        try {
            threadpool.shutdown();
            threadpool.awaitTermination(0, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int niter = 10;
        ParallelFor x = new ParallelFor() {
            public void process(int y, int id) {
                for (int i = 0; i < 5; i++) {
                    try {
                        Thread.sleep((int) (Math.random() * 1000));
                    } catch (InterruptedException e) {
                    }
                    System.out
                        .println("iteration " + y + " on processor " + id);

                }

            }
        };
        x.loop(niter);
        System.out.println("first join");
        x.loopOnce(niter);
        System.out.println("second join and finish");
    }

}
