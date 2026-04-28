package org.pspro_activ2_castellano_ramos_adrian.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorChat {
    public static void main(String[] args) {
        int puerto = 44444;
        int numMaxConexiones = 4;

        try (ServerSocket servidor = new ServerSocket(puerto)) {
            System.out.println("[LOG SERVER] Servidor iniciado en puerto " + puerto + ". Esperando clientes...");
            InfoHilos info = new InfoHilos(numMaxConexiones);

            while (info.getConexiones() < numMaxConexiones) {
                Socket socket = servidor.accept();
                System.out.println("[LOG SERVER] Nueva conexión entrante aceptada.");

                int idPosicion = info.getConexiones();
                info.anadirATabla(socket, idPosicion);

                // Las variables se actualizarán dentro del Hilo si el Nick es válido
                HiloServidorChat hilo = new HiloServidorChat(socket, info, idPosicion);
                hilo.start();
            }
            System.out.println("[LOG SERVER] Límite absoluto de conexiones del servidor alcanzado.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}