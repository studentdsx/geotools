/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2017, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.data.postgis;

import org.geotools.jdbc.JDBCDelegatingTestSetup;

public class PostGISJsonTestSetup extends JDBCDelegatingTestSetup {

    protected PostGISJsonTestSetup() {
        super(new PostGISTestSetup());
    }

    @Override
    protected void setUpData() throws Exception {
        dropTestJsonTable();
        createTestJsonTable();
    }

    private void createTestJsonTable() throws Exception {
        
        String sql = "CREATE TABLE \"jsontest\" ("
                + "\"id\" INT, \"name\" VARCHAR, \"jsonColumn\" JSON, \"jsonbColumn\" JSONB, PRIMARY KEY(id))";
        run(sql);
        
        // Quoting from PostgreSQL Documentation:
        // --------------------------------------
        // There are two JSON data types: json and jsonb. They accept almost identical sets of values as input. 
        // The major practical difference is one of efficiency. The json data type stores an exact copy of the input text [...]  
        // while jsonb data is stored in a decomposed binary format [...]. 
        // Because the json type stores an exact copy of the input text, it will preserve semantically-insignificant 
        // white space between tokens, as well as the order of keys within JSON objects. Also, if a JSON object within 
        // the value contains the same key more than once, all the key/value pairs are kept. 
        // By contrast, jsonb does not preserve white space, does not preserve the order of object keys, and does not keep 
        // duplicate object keys. If duplicate keys are specified in the input, only the last value is kept.
        // One semantically-insignificant detail worth noting is that in jsonb, numbers will be printed according 
        // to the behavior of the underlying numeric type. In practice this means that numbers entered with E notation 
        // will be printed without it, for example:
        sql = "INSERT INTO \"jsontest\" VALUES (0, 'numberEntry','{\"weight\": 1e-3 }', '{\"weight\": 1e-3}');" 
                + "INSERT INTO \"jsontest\" VALUES (1, 'entryWithSpaces','{\"title\"    :    \"Title\" }',"
                     + "'{\"title\"    :    \"Title\" }');"
                + "INSERT INTO \"jsontest\" VALUES (2, 'duppedKeyEntry', '{\"title\":\"Title1\", \"title\":\"Title2\"}',"
                     + "'{\"title\":\"Title1\", \"title\":\"Title2\"}');"
                + "INSERT INTO \"jsontest\" VALUES (3, 'nullKey', '{\"title\":null}', '{\"title\":null}');"
                + "INSERT INTO \"jsontest\" VALUES (4, 'nullEntry', NULL, NULL);";
        run(sql);
    }

    private void dropTestJsonTable() throws Exception {
        runSafe("DROP TABLE \"jsontest\" cascade");
    }

}
