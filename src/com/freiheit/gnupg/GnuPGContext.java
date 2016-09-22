/*
 * $Id: GnuPGContext.java,v 1.4 2006/07/03 15:32:16 sneumann Exp $
 * (c) Copyright 2005 freiheit.com technologies gmbh, Germany.
 *
 * This file is part of Java for GnuPG  (http://www.freiheit.com).
 *
 * Java for GnuPG is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * Please see COPYING for the complete licence.
 */

package com.freiheit.gnupg;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Start here, because for all operations, you first need to create a
 * GnuPGContext. Use one context object for every single thread or (really) take
 * care about synchronizing access to the context from different threads.
 * <p>
 * <em>How to create a context?</em>
 * 
 * <pre>
 * {
 *     &#064;code
 *     GnuPGContext ctx = new GnuPGContext();
 * }
 * </pre>
 * 
 * @author Stefan Richter, stefan@freiheit.com
 */

public class GnuPGContext extends GnuPGPeer {
    public static final String TAG = "GnuPGContext";
    static {
        System.loadLibrary("gnupg-for-java");
    }

    private String _version;
    private String _filename;
    private String _reqversion;
    private int _protocol;

    /** Creates a new Context (use on context for one thread!) */
    public GnuPGContext() {
        gpgmeGetEngineInfo();
        setInternalRepresentation(gpgmeNew());
    }

    /**
     * Returns the version of the underlying GPGME lib version.
     * 
     * @return library version as String (like "1.1.1")
     */
    public String getVersion() {
        return _version;
    }

    /**
     * Returns the executable gpg with path
     * 
     * @return gpg executable as String (like "/usr/bin/gpg")
     */
    public String getFilename() {
        return _filename;
    }

    /**
     * Returns the required version of the underlying GPGME lib.
     * 
     * @return required library version as String (like "1.1.1")
     */
    public String getRequiredVersion() {
        return _reqversion;
    }

    /**
     * Returns the used protocol of the crypto backend.
     * 
     * @return protocol
     */
    public int getProtocol() {
        return _protocol;
    }

    /**
     * Tells you, is the gpg engine uses ASCII armor. Please check GPG/GPGME for
     * documentation if you don't know what this means. (Try 'man gpg' on the
     * command line and look for 'armor')
     * 
     * @return boolean true if results will be ASCII armored, if false binary
     */
    public boolean isArmor() {
        return gpgmeGetArmor(getInternalRepresentation());
    }

    /**
     * Tell the gpg engine to use ASCII armor. Please check GPG/GPGME for
     * documentation if you don't know what this means. (Try 'man gpg' on the
     * command line and look for 'armor')
     * 
     * @param state set true if results should be ASCII armored, set false if
     *            binary
     */
    public void setArmor(boolean state) {
        gpgmeSetArmor(getInternalRepresentation(), state);
    }

    /**
     * Tells you, is the gpg engine is in text mode. Please check GPG/GPGME for
     * documentation if you don't know what this means.
     * 
     * @return boolean true means text mode is on
     */
    public boolean isTextmode() {
        return gpgmeGetTextmode(getInternalRepresentation());
    }

    /**
     * Get the current text mode from the gpg engine. Please check GPG/GPGME for
     * documentation if you don't know what this means.
     * 
     * @return boolean if textmode is currently enabled
     */
    public boolean getTextmode() {
        return gpgmeGetTextmode(getInternalRepresentation());
    }

    /**
     * Tell the gpg engine to set the text mode. Please check GPG/GPGME for
     * documentation if you don't know what this means.
     * 
     * @param state set true if you want text mode switched on
     */
    public void setTextmode(boolean state) {
        gpgmeSetTextmode(getInternalRepresentation(), state);
    }

    private long[] getInternalRepresentationFromRecipients(GnuPGKey[] recipients) {
        // note that these are pointers to addresses in the gnupg-for-java
        // shared lib
        long recipientsInternals[] = new long[recipients.length];
        for (int i = 0; i < recipients.length; i++) {
            if (recipients[i] != null)
                recipientsInternals[i] = recipients[i].getInternalRepresentation();
            else
                ; // FIXME: throw exception here? Just skip over it for now.
        }
        return recipientsInternals;
    }

