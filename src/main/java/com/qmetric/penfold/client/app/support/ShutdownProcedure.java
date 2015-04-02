package com.qmetric.penfold.client.app.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.SECONDS;

public class ShutdownProcedure implements Runnable
{
    private static final Logger log = LoggerFactory.getLogger(ShutdownProcedure.class);

    private static final int TIMEOUT_SECS = 120;

    private final ExecutorService executorService;

    private final Thread shutdownThread;

    public ShutdownProcedure(ExecutorService executorService)
    {
        this.executorService = executorService;
        shutdownThread = new Thread(this, format("penfold-client-shutdown-%s", executorService));
    }

    public void registerShutdownHook()
    {
        getRuntime().addShutdownHook(shutdownThread);
    }

    public void removeShutdownHook()
    {
        getRuntime().removeShutdownHook(shutdownThread);
    }

    @Override public void run()
    {
        log.info("penfold-client shutdown started");
        if (!executorService.isTerminated())
        {
            terminateExecutor();
        }
        else
        {
            log.info("executor-service is already terminated");
        }
        log.info("penfold-client shutdown completed");
    }

    public void runAndRemoveHook()
    {
        run();
        removeShutdownHook();
    }

    private void terminateExecutor()
    {
        try
        {
            stopAcceptingNewJobs();
            waitRunningJobsToTerminate();
        }
        catch (InterruptedException e)
        {
            forceShutdown();
        }
    }

    private void stopAcceptingNewJobs()
    {
        log.info("no new jobs accepted");
        if (!executorService.isShutdown())
        {
            executorService.shutdown();
        }
        else
        {
            log.info("executor-service is already shutdown");
        }
    }

    private void waitRunningJobsToTerminate() throws InterruptedException
    {
        log.info("terminating all executor-service jobs with timeout {} {}", TIMEOUT_SECS, SECONDS);
        if (executorService.awaitTermination(TIMEOUT_SECS, SECONDS))
        {
            log.info("all jobs terminated normally");
        }
        else
        {
            log.warn("running jobs did not complete within timeout - forcing shutdown");
            executorService.shutdownNow();
        }
    }

    private void forceShutdown()
    {
        log.warn("shutdown thread was interrupted - forcing executor shutdown");
        executorService.shutdownNow();
        // Preserve interrupt status
        currentThread().interrupt();
    }
}
