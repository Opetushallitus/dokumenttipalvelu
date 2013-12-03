package fi.vm.sade.valinta.dokumenttipalvelu.dao.impl;

import static org.joda.time.DateTime.now;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.code.morphia.Datastore;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

import fi.vm.sade.valinta.dokumenttipalvelu.dao.DocumentDao;
import fi.vm.sade.valinta.dokumenttipalvelu.dao.FlushDao;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.FileDescription;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.MetaData;

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

    private static final String GRIDFS_ID_FIELD = "_id";
    private static final String GRIDFS_FILENAME_FIELD = "filename";
    private static final String GRIDFS_CONTENT_TYPE_FIELD = "contentType";
    private static final String GRIDFS_LENGTH_FIELD = "length";
    private static final String GRIDFS_UPLOAD_DATE_FIELD = "uploadDate";
    private static final String GRIDFS_EXPIRATION_DATE_FIELD = "expirationDate";
    private static final String GRIDFS_ALIASES_FIELD = "aliases";
    private static final String GRIDFS_MD5_FIELD = "md5";

    private final GridFS documents;

    @Autowired
    public DocumentDaoImpl(Datastore datastore) {
        this.documents = new GridFS(datastore.getDB(), "documents");
    }

    @Override
    public Collection<MetaData> getAll() {
        return transform(this.documents.getFileList().toArray());
    }

    @Override
    public Collection<MetaData> getAll(Collection<String> tags) {
        BasicDBList list = new BasicDBList();
        list.addAll(tags);
        Collection<GridFSDBFile> dbojs = this.documents.find(new BasicDBObject(GRIDFS_ALIASES_FIELD, new BasicDBObject(
                "$all", list)));
        return transform(dbojs);
    }

    private Collection<MetaData> transform(Collection<? extends DBObject> dbojs) {
        return Collections2.transform(dbojs, new Function<DBObject, MetaData>() {
            public MetaData apply(DBObject file) {

                // "_id" , "filename" , "contentType" , "length" , "chunkSize" ,
                // "uploadDate" , "aliases" , "md5"
                Collection<String> tags = (Collection<String>) file.get(GRIDFS_ALIASES_FIELD);

                // obj.get("_id")
                return new MetaData((String) file.get(GRIDFS_ID_FIELD), (String) file.get(GRIDFS_FILENAME_FIELD),
                        (String) file.get(GRIDFS_CONTENT_TYPE_FIELD), (Date) file.get(GRIDFS_UPLOAD_DATE_FIELD),
                        (Date) file.get(GRIDFS_EXPIRATION_DATE_FIELD), tags, (Map) Collections.emptyMap(), (Long) file
                                .get(GRIDFS_LENGTH_FIELD), (String) file.get(GRIDFS_MD5_FIELD));
            }
        });
    }

    /**
     * TTL handling method
     */
    @Override
    public void flush() {
        this.documents
                .remove(new BasicDBObject(GRIDFS_EXPIRATION_DATE_FIELD, new BasicDBObject("$gte", now().toDate())));
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
        String mimeType = description.getMimeType();
        if (mimeType == null) {
            mimeType = StringUtils.EMPTY;
        }
        file.setContentType(mimeType);
        file.put(GRIDFS_ALIASES_FIELD, description.getTags());
        file.put(GRIDFS_EXPIRATION_DATE_FIELD, description.getExpirationDate());
        file.setMetaData(new BasicDBObject(Collections.emptyMap()));
        file.save();
        return new MetaData(file.getId().toString(), file.getFilename(), file.getContentType(), file.getUploadDate(),
                (Date) file.get(GRIDFS_EXPIRATION_DATE_FIELD), file.getAliases(), file.getMetaData().toMap(),
                file.getLength(), file.getMD5());
    }

}