/*
 * $Id: GnuPGTestSuite.java,v 1.3 2006/06/14 15:53:12 sneumann Exp $
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
package com.freiheit.gnupg.tests;

import java.io.File;
import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.freiheit.gnupg.*;

/**
   I will improve this TestSuite later, that everybody can run it without
   specific fingerprints. It should behave like the tests in gpgme.
   <p>
   But the tests can also be seen as code examples.

   @author Stefan Richter, stefan@freiheit.com
 */
public class GnuPGTestSuite extends TestCase{

    private static String HOME = "/tmp/gnupg-for-java-tests";
    private static String PLAINTEXT = "I am a not so secret text.";
    //Currently, you can not run these test without these fingerprints.
    //And: You need to know the passphrases...forget it.
    // but you could add your own fingerprints...
    private static String CD_FPR = "1C12878CFAA2ECDC81DC43DB12262834A6123FF6";
    //private static String SR_FPR = "F05F385DFA40962D0075AB9C2B80170D";
    private static String SR_FPR = "BE54B261FDDF09025D249CAF948C94764B9A38DB";

    //private static GnuPGKey person1 = null;
    //private static GnuPGKey person2 = null;


    public static Test suite() {
        System.out.println("suite()");
        TestSuite suite= new TestSuite(GnuPGTestSuite.class);
        GnuPGContext ctx = getContext();

        String person1Key = " <GnupgKeyParms format=\"internal\">\n"+
            "Key-Type: DSA\n"+
            "Key-Length: 1024\n"+
            "Subkey-Type: ELG-E\n"+
            "Subkey-Length: 1024\n"+
            "Name-Real: alpha\n"+
            "Name-Comment: just a test\n"+
            "Name-Email: alpha@alpha.org\n"+
            "Expire-Date: 0\n"+
            "Passphrase: alpha\n"+
            "</GnupgKeyParms>";
        ctx.genKey(person1Key,null,null);
        //String fpr1 = ctx.getGenkeyResult().getFpr();

        String person2Key = " <GnupgKeyParms format=\"internal\">\n"+
            "Key-Type: DSA\n"+
            "Key-Length: 1024\n"+
            "Subkey-Type: ELG-E\n"+
            "Subkey-Length: 1024\n"+
            "Name-Real: beta\n"+
            "Name-Comment: just a test\n"+
            "Name-Email: beta@beta.org\n"+
            "Expire-Date: 0\n"+
            "Passphrase: beta\n"+
        "</GnupgKeyParms>";
        ctx.genKey(person2Key,null,null);
        //String fpr2 = ctx.getGenkeyResult().getFpr();

        System.out.println("done suite()");
        return suite;
    }

    public static GnuPGContext getContext(){
        GnuPGContext ctx = new GnuPGContext();
        ctx.setEngineInfo(ctx.getProtocol(),ctx.getFilename(),HOME);
        return ctx;
    }

    public void testEngine(){
        GnuPGContext ctx = new GnuPGContext();
        ctx.getVersion();
        ctx.getFilename();
        ctx.getRequiredVersion();
    }

    public void testContextSetterAndGetter(){
        GnuPGContext ctx = new GnuPGContext();

        // Test set/isArmor()
        assertTrue(ctx.isArmor());//default: true
        ctx.setArmor(false);
        assertFalse(ctx.isArmor());
        ctx.setArmor(true);
        assertTrue(ctx.isArmor());

        // Test set/isTextmode()
        assertTrue(ctx.isTextmode());//default: true
        ctx.setTextmode(false);
        assertFalse(ctx.isTextmode());
        ctx.setTextmode(true);
        assertTrue(ctx.isTextmode());
    }

    public void testKeySearch(){
        GnuPGContext ctx = new GnuPGContext();
        GnuPGKey[] keys = ctx.searchKeys("stefan");

        assertNotNull(keys);

        for(int i=0; keys != null && i < keys.length; i++){
            assertNotNull(keys[i]);
            Iterator iter = keys[i].getSignatures();
            System.out.println(keys[i]);//Uncomment to print each key
            while(iter.hasNext()){
                assertNotNull((GnuPGSignature)iter.next());
            }
        }
    }

    public void testEncryptForOneRecipient(){
        GnuPGContext ctx = new GnuPGContext();
        ctx.setPassphraseListener(new GnuPGPassphraseWindow());
        GnuPGData plain = ctx.createDataObject(PLAINTEXT);
        GnuPGData cipher = ctx.createDataObject();

        GnuPGKey[] recipient = ctx.generateEmptyKeyArray(1);
        recipient[0] = ctx.getKeyByFingerprint(SR_FPR);

        ctx.encrypt(recipient, plain, cipher);

        assertNotNull(cipher.toString());
    }

    public void testEncryptForTwoRecipients(){
        GnuPGContext ctx = new GnuPGContext();
        ctx.setPassphraseListener(new GnuPGPassphraseWindow());
        GnuPGData plain = ctx.createDataObject(PLAINTEXT);
        GnuPGData cipher = ctx.createDataObject();

        GnuPGKey[] recipients = ctx.generateEmptyKeyArray(2);
        recipients[0] = ctx.getKeyByFingerprint(SR_FPR);
        recipients[1] = ctx.getKeyByFingerprint(CD_FPR);

        ctx.encrypt(recipients, plain, cipher);

        assertNotNull(cipher.toString());
    }

