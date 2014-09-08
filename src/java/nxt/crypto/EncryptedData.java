package nxt.crypto;

import nxt.NxtException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class EncryptedData {

    private static final ThreadLocal<SecureRandom> secureRandom = new ThreadLocal<SecureRandom>() {
        @Override
        protected SecureRandom initialValue() {
            return new SecureRandom();
        }
    };

    public static final EncryptedData EMPTY_DATA = new EncryptedData(new byte[0], new byte[0]);

    public static EncryptedData encrypt(byte[] plaintext, byte[] myPrivateKey, byte[] theirPublicKey) {
        if (plaintext.length == 0) {
            return EMPTY_DATA;
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(plaintext);
            gzip.flush();
            gzip.close();
            byte[] compressedPlaintext = bos.toByteArray();
            byte[] nonce = new byte[32];
            secureRandom.get().nextBytes(nonce);
            byte[] data = Crypto.aesEncrypt(compressedPlaintext, myPrivateKey, theirPublicKey, nonce);
            return new EncryptedData(data, nonce);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static EncryptedData readEncryptedData(ByteBuffer buffer, int length, int maxLength)
            throws NxtException.NotValidException {
        if (length == 0) {
            return EMPTY_DATA;
        }
        if (length > maxLength) {
            throw new NxtException.NotValidException("Max encrypted data length exceeded: " + length);
        }
        byte[] noteBytes = new byte[length];
        buffer.get(noteBytes);
        byte[] noteNonceBytes = new byte[32];
        buffer.get(noteNonceBytes);
        return new EncryptedData(noteBytes, noteNonceBytes);
    }

    private final byte[] data;
    private final byte[] nonce;

    public EncryptedData(byte[] data, byte[] nonce) {
        this.data = data;
        this.nonce = nonce;
    }

    public byte[] decrypt(byte[] myPrivateKey, byte[] theirPublicKey) {
        if (data.length == 0) {
            return data;
        }
        byte[] compressedPlaintext = Crypto.aesDecrypt(data, myPrivateKey, theirPublicKey, nonce);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressedPlaintext);
             GZIPInputStream gzip = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int nRead;
            while ((nRead = gzip.read(buffer, 0, buffer.length)) > 0) {
                bos.write(buffer, 0, nRead);
            }
            bos.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public int getSize() {
        return data.length + nonce.length;
    }

}
