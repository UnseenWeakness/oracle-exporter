package com.ideal.oracleexporter.demos.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;


@Service
@Slf4j
public class OracleMetricsService {

    private final JdbcTemplate jdbcTemplate;
    private final MeterRegistry meterRegistry;
    private final Map<String, Double> tablespaceUsage = new HashMap<>(); // 存储表空间使用率

    public OracleMetricsService(JdbcTemplate jdbcTemplate, MeterRegistry meterRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void initMetrics() {
        // 活跃会话数
        Gauge.builder("oracle.active.sessions", this::getActiveSessions)
             .description("Number of active sessions in Oracle 11g")
             .register(meterRegistry);

        // 物理读取次数（累计）
        Gauge.builder("oracle.physical.reads.total", this::getPhysicalReads)
             .description("Total number of physical reads from disk")
             .register(meterRegistry);

        // SQL 执行次数（累计）
        Gauge.builder("oracle.execute.count.total", this::getExecuteCount)
             .description("Total number of SQL statement executions")
             .register(meterRegistry);

        // 表空间使用率（动态标签）
        Gauge.builder("oracle.tablespace.used.percent", tablespaceUsage, map -> map.getOrDefault("SYSTEM", 0.0))
             .tag("tablespace", "SYSTEM")
             .description("Percentage of used space in SYSTEM tablespace")
             .register(meterRegistry);

        // 可以添加更多表空间的 Gauge，例如 USERS
        Gauge.builder("oracle.tablespace.used.percent", tablespaceUsage, map -> map.getOrDefault("USERS", 0.0))
             .tag("tablespace", "USERS")
             .description("Percentage of used space in USERS tablespace")
             .register(meterRegistry);
    }

    @Scheduled(fixedRate = 30000) // 每 30 秒更新一次指标
    public void collectOracleMetrics() {
        log.info("Collecting Oracle 11g metrics...");
        updateActiveSessions();
        updatePhysicalReads();
        updateExecuteCount();
        updateTablespaceUsage();
    }

    private double getActiveSessions() {
        try {
            String sql = "SELECT COUNT(*) FROM V$SESSION WHERE STATUS = 'ACTIVE' AND TYPE = 'USER'";
            return jdbcTemplate.queryForObject(sql, Double.class);
        } catch (Exception e) {
            log.error("Error fetching active sessions", e);
            return 0.0;
        }
    }

    private double getPhysicalReads() {
        try {
            String sql = "SELECT VALUE FROM V$SYSSTAT WHERE NAME = 'physical reads'";
            return jdbcTemplate.queryForObject(sql, Double.class);
        } catch (Exception e) {
            log.error("Error fetching physical reads", e);
            return 0.0;
        }
    }

    private double getExecuteCount() {
        try {
            String sql = "SELECT VALUE FROM V$SYSSTAT WHERE NAME = 'execute count'";
            return jdbcTemplate.queryForObject(sql, Double.class);
        } catch (Exception e) {
            log.error("Error fetching execute count", e);
            return 0.0;
        }
    }

    private void updateTablespaceUsage() {
        try {
            String sql = "SELECT d.tablespace_name, " +
                         "ROUND((SUM(d.bytes) - SUM(NVL(f.bytes, 0))) / SUM(d.bytes) * 100, 2) AS used_percent " +
                         "FROM dba_data_files d " +
                         "LEFT JOIN dba_free_space f ON d.tablespace_name = f.tablespace_name " +
                         "GROUP BY d.tablespace_name";
            jdbcTemplate.query(sql, rs -> {
                String tablespaceName = rs.getString("tablespace_name");
                double usedPercent = rs.getDouble("used_percent");
                tablespaceUsage.put(tablespaceName, usedPercent);
            });
        } catch (Exception e) {
            log.error("Error fetching tablespace usage", e);
        }
    }
}