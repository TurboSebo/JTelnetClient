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
        try (Socket socket = new Socket(host, port)) {
            System.out.println("ustawianie interfejsów I/O");
            InputStream in = socket.getInputStream();
            this.out = socket.getOutputStream();
            System.out.println("Łączenie z " + host + ":" + port);
            Thread tReceiver = new Thread(new TelnetReceiver(in, this));
            tReceiver.setDaemon(true);
            tReceiver.start();
            int b; //byte
            while (((b = System.in.read())!= -1) ){
                if (b == '\n') {
                    send(new byte[]{13, 10});
                } else {
                    send(new byte[]{(byte) b});
                }
            }

        } catch (IOException e) {
            System.out.println("Błąd połączenia: " + e.getMessage());
        }
    }

}
