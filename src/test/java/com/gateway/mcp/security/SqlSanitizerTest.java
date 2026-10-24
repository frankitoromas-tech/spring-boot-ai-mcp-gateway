package com.gateway.mcp.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit Test Suite for SqlSanitizer.
 *
 * @author Frank Emiliano Vargas Huamán (Frankosyni - Φρανκοσύνη/Francus.)
 */
class SqlSanitizerTest {

    private SqlSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new SqlSanitizer();
    }

    @Test
    @DisplayName("Should permit valid SELECT and append default LIMIT")
    void testValidSelectWithAppendedLimit() {
        String query = "SELECT id, name FROM departments";
        String sanitized = sanitizer.validateAndEnforceLimit(query, 50);

        assertThat(sanitized).isEqualTo("SELECT id, name FROM departments LIMIT 50");
    }

    @Test
    @DisplayName("Should preserve existing LIMIT if already specified")
    void testValidSelectWithExistingLimit() {
        String query = "SELECT * FROM employees LIMIT 10";
        String sanitized = sanitizer.validateAndEnforceLimit(query, 50);

        assertThat(sanitized).isEqualTo("SELECT * FROM employees LIMIT 10");
    }

    @Test
    @DisplayName("Should permit CTE query starting with WITH")
    void testValidCteQuery() {
        String query = "WITH top_salaries AS (SELECT * FROM employees WHERE salary > 50000) SELECT * FROM top_salaries";
        String sanitized = sanitizer.validateAndEnforceLimit(query, 25);

        assertThat(sanitized).contains("LIMIT 25");
    }

    @Test
    @DisplayName("Should block destructive DROP statement")
    void testBlockDropStatement() {
        assertThatThrownBy(() -> sanitizer.validateAndEnforceLimit("DROP TABLE employees", 100))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Security Violation");
    }

    @Test
    @DisplayName("Should block destructive DELETE statement")
    void testBlockDeleteStatement() {
        assertThatThrownBy(() -> sanitizer.validateAndEnforceLimit("DELETE FROM employees WHERE id = 1", 100))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    @DisplayName("Should block stacked multi-statement injection")
    void testBlockStackedQueryInjection() {
        String attack = "SELECT * FROM employees; DROP TABLE employees;";
        assertThatThrownBy(() -> sanitizer.validateAndEnforceLimit(attack, 100))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Multi-statement execution");
    }

    @Test
    @DisplayName("Should reject blank or null queries")
    void testRejectBlankQuery() {
        assertThatThrownBy(() -> sanitizer.validateAndEnforceLimit("   ", 100))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> sanitizer.validateAndEnforceLimit(null, 100))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
