package net.smackem.jobotwar.web.persist.sql;

import net.smackem.jobotwar.lang.Compiler;
import net.smackem.jobotwar.web.beans.RobotBean;
import net.smackem.jobotwar.web.persist.ConstraintViolationException;
import net.smackem.jobotwar.web.persist.NoSuchBeanException;
import net.smackem.jobotwar.web.persist.RobotDao;
import net.smackem.jobotwar.web.query.PQueryCompiler;
import net.smackem.jobotwar.web.query.Query;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.text.ParseException;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class SqlRobotDao extends SqlDao implements RobotDao {
    private static final Logger log = LoggerFactory.getLogger(SqlRobotDao.class);

    public SqlRobotDao(Supplier<Connection> connectionSupplier) {
        super(connectionSupplier);
    }

    @NotNull
    @Override
    public Collection<RobotBean> select(@NotNull Query query) throws ParseException {
        final String filterSource = query.filterSource();
        final String whereClause = filterSource != null
                ? PQueryCompiler.compile(query.filterSource(), new SqlEmittingVisitor("robot"))
                : "1=1";
        final String offsetClause = query.offset().map(offset -> " offset " + offset).orElse("");
        final String limitClause = query.limit().map(limit -> " limit " + limit).orElse("");
        try (final Connection conn = connect()) {
            final Statement stmt = conn.createStatement();
            final ResultSet rs = stmt.executeQuery("select * from robot where " + whereClause + offsetClause + limitClause);
            return loadResultSet(rs, SqlRobotDao::loadRobotBean);
        } catch (SQLException e) {
            throw handleSQLException(e);
        }
    }

    @NotNull
    @Override
    public List<RobotBean> get(@NotNull String... ids) {
        try (final Connection conn = connect()) {
            final PreparedStatement stmt = conn.prepareStatement("""
                select * from robot where id in (%s)
                """.formatted(repeatCommaSeparated("?", ids.length)));
            for (int param = 1; param <= ids.length; param++) {
                stmt.setObject(param, UUID.fromString(ids[param - 1]));
            }
            final ResultSet rs = stmt.executeQuery();
            return loadResultSet(rs, SqlRobotDao::loadRobotBean);
        } catch (SQLException e) {
            throw handleSQLException(e);
        }
    }

    @Override
    public void put(@NotNull RobotBean bean) throws ConstraintViolationException {
        if (bean.isFrozen() == false) {
            throw new IllegalArgumentException("bean must be frozen to be persisted");
        }
        try (final Connection conn = connect()) {
            final PreparedStatement stmt = conn.prepareStatement("""
                insert into robot values(
                    ?, --id
                    ?, --name
                    ?, --language
                    ?, --code
                    ?, --acceleration
                    ?, --rgb
                    ?, --dateCreated
                    null) --dateModified
                """);
            stmt.setObject(1, UUID.fromString(bean.id()));
            stmt.setString(2, bean.name());
            stmt.setString(3, bean.language().toString());
            stmt.setString(4, bean.code());
            stmt.setDouble(5, bean.acceleration());
            stmt.setInt(6, bean.rgb());
            stmt.setObject(7, bean.dateCreated());
            if (stmt.executeUpdate() != 1) {
                log.warn("error inserting robot with id " + bean.id());
                throw new ConstraintViolationException("error inserting robot");
            }
        } catch (SQLException e) {
            throw handleSQLException(e);
        }
    }

    @Override
    public void update(@NotNull RobotBean bean) throws NoSuchBeanException {
        if (bean.isFrozen() == false) {
            throw new IllegalArgumentException("bean must be frozen to be persisted");
        }
        try (final Connection conn = connect()) {
            final PreparedStatement stmt = conn.prepareStatement("""
                update robot set
                    name = ?,
                    language = ?, 
                    code = ?,
                    acceleration = ?,
                    rgb = ?,
                    date_modified = ?
                where id = ?
                """);
            stmt.setString(1, bean.name());
            stmt.setString(2, bean.language().toString());
            stmt.setString(3, bean.code());
            stmt.setDouble(4, bean.acceleration());
            stmt.setInt(5, bean.rgb());
            stmt.setObject(6, bean.dateModified());
            stmt.setObject(7, UUID.fromString(bean.id()));
            if (stmt.executeUpdate() == 0) {
                log.warn("no robot found with id " + bean.id());
                throw new NoSuchBeanException("no robot found with id " + bean.id());
            }
        } catch (SQLException e) {
            throw handleSQLException(e);
        }
    }

    @Override
    public boolean delete(@NotNull String id) {
        try (final Connection conn = connect()) {
            final PreparedStatement stmt = conn.prepareStatement("""
                delete from robot where id = ?
                """);
            stmt.setObject(1, UUID.fromString(id));
            return stmt.executeUpdate() != 0;
        } catch (SQLException e) {
            throw handleSQLException(e);
        }
    }

    @Override
    public long count() {
        try (final Connection conn = connect()) {
            final PreparedStatement stmt = conn.prepareStatement("""
                select count(*) from robot
                """);
            final ResultSet rs = stmt.executeQuery();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw handleSQLException(e);
        }
    }

    private static RobotBean loadRobotBean(ResultSet rs) throws SQLException {
        final RobotBean bean = new RobotBean(rs.getString(1))
                .name(rs.getString(2))
                .language(Enum.valueOf(Compiler.Language.class, rs.getString(3)))
                .code(rs.getString(4))
                .acceleration(rs.getDouble(5))
                .rgb(rs.getInt(6));
        final Timestamp dateCreated = rs.getTimestamp(7);
        if (dateCreated != null) {
            bean.dateCreated(dateCreated.toInstant().atOffset(ZoneOffset.UTC));
        }
        final Timestamp dateModified = rs.getTimestamp(8);
        if (dateModified != null) {
            bean.dateModified(dateModified.toInstant().atOffset(ZoneOffset.UTC));
        }
        return bean.freeze();
    }
}
