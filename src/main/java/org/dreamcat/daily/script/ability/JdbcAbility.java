package org.dreamcat.daily.script.ability;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.function.IConsumer;
import org.dreamcat.common.sql.DriverUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.daily.script.base.BaseHandler;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Properties;

/**
 * @author Jerry Will
 * @version 2023-08-14
 */
@Slf4j
public class JdbcAbility {

    @ArgParserField(value = {"j"})
    public String jdbcUrl;
    @ArgParserField(value = {"u"})
    public String user;
    @ArgParserField(value = {"p"})
    public String password;
    @ArgParserField(value = {"dc"})
    public String driverClass;
    @ArgParserField(value = {"dp"})
    public List<String> driverPaths; // driver directory

    @Setter
    transient BaseHandler handler;

    public void run(IConsumer<Connection> f) throws Exception {
        if (jdbcUrl == null) {
            f.accept(null);
            return;
        }

        Properties props = new Properties();
        if (ObjectUtil.isNotEmpty(user)) props.put("user", user);
        if (ObjectUtil.isNotEmpty(password)) props.put("password", password);
        log.info("jdbcUrl={}", jdbcUrl);
        log.info("props={}", props);

        // maybe aot mode, load driver directly
        if (ObjectUtil.isEmpty(driverClass) || ObjectUtil.isEmpty(driverPaths)) {
            // aot mode, load driver directly
            try (Connection connection = DriverManager.getConnection(jdbcUrl, props)) {
                f.accept(connection);
            }
            return;
        }

        List<URL> urls = DriverUtil.parseJarPaths(driverPaths);
        if (handler.verbose) {
            for (URL url : urls) {
                log.info("add url to classloader: {}", url);
            }
        }
        DriverUtil.runIsolated(jdbcUrl, props, urls, driverClass, c -> {
            f.accept(c);
            return null;
        });
    }
}