    private boolean hasNoRecipients(GnuPGKey[] recipients) {
        return !(recipients != null && recipients.length > 0);
    }

    /**
     * Gets the public key with the supplied fingerprint from the keyring. This
     * is also kind of a factory method to generate key objects, because you
     * always need a context to access the keys in your keyring.
     * 
     * @param fingerprint 16 char hex fingerprint of key in your keyring
     * @return GnuPGKey the public key that matches the fingerprint
     * @see com.freiheit.gnupg.GnuPGKey
     */
    public GnuPGKey getKeyByFingerprint(String fingerprint) throws GnuPGException {
        if (fingerprint == null || fingerprint.length() < 1) {
            return null;
        } else {
            return new GnuPGKey(this, fingerprint, false);
        }
    }

    /**
     * Gets the secret key with the supplied fingerprint from the keyring. This
     * is also kind of a factory method to generate secret key objects, because
     * you always need a context to access the keys in your keyring.
     * 
     * @param fingerprint 16 char hex fingerprint of key in your keyring
     * @return GnuPGKey the secret key that matches the fingerprint
     * @see com.freiheit.gnupg.GnuPGKey
     */
    public GnuPGKey getSecretKeyByFingerprint(String fingerprint)
            throws GnuPGException {
        if (fingerprint == null || fingerprint.length() < 1) {
            return null;
        } else {
            return new GnuPGKey(this, fingerprint, true);
        }
    }

    /**
     * Factory method to generate a GnuPGData instance from a File.
     * 
     * @param f should not be null or a directory and must be readable
     * @return GnuPGData instance with data from <code>f</code>, unless
     *         <code>f</code> is has no readable data in it, then it returns
     *         null
     * @throws IOException
     * @see com.freiheit.gnupg.GnuPGData
     */
    public GnuPGData createDataObject(File f) throws GnuPGException, IOException {
        if (f != null && f.exists() && !f.isDirectory() && f.canRead()) {
            return new GnuPGData(f);
        }
        return null;
    }

    /**
     * Factory method to generate a GnuPGData instance from a String.
     * 
     * @param data should not be null and should have a length > 0
     * @return GnuPGData instance based on <code>data</code>, unless
     *         <code>data</code> is null or has a length of 0, then it returns
     *         null
     * @see com.freiheit.gnupg.GnuPGData
     */
    public GnuPGData createDataObject(String data) throws GnuPGException {
        if (data == null || data.length() < 1) {
            return null;
        }
        else {
            return new GnuPGData(data);
        }
    }

    /**
     * Factory method to generate a GnuPGData instance from a byte array.
     * 
     * @param data should not be null and should have a length > 0
     * @return GnuPGData instance based on <code>data</code>, unless
     *         <code>data</code> is null or has a length of 0, then it returns
     *         null
     * @see com.freiheit.gnupg.GnuPGData
     */
    public GnuPGData createDataObject(byte[] data) throws GnuPGException {
        if (data == null || data.length < 1) {
            return null;
        }
        else {
            return new GnuPGData(data);
        }
    }

    /**
     * Factory method to generate an empty GnuPGData instance.
     * 
     * @return an empty GnuPGData instance
     * @see com.freiheit.gnupg.GnuPGData
     */
    public GnuPGData createDataObject() throws GnuPGException {
        return new GnuPGData();
    }

    /**
     * Convenience method to generate empty key arrays. To encrypt data, you
     * need to supply a list of recipients in an array. This is a little helper.
     * It will not search for keys! Its just an empty array.
     * 
     * @param withLengthOf length of the array to generate (number of
     *            recipients)
     * @return GnuPGKey empty array of keys
     * @see com.freiheit.gnupg.GnuPGKey
     */
    public GnuPGKey[] generateEmptyKeyArray(int withLengthOf) {
        if (withLengthOf < 1) {
            return null;
        }
        else {
            return new GnuPGKey[withLengthOf];
        }
    }

