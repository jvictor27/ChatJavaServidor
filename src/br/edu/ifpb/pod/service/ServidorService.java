/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.edu.ifpb.pod.service;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import  java.util.Arrays;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.edu.ifpb.pod.bean.ChatMessage;
import br.edu.ifpb.pod.bean.ChatMessage.Action;

//import sun.awt.windows.ThemeReader;

/**
 * @author João Victor
 */
public class ServidorService {

    private ServerSocket serverSocket;
    private Socket socket;
    
    // Armazena usuários que se conectarem no servidor
    private Map<String, ObjectOutputStream> mapOnlines = new HashMap<String, ObjectOutputStream>();

    public ServidorService() {
        try {
            serverSocket = new ServerSocket(5555);

            System.out.println("Servidor on!");

            // Mantém o server socket sempre esperando por uma nova conexão
            while (true) {
                socket = serverSocket.accept();

                new Thread(new ListenerSocket(socket)).start();
            }

        } catch (IOException ex) {
            Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private class ListenerSocket implements Runnable {

        private ObjectOutputStream output;
        private ObjectInputStream input;

        public ListenerSocket(Socket socket) {
            try {
                this.output = new ObjectOutputStream(socket.getOutputStream());
                this.input = new ObjectInputStream (socket.getInputStream());
            } catch (IOException ex) {
                Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public void run() {
            ChatMessage message = null;
            try {
                while ((message = (ChatMessage) input.readObject()) != null) {
                	String messageText = message.getText();
                    
                    if(!message.getAction().equals(Action.CONNECT) && !message.getAction().equals(Action.DISCONNECT)) {
                    	if(message.getText().equals("bye")) {
//                            disconnect(message, output);
//                            sendOnlines();
//                            return;
                    		message.setAction(Action.DISCONNECT);
                    	} 
//                    	else if (messageText.startsWith("rename")) {
//                    		message.setAction(Action.RENAME);
//                    	}
                    }
                	System.out.println(message.getAction());
                    Action action = message.getAction();

                    if (action.equals(Action.CONNECT)) {
                        boolean isConnect = connect(message, output);
                        if (isConnect) {
                            mapOnlines.put(message.getName(), output);
                            sendOnlines();
                        }
                    } else if (action.equals(Action.DISCONNECT)) {
                    	System.out.println("AQAQAQAQ");
                        disconnect(message, output);
                        sendOnlines();
                        return;
                    } else if (action.equals(Action.SEND_ONE)) {
                    	String[] vazio = new String[0];
                        sendOne(message, vazio);
                    } else if (messageText.startsWith("send")) {
                    	String[] messageSplit = messageText.split(" ");
//                    	for(String msg : messageSplit){
//                            System.out.println(msg);
//                    	} 
                    	if (messageSplit[1].equals("-user")) {
//                    		System.out.println(mapOnlines);
                    		Set<String> chaves = mapOnlines.keySet();
//                    		
                    		for(String chave : chaves){
//                              System.out.println(chave);
                              
                    		}
                    		message.setAction(Action.SEND_ONE);
//                    		System.out.println(messageSplit.length);
                    		
                    		for (Map.Entry<String, ObjectOutputStream> kv : mapOnlines.entrySet()) {
                                if (kv.getKey().equals(messageSplit[2])) {
//                                	System.out.println(kv.getKey() + messageSplit[2]);
                                	sendOne(message, messageSplit);
                                }
                    		}    
                    		
                    		
                    	}
//                        sendOne(message);
                    } else if (action.equals(Action.SEND_ALL)) {
                        sendAll(message);
                    }
                }
            } catch (IOException ex) {
                ChatMessage cm = new ChatMessage();
                cm.setName(message.getName());
                disconnect(cm, output);
                sendOnlines();
                System.out.println(message.getName() + " deixou o chat!");
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private boolean connect(ChatMessage message, ObjectOutputStream output) {
        if (mapOnlines.size() == 0) {
            message.setText("YES");
            send(message, output);
            return true;
        }

        if (mapOnlines.containsKey(message.getName())) {
            message.setText("NO");
            send(message, output);
            return false;
        } else {
            message.setText("YES");
            send(message, output);
            return true;
        }
    }

    private void disconnect(ChatMessage message, ObjectOutputStream output) {
        mapOnlines.remove(message.getName());

        message.setText(" até logo!");

        message.setAction(Action.SEND_ONE);

        sendAll(message);

        System.out.println("User " + message.getName() + " saiu da sala.");
    }

    private void send(ChatMessage message, ObjectOutputStream output) {
        try {
            output.writeObject(message);
        } catch (IOException ex) {
            Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void sendOne(ChatMessage message, String[] command) {
        for (Map.Entry<String, ObjectOutputStream> kv : mapOnlines.entrySet()) {
        	if(command.length >= 4) {
        		if (kv.getKey().equals(command[2])) {
        			message.setText("");
        			
        			for(int i = 0; i < command.length; i++) {
        				if(i > 2) {
        					message.setText(message.getText() + " " + (String) command[i]);
        				}
        			}
//        			for (String msg : command) {
////        				msg.
//        				message.setText(message.getText() + " " + msg);
//        			}
        			try {
                        kv.getValue().writeObject(message);
                    } catch (IOException ex) {
                        Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
                    }
        		}
        	} else if (kv.getKey().equals(message.getNameReserved())) {
                try {
                    kv.getValue().writeObject(message);
                } catch (IOException ex) {
                    Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void sendAll(ChatMessage message) {
        for (Map.Entry<String, ObjectOutputStream> kv : mapOnlines.entrySet()) {
            if (!kv.getKey().equals(message.getName())) {
                message.setAction(Action.SEND_ONE);
                try {
                    kv.getValue().writeObject(message);
                } catch (IOException ex) {
                    Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void sendOnlines() {
//    	System.out.println("qqqqqqqqqqqq");
        Set<String> setNames = new HashSet<String>();
        for (Map.Entry<String, ObjectOutputStream> kv : mapOnlines.entrySet()) {
            setNames.add(kv.getKey());
        }

        ChatMessage message = new ChatMessage();
        message.setAction(Action.USERS_ONLINE);
        message.setSetOnlines(setNames);

        for (Map.Entry<String, ObjectOutputStream> kv : mapOnlines.entrySet()) {
            message.setName(kv.getKey());
            try {
                kv.getValue().writeObject(message);
            } catch (IOException ex) {
                Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
