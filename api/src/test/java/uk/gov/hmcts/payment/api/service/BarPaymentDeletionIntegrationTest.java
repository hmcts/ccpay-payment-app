package uk.gov.hmcts.payment.api.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIf("isDockerAvailable")
class BarPaymentDeletionIntegrationTest {

    private static final String CASE_REFERENCE = "1747738470383168";
    private static final String OTHER_CASE_REFERENCE = "9999999999999999";

    static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable ex) {
            return false;
        }
    }

    @SuppressWarnings("resource")
    private static final PostgreSQLContainer BAR_POSTGRES =
        new PostgreSQLContainer("postgres:11")
            .withDatabaseName("bar")
            .withUsername("bar")
            .withPassword("bar");

    private JdbcTemplate barJdbcTemplate;
    private BarPaymentDeletionService barPaymentDeletionService;

    @BeforeAll
    static void startContainer() {
        BAR_POSTGRES.start();
    }

    @AfterAll
    static void stopContainer() {
        BAR_POSTGRES.stop();
    }

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
            BAR_POSTGRES.getJdbcUrl(),
            BAR_POSTGRES.getUsername(),
            BAR_POSTGRES.getPassword()
        );
        barJdbcTemplate = new JdbcTemplate(dataSource);
        createBarSchema();
        seedBarData();
        barPaymentDeletionService = new BarPaymentDeletionServiceImpl(barJdbcTemplate);
    }

    @Test
    void deleteBarPaymentsByCcdCaseNumberRemovesOnlyMatchingCaseData() {
        assertThat(count("payment_instruction")).isEqualTo(2);
        assertThat(count("case_fee_detail")).isEqualTo(2);
        assertThat(count("payment_instruction_status")).isEqualTo(2);
        assertThat(count("payment_instruction_payhub_reference")).isEqualTo(2);

        barPaymentDeletionService.deleteBarPaymentsByCcdCaseNumber(CASE_REFERENCE);

        assertThat(countByCaseReference(CASE_REFERENCE)).isZero();
        assertThat(count("payment_instruction_status WHERE payment_instruction_id = 1")).isZero();
        assertThat(count("payment_instruction_payhub_reference WHERE payment_instruction_id = 1")).isZero();
        assertThat(count("payment_instruction WHERE id = 1")).isZero();

        assertThat(countByCaseReference(OTHER_CASE_REFERENCE)).isEqualTo(1);
        assertThat(count("payment_instruction WHERE id = 2")).isEqualTo(1);
        assertThat(count("payment_instruction_status WHERE payment_instruction_id = 2")).isEqualTo(1);
        assertThat(count("payment_instruction_payhub_reference WHERE payment_instruction_id = 2")).isEqualTo(1);
    }

    @Test
    void deleteBarPaymentsByCcdCaseNumberIsIdempotentWhenNoDataExists() {
        barPaymentDeletionService.deleteBarPaymentsByCcdCaseNumber("0000000000000000");

        assertThat(count("payment_instruction")).isEqualTo(2);
        assertThat(count("case_fee_detail")).isEqualTo(2);
    }

    private void createBarSchema() {
        barJdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS payment_instruction ("
                + "id INTEGER PRIMARY KEY,"
                + "payer_name VARCHAR(255)"
                + ")"
        );
        barJdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS case_fee_detail ("
                + "id INTEGER PRIMARY KEY,"
                + "payment_instruction_id INTEGER NOT NULL,"
                + "case_reference VARCHAR(255) NOT NULL"
                + ")"
        );
        barJdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS payment_instruction_status ("
                + "id INTEGER PRIMARY KEY,"
                + "payment_instruction_id INTEGER NOT NULL,"
                + "status VARCHAR(50)"
                + ")"
        );
        barJdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS payment_instruction_payhub_reference ("
                + "id INTEGER PRIMARY KEY,"
                + "payment_instruction_id INTEGER NOT NULL,"
                + "reference VARCHAR(255)"
                + ")"
        );
        barJdbcTemplate.update("DELETE FROM payment_instruction_payhub_reference");
        barJdbcTemplate.update("DELETE FROM payment_instruction_status");
        barJdbcTemplate.update("DELETE FROM case_fee_detail");
        barJdbcTemplate.update("DELETE FROM payment_instruction");
    }

    private void seedBarData() {
        barJdbcTemplate.update("INSERT INTO payment_instruction (id, payer_name) VALUES (1, 'Payer One')");
        barJdbcTemplate.update("INSERT INTO payment_instruction (id, payer_name) VALUES (2, 'Payer Two')");

        barJdbcTemplate.update(
            "INSERT INTO case_fee_detail (id, payment_instruction_id, case_reference) VALUES (1, 1, ?)",
            CASE_REFERENCE
        );
        barJdbcTemplate.update(
            "INSERT INTO case_fee_detail (id, payment_instruction_id, case_reference) VALUES (2, 2, ?)",
            OTHER_CASE_REFERENCE
        );

        barJdbcTemplate.update(
            "INSERT INTO payment_instruction_status (id, payment_instruction_id, status) VALUES (1, 1, 'Pending')"
        );
        barJdbcTemplate.update(
            "INSERT INTO payment_instruction_status (id, payment_instruction_id, status) VALUES (2, 2, 'Pending')"
        );

        barJdbcTemplate.update(
            "INSERT INTO payment_instruction_payhub_reference (id, payment_instruction_id, reference) "
                + "VALUES (1, 1, 'RC-1111')"
        );
        barJdbcTemplate.update(
            "INSERT INTO payment_instruction_payhub_reference (id, payment_instruction_id, reference) "
                + "VALUES (2, 2, 'RC-2222')"
        );
    }

    private int count(String tableOrClause) {
        Integer count = barJdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableOrClause, Integer.class);
        return count == null ? 0 : count;
    }

    private int countByCaseReference(String caseReference) {
        Integer count = barJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM case_fee_detail WHERE case_reference = ?",
            Integer.class,
            caseReference
        );
        return count == null ? 0 : count;
    }
}
