package net.smackem.jobotwar.web.persist.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

class SqlDao {
    private static final Logger log = LoggerFactory.getLogger(SqlDao.class);
    private final Supplier<Connection> connectionSupplier;

    SqlDao(Supplier<Connection> connectionSupplier) {
        this.connectionSupplier = Objects.requireNonNull(connectionSupplier);
    }

    Connection connect() {
        return this.connectionSupplier.get();
    }

    RuntimeException handleSQLException(SQLException e) {
        log.error("database error", e);
        return new RuntimeException(e);
    }

    <T> Stream<T> streamQueryResults(String sql, BeanLoader<T> loader) {
        try {
            final Connection conn = connect();
            final Statement stmt = conn.createStatement();
            final ResultSet rs = stmt.executeQuery(Objects.requireNonNull(sql));
            final Stream<T> stream = Stream.iterate(null,
                    ignored -> {
                        try {
                            return rs.next();
                        } catch (SQLException e) {
                            throw handleSQLException(e);
                        }
                    },
                    ignored -> {
                        try {
                            return loader.load(rs);
                        } catch (SQLException e) {
                            throw handleSQLException(e);
                        }
                    });
            return stream.onClose(() -> {
                try {
                    conn.close();
                } catch (SQLException e) {
                    throw handleSQLException(e);
                }
            });
        } catch (SQLException e) {
            throw handleSQLException(e);
        }
    }

    @FunctionalInterface
    interface BeanLoader<T> {
        T load(ResultSet rs) throws SQLException;
    }
}
