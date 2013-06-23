package biz.itcf.nsmtp;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * IoHandlerAdapter handling SMTP sessions.
 * <p>
 * All SMTP commands will be accepted and successfully responded to except of the EXPN command,
 * which will be responded to with &quot;550 Unknown mailing list&quot;.
 * </p>
 * <p>
 * Note that this SMTP server does not perform any validation of the correct order of SMTP commands,
 * so it will accept a sequence like
 * <pre>
 * EHLO me
 * HELO you
 * DATA
 * .
 * RCPT TO:&lt;john.doe@acme.com&gt;
 * DATA
 * .
 * MAIL FROM:&lt;jane.doe@acme.com&gt;
 * </pre>
 * </p>
 */
public class SMTPHandler extends IoHandlerAdapter {

    private static final String STATE_ATTRIBUTE = "SMTP_STATE";

    private interface ProtocolState {

        ProtocolState handle(IoSession session, String message) throws Exception;

    }

    private class LoginState implements ProtocolState {

        @Override
        public ProtocolState handle(IoSession session, String message) throws Exception {
            session.write("220 Service ready");

            return COMMAND_STATE;
        }

    }

    private class CommandState implements ProtocolState {

        @Override
        public ProtocolState handle(IoSession session, String message) throws Exception {
            String command = message.toUpperCase();

            if (command.equals("DATA")) {
                session.write("354 Start mail input; end with <CRLF>.<CRLF>");

                return MAIL_DATA_STATE;
            } else if (command.startsWith("EHLO ")
                    || command.startsWith("HELO ")
                    || command.startsWith("MAIL FROM:")
                    || command.equals("NOOP")
                    || command.startsWith("NOOP ")
                    || command.startsWith("RCPT TO:")
                    || command.equals("RSET")) {

                session.write("250 OK");

                return this;
            } else if (command.startsWith("EXPN ")) {
                session.write("550 Unknown mailing list");

                return this;
            } else if (command.equals("HELP") || command.startsWith("HELP ")) {
                session.write("221 Service provided by NoopSMTP Server " + SMTPHandler.this.version);

                return this;
            } else if (command.startsWith("VRFY ")) {
                session.write("252 Cannot VRFY user, but will accept message and attempt delivery");

                return this;
            } else if (command.equals("QUIT")) {
                session.write("221 Service closing transmission channel");
                session.close(true);

                return this;
            } else {
                session.write("500 Unknown command");

                return this;
            }
        }

    }

    private class MailDataState implements ProtocolState {

        @Override
        public ProtocolState handle(IoSession session, String message) throws Exception {
            if (message.equals(".")) {
                session.write("250 OK");

                return COMMAND_STATE;
            } else {
                return this;
            }
        }
    }

    private final ProtocolState LOGIN_STATE = new LoginState();
    private final ProtocolState COMMAND_STATE = new CommandState();
    private final ProtocolState MAIL_DATA_STATE = new MailDataState();

    private String version;

    public SMTPHandler() {
        try
        {
            InputStream in = getClass().getResourceAsStream("version.properties");
            Properties properties = new Properties();
            properties.load(new InputStreamReader(in, "UTF-8"));
            version = properties.getProperty("nsmtp.version");
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).warn("Could not obtain version information.", e);
            version = "";
        }
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        session.setAttribute(STATE_ATTRIBUTE, LOGIN_STATE.handle(session, null));
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        session.removeAttribute(STATE_ATTRIBUTE);
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        ProtocolState state = (ProtocolState) session.getAttribute(STATE_ATTRIBUTE);
        ProtocolState newState = state.handle(session, (String) message);

        if (newState != state) {
            session.setAttribute(STATE_ATTRIBUTE, newState);
        }
    }

}
