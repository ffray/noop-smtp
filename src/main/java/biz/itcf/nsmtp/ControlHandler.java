package biz.itcf.nsmtp;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

/**
 * Simple IoHandlerAdapter initiating an orderly shutdown of the application
 * by calling {@link Stoppable#stop} upon receiving any kind of message.
 */
public class ControlHandler extends IoHandlerAdapter {

    private Stoppable stoppable;

    public ControlHandler(Stoppable stoppable)
    {
        this.stoppable = stoppable;
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        session.write("Stopping NoopSMTP Server");
        session.close(true);

        Thread stopper = new Thread(new Runnable() {
            @Override
            public void run() {
                stoppable.stop();
            }
        });

        stopper.start();
    }

}
