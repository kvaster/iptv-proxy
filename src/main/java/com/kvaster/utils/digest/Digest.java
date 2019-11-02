package com.kvaster.utils.digest;

import java.security.MessageDigest;

public class Digest {
    private static final char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private final MessageDigest md;

    public static Digest sha512() {
        return new Digest("SHA-512");
    }

    public static String sha512(String str) {
        return sha512().digest(str);
    }

    public static Digest sha256() {
        return new Digest("SHA-256");
    }

    public static String sha256(String str) {
        return sha256().digest(str);
    }

    public static Digest md5() {
        return new Digest("MD5");
    }

    public static String md5(String str) {
        return md5().digest(str);
    }

    private Digest(String algorithm) {
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String digest(String str) {
        md.update(str.getBytes());
        byte[] digest = md.digest();
        md.reset();
        return toHex(digest);
    }

    private static String toHex(byte[] digest) {
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(HEX[(b & 0xf0) >> 8]).append(HEX[b & 0x0f]);
        }

        return sb.toString();
    }
}
