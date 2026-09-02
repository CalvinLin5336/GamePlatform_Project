package com.example.demo.modules.board.config;

import org.hibernate.community.dialect.SQLiteDialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

/** 保留既有 Long ID / *_seq 資料表；SQLite 必須在目前交易取號。 */
public class BoardSequenceGenerator extends SequenceStyleGenerator {
    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner) {
        if (!(session.getJdbcServices().getDialect() instanceof SQLiteDialect))
            return super.generate(session, owner);

        String table = getDatabaseStructure().getPhysicalName().render();
        int increment = getDatabaseStructure().getIncrementSize();
        // 沿用 Hibernate 的號碼區塊大小，避開舊版已保留的 ID；不另外開取號交易。
        return session.doReturningWork(connection -> {
            try (var statement = connection.prepareStatement(
                    "update " + table + " set next_val = next_val + ? returning next_val")) {
                statement.setInt(1, increment);
                try (var result = statement.executeQuery()) {
                    if (!result.next()) throw new java.sql.SQLException("找不到 Board 編號序列：" + table);
                    return result.getLong(1) - increment;
                }
            }
        });
    }
}
