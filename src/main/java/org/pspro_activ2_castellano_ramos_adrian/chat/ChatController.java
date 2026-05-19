package org.pspro_activ2_castellano_ramos_adrian.chat;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class ChatController {
    @FXML private Label labelUsuario;
    @FXML private TextField inputMensaje;
    @FXML private TextArea areaChat;

    private Socket socket;
    private DataOutputStream fsalida;
    private DataInputStream fentrada;
    private String nombre;
    private String mensajesAnteriores = "";
    private boolean primerRecibo = true;

    private final String CARPETA_DESCARGAS = "Descargas_Chat";

    @FXML
    public void initialize() {
        new File(CARPETA_DESCARGAS).mkdir();
        pedirNombreYConectar();
    }

    private void pedirNombreYConectar() {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Acceso Seguro al Chat");
        dialog.setHeaderText("Introduce credenciales");

        ButtonType loginButtonType = new ButtonType("Entrar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        TextField username = new TextField(); username.setPromptText("Nickname");
        PasswordField password = new PasswordField(); password.setPromptText("Password");
        grid.add(new Label("Nick:"), 0, 0); grid.add(username, 1, 0);
        grid.add(new Label("Pass:"), 0, 1); grid.add(password, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) return new String[]{username.getText(), password.getText()};
            return null;
        });

        String[] result = dialog.showAndWait().orElse(null);
        if (result == null) System.exit(0);

        nombre = result[0];
        String pass = result[1];

        nombre = nombre.replaceAll("http[s]?://\\S+", "");
        if (!nombre.matches("^[a-zA-Z][^\\s#$%&€()=?¿!¡,;+\\-]*$")) {
            new Alert(Alert.AlertType.ERROR, "Nick inválido.").showAndWait();
            System.exit(0);
        }

        try {
            socket = new Socket("localhost", 44444);
            fsalida = new DataOutputStream(socket.getOutputStream());
            fentrada = new DataInputStream(socket.getInputStream());

            String controlAforo = fentrada.readUTF();
            if (controlAforo.equals("CHAT_LLENO")) {
                new Alert(Alert.AlertType.WARNING, "El chat está lleno. Espera.").showAndWait();
                System.exit(0);
                return;
            }

            fsalida.writeUTF(nombre);
            fsalida.writeUTF(pass);

            String respuesta = fentrada.readUTF();
            if (respuesta.equals("LOGIN_FAIL") || respuesta.equals("NICK_DUPLICADO")) {
                new Alert(Alert.AlertType.ERROR, "Error de acceso.").showAndWait();
                System.exit(0);
            }

            labelUsuario.setText("CONEXIÓN DEL CLIENTE CHAT: " + nombre);
            areaChat.appendText("🌟 Bienvenido.\n");

            new Thread(() -> {
                try {
                    while (true) {
                        String msg = fentrada.readUTF();

                        if (msg.equals("CLEAR_HISTORY")) {
                            Platform.runLater(() -> { areaChat.clear(); primerRecibo = true; });
                            continue;
                        }

                        Platform.runLater(() -> {
                            // --- RECEPCIÓN DE ARCHIVOS ---
                            if (msg.startsWith("FILEPUB|") || msg.startsWith("FILEPRIV|")) {
                                procesarArchivoRecibido(msg, msg.startsWith("FILEPRIV|"));
                            }
                            // --- PRIVADOS ---
                            else if (msg.startsWith("PRV|")) {
                                String[] partes = msg.split("\\|", 5);
                                if (partes.length >= 4) {
                                    String tipo = partes[1]; // PUEDE SER "FROM", "TO", O "AYUDA"

                                    if (tipo.equals("AYUDA")) {
                                        // Muestra directamente el texto de ayuda enviado por el servidor
                                        areaChat.appendText("\n" + partes[3]);
                                    } else {
                                        // Lógica original para privados
                                        String otroUsuario = partes[2];
                                        String msgCifrado = partes[3];
                                        String descifrado = SeguridadUtil.descifrarAES(msgCifrado);
                                        String texto = descifrado != null ? descifrado : "[Error descifrando]";
                                        areaChat.appendText("\n" + (tipo.equals("FROM") ? "[Privado de " : "[Tú a ") + otroUsuario + "]: " + texto);
                                    }
                                }
                            }
                            // --- HISTORIAL NORMAL ---
                            else {
                                if (primerRecibo) {
                                    areaChat.appendText(msg);
                                    mensajesAnteriores = msg; primerRecibo = false;
                                } else {
                                    String nuevos = msg.substring(mensajesAnteriores.length());
                                    areaChat.appendText(nuevos);
                                    mensajesAnteriores = msg;
                                }
                            }
                        });
                    }
                } catch (IOException e) { }
            }).start();

        } catch (IOException e) { System.exit(0); }
    }

    private void procesarArchivoRecibido(String msg, boolean esPrivado) {
        String[] partes = msg.split("\\|", 5);
        if (partes.length == 5) {
            String emisor = partes[2];
            String filename = partes[3];
            String base64Datos = partes[4];
            try {
                if (esPrivado) base64Datos = SeguridadUtil.descifrarAES(base64Datos);
                byte[] fileBytes = Base64.getDecoder().decode(base64Datos);
                Files.write(Paths.get(CARPETA_DESCARGAS + "/" + filename), fileBytes);

                String tipo = esPrivado ? "[PRIVADO CIFRADO de " + emisor + "]" : emisor;
                areaChat.appendText("\n📎 " + tipo + " envió: " + filename + " (Guardado en " + CARPETA_DESCARGAS + ")");
                mensajesAnteriores = areaChat.getText();
            } catch (Exception e) {
                areaChat.appendText("\n[Error al procesar archivo]");
            }
        }
    }

    @FXML
    private void handleEnviar() {
        String texto = inputMensaje.getText().trim();
        if (texto.isEmpty()) return;

        try {
            // Ahora siempre enviamos al servidor, incluso el /ayuda
            if (texto.equalsIgnoreCase("/ayuda") ||
                    texto.startsWith("/join ") ||
                    texto.startsWith("/ban ") ||
                    texto.startsWith("/tempmod ")) {
                fsalida.writeUTF(texto);
            }
            else if (texto.startsWith("/sendfile ")) {
                enviarArchivo(texto.substring(10).trim(), null);
            }
            else if (texto.startsWith("/sendfilepriv ")) {
                String[] partes = texto.split(" ", 3);
                if (partes.length == 3) enviarArchivo(partes[2], partes[1]);
            }
            else if (texto.startsWith("/p ")) {
                String[] partes = texto.split(" ", 3);
                if(partes.length == 3) {
                    fsalida.writeUTF("/p " + partes[1] + " " + SeguridadUtil.cifrarAES(partes[2]));
                }
            } else {
                fsalida.writeUTF(texto); // El nombre ya lo añade el servidor
            }
            inputMensaje.clear();
        } catch (IOException e) { }
    }

    private void enviarArchivo(String ruta, String destinatario) {
        try {
            ruta = ruta.replace("\"", "").replace("'", "");
            File file = new File(ruta);
            if (!file.exists() || file.length() > 45000) {
                areaChat.appendText("\n[Error] Archivo no existe o >45KB.");
                return;
            }
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String base64 = Base64.getEncoder().encodeToString(fileContent);
            if (destinatario == null) fsalida.writeUTF("/filepub " + file.getName() + " " + base64);
            else fsalida.writeUTF("/filepriv " + destinatario + " " + file.getName() + " " + SeguridadUtil.cifrarAES(base64));
        } catch (Exception e) {
            areaChat.appendText("\n[Error al leer archivo]");
        }
    }

    @FXML
    private void handleSalir() {
        try { fsalida.writeUTF("*****"); socket.close(); System.exit(0); } catch (IOException e) { System.exit(0); }
    }
}