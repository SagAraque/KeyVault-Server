package com.keyvault.controllers;

import com.keyvault.PasswordController;
import com.keyvault.database.models.SessionToken;
import com.keyvault.database.models.Users;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class RedisController {
    private JedisPool pool;
    private Jedis jedis;
    public RedisController(String password)
    {
        //pool = new JedisPool(new JedisPoolConfig(), "129.151.227.217");
        pool = new JedisPool(new JedisPoolConfig(), "localhost");
        jedis = pool.getResource();
        jedis.auth(password);
    }

    public void revalidateToken(SessionToken token)
    {
        jedis.select(1);

        if(jedis.exists(token.getValue()))
            jedis.expire(token.getValue(), 600);
    }

    public SessionToken generateToken(Users user) throws NoSuchAlgorithmException, NoSuchPaddingException {
        PasswordController passwordController = new PasswordController();
        Random random = ThreadLocalRandom.current();
        byte[] bytes = new byte[64];
        random.nextBytes(bytes);

        int idUser = user.getIdU();
        String token = passwordController.hashData(idUser + System.currentTimeMillis() + new String(bytes));

        jedis.select(1);
        jedis.set(token, String.valueOf(idUser));
        jedis.expire(token, 600);

        return new SessionToken(token, user);
    }

    public boolean checkSessionToken(SessionToken token)
    {
        jedis.select(1);
        String tokenId = jedis.get(token.getValue());

        if(tokenId != null)
            return tokenId.equals(String.valueOf(token.getUser().getIdU()));

        return false;
    }

    public String generateVerifyToken(Users user)
    {
        jedis.select(2);
        int authNum = new Random().nextInt(100000, 999999);

        jedis.set(String.valueOf(authNum), String.valueOf(user.getIdU()));
        jedis.expire(String.valueOf(authNum), 240);

        return String.valueOf(authNum);
    }

    public boolean validateVerifyToken(String authNum, Users user)
    {
        jedis.select(2);

        String idUser = jedis.get(authNum);

        if(idUser != null)
            return idUser.equals(String.valueOf(user.getIdU()));

        return false;
    }

    public String getConfigValue(String key)
    {
        jedis.select(0);

        return jedis.get(key);
    }

}
