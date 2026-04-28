package org.pspro_activ2_castellano_ramos_adrian.chat;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.*;
import java.net.Socket;

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

    @FXML
    public void initialize() {
        pedirNombreYConectar();
    }

    private void pedirNombreYConectar() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Acceso al Chat");
        dialog.setHeaderText("Identificación de usuario [cite: 28]");
        dialog.setContentText("Introduce tu nombre o nick:");
        nombre = dialog.showAndWait().orElse("Anonimo");

        try {
            socket = new Socket("localhost", 44444);
            fsalida = new DataOutputStream(socket.getOutputStream());
            fentrada = new DataInputStream(socket.getInputStream());

            fsalida.writeUTF(nombre);
            if (fentrada.readUTF().equals("NICK_DUPLICADO")) { //
                new Alert(Alert.AlertType.ERROR, "Nick duplicado").showAndWait();
                System.exit(0);
            }

            labelUsuario.setText("CONEXIÓN DEL CLIENTE CHAT: " + nombre);
            areaChat.appendText("🌟 Bienvenido. Escribe /ayuda para comandos.\n");

            new Thread(() -> {
                try {
                    while (true) {
                        String msg = fentrada.readUTF();

                        if (msg.equals("CLEAR_HISTORY")) {
                            Platform.runLater(() -> {
                                areaChat.clear();
                                primerRecibo = true; // Reiniciar para el nuevo canal
                            });
                            continue;
                        }

                        Platform.runLater(() -> {
                            if (msg.startsWith("PRV|")) {
                                areaChat.appendText("\n" + msg.substring(4));
                            } else {
                                if (primerRecibo) {
                                    mensajesAnteriores = msg;
                                    primerRecibo = false;
                                } else {
                                    String nuevos = msg.substring(mensajesAnteriores.length());
                                    areaChat.setText(areaChat.getText() + nuevos);
                                    mensajesAnteriores = msg;
                                }
                            }
                        });
                    }
                } catch (IOException e) { }
            }).start();

        } catch (IOException e) { System.exit(0); }
    }

    @FXML
    private void handleEnviar() {
        String texto = inputMensaje.getText().trim();
        if (texto.isEmpty()) return;

        try {
            if (texto.equalsIgnoreCase("/ayuda")) {
                areaChat.appendText("\n--- AYUDA ---" +
                        "\n/join <canal> : Cambiar de sala" +
                        "\n/p <nick> <msg> : Mensaje privado" +
                        "\n/ayuda : Ver esto");
            } else if (texto.startsWith("/join ") || texto.startsWith("/p ")) {
                fsalida.writeUTF(texto);
                if(texto.startsWith("/p ")) areaChat.appendText("\n[Privado enviado]");
            } else {
                fsalida.writeUTF(nombre + "> " + texto); // [cite: 31]
            }
            inputMensaje.clear();
        } catch (IOException e) { }
    }

    @FXML
    private void handleSalir() {
        try {
            fsalida.writeUTF("*****"); // [cite: 33]
            socket.close();
            System.exit(0);
        } catch (IOException e) { System.exit(0); }
    }
}