package edu.us;

import java.io.IOException;
import java.io.InputStream;

public class TelnetReceiver implements Runnable {

    private final InputStream inputStream;
    private TelnetState state;
    private final TelnetClient telnetClient;
    private int lastCommand;

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
            System.out.println("\nPołączenie zamknięte przez serwer.");
            System.exit(0);
        } catch (IOException e) {
            System.out.println("Błąd odbioru danych: " + e.getMessage());
            System.exit(-1);
        }
    }

    private void processByte(int value) {
        switch (state) {
            case DATA:
                if (value == TelnetConstants.IAC) state = TelnetState.IAC_RECEIVED;
                else
                {
                    System.out.write((char) value);
                    System.out.flush();
                }
                break;
            case IAC_RECEIVED:
                switch (value) {
                    case TelnetConstants.IAC:
                    System.out.print((char) value);
                    state = TelnetState.DATA;
                    break;
                    case TelnetConstants.WILL:
                        lastCommand = value;
                        state = TelnetState.NEGOTIATION;
                        break;
                    case TelnetConstants.DONT:
                        lastCommand = value;
                        state = TelnetState.NEGOTIATION;
                        break;
                    case TelnetConstants.WONT:
                        lastCommand = value;
                        state = TelnetState.NEGOTIATION;
                        break;
                    case TelnetConstants.DO:
                        lastCommand = value;
                        state = TelnetState.NEGOTIATION;
                        break;
                    case TelnetConstants.SB:
                        state = TelnetState.SUBNEGOTIATION;
                        break;
                    default:
                        state = TelnetState.DATA;
                       // System.out.println(value);
                        break;
                }
                break;
                case NEGOTIATION:
                    switch (lastCommand) {
                        case TelnetConstants.WILL:
                            try {
                                telnetClient.send(new byte[]{(byte) TelnetConstants.IAC, (byte) TelnetConstants.DONT, (byte) value});                            }catch (IOException e){
                                e.printStackTrace();
                            }
                            break;
                        case TelnetConstants.DO:
                            try {
                                telnetClient.send(new byte[]{(byte) TelnetConstants.IAC, (byte) TelnetConstants.WONT, (byte) value});                            }catch (IOException e){
                                e.printStackTrace();
                            }
                            break;
                        case TelnetConstants.WONT:
                            break;
                        case TelnetConstants.DONT:
                            break;

                    }
                    state = TelnetState.DATA;
                    break;
            case SUBNEGOTIATION:
                    if (value == TelnetConstants.IAC) {
                        state = TelnetState.SUBNEGOTIATION_IAC;
                    }
                    break;
            case SUBNEGOTIATION_IAC:
                if (value == TelnetConstants.SE) {
                    state = TelnetState.DATA;
                }
                else state = TelnetState.SUBNEGOTIATION;
                break;
            default:
                System.out.println("null state");
                break;
        }
    }

}
