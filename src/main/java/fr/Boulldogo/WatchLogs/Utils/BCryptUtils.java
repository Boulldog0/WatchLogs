package fr.Boulldogo.WatchLogs.Utils;

import org.mindrot.jbcrypt.BCrypt;

public class BCryptUtils {

    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}