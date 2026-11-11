package edu.us;

import java.util.Scanner;

import static edu.us.TelnetConstants.DEFAULT_HOST;
import static edu.us.TelnetConstants.DEFAULT_PORT;

public class Main {
    public static void main(String[] args) {
        System.out.println("Telnet klient!");
        System.out.println("-----------------");
        System.out.println("Sebastian Kałuża");
        System.out.println("Protokoły Internetowe \n \n");

        Scanner scanner = new Scanner(System.in);
        /*
        int port = scanner.nextInt();
        String host = scanner.next();
        */
        TelnetClient client = new TelnetClient(DEFAULT_HOST, DEFAULT_PORT);
        client.connect();

    }
}