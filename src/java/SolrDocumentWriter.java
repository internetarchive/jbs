/*
 * Copyright 2010 Internet Archive
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

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.client.solrj.*;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;

/**
 * 
 */
public class SolrDocumentWriter extends DocumentWriterBase
{
  private SolrServer server;

  public SolrDocumentWriter( URL url )
    throws IOException
  {
    this.server = new CommonsHttpSolrServer( url );
  }

  public void add( String key, DocumentProperties properties )
    throws IOException
  {
    // TODO: Create Solr XML document, the post it.
    SolrInputDocument doc = new SolrInputDocument();

    doc.addField( "id",  key );
    
    for ( String p : new String[] { "url", "title", "length", "collection", "boiled", "site", "type" } )
      {
        doc.addField( p, properties.get( p ) );
      }

    doc.addField( "content", properties.get( "content_parsed" ) );

    // Solr requires the date to be in the form: 1995-12-31T23:59:59Z
    // See the Solr schema docs.
    String date = properties.get( "date" );
    if ( date.length() == "yyyymmddhhmmss".length() )
      {
        doc.addField( "date", date.substring(0,4)  + "-" + date.substring(4,6)   + "-" + date.substring(6,8)   + "T" + 
                              date.substring(8,10) + ":" + date.substring(10,12) + ":" + date.substring(12,14) + "Z" );
      }

    try
      {
        this.server.add( doc );
      }
    catch ( SolrServerException sse )
      {
        throw new IOException( sse );
      }
  }

  public void commit( )
    throws IOException
  {
    try
      {
        this.server.commit();
      }
    catch ( SolrServerException sse )
      {
        throw new IOException( sse );
      }
  }
}