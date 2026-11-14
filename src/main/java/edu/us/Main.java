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
        int port = DEFAULT_PORT;
        String host = DEFAULT_HOST;

        if (args.length == 0) {
            System.out.print("Port: ");
            port = scanner.nextInt();
            System.out.print("Host: ");
            host = scanner.next();
        }else if (args[0].equals("-d") || args[0].equals("--default")) {
            System.out.println("Connecting to localhost");
        }

        TelnetClient client = new TelnetClient(host, port);
        client.connect();

    }
}