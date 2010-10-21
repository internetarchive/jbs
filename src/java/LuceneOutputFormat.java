
import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.hadoop.conf.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.util.*;
import org.apache.lucene.store.*;

/**
 * This class is derived from Nutch's LuceneOutputFormat class.  It does
 * primarily two things of interest:
 *  1. Creates a Lucene index in a local (not HDFS) ${temp} directory,
 *     into which the documents are added.
 *  2. Closes that index and copies it into HDFS.
 */
public class LuceneOutputFormat extends FileOutputFormat<Text, MapWritable>
{
  public FileSystem fs;

  public Path temp;
  public Path perm;
  
  public IndexWriter indexer;

  public RecordWriter<Text, MapWritable> getRecordWriter( final FileSystem fs,
                                                          final JobConf job,
                                                          final String name,
                                                          final Progressable progress )
    throws IOException
  {
    // Open Lucene index in ${temp}
    this.fs = FileSystem.get(job);

    this.perm = new Path( FileOutputFormat.getOutputPath( job ), name );
    this.temp = job.getLocalPath( "index/_"  + (new Random().nextInt()) );

    this.fs.delete( perm, true ); // delete old, if any

    indexer = new IndexWriter( new NIOFSDirectory( new File( fs.startLocalOutput( perm, temp ).toString( ) ) ),
                               new KeywordAnalyzer( ),
                               IndexWriter.MaxFieldLength.UNLIMITED );
    
    indexer.setMergeFactor      ( job.getInt("indexer.mergeFactor", 10) );
    indexer.setMaxMergeDocs     ( job.getInt("indexer.maxMergeDocs", Integer.MAX_VALUE) );
    indexer.setTermIndexInterval( job.getInt("indexer.termIndexInterval", 128) );
    indexer.setMaxFieldLength   ( job.getInt("indexer.max.tokens", Integer.MAX_VALUE) );
    indexer.setUseCompoundFile  ( false );

    LuceneDocumentWriter docWriter = buildDocumentWriter( job, indexer );
    
    return new LuceneRecordWriter( docWriter );
  }

  public class LuceneRecordWriter implements RecordWriter<Text, MapWritable>
  {
    DocumentWriter docWriter;

    public LuceneRecordWriter( LuceneDocumentWriter docWriter )
      throws IOException
    {
      this.docWriter = docWriter;
    }

    // Delegate to docWriter
    public void write( Text key, MapWritable properties )
      throws IOException
    {
      this.docWriter.add( key, properties );
    }

    public void close( Reporter reporter )
      throws IOException
    {
      // Optimize and close the IndexWriter
      indexer.optimize();
      indexer.close();
      
      // Copy the index from ${temp} to HDFS and touch a "done" file.
      fs.completeLocalOutput(perm, temp);
      fs.createNewFile(new Path(perm, "done"));
    }
    
  }
  
  protected LuceneDocumentWriter buildDocumentWriter( JobConf job, IndexWriter indexer )
  {
    CustomAnalyzer analyzer = new CustomAnalyzer( job.getBoolean( "indexer.analyzer.custom.omitNonAlpha", true ),
                                                  new HashSet<String>( Arrays.asList( job.get( "indexer.analyzer.stopWords", "" ).trim().split( "\\s+" ) ) ) );
    
    LuceneDocumentWriter writer = new LuceneDocumentWriter( indexer, analyzer );

    TypeNormalizer normalizer = new TypeNormalizer( );
    Map<String,String> aliases = normalizer.parseAliases( job.get( "indexer.typeNormalizer.aliases", "" ) );

    if ( job.getBoolean( "indexer.typeNormalizer.useDefaults", true ) )
      {
        Map<String,String> defaults = normalizer.getDefaultAliases( );
        defaults.putAll( aliases );

        aliases = defaults;
      }
    normalizer.setAliases( aliases );

    TypeFilter typeFilter = new TypeFilter( );
    Set<String> allowedTypes = typeFilter.parse( job.get( "indexer.typeFilter.allowed", "" ) );

    if ( job.getBoolean( "indexer.typeFilter.useDefaults", true ) )
      {
        Set<String> defaults = typeFilter.getDefaultAllowed( );
        defaults.addAll( allowedTypes );

        allowedTypes = defaults;
      }
    typeFilter.setAllowed( allowedTypes );
    typeFilter.setTypeNormalizer( normalizer );

    writer.setFilter( "type",   typeFilter );
    writer.setFilter( "robots", new RobotsFilter( ) );
    
    Map<String,FieldHandler> handlers = new HashMap<String,FieldHandler>( );
    handlers.put( "url"       , new SimpleFieldHandler( "url",        Field.Store.YES, Field.Index.NO ) );
    handlers.put( "title"     , new SimpleFieldHandler( "title",      Field.Store.YES, Field.Index.ANALYZED ) );
    handlers.put( "length"    , new SimpleFieldHandler( "length",     Field.Store.YES, Field.Index.NO ) );
    handlers.put( "collection", new SimpleFieldHandler( "collection", Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS ) );
    handlers.put( "content"   , new BodyHandler( ) );
    handlers.put( "date"      , new DateHandler( ) );
    handlers.put( "site"      , new SiteHandler( ) );
    handlers.put( "type"      , new TypeHandler( normalizer ) );  

    writer.setHandlers( handlers );
    
    return writer;
  }

    
}
