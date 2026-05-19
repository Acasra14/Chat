package org.pspro_activ2_castellano_ramos_adrian.chat;

import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServidorChat {

    public static synchronized void log(String mensaje) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String logFinal = "[" + timeStamp + "] " + mensaje;
        System.out.println(logFinal);
        try (PrintWriter out = new PrintWriter(new FileWriter("server_backup.log", true))) {
            out.println(logFinal);
        } catch (IOException e) {
            System.err.println("Error al guardar backup del log.");
        }
    }

    public static void main(String[] args) {
        int puerto = 44444;
        int numMaxConexiones = 4; // Ajustamos el límite a 4 usuarios concurrentes

        try (ServerSocket servidor = new ServerSocket(puerto)) {
            log("[LOG SERVER] Servidor SEGURO iniciado en puerto " + puerto + ". Esperando clientes...");
            InfoHilos info = new InfoHilos(numMaxConexiones);

            // --- NUEVO: BUCLE INFINITO PARA SEGUIR ESCUCHANDO SIEMPRE ---
            while (true) {
                Socket socket = servidor.accept();
                log("[LOG SERVER] Intento de conexión entrante desde " + socket.getInetAddress());

                // --- CONTROL DE SALA LLENA ---
                if (info.getActuales() >= numMaxConexiones) {
                    log("[SEGURIDAD] Conexión rechazada: El chat está lleno (" + info.getActuales() + "/" + numMaxConexiones + ").");
                    try {
                        DataOutputStream fsalidaTemp = new DataOutputStream(socket.getOutputStream());
                        fsalidaTemp.writeUTF("CHAT_LLENO"); // Enviamos señal de rechazo por exceso de aforo
                        socket.close();
                    } catch (IOException e) {
                        log("[ERROR] No se pudo enviar el mensaje de chat lleno al cliente.");
                    }
                    continue; // Saltamos al siguiente ciclo del bucle sin crear hilos
                }

                // Si hay hueco libre, buscamos cuál es la ranura disponible en el array
                // Si hay hueco libre, buscamos cuál es la ranura disponible en el array
                int idPosicion = info.getPosicionLibre();
                if (idPosicion == -1) {
                    idPosicion = info.getPosicionLibre();
                }

                if (idPosicion != -1) {
                    info.anadirATabla(socket, idPosicion);
                    HiloServidorChat hilo = new HiloServidorChat(socket, info, idPosicion);
                    hilo.start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}