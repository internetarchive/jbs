/*
 * Copyright 2011 Internet Archive
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.archive.jbs.lucene;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.lucene.document.*;
import org.apache.lucene.index.*;

import org.archive.jbs.Document;
import org.archive.jbs.util.*;

/**
 *
 */ 
public class NutchWAXSiteHandler implements FieldHandler
{

  public NutchWAXSiteHandler( )
  {
  }

  public void handle( org.apache.lucene.document.Document doc, Document document )
  {
    try
      {
        URL u = new URL( document.get( "url" ) );

        String host = IDN.toUnicode( u.getHost(), IDN.ALLOW_UNASSIGNED );
        
        host = host.replaceAll( "^www[0-9]*.", "" );

        doc.add( new Field( "site", host, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS) );
      }
    catch ( MalformedURLException mue )
      {
        // Very strange for the URL of a crawled page to be malformed.
        // But, in that case, just skip it.
      }
  }

}
