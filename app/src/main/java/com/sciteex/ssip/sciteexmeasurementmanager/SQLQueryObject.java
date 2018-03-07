package com.sciteex.ssip.sciteexmeasurementmanager;

import java.sql.PreparedStatement;

/**
 * Created by Gajos on 12/21/2017.
 */

public class SQLQueryObject {
    private String name;
    private PreparedStatement stmt;

    public void setName(String name)
    {
        this.name = name;
    }
    public void setStatement(PreparedStatement stmt)
    {
        this.stmt = stmt;
    }

    public String getName()
    {
        return this.name;
    }

    public PreparedStatement getStatement()
    {
        return this.stmt;
    }

    SQLQueryObject(String name, PreparedStatement stmt)
    {
        this.name = name;
        this.stmt = stmt;
    }
}
