package biz.itcf.nsmtp;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

/**
 * Main program starting an SMTP Server and a Control Server.
 * The Control Server is initiating an orderly shutdown of the application upon receiving any command ended by CRLF.
 * <p>
 * If not overriden by one of the options, ports {@value #DEFAULT_SMTP_SERVER_PORT} will be used for the SMTP Server
 * and {@value #DEFAULT_CONTROL_SERVER_PORT} for the Control Server.
 * Both servers will listen to all available interfaces in that case.
 * </p>
 */
public class NoopSMTP implements Stoppable {

    public static final int DEFAULT_SMTP_SERVER_PORT = 9025;
    public static final int DEFAULT_CONTROL_SERVER_PORT = 9026;

    public static void main(String [] args) throws Exception {
        String host = null;
        int port = DEFAULT_SMTP_SERVER_PORT;
        boolean ssl = false;

        String controlHost = null;
        int controlPort = DEFAULT_CONTROL_SERVER_PORT;
        boolean controlSsl = false;

        boolean printUsage = false;

        for (String arg : args) {
            if (arg.equals("-?")) {
                printUsage = true;
            } else if (arg.startsWith("-h")) {
                host = arg.substring(2);
            } else if (arg.startsWith("-p")) {
                try {
                    port = Integer.valueOf(arg.substring(2));
                } catch (NumberFormatException e) {
                    printUsage = true;
                }
            } else if (arg.equals("-s")) {
                ssl = true;
            } else if (arg.startsWith("-ch")) {
                controlHost = arg.substring(3);
            } else if (arg.startsWith("-cp")) {
                try {
                    controlPort = Integer.valueOf(arg.substring(3));
                } catch (NumberFormatException e) {
                    printUsage = true;
                }
            } else if (arg.equals("-cs")) {
                controlSsl = true;
            } else {
                printUsage = true;
            }
        }

        if (printUsage)
        {
            System.err.println("Usage: " + NoopSMTP.class.getName() + " OPTIONS");
            System.err.println("Options:");
            System.err.println("-?      Show this help.");
            System.err.println("-hHOST  Hostname or IP to bind SMTP Server to. Defaults to all available addresses.");
            System.err.println("-pPORT  Port number to bind SMTP Server to. Defaults to " + DEFAULT_SMTP_SERVER_PORT + ".");
            System.err.println("-s      Enabled SSL for SMTP Server.");
            System.err.println("-chHOST Hostname or IP to bind Control Server to. Defaults to all available addresses.");
            System.err.println("-cpPORT Port number to bind Control Server to. Defaults to " + DEFAULT_CONTROL_SERVER_PORT + ".");
            System.err.println("-cs     Enabled SSL for Control Server.");
            System.exit(1);
        } else {
            SSLContext sslContext = null;

            if (ssl) {
                sslContext = new SSLContextBuilder().build();
            }

            SSLContext controlSslContext = null;
            if (controlSsl) {
                controlSslContext = new SSLContextBuilder().build();
            }

            NoopSMTP smtp = new NoopSMTP(host, port, sslContext, controlHost, controlPort, controlSslContext);

            smtp.start();
        }
    }

    private boolean started;

    private LineBasedServer smtpServer;
    private LineBasedServer controlServer;

    public NoopSMTP(String host, int port, SSLContext smtpSslContext, String controlHost, int controlPort, SSLContext controlSslContext) {
        this.smtpServer = new LineBasedServer(new SMTPHandler(), host, port, smtpSslContext, "ASCII");
        this.controlServer = new LineBasedServer(new ControlHandler(this), controlHost, controlPort, controlSslContext, "UTF-8");
    }

    public void start() throws IOException {
        if (!started) {
            try
            {
                startControlServer();
                startSMTPServer();

                started = true;
            } catch (IOException e)
            {
                stopSMTPServer();
                stopControlServer();

                throw e;
            }
        }
    }

    private void startSMTPServer() throws IOException {
        smtpServer.start();
    }

    private void startControlServer() throws IOException {
        controlServer.start();
    }

    private void stopSMTPServer()
    {
        smtpServer.stop();
    }

    private void stopControlServer()
    {
        controlServer.stop();
    }

    @Override
    public void stop() {
        if (started) {
            stopSMTPServer();
            stopControlServer();

            started = false;
        }
    }

    private class LineBasedServer
    {

        private NioSocketAcceptor acceptor;
        private InetSocketAddress address;

        public LineBasedServer(IoHandler handler, String host, int port, SSLContext sslContext, String charset) {
            acceptor = new NioSocketAcceptor();

            if (sslContext != null) {
                acceptor.getFilterChain().addLast("ssl", new SslFilter(sslContext));
            }

            acceptor.getFilterChain().addLast("codec",
                    new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName(charset), "\r\n", "\r\n")));
            acceptor.setHandler(handler);
            acceptor.setCloseOnDeactivation(true);
            acceptor.setReuseAddress(true);

            if (host == null) {
                this.address = new InetSocketAddress(port);
            } else {
                this.address = new InetSocketAddress(host, port);
            }
        }

        public void start() throws IOException {
            acceptor.bind(address);
        }

        public void stop()
        {
            if (acceptor.isActive())
            {
                acceptor.unbind();
                acceptor.dispose(true);
            }
        }
    }
}
