package org.pspro_activ2_castellano_ramos_adrian.chat;

import java.net.Socket;
import java.util.HashMap;

public class InfoHilos {
    private int conexiones;
    private int actuales;
    private int maximo;
    private Socket[] tabla;
    private String[] nicks;
    private String[] canalesActivos; // NUEVO: Canal en el que está cada usuario
    private HashMap<String, String> historiales; // NUEVO: Historial por canal

    public InfoHilos(int maximo) {
        this.maximo = maximo;
        this.conexiones = 0;
        this.actuales = 0;
        this.tabla = new Socket[maximo];
        this.nicks = new String[maximo];
        this.canalesActivos = new String[maximo];
        this.historiales = new HashMap<>();
        this.historiales.put("General", ""); // Canal por defecto
    }

    // --- GESTIÓN DE CANALES ---
    public synchronized String getHistorialCanal(String canal) {
        return historiales.getOrDefault(canal, "");
    }

    public synchronized void addMensajeACanal(String canal, String mensaje) {
        String actual = historiales.getOrDefault(canal, "");
        historiales.put(canal, actual + mensaje + "\n");
    }

    public synchronized void setCanalUsuario(int pos, String canal) {
        this.canalesActivos[pos] = canal;
        if (!historiales.containsKey(canal)) {
            historiales.put(canal, ""); // Crear canal si no existe
        }
    }

    public synchronized String getCanalUsuario(int pos) {
        return canalesActivos[pos];
    }

    // --- MÉTODOS EXISTENTES ACTUALIZADOS ---
    public synchronized int getConexiones() { return conexiones; }
    public synchronized void setConexiones(int c) { this.conexiones = c; }
    public synchronized int getActuales() { return actuales; }
    public synchronized void setActuales(int a) { this.actuales = a; }
    public Socket[] getTabla() { return tabla; }

    public synchronized boolean existeNick(String nick) {
        for (String n : nicks) { if (n != null && n.equalsIgnoreCase(nick)) return true; }
        return false;
    }

    public synchronized void registrarNick(int pos, String nick) { this.nicks[pos] = nick; }

    public synchronized void liberarPosicion(int pos) {
        this.tabla[pos] = null;
        this.nicks[pos] = null;
        this.canalesActivos[pos] = null;
    }

    public synchronized int getPosicionPorNick(String nick) {
        for (int i = 0; i < maximo; i++) {
            if (nicks[i] != null && nicks[i].equalsIgnoreCase(nick)) return i;
        }
        return -1;
    }

    public synchronized void anadirATabla(Socket s, int pos) {
        this.tabla[pos] = s;
    }
}