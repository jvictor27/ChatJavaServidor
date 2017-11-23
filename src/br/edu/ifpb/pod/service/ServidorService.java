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
                    
                    Action action = message.getAction();

                    if (action.equals(Action.CONNECT)) {
                        boolean isConnect = connect(message, output);
                        if (isConnect) {
                            mapOnlines.put(message.getName(), output);
                            sendOnlines(message, output);
                        }
                    } else if (action.equals(Action.DISCONNECT)) {
                        disconnect(message, output);
                        sendOnlines(message, output);
                        return;
                    } else if (action.equals(Action.SEND_ONE)) {
                    	if (messageText.equals("list")) {
                    		sendOnlines(message, output);
                    	}
                    	String[] vazio = new String[0];
                        sendOne(message, vazio);
                    }  else if (action.equals(Action.SEND_ALL)) {
                    	String[] textSplit = message.getText().split(" ");
                    	if(message.getText().startsWith("rename") && textSplit.length == 2) {

                    		String[] nameSplit = message.getName().split(" ");
//                    		message.setText(nameSplit[0] + "mudou o nome para -> " + nameSplit[1]);
//                    		Set<String> setNames = new HashSet<String>();
                    		for (Map.Entry<String, ObjectOutputStream> kv : mapOnlines.entrySet()) {

                                if(kv.getKey().equals(nameSplit[0])) {
                                	mapOnlines.put(nameSplit[1], mapOnlines.get(nameSplit[0]));
//                                	kv.getKey().replaceAll(nameSplit[0], nameSplit[1]);
                                	mapOnlines.remove(nameSplit[0]);
                                	message.setName(nameSplit[1]);
                                	System.out.println("AQUI" + mapOnlines.entrySet());
                                	sendOnlines(message, output);
                                	return;
                                }
                            }
                    		
                    		
                    		
                    	}
                        sendAll(message);
                    }
                }
            } catch (IOException ex) {
                ChatMessage cm = new ChatMessage();
                cm.setName(message.getName());
                disconnect(cm, output);
                sendOnlines(message, output);
                System.out.println(message.getName() + " deixou o chat!");
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private boolean connect(ChatMessage message, ObjectOutputStream output) {
        if (mapOnlines.size() == 0) {
        	System.out.println("PRIMEIRO CLIENTE");
            message.setText("YES");
            send(message, output);
            return true;
        }

        if (mapOnlines.containsKey(message.getName())) {
        	System.out.println("segund if");
            message.setText("NO");
            send(message, output);
            return false;
        } else {
        	System.out.println("else");
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
        	if (message.getText().equals("command_erro_111")) {
        		message.setAction(Action.COMMAND_ERRO);
        		output.writeObject(message);
        	} else if (message.getText().equals("command_erro_999")) {
        		message.setAction(Action.COMMAND_ERRO);
        		output.writeObject(message);
        	} else {
        		output.writeObject(message);
        	}
           
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
        	String[] textSplit = message.getText().split(" ");
        	
            if (!kv.getKey().equals(message.getName())) {
                message.setAction(Action.SEND_ONE);
                if(message.getText().startsWith("rename") && textSplit.length == 2) {
                	message.setName(textSplit[1]);
                	message.setText("");
                }
                try {
                    kv.getValue().writeObject(message);
                } catch (IOException ex) {
                    Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
                }
            } 
        }
    }

    private void sendOnlines(ChatMessage message, ObjectOutputStream output) {
//    	System.out.println("qqqqqqqqqqqq");
        Set<String> setNames = new HashSet<String>();
        
        for (Map.Entry<String, ObjectOutputStream> kv : mapOnlines.entrySet()) {
            setNames.add(kv.getKey());
        }
        
        if(message.getText().equals("list")) {
        	message.setSetOnlines(setNames);
        	message.setAction(Action.USERS_ONLINE);
            send(message, output);
            return;
        }
        
        System.out.println("SSSSSS" + setNames);
        message = new ChatMessage();
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
