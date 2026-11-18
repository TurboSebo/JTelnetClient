package edu.us;

public enum TelnetState {
    DATA,
    IAC_RECEIVED,
    NEGOTIATION,
    SUBNEGOTIATION,
    SUBNEGOTIATION_IAC,
    CR_RECEIVED,
    WILL_RECEIVED,
    DO_RECEIVED
}
