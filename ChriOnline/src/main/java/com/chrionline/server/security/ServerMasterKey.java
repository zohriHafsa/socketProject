package com.chrionline.server.security;

import com.chrionline.shared.security.AESCipher;

import javax.crypto.SecretKey;

public class ServerMasterKey {
    private static SecretKey masterKey= null;
    public  static  SecretKey get() throws  Exception{
        if(masterKey==null){
            byte[] seed = "ChrionlineMasterKey2026Secure!!".getBytes("UTF-8");
            //AES key must be exactly 32 bytes for AES-256
            byte[] keyBytes = java.util.Arrays.copyOf(seed,32);
            masterKey = AESCipher.keyFromBytes(keyBytes);
        }
        return masterKey;
    }
}