    /**
     * List all public keys in keyring.
     * 
     * @return GnuPGKey array of key objects with all public keys
     * @see com.freiheit.gnupg.GnuPGKey
     */
    public GnuPGKey[] listKeys() throws GnuPGException {
        return searchKeys("");
    }

    /**
     * List all secret keys in keyring.
     * 
     * @return GnuPGKey array of key objects with all secret keys
     * @see com.freiheit.gnupg.GnuPGKey
     */
    public GnuPGKey[] listSecretKeys() throws GnuPGException {
        return searchSecretKeys("");
    }

    /**
     * Find all public keys matching <em>query</em> in keyring.
     * 
     * @param query allows the same expressions as gpg on command line
     * @return GnuPGKey array of key objects with all matching keys
     * @see com.freiheit.gnupg.GnuPGKey
     */
    public GnuPGKey[] searchKeys(String query) throws GnuPGException {
        if (query == null) {
            query = new String("");
        }
        return gpgmeKeylist(getInternalRepresentation(), query, false);
    }

    /**
     * Find all keys matching <em>query</em> in keyring.
     * 
     * @param query allows the same expressions as gpg on command line
     * @return GnuPGKey array of key objects with all matching keys
     * @see com.freiheit.gnupg.GnuPGKey
     */
    public GnuPGKey[] searchSecretKeys(String query) throws GnuPGException {
        if (query == null) {
            query = new String("");
        }
        return gpgmeKeylist(getInternalRepresentation(), query, true);
    }

    /**
     * Encrypt plain text and return ASCII-armored text.
     * 
     * @param recipients array of keys to encrypt to
     * @param plain the plain text to be encrypted
     * @return String encrypted data in ASCII-armored text
     */
    public String encryptToAscii(GnuPGKey[] recipients, String plain) {
        final GnuPGData plainData = createDataObject(plain);
        final String ret = encryptToAscii(recipients, plainData);
        plainData.destroy();
        return ret;
    }

    /**
     * Encrypt a byte array and return ASCII-armored text.
     * 
     * @param recipients array of keys to encrypt to
     * @param plain the byte[] to be encrypted
     * @return String encrypted data in ASCII-armored text
     */
    public String encryptToAscii(GnuPGKey[] recipients, byte[] plain) {
        final GnuPGData plainData = createDataObject(plain);
        final String ret = encryptToAscii(recipients, plainData);
        plainData.destroy();
        return ret;
    }

    /**
     * Encrypt plain text and return ASCII-armored text.
     * 
     * @param recipients array of keys to encrypt to
     * @param plain the GnuPGData to be encrypted
     * @return String encrypted data in ASCII-armored text
     */
    public String encryptToAscii(GnuPGKey[] recipients, GnuPGData plain) {
        long l = getInternalRepresentation();
        boolean previous = gpgmeGetArmor(l);
        gpgmeSetArmor(l, true);
        GnuPGData cipher = createDataObject();
        encrypt(recipients, plain, cipher);
        final String ret = cipher.toString();
        cipher.destroy();
        if (previous == false) // maintain the original ASCII-Armor state
            gpgmeSetArmor(l, false);
        return ret;
    }

    /**
     * Encrypt a byte array and return encrypted data in binary form.
     * 
     * @param recipients array of keys to encrypt to
     * @param plain the plain binary data to be encrypted
     * @return byte[] encrypted data in binary data
     */
    public byte[] encryptToBinary(GnuPGKey[] recipients, byte[] plain) {
        final GnuPGData plainData = createDataObject(plain);
        final byte[] ret = encryptToBinary(recipients, plainData);
        plainData.destroy();
        return ret;
    }

    /**
     * Encrypt plain text and return encrypted data in binary form.
     * 
     * @param recipients array of keys to encrypt to
     * @param plain the plain text to be encrypted
     * @return byte[] encrypted data in binary data
     */
    public byte[] encryptToBinary(GnuPGKey[] recipients, String plain) {
        final GnuPGData plainData = createDataObject(plain);
        final byte[] ret = encryptToBinary(recipients, plainData);
        plainData.destroy();
        return ret;
    }

