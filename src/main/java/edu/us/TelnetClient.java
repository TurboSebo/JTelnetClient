package edu.us;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class TelnetClient {

        final int port;
        final String host;
        private OutputStream out;

    public TelnetClient(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public synchronized void send (byte [] data) throws IOException{
            if (out != null) {
                out.write(data);
                out.flush();
            }
        }
    public void connect() {
            Thread terminalRestoreHook = null;
            boolean terminalConfigured = false;
            try (Socket socket = new Socket(host, port)) {
                System.out.println("ustawianie interfejsów I/O");
                InputStream in = socket.getInputStream();
                this.out = socket.getOutputStream();
            System.out.println("Łączenie z " + host + ":" + port);
            Thread tReceiver = new Thread(new TelnetReceiver(in, this));
            tReceiver.setDaemon(true);
            tReceiver.start();

            runStty("-icanon", "-echo");
            terminalConfigured = true;
            terminalRestoreHook = new Thread(TelnetClient::restoreTerminal, "restore-terminal");
            Runtime.getRuntime().addShutdownHook(terminalRestoreHook);

            int b; //byte
            while (((b = System.in.read())!= -1) ){
                if (b == '\n') {
                    send(new byte[]{(byte) TelnetConstants.CR, (byte) TelnetConstants.LF});
                }
                else if (b == TelnetConstants.IAC) {
                    send(new byte[]{(byte) TelnetConstants.IAC, (byte) TelnetConstants.IAC});
                }
                else if (b == TelnetConstants.CR) {
                    send(new byte[]{(byte) TelnetConstants.CR, (byte) TelnetConstants.NUL});
                }
                else {
                    send(new byte[]{(byte) b});
                }
            }

        } catch (IOException e) {
            System.out.println("Błąd połączenia: " + e.getMessage());
        } finally {
            if (terminalConfigured) {
                restoreTerminal();
                if (terminalRestoreHook != null) {
                    try {
                        Runtime.getRuntime().removeShutdownHook(terminalRestoreHook);
                    } catch (IllegalStateException ignored) {
                        // JVM shutdown is already in progress.
                    }
                }
            }
        }
    }

    private static void runStty(String... arguments) throws IOException {
        String[] command = new String[arguments.length + 1];
        command[0] = "stty";
        System.arraycopy(arguments, 0, command, 1, arguments.length);
        Process process = new ProcessBuilder(command)
                .redirectInput(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
        try {
            if (process.waitFor() != 0) {
                throw new IOException("Nie udało się ustawić terminala za pomocą stty");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Przerwano ustawianie terminala", e);
        }
    }

    private static void restoreTerminal() {
        try {
            runStty("sane");
        } catch (IOException e) {
            System.err.println("Błąd przywracania ustawień terminala: " + e.getMessage());
        }
    }

}
