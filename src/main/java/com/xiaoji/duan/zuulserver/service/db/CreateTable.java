package com.xiaoji.duan.zuulserver.service.db;

public class CreateTable extends AbstractSql {

	public CreateTable() {
		initDdl();
	}

	private void initDdl() {

		ddl.add("" + 
				"create table if not exists `gateway_api_define` (" +
				"		  `id` varchar(50) not null," +
				"		  `path` varchar(255) not null," +
				"		  `service_id` varchar(50) default null," +
				"		  `url` varchar(255) default null," +
				"		  `retryable` tinyint(1) default null," +
				"		  `enabled` tinyint(1) not null," +
				"		  `strip_prefix` int(11) default null," +
				"		  `api_name` varchar(255) default null," +
				"		  primary key (`id`)" +
				"		) engine=innodb default charset=utf8;" +
				"");
	}
}
