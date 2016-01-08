package pl.maxmati.po.ftp.server;

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

    public Watchdog(int timeout, WatchdogTask quit) {
        this.timeout = timeout;
        this.timeoutTask = quit;

        reset();
    }

    public void reset() {
        if(timerTask != null)
            timerTask.cancel();

        timerTask = new TimerTask() {
            @Override
            public void run() {
                System.out.println("Watchdog timeout.");
                timeoutTask.run();
            }
        };
        timer.schedule(timerTask, timeout);
    }


    @SuppressWarnings("WeakerAccess")
    @FunctionalInterface
    public interface WatchdogTask{
        void run();
    }
}
