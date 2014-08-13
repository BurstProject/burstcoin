package nxt.crypto;

public final class XoredData {

    private final byte[] data;
    private final byte[] nonce;

    public XoredData(byte[] data, byte[] nonce) {
        this.data = data;
        this.nonce = nonce;
    }

    public static XoredData encrypt(byte[] plaintext, byte[] myPrivateKey, byte[] theirPublicKey) {
        byte[] nonce = Crypto.xorEncrypt(plaintext, 0, plaintext.length, myPrivateKey, theirPublicKey);
        return new XoredData(plaintext, nonce);
    }

    public byte[] decrypt(byte[] myPrivateKey, byte[] theirPublicKey) {
        byte[] ciphertext = new byte[getData().length];
        System.arraycopy(getData(), 0, ciphertext, 0, ciphertext.length);
        Crypto.xorDecrypt(ciphertext, 0, ciphertext.length, myPrivateKey, theirPublicKey, getNonce());
        return ciphertext;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getNonce() {
        return nonce;
    }

}
