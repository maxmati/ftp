package pl.maxmati.po.ftp.server;

import pl.maxmati.po.ftp.server.database.dao.UsersDAO;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Created by maxmati on 1/8/16
 */
public class UsersManager {
    private final UsersDAO dao;

    public UsersManager(UsersDAO dao) {
        this.dao = dao;
    }

    public User getByName(String username) {
        return dao.findUserByUsername(username);
    }

    public boolean validatePassword(User user, String password){
        String hash = generatePasswordHash(password, user.getSalt());
        boolean valid = user.getPassword().equals(hash);

        if(valid)
            System.out.println("Successfully validated password for : " + user.getUsername());
        else
            System.out.println("Failed validation of password for: " + user.getUsername()
                    + " (expected: " + user.getPassword() + " have: " + hash + ")");

        return valid;
    }

    private String generatePasswordHash(String password, String salt){
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 128);
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = f.generateSecret(spec).getEncoded();
            Base64.Encoder enc = Base64.getEncoder();
            return enc.encodeToString(hash);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
