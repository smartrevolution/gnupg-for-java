/*
 * $Id: GnuPGPassphraseWindow.java,v 1.2 2005/01/28 16:05:44 stefan Exp $
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

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

/**
   Requests a passphrase for a crypto operation from the a swing dialog.
   This is triggered by gpgme. You must register this as a listener
   to the GnuPGContext. It generates a Swing-Dialog.

   @see com.freiheit.gnupg.GnuPGContext

   @author Stefan Richter, stefan@freiheit.com
 */
public class GnuPGPassphraseWindow implements GnuPGPassphraseListener{
    private JFrame _win;
    private boolean _internalWin;

    /**
       Default-Constructor.
     */
    public GnuPGPassphraseWindow(){
        this((JFrame)null);
    }

    /**
       Use this, if you use this library from a swing application.
       @param f where this passphrase dialog should be modal to.
       (See Swing-Documentation, if you don't know, what modal dialogs are!)
     */
    public GnuPGPassphraseWindow(JFrame f){
        if(f != null){
            _win = f;
            _internalWin = false;
        }
        else{
            _win = new JFrame("GnuPG for Java - freiheit.com technologies gmbh, 2004");
            _internalWin = true;
        }
    }

    /**
       Opens a Swing modal dialog, asks for the passphrase and returns it to gpgme.
     */
    public String getPassphrase(String hint, String passphraseInfo, long wasBad){
        JPasswordField passwordField = new JPasswordField(20);
        JOptionPane optionPane = new JOptionPane();
        optionPane.setMessage( new Object[] { hint, passwordField } );
        optionPane.setMessageType( JOptionPane.QUESTION_MESSAGE );
        optionPane.setOptionType( JOptionPane.OK_CANCEL_OPTION );
        JDialog dialog = optionPane.createDialog( _win, "Enter GnuPG Passphrase and click OK..." );
        dialog.setVisible(true);
        Integer value = (Integer)optionPane.getValue();
        if (value.intValue() == JOptionPane.CANCEL_OPTION || value.intValue() == JOptionPane.CLOSED_OPTION ){
            dialog.dispose();
            if(_internalWin){
                _win.dispose();
            }
            return null;
        }
        else{
            StringBuffer buf = new StringBuffer();
            //the newline is REALLY important for gpgme. Don't remove it!
            //FIXME: I also add one newline in the native code, but there is still an error condition
            buf.append(passwordField.getPassword()).append("\n");
            String passphrase = buf.toString();
            dialog.dispose();
            if(_internalWin){
                _win.dispose();
            }
            return passphrase;
        }
    }
}
/*
 * Local variables:
 * c-basic-offset: 4
 * indent-tabs-mode: nil
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
