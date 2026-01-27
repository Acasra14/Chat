package org.pspro_activ2_castellano_ramos_adrian.chat;

import java.io.*;
import java.net.*;

public class HiloServidorChat extends Thread {
    private Socket socket;
    private InfoHilos infoh;
    private DataInputStream fentrada;

    public HiloServidorChat(Socket s, InfoHilos infoh) {
        this.socket = s;
        this.infoh = infoh;
        try {
            this.fentrada = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // Enviar historial actual al conectar
        enviarMensajesATodos(infoh.getMensajes());

        while (true) {
            try {
                String cadena = fentrada.readUTF();

                if (cadena.trim().equals("*****")) {
                    infoh.setActuales(infoh.getActuales() - 1);
                    break;
                }

                infoh.setMensajes(infoh.getMensajes() + cadena + "\n");
                enviarMensajesATodos(infoh.getMensajes());
            } catch (IOException e) {
                break;
            }
        }

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void enviarMensajesATodos(String txt) {
        Socket[] tabla = infoh.getTabla();
        for (int i = 0; i < infoh.getConexiones(); i++) {
            Socket s = tabla[i];
            if (s != null && !s.isClosed()) {
                try {
                    DataOutputStream fout = new DataOutputStream(s.getOutputStream());
                    fout.writeUTF(txt);
                } catch (IOException e) {
                    // El cliente pudo cerrar la conexión
                }
            }
        }
    }
}