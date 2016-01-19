package pl.maxmati.ftp.common;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by maxmati on 1/8/16
 */
public class Watchdog {
    private final int timeout;
    private final WatchdogTask timeoutTask;
    private TimerTask timerTask = null;
    private final Timer timer = new Timer("Watchdog timer");
    private final boolean autorestart;

    public Watchdog(int timeout, WatchdogTask timeoutTask) {
        this(timeout, timeoutTask, false);
    }

    public Watchdog(int timeout, WatchdogTask quit, boolean autorestart) {
        this.timeout = timeout;
        this.timeoutTask = quit;
        this.autorestart = autorestart;

        reset();
    }

    public void reset() {
        if(timerTask != null)
            timerTask.cancel();

        timerTask = getTimerTask();
        timer.schedule(timerTask, timeout);
    }

    private TimerTask getTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                System.out.println("Watchdog timeout.");
                timeoutTask.run();
                if(autorestart) {
                    timerTask = getTimerTask();
                    timer.schedule(timerTask, timeout);
                }
            }
        };
    }

    public void stop() {
        if(timerTask != null)
            timerTask.cancel();
    }


    @SuppressWarnings("WeakerAccess")
    @FunctionalInterface
    public interface WatchdogTask{
        void run();
    }
}
