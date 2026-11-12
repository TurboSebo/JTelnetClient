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
            case DATA -> {
                if (value == TelnetConstants.IAC) {
                    state = TelnetState.IAC_RECEIVED;
                }
            }
            case IAC_RECEIVED -> {
                if (value == TelnetConstants.SB) {
                    state = TelnetState.SUBNEGOTIATION;
                } else if (value == TelnetConstants.WILL
                        || value == TelnetConstants.WONT
                        || value == TelnetConstants.DO
                        || value == TelnetConstants.DONT) {
                    state = TelnetState.NEGOTIATION;
                } else {
                    state = TelnetState.DATA;
                }
            }
            case SUBNEGOTIATION -> {
                if (value == TelnetConstants.IAC) {
                    state = TelnetState.SUBNEGOTIATION_IAC;
                }
            }
            case SUBNEGOTIATION_IAC -> {
                state = value == TelnetConstants.SE
                        ? TelnetState.DATA
                        : TelnetState.SUBNEGOTIATION;
            }
            case NEGOTIATION -> state = TelnetState.DATA;
        }
    }
}
