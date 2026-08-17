package com.pushkar.codereview.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "management.health.db.enabled=false",
        "github.app.id=123456",
        "github.app.private-key=test-key-content",
        "gemini.api.key=test-gemini-api-key"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(ActuatorHealthTest.TestConfig.class)
class ActuatorHealthTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testActuatorHealthEndpoint_ReturnsStatusUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testActuatorLivenessProbe_ReturnsStatusUp() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testActuatorReadinessProbe_ReturnsStatusUp() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public DataSource dataSource() {
            return new StubDataSource();
        }
    }

    static class StubDataSource implements DataSource {
        private static ResultSet createDummyResultSet() {
            AtomicBoolean first = new AtomicBoolean(true);
            return (ResultSet) Proxy.newProxyInstance(
                    ResultSet.class.getClassLoader(),
                    new Class<?>[]{ResultSet.class},
                    (proxyRs, methodRs, argsRs) -> {
                        if ("next".equals(methodRs.getName())) return first.getAndSet(false);
                        if ("getInt".equals(methodRs.getName()) || "getObject".equals(methodRs.getName())) return 1;
                        if (methodRs.getReturnType() == boolean.class) return false;
                        if (methodRs.getReturnType() == int.class) return 0;
                        return null;
                    }
            );
        }

        @Override
        public Connection getConnection() throws SQLException {
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("getMetaData".equals(method.getName())) {
                            return Proxy.newProxyInstance(
                                    DatabaseMetaData.class.getClassLoader(),
                                    new Class<?>[]{DatabaseMetaData.class},
                                    (proxyMd, methodMd, argsMd) -> {
                                        if ("getDatabaseProductName".equals(methodMd.getName())) return "PostgreSQL";
                                        if ("getDatabaseProductVersion".equals(methodMd.getName())) return "15.0";
                                        if ("getDriverName".equals(methodMd.getName())) return "PostgreSQL JDBC Driver";
                                        if ("getDriverVersion".equals(methodMd.getName())) return "42.0";
                                        if (ResultSet.class.isAssignableFrom(methodMd.getReturnType())) return createDummyResultSet();
                                        if (methodMd.getReturnType() == boolean.class) return false;
                                        if (methodMd.getReturnType() == int.class) return 0;
                                        return null;
                                    }
                            );
                        }
                        if ("prepareStatement".equals(method.getName()) || "createStatement".equals(method.getName())) {
                            return Proxy.newProxyInstance(
                                    PreparedStatement.class.getClassLoader(),
                                    new Class<?>[]{PreparedStatement.class},
                                    (proxyStmt, methodStmt, argsStmt) -> {
                                        if ("executeQuery".equals(methodStmt.getName())) return createDummyResultSet();
                                        if (methodStmt.getReturnType() == boolean.class) return false;
                                        if (methodStmt.getReturnType() == int.class) return 0;
                                        return null;
                                    }
                            );
                        }
                        if ("getAutoCommit".equals(method.getName())) return true;
                        if ("isClosed".equals(method.getName())) return false;
                        if ("isValid".equals(method.getName())) return true;
                        if ("toString".equals(method.getName())) return "StubConnection";
                        if (method.getReturnType() == boolean.class) return false;
                        if (method.getReturnType() == int.class) return 0;
                        return null;
                    }
            );
        }

        @Override public Connection getConnection(String username, String password) throws SQLException { return getConnection(); }
        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter out) {}
        @Override public void setLoginTimeout(int seconds) {}
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException { return null; }
        @Override public <T> T unwrap(Class<T> iface) { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }
}
