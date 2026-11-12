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
    public void connect() {
        try (Socket socket = new Socket(host, port)) {
            System.out.println("ustawianie interfejsów I/O");
            InputStream in = socket.getInputStream();
            out = socket.getOutputStream();
            System.out.println("Łączenie z " + host + ":" + port);




        } catch (IOException e) {
            System.out.println("Błąd połączenia: " + e.getMessage());
        }
    }

}
