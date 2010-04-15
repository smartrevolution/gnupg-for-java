/*
 * $Id: KeySearch.java,v 1.1 2005/01/24 13:56:51 stefan Exp $
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
import com.freiheit.gnupg.*;

public class KeySearch{
    public static void main(String[] args){
        GnuPGContext ctx = new GnuPGContext();
        GnuPGKey[] keylist;

        if(args != null && args.length == 1){
            keylist = ctx.searchKeys(args[0]);
        }
        else{
            keylist = ctx.searchKeys("");
        }
        
        for(GnuPGKey key : keylist){
            System.out.printf("%s\n", key);
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
