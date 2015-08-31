package com.hortonworks.topology;


import java.io.Serializable;
import java.util.Map;

/**
 * Created by pshah on 8/26/15.
 * Interface abstraction to capture the behaviour for tuples from datafeed
 * that for some reason could not be successfully parsed by ParserBolt.
 */
public interface UnparsedTupleHandler extends Serializable {
    /**
     * save method will be called for all tuples that could not be parsed by
     * ParserBolt that was provided with an implementation of this interface.
     *
     * @param data raw data from ParserBolt that could not be parsed.
     */
    void save (byte[] data) throws Exception;

    /**
     * prepare method to do implementation specific one time initialization
     * @param conf topology configuration passed as a map
     */
    void prepare (Map conf) throws Exception;

    /**
     * implementation related cleanup tasks
     */
    void cleanup () throws Exception;
}
