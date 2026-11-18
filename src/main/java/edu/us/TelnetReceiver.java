package edu.us;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class TelnetReceiver implements Runnable {

    private final InputStream inputStream;
    private TelnetState state;
    private final TelnetClient telnetClient;
    private int subnegotiationOption = -1;
    private int subnegotiationCommand = -1;
    private final ByteArrayOutputStream textBuffer = new ByteArrayOutputStream();

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
                if (inputStream.available() == 0 && textBuffer.size() > 0) {
                    System.out.print(textBuffer.toString(StandardCharsets.UTF_8));
                    System.out.flush();
                    textBuffer.reset();
                }
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
                if (value == TelnetConstants.IAC) {
                    state = TelnetState.IAC_RECEIVED;
                } else if (value == TelnetConstants.CR) {
                    textBuffer.write(value);
                    state = TelnetState.CR_RECEIVED;
                } else {
                    textBuffer.write(value);
                }
                break;

            case IAC_RECEIVED:
                switch (value) {
                    case TelnetConstants.IAC:
                        textBuffer.write(value);
                        state = TelnetState.DATA;
                        break;
                    case TelnetConstants.WILL:
                        state = TelnetState.WILL_RECEIVED;
                        break;
                    case TelnetConstants.DO:
                        state = TelnetState.DO_RECEIVED;
                        break;
                    case TelnetConstants.WONT:
                    case TelnetConstants.DONT:
                        state = TelnetState.NEGOTIATION;
                        break;
                    case TelnetConstants.AYT:
                        try {
                            telnetClient.send("Yes\r\n".getBytes(StandardCharsets.UTF_8));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        state = TelnetState.DATA;
                        break;
                    case TelnetConstants.EC:
                        removeLastCharacter();
                        state = TelnetState.DATA;
                        break;
                    case TelnetConstants.EL:
                        textBuffer.reset();
                        state = TelnetState.DATA;
                        break;
                    case TelnetConstants.SB:
                        subnegotiationOption = -1;
                        subnegotiationCommand = -1;
                        state = TelnetState.SUBNEGOTIATION;
                        break;
                    default:
                        state = TelnetState.DATA;
                        break;
                }
                break;

            case NEGOTIATION:
                // Konsumuje 1 bajt opcji po WONT / DONT bez odsyłania odpowiedzi (RFC 855)
                state = TelnetState.DATA;
                break;

            case WILL_RECEIVED:
                try {
                    switch (value) {
                        case 1: // ECHO
                        case 3: // SUPPRESS-GO-AHEAD
                            telnetClient.send(new byte[]{
                                    (byte) TelnetConstants.IAC,
                                    (byte) TelnetConstants.DO,
                                    (byte) value
                            });
                            break;
                        default:
                            telnetClient.send(new byte[]{
                                    (byte) TelnetConstants.IAC,
                                    (byte) TelnetConstants.DONT,
                                    (byte) value
                            });
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                state = TelnetState.DATA;
                break;

            case DO_RECEIVED:
                try {
                    switch (value) {
                        case TelnetConstants.TERMINAL_TYPE:
                            telnetClient.send(new byte[]{
                                    (byte) TelnetConstants.IAC,
                                    (byte) TelnetConstants.WILL,
                                    (byte) TelnetConstants.TERMINAL_TYPE
                            });
                            break;
                        case TelnetConstants.TERMINAL_NAWS:
                            telnetClient.send(new byte[]{
                                    (byte) TelnetConstants.IAC,
                                    (byte) TelnetConstants.WILL,
                                    (byte) TelnetConstants.TERMINAL_NAWS
                            });
                            sendNAWS();
                            break;
                        default:
                            telnetClient.send(new byte[]{
                                    (byte) TelnetConstants.IAC,
                                    (byte) TelnetConstants.WONT,
                                    (byte) value
                            });
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                state = TelnetState.DATA;
                break;

            case SUBNEGOTIATION:
                if (value == TelnetConstants.IAC) {
                    state = TelnetState.SUBNEGOTIATION_IAC;
                } else if (subnegotiationOption == -1) {
                    subnegotiationOption = value;
                } else if (subnegotiationCommand == -1) {
                    subnegotiationCommand = value;
                }
                break;

            case SUBNEGOTIATION_IAC:
                if (value == TelnetConstants.SE) {
                    if (subnegotiationOption == TelnetConstants.TERMINAL_TYPE
                            && subnegotiationCommand == TelnetConstants.SEND) {
                        sendTerminalType();
                    }
                    subnegotiationOption = -1;
                    subnegotiationCommand = -1;
                    state = TelnetState.DATA;
                } else {
                    state = TelnetState.SUBNEGOTIATION;
                }
                break;

            case CR_RECEIVED:
                if (value == TelnetConstants.NUL) {
                    state = TelnetState.DATA;
                } else if (value == TelnetConstants.IAC) {
                    state = TelnetState.IAC_RECEIVED;
                } else if (value == TelnetConstants.CR) {
                    textBuffer.write(value);
                } else {
                    textBuffer.write(value);
                    state = TelnetState.DATA;
                }
                break;

            default:
                System.out.println("null state");
                break;
        }
    }

    private void removeLastCharacter() {
        String text = textBuffer.toString(StandardCharsets.UTF_8);
        if (text.isEmpty()) {
            return;
        }

        int lastCodePoint = text.offsetByCodePoints(text.length(), -1);
        byte[] remaining = text.substring(0, lastCodePoint).getBytes(StandardCharsets.UTF_8);
        textBuffer.reset();
        textBuffer.writeBytes(remaining);
    }

    private void sendTerminalType() {
        byte[] terminalType = "xterm".getBytes(StandardCharsets.US_ASCII);
        byte[] response = new byte[6 + terminalType.length];
        response[0] = (byte) TelnetConstants.IAC;
        response[1] = (byte) TelnetConstants.SB;
        response[2] = (byte) TelnetConstants.TERMINAL_TYPE;
        response[3] = (byte) TelnetConstants.IS;
        System.arraycopy(terminalType, 0, response, 4, terminalType.length);
        response[4 + terminalType.length] = (byte) TelnetConstants.IAC;
        response[5 + terminalType.length] = (byte) TelnetConstants.SE;

        try {
            telnetClient.send(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendNAWS() throws IOException {
        int[] windowSize = getTerminalSize();
        int columns = windowSize[0];
        int rows = windowSize[1];
        telnetClient.send(new byte[]{
                (byte) TelnetConstants.IAC,
                (byte) TelnetConstants.SB,
                (byte) TelnetConstants.TERMINAL_NAWS,
                (byte) (columns >> 8),
                (byte) columns,
                (byte) (rows >> 8),
                (byte) rows,
                (byte) TelnetConstants.IAC,
                (byte) TelnetConstants.SE
        });
    }

    private int[] getTerminalSize() {
        int fallbackColumns = 80;
        int fallbackRows = 24;

        try {
            int columns = Integer.parseInt(System.getenv("COLUMNS"));
            int rows = Integer.parseInt(System.getenv("LINES"));
            if (columns > 0 && rows > 0) {
                return new int[]{columns, rows};
            }
        } catch (NumberFormatException ignored) {
        }

        try {
            Process process = new ProcessBuilder("stty", "size")
                    .redirectInput(ProcessBuilder.Redirect.INHERIT)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.US_ASCII))) {
                String line = reader.readLine();
                if (line != null && process.waitFor() == 0) {
                    String[] dimensions = line.trim().split("\\s+");
                    if (dimensions.length == 2) {
                        int rows = Integer.parseInt(dimensions[0]);
                        int columns = Integer.parseInt(dimensions[1]);
                        if (columns > 0 && rows > 0) {
                            return new int[]{columns, rows};
                        }
                    }
                }
            }
        } catch (IOException | NumberFormatException ignored) {
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return new int[]{fallbackColumns, fallbackRows};
    }
}