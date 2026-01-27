package org.pspro_activ2_castellano_ramos_adrian.chat;

import java.net.Socket;

public class InfoHilos {
    private int conexiones;      // Índice para el array
    private int actuales;        // Clientes conectados actualmente
    private int maximo;          // Límite de conexiones
    private Socket[] tabla;      // Almacén de sockets de los clientes
    private String mensajes;     // Historial de la conversación

    public InfoHilos(int maximo) {
        this.maximo = maximo;
        this.conexiones = 0;
        this.actuales = 0;
        this.tabla = new Socket[maximo];
        this.mensajes = "";
    }

    public synchronized int getConexiones() { return conexiones; }
    public synchronized void setConexiones(int c) { this.conexiones = c; }

    public synchronized int getActuales() { return actuales; }
    public synchronized void setActuales(int a) { this.actuales = a; }

    public synchronized String getMensajes() { return mensajes; }
    public synchronized void setMensajes(String m) { this.mensajes = m; }

    public synchronized void anadirATabla(Socket s, int pos) {
        this.tabla[pos] = s;
    }

    public Socket[] getTabla() { return tabla; }
}