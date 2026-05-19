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
            fsalida.writeUTF("CONEXION_ACEPTADA");
            nick = fentrada.readUTF();
            String passPlana = fentrada.readUTF();

            if (infoh.cuentaBloqueada(nick)) {
                fsalida.writeUTF("LOGIN_FAIL");
                socket.close();
                return;
            }

            String hashGenerado = SeguridadUtil.generarHashSHA256(passPlana);
            if (!infoh.validarCredenciales(nick, hashGenerado)) {
                infoh.registrarIntentoFallido(nick);
                fsalida.writeUTF("LOGIN_FAIL");
                socket.close();
                return;
            }

            if (infoh.existeNick(nick)) {
                fsalida.writeUTF("NICK_DUPLICADO");
                socket.close();
                return;
            }

            infoh.resetearIntentos(nick);
            fsalida.writeUTF("NICK_OK");

            String rol = infoh.getRolDesdeCSV(nick);
            infoh.registrarNick(idPosicion, nick);
            infoh.setCanalUsuario(idPosicion, "General");
            infoh.setRolUsuario(idPosicion, rol);
            infoh.setActuales(infoh.getActuales() + 1);
            infoh.setConexiones(infoh.getConexiones() + 1);

            infoh.addMensajeACanal("General", "> Entra en el Chat ... " + nick + " [" + rol + "]");
            enviarMensajeAlCanal("General", infoh.getHistorialCanal("General"));

            while (true) {
                String cadena = fentrada.readUTF();
                String canalActual = infoh.getCanalUsuario(idPosicion);

                if (cadena.trim().equals("*****")) {
                    desconectarUsuario("> Abandona el Chat ... " + nick);
                    break;
                }
                else if (cadena.equalsIgnoreCase("/ayuda")) {
                    String mensajeAyuda = "\n--- AYUDA ---\n/join <canal>\n/p <nick> <msg>\n/sendfile <ruta>\n/sendfilepriv <nick> <ruta>";
                    if ("MODERADOR".equals(rol)) mensajeAyuda += "\n\n--- MODERADOR ---\n/ban <nick>\n/tempmod <nick> <segundos>";
                    fsalida.writeUTF("PRV|AYUDA|Sistema|" + mensajeAyuda + "|0");
                }
                else if (cadena.startsWith("/join ")) {
                    String nuevoCanal = cadena.substring(6).trim();
                    infoh.quitarSilenciado(nick); // Limpiamos silenciado al moverse
                    infoh.addMensajeACanal(canalActual, "> " + nick + " se ha ido a #" + nuevoCanal);
                    enviarMensajeAlCanal(canalActual, infoh.getHistorialCanal(canalActual));
                    infoh.setCanalUsuario(idPosicion, nuevoCanal);
                    fsalida.writeUTF("CLEAR_HISTORY");
                    infoh.addMensajeACanal(nuevoCanal, "> " + nick + " ha entrado en #" + nuevoCanal);
                    enviarMensajeAlCanal(nuevoCanal, infoh.getHistorialCanal(nuevoCanal));
                }
                else if (cadena.startsWith("/p ")) procesarPrivado(cadena);
                else if (cadena.startsWith("/filepub ")) {
                    String[] partes = cadena.split(" ", 3);
                    if(partes.length == 3) {
                        infoh.addMensajeACanal(canalActual, "📎 " + nick + " compartió: " + partes[1]);
                        enviarMensajeAlCanal(canalActual, "FILEPUB|0|" + nick + "|" + partes[1] + "|" + partes[2]);
                    }
                }
                else if (cadena.startsWith("/filepriv ")) {
                    String[] partes = cadena.split(" ", 4);
                    if(partes.length == 4) {
                        int posDest = infoh.getPosicionPorNick(partes[1]);
                        if (posDest != -1) {
                            new DataOutputStream(infoh.getTabla()[posDest].getOutputStream())
                                    .writeUTF("FILEPRIV|0|" + nick + "|" + partes[2] + "|" + partes[3]);
                            fsalida.writeUTF("PRV|TO|" + partes[1] + "|Archivo cifrado enviado.|0");
                        }
                    }
                }
                else if (cadena.startsWith("/ban ")) {
                    if (rol.equals("MODERADOR")) ejecutarBan(cadena.substring(5).trim(), canalActual);
                }
                else if (cadena.startsWith("/tempmod ")) {
                    if (rol.equals("MODERADOR")) procesarTempMod(cadena, canalActual);
                }
                // MENSAJE NORMAL: Validamos si está silenciado en el canal actual
                else {
                    if (infoh.estaSilenciadoEnCanal(nick, canalActual)) {
                        fsalida.writeUTF("PRV|AYUDA|Sistema|⚠️ Estás silenciado en este canal.|0");
                    } else {
                        infoh.addMensajeACanal(canalActual, nick + "> " + cadena);
                        enviarMensajeAlCanal(canalActual, infoh.getHistorialCanal(canalActual));
                    }
                }
            }
        } catch (IOException e) {
        } finally {
            try { socket.close(); } catch (IOException e) { }
        }
    }

    private void enviarMensajeAlCanal(String canal, String txt) {
        Socket[] tabla = infoh.getTabla();
        for (int i = 0; i < infoh.getTabla().length; i++) {
            if (tabla[i] != null && canal.equals(infoh.getCanalUsuario(i))) {
                try { new DataOutputStream(tabla[i].getOutputStream()).writeUTF(txt); } catch (IOException e) { }
            }
        }
    }

    private void procesarPrivado(String cadena) throws IOException {
        String[] partes = cadena.split(" ", 3);
        if (partes.length == 3) {
            int posDest = infoh.getPosicionPorNick(partes[1]);
            if (posDest != -1) {
                new DataOutputStream(infoh.getTabla()[posDest].getOutputStream())
                        .writeUTF("PRV|FROM|" + nick + "|" + partes[2] + "|0");
                fsalida.writeUTF("PRV|TO|" + partes[1] + "|" + partes[2] + "|0");
            }
        }
    }

    private void ejecutarBan(String nickAExpulsar, String canal) {
        infoh.silenciarEnCanal(nickAExpulsar, canal);
        infoh.addMensajeACanal(canal, "🚨 " + nickAExpulsar + " ha sido silenciado.");
        enviarMensajeAlCanal(canal, infoh.getHistorialCanal(canal));
    }

    private void procesarTempMod(String cadena, String canal) {
        try {
            String[] partes = cadena.split(" ");
            String target = partes[1];
            int pos = infoh.getPosicionPorNick(target);
            if (pos != -1) {
                infoh.setRolUsuario(pos, "MODERADOR");
                infoh.addMensajeACanal(canal, "👑 " + target + " es moderador temporal.");
                enviarMensajeAlCanal(canal, infoh.getHistorialCanal(canal));
            }
        } catch (Exception e) {}
    }

    private void desconectarUsuario(String mensajeSalida) {
        String canal = infoh.getCanalUsuario(idPosicion);
        infoh.addMensajeACanal(canal, mensajeSalida);
        enviarMensajeAlCanal(canal, infoh.getHistorialCanal(canal));
        infoh.liberarPosicion(idPosicion);
        infoh.setActuales(infoh.getActuales() - 1);
    }
}