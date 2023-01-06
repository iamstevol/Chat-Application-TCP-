package org.example.iamstevol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{

    private ArrayList<ConnectionaHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;       //Thread pool

    public Server() {
        connections = new ArrayList<>();
        done = false;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                ConnectionaHandler handler = new ConnectionaHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            shutdown();
        }
    }

    public void broadcast(String message) {
        for(ConnectionaHandler ch : connections) {
            if(ch != null) {
                ch.sendMessage(message);
            }
        }
    }

    public void shutdown() {
        try {
            done = true;
            if (!server.isClosed()) {
                server.close();
            }
            for (ConnectionaHandler ch : connections) {
                ch.shutdown();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    class ConnectionaHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String name;

        public ConnectionaHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Please enter a name");
                name = in.readLine();
                System.out.println(name + " connected successfully \uD83D\uDC4D");
                broadcast(name + " joined the chat \uD83D\uDE00");
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/name ")) {
                        String[] messageSplit = message.split(" ", 2);
                        if (messageSplit.length == 2) {
                            broadcast(name + " has successfully renamed to " + messageSplit);
                            System.out.println(name + " renamed self to " + messageSplit);
                            name = messageSplit[1];
                            out.println("Successfully changed name to " + name);
                        } else {
                            out.println("No name provided");
                        }
                    } else if (message.startsWith("quit")) {
                        broadcast(name + " left the chat");
                        shutdown();
                    } else {
                        broadcast(name + ": " + message);
                    }
                }
            } catch(IOException e) {
                shutdown();
            }

        }
        public void sendMessage(String message) {
            out.println(message);
        }

        public void shutdown() {
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {

            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
