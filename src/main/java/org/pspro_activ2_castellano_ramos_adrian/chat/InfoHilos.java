package org.pspro_activ2_castellano_ramos_adrian.chat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.Socket;
import java.util.HashMap;

public class InfoHilos {
    private int conexiones;
    private int actuales;
    private int maximo;
    private Socket[] tabla;
    private String[] nicks;
    private String[] canalesActivos;
    private String[] rolesActivos;

    private HashMap<String, String> dbPasswords = new HashMap<>();
    private HashMap<String, String> dbRoles = new HashMap<>();
    private HashMap<String, Integer> intentosFallidos = new HashMap<>();
    private HashMap<String, String> historiales = new HashMap<>();

    // --- NUEVO: GESTIÓN DE SILENCIADOS POR CANAL ---
    private HashMap<String, String> usuariosSilenciadosEnCanal = new HashMap<>(); // <Nick, NombreCanal>

    public InfoHilos(int maximo) {
        this.maximo = maximo;
        this.conexiones = 0;
        this.actuales = 0;
        this.tabla = new Socket[maximo];
        this.nicks = new String[maximo];
        this.canalesActivos = new String[maximo];
        this.rolesActivos = new String[maximo];
        this.historiales.put("General", "");
        cargarUsuariosCSV();
    }

    // --- MÉTODOS DE SILENCIADO ---
    public synchronized void silenciarEnCanal(String nick, String canal) {
        usuariosSilenciadosEnCanal.put(nick, canal);
    }

    public synchronized boolean estaSilenciadoEnCanal(String nick, String canal) {
        return canal.equals(usuariosSilenciadosEnCanal.get(nick));
    }

    public synchronized void quitarSilenciado(String nick) {
        usuariosSilenciadosEnCanal.remove(nick);
    }

    private void cargarUsuariosCSV() {
        try (BufferedReader br = new BufferedReader(new FileReader("usuarios.csv"))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(",");
                if (partes.length >= 3) {
                    dbPasswords.put(partes[0].trim(), partes[1].trim());
                    dbRoles.put(partes[0].trim(), partes[2].trim());
                }
            }
            System.out.println("[SEGURIDAD] Fichero CSV de usuarios cargado correctamente.");
        } catch (Exception e) {
            System.out.println("[ERROR] No se pudo leer usuarios.csv.");
        }
    }

    public synchronized int getPosicionLibre() {
        for (int i = 0; i < maximo; i++) {
            if (tabla[i] == null) return i;
        }
        return -1;
    }

    public synchronized boolean validarCredenciales(String nick, String hashRecibido) {
        if (!dbPasswords.containsKey(nick)) return false;
        return dbPasswords.get(nick).equals(hashRecibido);
    }

    public synchronized String getRolDesdeCSV(String nick) { return dbRoles.getOrDefault(nick, "ORDINARIO"); }
    public synchronized boolean cuentaBloqueada(String nick) { return intentosFallidos.getOrDefault(nick, 0) >= 3; }
    public synchronized void registrarIntentoFallido(String nick) { intentosFallidos.put(nick, intentosFallidos.getOrDefault(nick, 0) + 1); }
    public synchronized void resetearIntentos(String nick) { intentosFallidos.put(nick, 0); }
    public synchronized void setRolUsuario(int pos, String rol) { this.rolesActivos[pos] = rol; }
    public synchronized String getRolUsuario(int pos) { return rolesActivos[pos]; }
    public synchronized String getHistorialCanal(String canal) { return historiales.getOrDefault(canal, ""); }

    public synchronized void addMensajeACanal(String canal, String mensaje) {
        historiales.put(canal, historiales.getOrDefault(canal, "") + mensaje + "\n");
    }

    public synchronized void setCanalUsuario(int pos, String canal) {
        this.canalesActivos[pos] = canal;
        if (!historiales.containsKey(canal)) historiales.put(canal, "");
    }

    public synchronized String getCanalUsuario(int pos) { return canalesActivos[pos]; }
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
        this.rolesActivos[pos] = null;
    }

    public synchronized int getPosicionPorNick(String nick) {
        for (int i = 0; i < maximo; i++) {
            if (nicks[i] != null && nicks[i].equalsIgnoreCase(nick)) return i;
        }
        return -1;
    }

    public synchronized void anadirATabla(Socket s, int pos) { this.tabla[pos] = s; }
}