package com.xiaoji.duan.zuulserver.service.db;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSql {

    protected static List<AbstractSql> sqls = new ArrayList<>();
    protected static List<AbstractSql> pachesqls = new ArrayList<>();
    
    static {
    	sqls.add(new CreateTable());
    	pachesqls.add(new CreateTable());
    }

    protected List<String> ddl = new ArrayList<>();
    protected List<String> data = new ArrayList<>();

    protected List<String> patchDdl = new ArrayList<>();
    protected List<String> patchData = new ArrayList<>();

    public static List<AbstractSql> getSqls() {
        return sqls;
    }

    public List<String> getDdl() {
        return ddl;
    }

    public List<String> getData() {
        return data;
    }

    public List<String> getPatchDdl() {
        return patchDdl;
    }

    public List<String> getPatchData() {
        return patchData;
    }

}
