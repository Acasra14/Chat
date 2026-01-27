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
        // Pedir nombre al usuario
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Acceso al Chat");
        dialog.setHeaderText(null);
        dialog.setContentText("Introduce tu nombre o nick:");
        nombre = dialog.showAndWait().orElse("Anonimo");
        labelUsuario.setText("CONEXIÓN DEL CLIENTE CHAT: " + nombre);

        try {
            socket = new Socket("localhost", 44444);
            fsalida = new DataOutputStream(socket.getOutputStream());
            fentrada = new DataInputStream(socket.getInputStream());

            // Hilo de escucha
            new Thread(() -> {
                try {
                    while (true) {
                        String msg = fentrada.readUTF();
                        Platform.runLater(() -> {
                            if (primerRecibo) {
                                // Guardamos lo que ya había en el chat para ocultarlo
                                mensajesAnteriores = msg;
                                primerRecibo = false;
                            } else {
                                // Mostramos solo los mensajes generados tras nuestra entrada
                                String nuevos = msg.substring(mensajesAnteriores.length());
                                areaChat.setText(nuevos);
                            }
                        });
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> areaChat.appendText("\nConexión perdida con el servidor."));
                }
            }).start();

        } catch (IOException e) {
            System.err.println("No se pudo conectar al servidor.");
        }
    }

    @FXML
    private void handleEnviar() {
        String texto = inputMensaje.getText().trim();
        if (!texto.isEmpty()) {
            try {
                fsalida.writeUTF(nombre + "> " + texto);
                inputMensaje.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleSalir() {
        try {
            if (fsalida != null) fsalida.writeUTF("*****");
            if (socket != null) socket.close();
            System.exit(0);
        } catch (IOException e) {
            System.exit(0);
        }
    }
}