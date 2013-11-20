package fi.vm.sade.valinta.dokumenttipalvelu.dao;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Used by db clearing background job
 */
public interface FlushDao {

    void flush(); // delete expired documents
}
