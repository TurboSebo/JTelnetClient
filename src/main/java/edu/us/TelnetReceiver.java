package edu.us;

import java.io.IOException;
import java.io.InputStream;

public class TelnetReceiver implements Runnable {

    private final InputStream inputStream;
    private TelnetState state;
    private final TelnetClient telnetClient;

    public TelnetReceiver(InputStream inputStream, TelnetClient telnetClient) {
        this.inputStream = inputStream;
        this.telnetClient = telnetClient;
        this.state = TelnetState.DATA;
    }

    @Override
    public void run() {
        try {
            int value;
            while ((value = inputStream.read()) != -1) {
                processByte(value);
            }
        } catch (IOException e) {
            System.out.println("Błąd odbioru danych: " + e.getMessage());
        }
    }

    private void processByte(int value) {
        switch (state) {
            case DATA:
                System.out.print((char) value);
                break;
            case IAC_RECEIVED:
                System.out.printf("IAC received");
                break;
            default:
                System.out.println("null");
                break;
        }
    }

}
