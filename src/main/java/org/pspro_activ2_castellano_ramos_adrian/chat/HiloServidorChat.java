package org.pspro_activ2_castellano_ramos_adrian.chat;

import java.io.*;
import java.net.*;

public class HiloServidorChat extends Thread {
    private Socket socket;
    private InfoHilos infoh;
    private DataInputStream fentrada;
    private DataOutputStream fsalida;
    private int idPosicion;
    private String nick;

    public HiloServidorChat(Socket s, InfoHilos infoh, int idPosicion) {
        this.socket = s;
        this.infoh = infoh;
        this.idPosicion = idPosicion;
        try {
            this.fentrada = new DataInputStream(socket.getInputStream());
            this.fsalida = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public void run() {
        try {
            nick = fentrada.readUTF();
            if (infoh.existeNick(nick)) {
                System.out.println("[LOG SERVER] Conexión rechazada: Nick '" + nick + "' ya está en uso.");
                fsalida.writeUTF("NICK_DUPLICADO");
                socket.close();
                return;
            }

            fsalida.writeUTF("NICK_OK");
            infoh.registrarNick(idPosicion, nick);
            infoh.setCanalUsuario(idPosicion, "General");
            infoh.setActuales(infoh.getActuales() + 1);
            infoh.setConexiones(infoh.getConexiones() + 1);

            // LOG DE ENTRADA
            System.out.println("[LOG SERVER] Cliente aceptado: " + nick + " (Conectados: " + infoh.getActuales() + ")");
            System.out.println("[LOG SERVER] " + nick + " ha entrado por defecto al canal #General");

            String avisoEntrada = "> Entra en el Chat ... " + nick;
            infoh.addMensajeACanal("General", avisoEntrada);
            enviarMensajeAlCanal("General", infoh.getHistorialCanal("General"));

            while (true) {
                String cadena = fentrada.readUTF();
                String canalActual = infoh.getCanalUsuario(idPosicion);

                if (cadena.trim().equals("*****")) {
                    // LOG DE SALIDA
                    System.out.println("[LOG SERVER] Cliente desconectado: " + nick);
                    infoh.addMensajeACanal(canalActual, "> Abandona el Chat ... " + nick);
                    enviarMensajeAlCanal(canalActual, infoh.getHistorialCanal(canalActual));
                    infoh.liberarPosicion(idPosicion);
                    infoh.setActuales(infoh.getActuales() - 1);
                    break;
                }

                if (cadena.startsWith("/join ")) {
                    String nuevoCanal = cadena.substring(6).trim();

                    // LOG DE CAMBIO DE CANAL
                    System.out.println("[LOG SERVER] " + nick + " se ha cambiado del canal #" + canalActual + " al canal #" + nuevoCanal);

                    infoh.addMensajeACanal(canalActual, "> " + nick + " se ha ido a #" + nuevoCanal);
                    enviarMensajeAlCanal(canalActual, infoh.getHistorialCanal(canalActual));

                    infoh.setCanalUsuario(idPosicion, nuevoCanal);
                    fsalida.writeUTF("CLEAR_HISTORY");
                    infoh.addMensajeACanal(nuevoCanal, "> " + nick + " ha entrado en #" + nuevoCanal);
                    enviarMensajeAlCanal(nuevoCanal, infoh.getHistorialCanal(nuevoCanal));
                }
                else if (cadena.startsWith("/p ")) {
                    procesarPrivado(cadena);
                }
                else {
                    infoh.addMensajeACanal(canalActual, cadena);
                    enviarMensajeAlCanal(canalActual, infoh.getHistorialCanal(canalActual));
                }
            }
        } catch (IOException e) {
            System.out.println("[LOG SERVER] Desconexión abrupta de " + nick);
        } finally {
            try { socket.close(); } catch (IOException e) { }
        }
    }

    private void enviarMensajeAlCanal(String canal, String txt) {
        Socket[] tabla = infoh.getTabla();
        for (int i = 0; i < infoh.getTabla().length; i++) {
            if (tabla[i] != null && canal.equals(infoh.getCanalUsuario(i))) {
                try {
                    new DataOutputStream(tabla[i].getOutputStream()).writeUTF(txt);
                } catch (IOException e) { }
            }
        }
    }

    private void procesarPrivado(String cadena) throws IOException {
        String[] partes = cadena.split(" ", 3);
        if (partes.length == 3) {
            int posDest = infoh.getPosicionPorNick(partes[1]);

            // LOG DE MENSAJE PRIVADO
            System.out.println("[LOG SERVER] " + nick + " ha enviado un mensaje privado a " + partes[1]);

            if (posDest != -1) {
                new DataOutputStream(infoh.getTabla()[posDest].getOutputStream())
                        .writeUTF("PRV|[Privado de " + nick + "]: " + partes[2]);
            } else {
                fsalida.writeUTF("PRV|Usuario no encontrado.");
            }
        }
    }
}