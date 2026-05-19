package org.pspro_activ2_castellano_ramos_adrian.chat;

import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class SeguridadUtil {
    // Clave secreta estática para el cifrado AES (en un entorno real se generaría dinámicamente)
    private static final String AES_KEY = "1234567890123456";

    // --- HASHING SHA-256 PARA PASSWORDS ---
    public static String generarHashSHA256(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar SHA-256", e);
        }
    }

    // --- CIFRADO AES PARA MENSAJES PRIVADOS Y ARCHIVOS ---
    public static String cifrarAES(String datos) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(AES_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] datosCifrados = cipher.doFinal(datos.getBytes());
            return Base64.getEncoder().encodeToString(datosCifrados);
        } catch (Exception e) {
            return null;
        }
    }

    public static String descifrarAES(String datosCifradosBase64) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(AES_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] datosDescifrados = cipher.doFinal(Base64.getDecoder().decode(datosCifradosBase64));
            return new String(datosDescifrados);
        } catch (Exception e) {
            return null;
        }
    }
}