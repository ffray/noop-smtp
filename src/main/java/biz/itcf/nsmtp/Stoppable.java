package biz.itcf.nsmtp;

/**
 * Callback interface to stop the running NoopSMTP application.
 */
public interface Stoppable {

    /**
     * Stops the entire application.
     * Does not fail if application is not running.
     */
    void stop();

}
