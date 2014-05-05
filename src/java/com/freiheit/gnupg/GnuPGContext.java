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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
   Start here, because for all operations, you first need to create a
   GnuPGContext. Use one context object for every single thread or
   (really) take care about synchronizing access to the context from
   different threads.
   <p>
   <em>How to create a context?</em>
   <pre>
   {@code
     GnuPGContext ctx = new GnuPGContext();
   }
   </pre>
   If you execute an operation, where GnuPG needs a passphrase
   (password, mantra) from you, you need to tell the
   library which Class is listening for such a request and is able to
   deliver the entered passphrase to the library. You can implement
   such a listener by yourself, but you can also use two
   pre-fabricated ones. (Of course, normally you should install
   gpg-agent/pinentry for this job...but some will need this ability
   to get passphrases from a database or so. And: This release is not
   checking for the existence of gpg-agent...so it won't work anyway.)
   <p>
   <em>How to add a Passphrase-Listener to read a password from the console?</em>
   <pre>
   {@code
     ctx.setPassphraseListener(new GnuPGPassphraseConsole());
     //caution: you can still see the password while you are typing...
   }
   </pre>
   <em>How to add a Passphrase-Listener to read a password from the swing dialog?</em>
   <pre>
   {@code
     ctx.setPassphraseListener(new GnuPGPassphraseWindow());
   }
   </pre>
   @author Stefan Richter, stefan@freiheit.com
 */

public class GnuPGContext extends GnuPGPeer{
    private String _version;
    private String _filename;
    private String _reqversion;
    private int _protocol;
    private GnuPGPassphraseListener _passphraseListener = null;

    /** Creates a new Context (use on context for one thread!)*/
    public GnuPGContext(){
        gpgmeGetEngineInfo();
        setInternalRepresentation(gpgmeNew());
    }

    /**
        Returns the version of the underlying GPGME lib version.
        @return library version as String (like "1.1.1")
    */
    public String getVersion(){
        return _version;
    }

    /**
       Returns the executable gpg with path
       @return gpg executable as String (like "/usr/bin/gpg")
     */
    public String getFilename(){
        return _filename;
    }


    /**
        Returns the required version of the underlying GPGME lib.

        @return required library version as String (like "1.1.1")
    */
    public String getRequiredVersion(){
        return _reqversion;
    }

    /**
    Returns the used protocol of the crypto backend.

    @return protocol
    */
    public int getProtocol(){
        return _protocol;
    }

    /**
       Tells you, is the gpg engine uses ASCII armor.
       Please check GPG/GPGME for documentation if you don't know what this means.
       (Try 'man gpg' on the command line and look for 'armor')

       @return boolean true if results will be ASCII armored, if false binary
     */
    public boolean isArmor(){
        long armorState = gpgmeGetArmor(getInternalRepresentation());
        return (armorState == 1);
    }

    /**
       Tell the gpg engine to use ASCII armor.
       Please check GPG/GPGME for documentation if you don't know what this means.
       (Try 'man gpg' on the command line and look for 'armor')

       @param state set true if results should be ASCII armored, set false if binary
     */
    public void setArmor(boolean state){
        long armorState = (state ? 1 : 0);
        gpgmeSetArmor(getInternalRepresentation(), armorState);
    }

    /**
       Tells you, is the gpg engine is in text mode.
       Please check GPG/GPGME for documentation if you don't know what this means.

       @return boolean true means text mode is on
     */
    public boolean isTextmode(){
        boolean result = false;
        long mode = gpgmeGetTextmode(getInternalRepresentation());
        if(mode == 1){
            result = true;
        }
        return result;
    }

    /**
       Tell the gpg engine to set the text mode.
       Please check GPG/GPGME for documentation if you don't know what this means.

       @param state set true if you want text mode switched on
     */
    public void setTextmode(boolean state){
        long mode = (state ? 1 : 0);
        gpgmeSetTextmode(getInternalRepresentation(), mode);
    }

    /**
       Sets a listener, if GPGME needs to request a passphrase
       from the user (or even from a program or a database..)
       <p>
       A passphrase-listener is global to a jvm. This means,
       you can register only <em>one</em> listener.
       <p>
       I am working on this, but it is not so easy to access
       jvm methods from a specific object from a non-jvm thread,
       which the passphrase callback is...

       @param l a GnuPGPassphraseListener implementation

       @see com.freiheit.gnupg.GnuPGPassphraseListener
     */
    public void setPassphraseListener(GnuPGPassphraseListener l){
        _passphraseListener = l;
    }

    public String passphraseCallback(String hint, String passphraseInfo, long wasBad){
        return _passphraseListener.getPassphrase(hint, passphraseInfo, wasBad);
    }

    private long[] getInternalRepresentationFromRecipients(GnuPGKey[] recipients) {
        // note that these are pointers to addresses in the javagnupg shared lib
        long recipientsInternals[] = new long[recipients.length];
        for (int i=0; i < recipients.length; i++) {
            if (recipients[i] != null)
                recipientsInternals[i] = recipients[i].getInternalRepresentation();
            else
                ; // FIXME: throw exception here? Just skip over it for now.
        }
        return recipientsInternals;
    }

    private boolean hasNoRecipients(GnuPGKey[] recipients){
        return !(recipients != null && recipients.length > 0);
    }

    /**
       Gets the key with the supplied fingerprint from the keyring.
       This is also kind of a factory method to generate key objects,
       because you always need a context to access the keys in your keyring.

       @param fingerprint gpg fingerprint (must be in your keyring)
       @return GnuPGKey the key with the matching fingerprint

       @see com.freiheit.gnupg.GnuPGKey
     */
    public GnuPGKey getKeyByFingerprint(String fingerprint) throws GnuPGException{
        if(fingerprint == null || fingerprint.length() < 1) {
            return null;
        }
        else{
            return new GnuPGKey(this, fingerprint);
        }
    }

    /**
       Factory method to generate a GnuPGData-Object from a String.

       @param data should not be null and should have a length > 0
       @return GnuPGData null is data is null or empty, otherwise a GnuPGData-Object
     */
    public GnuPGData createDataObject(String data) throws GnuPGException{
        if(data == null || data.length() < 1){
            return null;
        }
        else{
            return new GnuPGData(data);
        }
    }

    /**
       Factory method to generate a GnuPGData-Object from a byte array.

       @param data should not be null and should have a length > 0
       @return GnuPGData null is data is null or empty, otherwise a GnuPGData-Object
     */
    public GnuPGData createDataObject(byte[] data) throws GnuPGException{
        if(data == null || data.length < 1){
            return null;
        }
        else{
            return new GnuPGData(data);
        }
    }

    /**
       Factory method to generate an empty GnuPGData-Object.

       @param data should not be null and should have a length > 0
       @return GnuPGData null is data is null or empty, otherwise a GnuPGData-Object
     */
    public GnuPGData createDataObject() throws GnuPGException{
        return new GnuPGData();
    }

    /**
       Convenience method to generate empty key arrays.
       To encrypt data, you need to supply a list of recipients
       in an array. This is a little helper. It will not search for
       keys! Its just an empty array.

       @param withLengthOf length of the array to generate (number of recipients)
       @return GnuPGKey empty array of keys

       @see com.freiheit.gnupg.GnuPGKey
     */
    public GnuPGKey[] generateEmptyKeyArray(int withLengthOf){
        if(withLengthOf < 1){
            return null;
        }
        else{
            return new GnuPGKey[withLengthOf];
        }
    }

    /**
       Find all keys matching <em>query</em> in keyring.

       @param query allows the same expressions as gpg on command line
       @return GnuPGKey array of key objects with all matching keys

       @see com.freiheit.gnupg.GnuPGKey
     */
    public GnuPGKey[] searchKeys(String query) throws GnuPGException{
        if (query == null || query.length() < 1) {
            return null;
        } else{
            return gpgmeKeylist(getInternalRepresentation(), query);
        }
    }

    /**
       Encrypts the data from <em>plain</em> with the public key
       of each recipient. The result is stored in <em>cipher</em>.

       @param recipients Array with the public keys of all recipients
       @param plain text, that should be encrypted
       @param cipher text, the encrypted plain text after method call

       @see com.freiheit.gnupg.GnuPGData
       @see com.freiheit.gnupg.GnuPGKey
     */
    public void encrypt(GnuPGKey[] recipients, GnuPGData plain, GnuPGData cipher) throws GnuPGException{
        if (hasNoRecipients(recipients) || plain == null || cipher == null) throw new GnuPGException("Encryption-Arguments not complete.");

        // note that these are pointers to addresses in the javagnupg shared lib
        long recipientsInternals[] = getInternalRepresentationFromRecipients(recipients);
        gpgmeOpEncrypt(this.getInternalRepresentation(), recipientsInternals,
                       plain.getInternalRepresentation(), cipher.getInternalRepresentation());
    }

    /*
       Not finished.
     */
//     public void encryptAndSign(GnuPGKey[] recipients, GnuPGData plain, GnuPGData cipher) throws GnuPGException{
//         if (hasNoRecipients(recipients) || plain == null || cipher == null) throw new GnuPGException("Encryption-Arguments not complete.");

//         long recipientsInternals[] = getInternalRepresentationFromRecipients(recipients);

//         gpgmeOpEncryptSign(this.getInternalRepresentation(), recipientsInternals,
//                            plain.getInternalRepresentation(), cipher.getInternalRepresentation());
//     }

    /**
       Decrypts the data from <em>cipher</em> and stores the result
       in <em>plain</em>.

       @param cipher text, holds the cipher to be decrypted
       @param plain text, holds the decrypted text after decryption

       @see com.freiheit.gnupg.GnuPGData
     */
    public void decrypt(GnuPGData cipher, GnuPGData plain) throws GnuPGException{
        if(_passphraseListener == null) throw new GnuPGException("Aborting: No GnuPGPassphraseListener set.");
        if (cipher == null || plain == null) return;

        gpgmeOpDecrypt(this.getInternalRepresentation(),
                       cipher.getInternalRepresentation(), plain.getInternalRepresentation());
    }

    public void changePassphrase( GnuPGKey key ) throws GnuPGException {
        if(_passphraseListener == null) throw new GnuPGException("Aborting: No GnuPGPassphraseListener set.");

        if ( key == null ) return;

        gpgmeOpChangePassphrase( this.getInternalRepresentation(), key.getInternalRepresentation() );

    }


    /*
      Not finished.
       Decrypts the data in <em>cipher</em> and verfies the signature on the <em>cipher</em>.

       @param cipher encrypted and signed data
       @param plain will contain the result after the method call
     */
//     public void decryptAndVerify(GnuPGData cipher, GnuPGData plain) throws GnuPGException{
//         if(_passphraseListener == null) throw new GnuPGException("Aborting: No GnuPGPassphraseListener set.");
//         if (cipher == null || plain == null) return;

//         gpgmeOpDecryptVerify(this.getInternalRepresentation(),
//                              cipher.getInternalRepresentation(), plain.getInternalRepresentation());
//     }

    /**
       Signs the data in <em>plain</em> and stores the result in <em>signature</em>.

       @param plain data that you want to sign
       @param signature result of the operation
     */
    public void sign(GnuPGData plain, GnuPGData signature) throws GnuPGException{
        if(_passphraseListener == null) throw new GnuPGException("Aborting: No GnuPGPassphraseListener set.");
        if (plain == null || signature == null) throw new GnuPGException("Parameters not complete or null.");

        gpgmeOpSign(this.getInternalRepresentation(),
                    plain.getInternalRepresentation(), signature.getInternalRepresentation());
    }

    /**
       Verifies a signature.

       @param signature TODO
       @param signed TODO
       @param plain TODO
     */
    public void verify(GnuPGData signature, GnuPGData signed, GnuPGData plain) throws GnuPGException{
        if (signature == null || signed == null|| plain == null)  throw new GnuPGException("Parameters not complete or null.");
        gpgmeOpVerify(this.getInternalRepresentation(),
                      signature.getInternalRepresentation(),
                      signed.getInternalRepresentation(),
                      plain.getInternalRepresentation());
    }

    /**
       Adds a Signer to this context. All signature operation will uses
       this/these signer(s), until you clear the signers from the context.
       You remove all signers at once with a call to clearSigners().

       @param key that should be used for signing operations

       @see com.freiheit.gnupg.GnuPGKey
     */
    public void addSigner(GnuPGKey key) throws GnuPGException{
        if(key == null)  throw new GnuPGException("Parameters not complete or null.");
        gpgmeAddSigners(getInternalRepresentation(), key.getInternalRepresentation());
    }

    /**
       Removes all signers from this context. You add Signers with addSigner().
     */
    public void clearSigners() throws GnuPGException{
        gpgmeClearSigners(getInternalRepresentation());
    }

    /**
       Imports a Key (private or public). You can supply the key in ASCII armor.
     */
    public void importKey(GnuPGData keydata) throws GnuPGException{
        gpgmeOpImport(getInternalRepresentation(), keydata.getInternalRepresentation());
    }

    /**
       This calls immediately the release method for the context
       in the underlying gpgme library. This method is called by
       the finalizer of the class anyway, but you can call it yourself
       if you want to get rid of a context at once and don't want to
       wait for the non-deterministic garbage-collector of the JVM.
    */
    public void destroy(){
        if(getInternalRepresentation() != 0){
            gpgmeRelease(getInternalRepresentation());
            setInternalRepresentation(0);
        }
    }

    /**
       Releases underlying datastructures. Simple calls the destroy() method.
     */
    protected void finalize(){
        destroy();
    }

    /**
    Generates a new Key.
    */
    public void genKey(String params,GnuPGData pub, GnuPGData secret) throws GnuPGException{
        gpgmeOpGenKey(getInternalRepresentation(),params);
    }

    /**
    Sets the engine info for the context
    */
    public void setEngineInfo(int proto, String fileName, String homeDir){

        // note that this is a pointer to and address in the javagnupg shared lib
        long ctx = getInternalRepresentation();
        gpgmeCtxSetEngineInfo(ctx,proto,fileName,homeDir);
    }

    /**
     * Returns the result of a key generation. This must bedirectly called, after the key
     * has been generated.
     * @return the result of a key generation
     */
    public GnuPGGenkeyResult getGenkeyResult(){
        return gpgmeOpGenkeyResult(getInternalRepresentation());

    }

    /**
     * Export the keys defined by the pattern.
     * @param pattern pattern for the keys
     * @param reserved not used, must be set 0. For later use revered.
     * @param data empty data object. Will be filled with the keys.
     */
    public void export(String pattern, long reserved, GnuPGData data){
        gpgmeOpExport(getInternalRepresentation(), pattern, 0, data.getInternalRepresentation());
    }


    /**
     * Deletes a Key from key ring. When allowSecret, a secret Key will be deleted
     * @param key key to delete
     * @param allowSecret if a secret key shall be deleted.
     */
    public void delete(GnuPGKey key, boolean allowSecret){
        gpgmeOpDelete(getInternalRepresentation(),key.getInternalRepresentation(),allowSecret);
    }
    /* Native methods:
       All these methods are implemented as JNI calls in:
         GnuPGContext.c
       The naming is as close as possible to the corresponding
       GPGME methods. So, if you want to know what these methods
       are actually doing: Please refer to the GPGME docs.
     */
    private native static void gpgmeCheckVersion();
    private native void gpgmeGetEngineInfo();
    private native long gpgmeNew();
    private native void gpgmeOpEncrypt(long l, long[] recipientsInternals, long m, long n);
    private native void gpgmeOpDecrypt(long l, long m, long n);
    private native void gpgmeOpChangePassphrase(long l, long m );
    private native void gpgmeRelease(long l);
    @SuppressWarnings("unused")
    private native void gpgmeOpEncryptSign(long context, int[] recipients, long plain, long cipher);
    @SuppressWarnings("unused")
    private native void gpgmeOpDecryptVerify(long context, long cipher, long plain);
    private native void gpgmeOpSign(long context, long l, long m);
    private native void gpgmeOpVerify(long context, long l, long m, long n);
    private native GnuPGKey[] gpgmeKeylist(long l, String query);
    private native void gpgmeAddSigners(long l, long m);
    private native void gpgmeClearSigners(long context);
    private native void gpgmeOpImport(long context, long l);
    private native void gpgmeOpExport(long context, String pattern, long reserved, long l);
    private native void gpgmeOpGenKey(long context,String params);
    private native void gpgmeCtxSetEngineInfo(long context,int proto,String fileName,String homeDir);
    private native GnuPGGenkeyResult gpgmeOpGenkeyResult(long context);
    private native void gpgmeOpDelete(long context, long l, boolean allowSecret);

    //getters/setters for members, no caching, always direct access to gpgme
    private native long gpgmeGetArmor(long l);
    private native void gpgmeSetArmor(long l, long state);
    private native long gpgmeGetTextmode(long l);
    private native void gpgmeSetTextmode(long l, long state);

    private static final String NATIVE_LIBRARY_BASENAME = "libjavagnupg";

    /*
      It is not possible, to load a shared library directly
      from a Jar-Archive. But there is an acceptable workaround:
      - Put the shared library in the Jar-Archive
      - When the classes get loaded:
        - read the shared library from the Jar-Archive
        - save it to the standard temp location in the file systems on this platform
        - load it from there
        - tell java, to automatically get rid of the lib, when the JVM exits (deleteOnExit)

      This way, the library containing the JNI calls must not be installed extra.
      It is delivered inside the Jar-Archive, stored temporarily and disappears on exit.
     */
    static {
        // TODO all this guessing is still rather rough, however it will work
        // with most *ix systems and *MAY* work with Windows once we've come
        // to test such a setup...
        String nativeLibraryExtension = getNativeLibraryExtension();

        System.out.println("guessed native library extension " + nativeLibraryExtension);

        String libName = getNativeLibraryName();

        System.out.println("guessed native library name " + libName);

        try{
            InputStream in = GnuPGContext.class.getResource(libName).openStream();
            File tempLib = File.createTempFile(NATIVE_LIBRARY_BASENAME, "." + nativeLibraryExtension);
            FileOutputStream out = new FileOutputStream(tempLib);
            byte[] buf = new byte[4096];
            for (int i = in.read(buf); i != -1; i = in.read(buf)){
                out.write(buf, 0, i);
            }
            out.close();
            tempLib.deleteOnExit();
            System.out.println("Trying to load temporary library: " +  tempLib.getPath());
            System.load(tempLib.getPath());
            //This call is really important, as it initializes the GPGME thread system!!!!
            gpgmeCheckVersion();//FIXME: But: This is NOT working correctly!!!
            //----------------------------------------------------------------------------
        }
        catch(Throwable e){
            System.out.println("Unable to load shared library from Jar-File.");
            System.out.println("Maybe GnuPG for Java is not allowed to write to your disk? (Access problems?)");
            System.out.println("Or the used GnuPG jar-file is not compiled for the current environment/architecture (JavaGnuPG is platform dependent!).");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Determines the native library name for the system this is running on.
     * If running on unix, we also try to guess the architecture's bit-ness
     * which has an impact on the library's name
     * @return the name
     */
    private static String getNativeLibraryName() {
        // if all fails we assume windblows
        String libName = "/c/";
		libName += System.getProperty("os.name").replace(" ", "_");
		if(System.getProperty("os.arch") == "amd64"){
       		libName +=  "/libjavagnupg.64";			
		}else{
       		libName +=  "/libjavagnupg.32";			
		}
 
        return libName + "." + getNativeLibraryExtension();
    }

    /**
     * Guesses the native library extension for the system this is running on.
     * On unix systems, the extension is "so", on windows it is "dll".
     * @return the extension
     */
    private static String getNativeLibraryExtension() {

        // try to detect a unix system
        File libDir = null;
        for (String dirName : new String[] { "/lib", "/usr/lib" }) {
            libDir = new File(dirName);
            if (libDir.exists() && libDir.isDirectory()) {
                String[] found = libDir.list();
                if (found != null && found.length > 0) {
                    for (String file : found) {
                        // if we find any file matching lib*.so, we assume unix
                        if (file.startsWith("lib") && file.endsWith(".so")) {
                            return "so";
                        }
                        if (file.startsWith("lib") && file.endsWith(".dylib")) {
                            return "dylib";
                        }
                    }
                }
            }
        }

        // could not detect a unix system? assume winblows...
        return "dll";
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * indent-tabs-mode: nil
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
