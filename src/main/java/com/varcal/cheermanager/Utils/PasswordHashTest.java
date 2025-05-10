package com.varcal.cheermanager.Utils;

import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.Scanner;

public class PasswordHashTest {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Solicitar la contraseña al usuario
        System.out.print("Introduce una contraseña para hashear: ");
        String password = scanner.nextLine();

        // Generar el hash de la contraseña
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        System.out.println("Hash generado: " + hashedPassword);

        // Probar la comparación de contraseñas
        System.out.print("Introduce la contraseña nuevamente para verificar: ");
        String passwordToVerify = scanner.nextLine();

        boolean isMatch = BCrypt.checkpw(passwordToVerify, hashedPassword);
        if (isMatch) {
            System.out.println("¡La contraseña coincide con el hash!");
        } else {
            System.out.println("La contraseña no coincide con el hash.");
        }

        scanner.close();
    }
}
