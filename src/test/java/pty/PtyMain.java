package pty;

//import com.google.common.collect.Lists;
import com.jediterm.pty.PtyProcessTtyConnector;
import com.jediterm.terminal.LoggingTtyConnector;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.ui.AbstractTerminalFrame;
import com.pty4j.PtyProcess;
import io.nayasis.common.basica.etc.Platform;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author traff
 */
public class PtyMain extends AbstractTerminalFrame {

  public static void main(final String[] arg) {
//    BasicConfigurator.configure();
//    Logger.getRootLogger().setLevel(Level.INFO);
    PtyMain pty = new PtyMain();

//    pty.openSession(  )


  }

  @Override
  public TtyConnector createTtyConnector() {
    try {
      Map<String,String> envs = new HashMap<>( System.getenv() );
      String[] command;

      if ( Platform.isWindows ) {
        command = new String[]{"cmd.exe"};
//        command = new String[]{"dir"};
//        command = new String[]{"cmd", "/c", "c:", "&&", "cd", "\"c:\\Windows\"", "&&", "dir" };
      } else {
        command = new String[]{"/bin/bash", "--login"};
        envs.put("TERM", "xterm");
      }

      PtyProcess process = PtyProcess.exec( command, envs, null );

      return new LoggingPtyProcessTtyConnector( process, Charset.forName(Platform.osCharset) );
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static class LoggingPtyProcessTtyConnector extends PtyProcessTtyConnector implements LoggingTtyConnector {
    private List<char[]> myDataChunks = new ArrayList<>();

    public LoggingPtyProcessTtyConnector(PtyProcess process, Charset charset) {
      super(process, charset);
    }

    @Override
    public int read(char[] buf, int offset, int length) throws IOException {
      int len = super.read(buf, offset, length);
      if (len > 0) {
        char[] arr = Arrays.copyOfRange(buf, offset, len);
        myDataChunks.add(arr);
      }
      return len;
    }

    public List<char[]> getChunks() {
      return new ArrayList<>( myDataChunks );
    }

    @Override
    public void write(String string) throws IOException {
      LOG.debug("Writing in OutputStream : " + string);
      super.write(string);
    }

    @Override
    public void write(byte[] bytes) throws IOException {
      LOG.debug("Writing in OutputStream : " + Arrays.toString(bytes) + " " + new String(bytes));
      super.write(bytes);
    }
  }
}
