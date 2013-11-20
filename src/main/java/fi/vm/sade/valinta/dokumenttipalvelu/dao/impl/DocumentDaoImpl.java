package fi.vm.sade.valinta.dokumenttipalvelu.dao.impl;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.code.morphia.Datastore;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

import fi.vm.sade.valinta.dokumenttipalvelu.dao.DocumentDao;
import fi.vm.sade.valinta.dokumenttipalvelu.dao.FlushDao;
import fi.vm.sade.valinta.dokumenttipalvelu.domain.FileDescription;
import fi.vm.sade.valinta.dokumenttipalvelu.domain.MetaData;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         TTL is implemented as background job.
 * 
 */
@Repository
public class DocumentDaoImpl implements DocumentDao, FlushDao {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentDaoImpl.class);
    private final GridFS documents;

    @Autowired
    public DocumentDaoImpl(Datastore datastore) {
        DB db = datastore.getDB();
        // db.getCollection("documents")
        // .ensureIndex(new BasicDBObject("status",
        // 1).append("expireAfterSeconds", 1), "ttl");
        this.documents = new GridFS(db, "documents");
        // db.getCollection("documents.chunks")

        // .ensureIndex(new BasicDBObject("uploadDate",
        // 1).append("expireAfterSeconds", 5), "ttl");
        // db.getCollection("documents.files")

        // .ensureIndex(new BasicDBObject("uploadDate",
        // 1).append("expireAfterSeconds", 5), "ttl");
        // { "status": 1 }, { expireAfterSeconds: 3600 }

    }

    /*
     * RuntimeException( "key should never be null" ); else if ( key.equals(
     * "_id" ) ) return _id; else if ( key.equals( "filename" ) ) return
     * _filename; else if ( key.equals( "contentType" ) ) return _contentType;
     * else if ( key.equals( "length" ) ) return _length; else if ( key.equals(
     * "chunkSize" ) ) return _chunkSize; else if ( key.equals( "uploadDate" ) )
     * return _uploadDate; else if ( key.equals( "md5" ) ) return _md5; return
     * _extradata.get( key ); }(non-Javadoc)
     * 
     * @see fi.vm.sade.valinta.dokumenttipalvelu.dao.DocumentDao#getAll()
     */
    @Override
    public Collection<MetaData> getAll() {
        return Collections2.transform(this.documents.getFileList().toArray(), new Function<DBObject, MetaData>() {
            public MetaData apply(DBObject obj) {
                // obj.get("_id")
                return null;// new MetaData();
            }
        });
    }

    /**
     * TTL handling method
     */
    @Override
    public void flush() {
        LOG.info("Removing expired documents: {} documents removed!", 0);
    }

    @Override
    public InputStream get(String documentId) {

        GridFSDBFile gridFSFile = documents.findOne(new BasicDBObject("_id", documentId));
        return gridFSFile.getInputStream();
    }

    @Override
    public MetaData put(FileDescription description, InputStream documentData) {
        String filename = description.getFilename();
        GridFSInputFile file = documents.createFile(documentData, filename);
        file.setId(UUID.randomUUID().toString());
        String mimeType = StringUtils.EMPTY;
        String extension = getExtension(filename);
        if ("pdf".equalsIgnoreCase(extension)) {
            mimeType = "application/pdf";
        } else if ("xls".equalsIgnoreCase(extension)) {
            mimeType = "application/vnd.ms-excel";
        }
        file.setContentType(mimeType);
        file.put("aliases", Arrays.asList(description.getServiceName(), description.getDocumentType()));

        file.setMetaData(new BasicDBObject(description.getMetaData()));
        file.save();
        return new MetaData(file.getId().toString(), file.getFilename(), file.getContentType(), file.getUploadDate(),
                file.getAliases(), file.getMetaData().toMap(), file.getLength(), file.getMD5());
    }

    private static String getExtension(String filename) {
        String dotSeparated[] = filename.split("\\.");
        try {
            return dotSeparated[dotSeparated.length - 1];
        } catch (Exception e) {
            LOG.warn("Couldn't get extension for filename {}", filename);
        }
        return StringUtils.EMPTY;
    }
}