    /**
     * Encrypt plain data and return encrypted data in binary form.
     * 
     * @param recipients array of keys to encrypt to
     * @param plain the GnuPGData to be encrypted
     * @return String encrypted data in ASCII-armored text
     */
    public byte[] encryptToBinary(GnuPGKey[] recipients, GnuPGData plain) {
        long l = getInternalRepresentation();
        boolean previous = gpgmeGetArmor(l);
        gpgmeSetArmor(l, true);
        GnuPGData cipher = createDataObject();
        encrypt(recipients, plain, cipher);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(plain.size());
        BufferedOutputStream out = new BufferedOutputStream(baos, 8192);
        try {
            cipher.write(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        cipher.destroy();
        if (previous == false) // maintain the original ASCII-Armor state
            gpgmeSetArmor(l, false);
        return baos.toByteArray();
    }

    /**
     * Encrypts the text in <em>plain</em> with the public key of each
     * recipient. The result is returned as GnuPGData.
     * 
     * @param recipients array of the keys to encrypt to
     * @param plain bytes to be encrypted
     * @return GnuPGData the encrypted data
     * @see com.freiheit.gnupg.GnuPGData
     * @see com.freiheit.gnupg.GnuPGKey
     */
    public GnuPGData encrypt(GnuPGKey[] recipients, byte[] plain)
            throws GnuPGException {
        if (hasNoRecipients(recipients) || plain == null || plain.length == 0)
            throw new GnuPGException("Encryption arguments not complete.");

        final GnuPGData plainData = createDataObject(plain);
        GnuPGData cipherData = createDataObject();

        gpgmeOpEncrypt(this.getInternalRepresentation(),
                getInternalRepresentationFromRecipients(recipients),
                plainData.getInternalRepresentation(),
                cipherData.getInternalRepresentation());
        plainData.destroy();
        return cipherData;
    }

    /**
     * Encrypts the text in <em>plain</em> with the public key of each
     * recipient. The result is returned as GnuPGData.
     * 
     * @param recipients array of the keys to encrypt to
     * @param plain the text to be encrypted
     * @return GnuPGData the encrypted data
     * @see com.freiheit.gnupg.GnuPGData
     * @see com.freiheit.gnupg.GnuPGKey
     */
    public GnuPGData encrypt(GnuPGKey[] recipients, String plain)
            throws GnuPGException {
        if (hasNoRecipients(recipients) || plain == null || plain.equals(""))
            throw new GnuPGException("Encryption arguments not complete.");

        final GnuPGData plainData = createDataObject(plain);
        GnuPGData cipherData = createDataObject();

        gpgmeOpEncrypt(this.getInternalRepresentation(),
                getInternalRepresentationFromRecipients(recipients),
                plainData.getInternalRepresentation(),
                cipherData.getInternalRepresentation());
        plainData.destroy();
        return cipherData;
    }

    /**
     * Encrypts the data from <em>plain</em> with the public key of each
     * recipient. The result is stored in <em>cipher</em>.
     * 
     * @param recipients Array with the public keys of all recipients
     * @param plain text, that should be encrypted
     * @param cipher text, the encrypted plain text after method call
     * @see com.freiheit.gnupg.GnuPGData
     * @see com.freiheit.gnupg.GnuPGKey
     */
    public void encrypt(GnuPGKey[] recipients, GnuPGData plain, GnuPGData cipher)
            throws GnuPGException {
        if (hasNoRecipients(recipients) || plain == null || cipher == null)
            throw new GnuPGException("Encryption arguments not complete.");

        // note that these are pointers to addresses in the gnupg-for-java
        // shared lib
        long context = this.getInternalRepresentation();
        if (gpgmeGetSignersLength(context) == 0)
            gpgmeOpEncrypt(context,
                    getInternalRepresentationFromRecipients(recipients),
                    plain.getInternalRepresentation(),
                    cipher.getInternalRepresentation());
        else
            gpgmeOpEncryptSign(context,
                    getInternalRepresentationFromRecipients(recipients),
                    plain.getInternalRepresentation(),
                    cipher.getInternalRepresentation());
    }

    public void encryptAndSign(GnuPGKey[] recipients, GnuPGData plain, GnuPGData cipher)
            throws GnuPGException {
        if (hasNoRecipients(recipients) || plain == null || cipher == null)
            throw new GnuPGException("Encryption-Arguments not complete.");

        gpgmeOpEncryptSign(this.getInternalRepresentation(),
                getInternalRepresentationFromRecipients(recipients),
                plain.getInternalRepresentation(),
                cipher.getInternalRepresentation());
    }

    /**
     * Decrypts the data from <em>cipher</em> and returns the result.
     * 
     * @param cipher the data to be decrypted
     * @return plain the resulting decrypted data
     * @see com.freiheit.gnupg.GnuPGData
     */
    public GnuPGData decrypt(byte[] cipher) throws GnuPGException {
        if (cipher == null || cipher.length == 0)
            throw new GnuPGException("Encryption arguments not complete.");

        GnuPGData plainData = createDataObject();
        final GnuPGData cipherData = createDataObject(cipher);

        gpgmeOpDecrypt(this.getInternalRepresentation(),
                cipherData.getInternalRepresentation(),
                plainData.getInternalRepresentation());
        cipherData.destroy();
        return plainData;
    }

    /**
     * Decrypts the data from <em>cipher</em> and returns the result.
     * 
     * @param cipher the data to be decrypted
     * @return plain the resulting decrypted data
     * @see com.freiheit.gnupg.GnuPGData
     */
    public GnuPGData decrypt(String cipher) throws GnuPGException {
        if (cipher == null || cipher.length() == 0)
            throw new GnuPGException("Encryption arguments not complete.");

        GnuPGData plainData = createDataObject();
        final GnuPGData cipherData = createDataObject(cipher);

        gpgmeOpDecrypt(this.getInternalRepresentation(),
                cipherData.getInternalRepresentation(),
                plainData.getInternalRepresentation());
        cipherData.destroy();
        return plainData;
    }

    /**
     * Decrypts the data from <em>cipher</em> and stores the result in
     * <em>plain</em>.
     * 
     * @param cipher text, holds the cipher to be decrypted
     * @param plain text, holds the decrypted text after decryption
     * @see com.freiheit.gnupg.GnuPGData
     */
    public void decrypt(GnuPGData cipher, GnuPGData plain) throws GnuPGException {
        if (cipher == null || plain == null)
            return;

        gpgmeOpDecrypt(this.getInternalRepresentation(),
                cipher.getInternalRepresentation(), plain.getInternalRepresentation());
    }

    /**
     * Decrypts the data from <code>cipher</code> and stores the result in
     * <code>plain</code>, then verifies the signature of the data. If the
     * decryption or verification fails, it throws a <code>GnuPGException</code>
     * .
     * 
     * @param cipher holds the data to be decrypted and verified
     * @param plain holds the decrypted data after decryption
     * @see com.freiheit.gnupg.GnuPGData
     */
    public void decryptVerify(GnuPGData cipher, GnuPGData plain) throws GnuPGException {
        if (cipher == null || plain == null)
            return;

        gpgmeOpDecryptVerify(this.getInternalRepresentation(),
                cipher.getInternalRepresentation(),
                plain.getInternalRepresentation());
    }

    public void changePassphrase(GnuPGKey key) throws GnuPGException {
        if (key == null)
            return;

        gpgmeOpChangePassphrase(this.getInternalRepresentation(), key.getInternalRepresentation());

    }

    /*
     * Not finished. Decrypts the data in <em>cipher</em> and verfies the
     * signature on the <em>cipher</em>.
     * @param cipher encrypted and signed data
     * @param plain will contain the result after the method call
     */
    // public void decryptAndVerify(GnuPGData cipher, GnuPGData plain) throws
    // GnuPGException{
    // if (cipher == null || plain == null) return;

    // gpgmeOpDecryptVerify(this.getInternalRepresentation(),
    // cipher.getInternalRepresentation(), plain.getInternalRepresentation());
    // }

    /**
     * Signs the data in <em>plain</em> and stores the result in
     * <em>signature</em>.
     * 
     * @param plain data that you want to sign
     * @param signature result of the operation
     */
    public void sign(GnuPGData plain, GnuPGData signature) throws GnuPGException {
        if (plain == null || signature == null)
            throw new GnuPGException("Parameters not complete or null.");

        gpgmeOpSign(this.getInternalRepresentation(),
                plain.getInternalRepresentation(), signature.getInternalRepresentation());
    }

    /**
     * Verifies a signature.
     * 
     * @param signature TODO
     * @param signed TODO
     * @param plain TODO
     */
    public void verify(GnuPGData signature, GnuPGData signed, GnuPGData plain)
            throws GnuPGException {
        if (signature == null || signed == null || plain == null)
            throw new GnuPGException("Parameters not complete or null.");
        gpgmeOpVerify(this.getInternalRepresentation(),
                signature.getInternalRepresentation(),
                signed.getInternalRepresentation(),
                plain.getInternalRepresentation());
    }

    /**
     * Adds a Signer to this context. All signature operation will uses
     * this/these signer(s), until you clear the signers from the context. You
     * remove all signers at once with a call to clearSigners().
     * 
     * @param key that should be used for signing operations
     * @see com.freiheit.gnupg.GnuPGKey
     */
    public void addSigner(GnuPGKey key) throws GnuPGException {
        if (key == null)
            throw new GnuPGException("Parameters not complete or null.");
        gpgmeAddSigners(getInternalRepresentation(), key.getInternalRepresentation());
    }

    /**
     * Removes all signers from this context. You add Signers with addSigner().
     */
    public void clearSigners() throws GnuPGException {
        gpgmeClearSigners(getInternalRepresentation());
    }

    /**
     * Get a specific Signer at a given index. You add Signers with addSigner().
     * 
     * @param index which signer in the list of Signers
     * @return the GnuPGKey at index, or null if it doesn't exist
     * @see com.freiheit.gnupg.GnuPGKey
     */
    public GnuPGKey getSigner(int index) throws GnuPGException {
        return new GnuPGKey(gpgmeGetSigner(getInternalRepresentation(), index));
    }

    /**
     * Gets the number of Signers currently in the GnuPGContext. You add Signers
     * with addSigner().
     * 
     * @return long the number of Signers currently in the GnuPGContext
     * @see com.freiheit.gnupg.GnuPGKey
     */
    public long getSignersLength() throws GnuPGException {
        return gpgmeGetSignersLength(getInternalRepresentation());
    }

    /**
     * Imports a Key (private or public). You can supply the key in ASCII armor.
     */
    public void importKey(GnuPGData keydata) throws GnuPGException {
        gpgmeOpImport(getInternalRepresentation(), keydata.getInternalRepresentation());
    }

    /**
     * Imports a Key (private or public). You can supply the key in ASCII armor.
     */
    public void importKey(File file) throws GnuPGException, IOException {
        GnuPGData keydata = createDataObject(file);
        if (keydata == null) {
            System.out.println("importkey: parsing key data failed");
            return;
        }
        gpgmeOpImport(getInternalRepresentation(), keydata.getInternalRepresentation());
    }

    /**
     * This calls immediately the release method for the context in the
     * underlying gpgme library. This method is called by the finalizer of the
     * class anyway, but you can call it yourself if you want to get rid of a
     * context at once and don't want to wait for the non-deterministic
     * garbage-collector of the JVM.
     */
    public void destroy() {
        if (getInternalRepresentation() != 0) {
            gpgmeRelease(getInternalRepresentation());
            setInternalRepresentation(0);
        }
    }

    /**
     * Releases underlying datastructures. Simple calls the destroy() method.
     */
    @Override
    protected void finalize() {
        destroy();
    }

    /**
     * Generates a new PGP key and stores it in the keyring.
     */
    public void genPgpKey(String params) throws GnuPGException {
        gpgmeOpGenKey(getInternalRepresentation(), params);
    }

    /**
     * Generates a new Key.
     */
    public void genKey(String params, GnuPGData pub, GnuPGData secret) throws GnuPGException {
        gpgmeOpGenKey(getInternalRepresentation(), params);
    }

    /**
     * Sets the engine info for the context
     */
    public void setEngineInfo(int proto, String fileName, String homeDir) {

        // note that this is a pointer to and address in the gnupg-for-java
        // shared
        // lib
        long ctx = getInternalRepresentation();
        gpgmeCtxSetEngineInfo(ctx, proto, fileName, homeDir);
    }

    /**
     * Returns the result of a key generation. This must bedirectly called,
     * after the key has been generated.
     * 
     * @return the result of a key generation
     */
    public GnuPGGenkeyResult getGenkeyResult() {
        return gpgmeOpGenkeyResult(getInternalRepresentation());

    }

    /**
     * Export the keys defined by the pattern.
     * 
     * @param pattern pattern for the keys
     * @param reserved not used, must be set 0. For later use revered.
     * @param data empty data object. Will be filled with the keys.
     */
    public void export(String pattern, long reserved, GnuPGData data) {
        gpgmeOpExport(getInternalRepresentation(), pattern, 0, data.getInternalRepresentation());
    }

    /**
     * Deletes a Key from key ring. When allowSecret, a secret Key will be
     * deleted
     * 
     * @param key key to delete
     * @param allowSecret if a secret key shall be deleted.
     */
    public void delete(GnuPGKey key, boolean allowSecret) {
        gpgmeOpDelete(getInternalRepresentation(), key.getInternalRepresentation(), allowSecret);
    }

    /*
     * Native methods: All these methods are implemented as JNI calls in:
     * GnuPGContext.c The naming is as close as possible to the corresponding
     * GPGME methods. So, if you want to know what these methods are actually
     * doing: Please refer to the GPGME docs.
     */
    private native void gpgmeGetEngineInfo();

    private native long gpgmeNew();

    private native void gpgmeOpEncrypt(long l, long[] recipientsInternals, long m, long n);

    private native void gpgmeOpDecrypt(long l, long m, long n);

    private native void gpgmeOpChangePassphrase(long l, long m);

    private native void gpgmeRelease(long l);

    private native void gpgmeOpEncryptSign(long context, long[] recipients, long plain, long cipher);

    private native void gpgmeOpDecryptVerify(long context, long cipher, long plain);

    private native void gpgmeOpSign(long context, long l, long m);

    private native void gpgmeOpVerify(long context, long l, long m, long n);

    private native GnuPGKey[] gpgmeKeylist(long l, String query, boolean secret_only);

    private native void gpgmeAddSigners(long l, long m);

    private native void gpgmeClearSigners(long context);

    private native long gpgmeGetSigner(long context, int index);

    private native long gpgmeGetSignersLength(long context);

    private native void gpgmeOpImport(long context, long l);

    private native void gpgmeOpExport(long context, String pattern, long reserved, long l);

    private native void gpgmeOpGenKey(long context, String params);

    private native void gpgmeCtxSetEngineInfo(long context, int proto, String fileName,
            String homeDir);

    private native GnuPGGenkeyResult gpgmeOpGenkeyResult(long context);

    private native void gpgmeOpDelete(long context, long l, boolean allowSecret);

    // getters/setters for members, no caching, always direct access to gpgme
    private native boolean gpgmeGetArmor(long l);

    private native void gpgmeSetArmor(long l, boolean state);

    private native boolean gpgmeGetTextmode(long l);

    private native void gpgmeSetTextmode(long l, boolean state);

}

/* Local Variables: tab-width: 4; c-basic-offset: 4; indent-tabs-mode: nil; End: */
