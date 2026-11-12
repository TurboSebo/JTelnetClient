package edu.us;

public enum TelnetState {
    DATA,
    IAC_RECEIVED,
    NEGOTIATION,
    SUBNEGOTIATION,
    SUBNEGOTIATION_IAC
}