    public void testDecryptFromOneRecipient(){
        GnuPGContext ctx = new GnuPGContext();
        ctx.setPassphraseListener(new GnuPGPassphraseWindow());
        GnuPGData plain = ctx.createDataObject(PLAINTEXT);
        GnuPGData cipher = ctx.createDataObject();
        GnuPGData decrypted = ctx.createDataObject();

        GnuPGKey[] recipient = ctx.generateEmptyKeyArray(1);
        recipient[0] = ctx.getKeyByFingerprint(SR_FPR);

        ctx.encrypt(recipient, plain, cipher);
        ctx.decrypt(cipher, decrypted);

        assertNotNull(decrypted.toString());
        assertEquals(decrypted.toString(), PLAINTEXT);
    }

    public void testAddAndClearSigners(){
        GnuPGContext ctx = new GnuPGContext();
        GnuPGKey signer1 = ctx.getKeyByFingerprint(SR_FPR);
        GnuPGKey signer2 = ctx.getKeyByFingerprint(CD_FPR);

        ctx.addSigner(signer1);
        ctx.addSigner(signer2);
        ctx.clearSigners();
    }

    public void testSign(){
        GnuPGContext ctx = new GnuPGContext();
        ctx.setPassphraseListener(new GnuPGPassphraseWindow());
        GnuPGData plain = ctx.createDataObject(PLAINTEXT);
        GnuPGData signature = ctx.createDataObject();

        GnuPGKey signer = ctx.getKeyByFingerprint(SR_FPR);
        ctx.addSigner(signer);
        ctx.sign(plain, signature);

        System.out.println(plain.toString());
        System.out.println(signature.toString());

        assertNotNull(signature.toString());
    }

    public void testEncrypt() {
        GnuPGContext ctx = new GnuPGContext();
        ctx.setPassphraseListener(new GnuPGPassphraseConsole());

        String plaintext = "HALLLLLLLLLLO";
        System.out.println( "plaintext: " + plaintext );
        GnuPGData plain = ctx.createDataObject(plaintext);
        System.out.println( "plain (from data object): \"" + plain.toString()  + "\"");
        GnuPGData encrypted = ctx.createDataObject();

        GnuPGKey[] recipients = ctx.generateEmptyKeyArray(1);
        recipients[0] = ctx.getKeyByFingerprint(SR_FPR);

        ctx.encrypt(recipients, plain, encrypted);

        System.out.println("encrypted (from data object: \"" + encrypted.toString()  + "\"");

        String encStr = encrypted.toString();

        assertNotNull( encStr );
        assertFalse("Encrypted message cannot be empty.", encStr.length() == 0);

    }

//     public void testEncryptAndSign(){
//         GnuPGContext ctx = new GnuPGContext();
//         ctx.setPassphraseListener(new GnuPGPassphraseWindow());
//         GnuPGData plain = new GnuPGData(PLAINTEXT);
//         GnuPGData cipher = new GnuPGData();
//
//         GnuPGKey[] recipient = ctx.generateEmptyKeyArray(1);
//         recipient[0] = ctx.getKeyByFingerprint(SR_FPR);
//
//         ctx.addSigner(recipient[0]);
//
//         ctx.encryptAndSign(recipient, plain, cipher);
//
//         assertNotNull(cipher.toString());
//     }
//
//     public void testDecryptAndVerify(){
//         GnuPGContext ctx = new GnuPGContext();
//         ctx.setPassphraseListener(new GnuPGPassphraseWindow());
//         GnuPGData plain = new GnuPGData(PLAINTEXT);
//         GnuPGData cipher = new GnuPGData();
//         GnuPGData decrypted = new GnuPGData();
//
//         GnuPGKey[] recipient = ctx.generateEmptyKeyArray(1);
//         recipient[0] = ctx.getKeyByFingerprint(SR_FPR);
//
//         ctx.addSigner(recipient[0]);
//
//         ctx.encryptAndSign(recipient, plain, cipher);
//
//         ctx.decryptAndVerify(cipher, decrypted);
//
//         assertNotNull(decrypted.toString());
//     }

//    public void testGenKey(){
//        final String dirName = "/home/stefan/tmp/.gnupg";
//        GnuPGContext ctx = new GnuPGContext();
//        System.err.println("Proto: "+ ctx.getProtocol());
//        ctx.setEngineInfo(ctx.getProtocol(),ctx.getFilename(),dirName);
//        String keyText=" <GnupgKeyParms format=\"internal\">\n"+
//           "Key-Type: DSA\n"+
//           "Key-Length: 1024\n"+
//           "Subkey-Type: ELG-E\n"+
//           "Subkey-Length: 1024\n"+
//           "Name-Real: Binding Test\n"+
//           "Name-Comment: with stupid passphrase\n"+
//           "Name-Email: bind@foo.bar\n"+
//           "Expire-Date: 0\n"+
//           "Passphrase: abc\n"+
//           "</GnupgKeyParms>";
//        ctx.genKey(keyText,null,null);
//        GnuPGGenkeyResult genkeyResult = ctx.getGenkeyResult();
//        GnuPGKey[] keys = ctx.searchKeys("binding");
//        assertNotNull(keys);
//        assertEquals("Fingerprint are not equal!",keys[0].getFingerprint(),genkeyResult.getFpr());
//        assertTrue(genkeyResult.isPrimary());
//        assertTrue(genkeyResult.isSub());
//        File dir  = new File(dirName);
//        dir.deleteOnExit();
//
//    }

}
/*
 * Local variables:
 * c-basic-offset: 4
 * indent-tabs-mode: nil
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
