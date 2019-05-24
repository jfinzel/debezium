/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.mysql;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;

import io.debezium.config.CommonConnectorConfig;
import io.debezium.connector.AbstractSourceInfoStructMaker;

public class MySqlSourceInfoStructMaker extends AbstractSourceInfoStructMaker<SourceInfo> {
    public static final String TIMESTAMP_KEY = "ts_ms";

    private final Schema schema;

    public MySqlSourceInfoStructMaker(String connector, String version, CommonConnectorConfig connectorConfig) {
        super(connector, version, connectorConfig);
        schema = commonSchemaBuilder()
                .name("io.debezium.connector.mysql.Source")
                .field(SourceInfo.SERVER_ID_KEY, Schema.INT64_SCHEMA)
                .field(TIMESTAMP_KEY, Schema.INT64_SCHEMA)
                .field(SourceInfo.GTID_KEY, Schema.OPTIONAL_STRING_SCHEMA)
                .field(SourceInfo.BINLOG_FILENAME_OFFSET_KEY, Schema.STRING_SCHEMA)
                .field(SourceInfo.BINLOG_POSITION_OFFSET_KEY, Schema.INT64_SCHEMA)
                .field(SourceInfo.BINLOG_ROW_IN_EVENT_OFFSET_KEY, Schema.INT32_SCHEMA)
                .field(SourceInfo.SNAPSHOT_KEY, SchemaBuilder.bool().optional().defaultValue(false).build())
                .field(SourceInfo.THREAD_KEY, Schema.OPTIONAL_INT64_SCHEMA)
                .field(SourceInfo.DB_NAME_KEY, Schema.OPTIONAL_STRING_SCHEMA)
                .field(SourceInfo.TABLE_NAME_KEY, Schema.OPTIONAL_STRING_SCHEMA)
                .field(SourceInfo.QUERY_KEY, Schema.OPTIONAL_STRING_SCHEMA)
                .build();
    }

    @Override
    public Schema schema() {
        return schema;
    }

    @Override
    public Struct struct(SourceInfo sourceInfo) {
        Struct result = commonStruct();
        result.put(SourceInfo.SERVER_ID_KEY, sourceInfo.getServerId());
        if (sourceInfo.getCurrentGtid() != null) {
            // Don't put the GTID Set into the struct; only the current GTID is fine ...
            result.put(SourceInfo.GTID_KEY, sourceInfo.getCurrentGtid());
        }
        result.put(SourceInfo.BINLOG_FILENAME_OFFSET_KEY, sourceInfo.getCurrentBinlogFilename());
        result.put(SourceInfo.BINLOG_POSITION_OFFSET_KEY, sourceInfo.getCurrentBinlogPosition());
        result.put(SourceInfo.BINLOG_ROW_IN_EVENT_OFFSET_KEY, sourceInfo.getCurrentRowNumber());
        result.put(TIMESTAMP_KEY, sourceInfo.getBinlogTimestampSeconds() * 1_000);
        if (sourceInfo.isLastSnapshot()) {
            // if the snapshot is COMPLETED, then this will not happen.
            result.put(SourceInfo.SNAPSHOT_KEY, true);
        }
        if (sourceInfo.getThreadId() >= 0) {
            result.put(SourceInfo.THREAD_KEY, sourceInfo.getThreadId());
        }
        if (sourceInfo.getTableId() != null) {
            result.put(SourceInfo.DB_NAME_KEY, sourceInfo.getTableId().catalog());
            result.put(SourceInfo.TABLE_NAME_KEY, sourceInfo.getTableId().table());
        }
        if (sourceInfo.getQuery() != null) {
            result.put(SourceInfo.QUERY_KEY, sourceInfo.getQuery());
        }
        return result;
    }
}