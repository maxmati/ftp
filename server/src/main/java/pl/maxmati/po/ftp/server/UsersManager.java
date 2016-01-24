package pl.maxmati.po.ftp.server;

import pl.maxmati.ftp.common.beans.User;
import pl.maxmati.po.ftp.server.database.dao.UsersDAO;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Created by maxmati on 1/8/16
 */
public class UsersManager {
    private final UsersDAO dao;
    private final SecureRandom random = new SecureRandom();


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

    private String generateSalt(){
        return new BigInteger(130, random).toString(32);
    }

    public void changePassword(User user, String password) {
        String salt = generateSalt();
        user.setSalt(salt);
        user.setPassword(generatePasswordHash(password, salt));
        dao.save(user);
    }

    public void changeName(User user, String username) {
        user.setUsername(username);
        dao.save(user);
    }

    public User createUser(String username, String password) {
        String salt = generateSalt();
        password = generatePasswordHash(password, salt);
        User user = new User(username, password, salt);
        dao.save(user);
        return user;
    }
}
