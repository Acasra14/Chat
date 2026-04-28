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
                fsalida.writeUTF("NICK_DUPLICADO");
                socket.close();
                return;
            }

            fsalida.writeUTF("NICK_OK");
            infoh.registrarNick(idPosicion, nick);
            infoh.setCanalUsuario(idPosicion, "General"); // Canal inicial [cite: 7]
            infoh.setActuales(infoh.getActuales() + 1);
            infoh.setConexiones(infoh.getConexiones() + 1);

            // Notificar entrada al canal General [cite: 35]
            String avisoEntrada = "> Entra en el Chat ... " + nick;
            infoh.addMensajeACanal("General", avisoEntrada);
            enviarMensajeAlCanal("General", infoh.getHistorialCanal("General"));

            while (true) {
                String cadena = fentrada.readUTF();
                String canalActual = infoh.getCanalUsuario(idPosicion);

                if (cadena.trim().equals("*****")) { // Salida del usuario [cite: 33]
                    infoh.addMensajeACanal(canalActual, "> Abandona el Chat ... " + nick);
                    enviarMensajeAlCanal(canalActual, infoh.getHistorialCanal(canalActual));
                    infoh.liberarPosicion(idPosicion);
                    infoh.setActuales(infoh.getActuales() - 1);
                    break;
                }

                // COMANDO CAMBIO DE CANAL
                if (cadena.startsWith("/join ")) {
                    String nuevoCanal = cadena.substring(6).trim();
                    // Avisar salida del canal viejo
                    infoh.addMensajeACanal(canalActual, "> " + nick + " se ha ido a #" + nuevoCanal);
                    enviarMensajeAlCanal(canalActual, infoh.getHistorialCanal(canalActual));

                    // Cambiar y avisar entrada al nuevo
                    infoh.setCanalUsuario(idPosicion, nuevoCanal);
                    fsalida.writeUTF("CLEAR_HISTORY"); // Señal para el cliente
                    infoh.addMensajeACanal(nuevoCanal, "> " + nick + " ha entrado en #" + nuevoCanal);
                    enviarMensajeAlCanal(nuevoCanal, infoh.getHistorialCanal(nuevoCanal));
                }
                // MENSAJES PRIVADOS
                else if (cadena.startsWith("/p ")) {
                    procesarPrivado(cadena);
                }
                // MENSAJE NORMAL
                else {
                    infoh.addMensajeACanal(canalActual, cadena);
                    enviarMensajeAlCanal(canalActual, infoh.getHistorialCanal(canalActual));
                }
            }
        } catch (IOException e) {
            System.out.println("[LOG] Desconexión de " + nick);
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
            if (posDest != -1) {
                new DataOutputStream(infoh.getTabla()[posDest].getOutputStream())
                        .writeUTF("PRV|[Privado de " + nick + "]: " + partes[2]);
            } else {
                fsalida.writeUTF("PRV|Usuario no encontrado.");
            }
        }
    }
}