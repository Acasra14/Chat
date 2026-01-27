package org.pspro_activ2_castellano_ramos_adrian.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorChat {
    public static void main(String[] args) {
        int puerto = 44444;
        int numMaxConexiones = 4;

        try (ServerSocket servidor = new ServerSocket(puerto)) {
            System.out.println("Servidor iniciado en puerto " + puerto + "...");
            InfoHilos info = new InfoHilos(numMaxConexiones);

            while (info.getConexiones() < numMaxConexiones) {
                Socket socket = servidor.accept();
                System.out.println("Nueva conexión aceptada.");

                info.anadirATabla(socket, info.getConexiones());
                info.setActuales(info.getActuales() + 1);
                info.setConexiones(info.getConexiones() + 1);

                HiloServidorChat hilo = new HiloServidorChat(socket, info);
                hilo.start();
            }
            System.out.println("Límite de conexiones alcanzado.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}