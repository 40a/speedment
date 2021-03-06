/**
 *
 * Copyright (c) 2006-2018, Speedment, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); You may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.speedment.runtime.connector.mysql.internal;

import com.speedment.common.injector.annotation.Inject;
import com.speedment.runtime.config.Column;
import com.speedment.runtime.config.Dbms;
import com.speedment.runtime.core.db.*;


import java.sql.Driver;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.speedment.runtime.core.exception.SpeedmentException;
import com.speedment.runtime.core.internal.db.AbstractDatabaseNamingConvention;
import com.speedment.runtime.core.internal.db.AbstractDbmsType;
import com.sun.corba.se.spi.orb.ORBVersion;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toSet;

/**
 *
 * @author Per Minborg
 * @author Emil Forslund
 */
public final class MySqlDbmsType extends AbstractDbmsType {

    private static final String OLD_DRIVER = "com.mysql.jdbc.Driver";
    private static final String NEW_DRIVER = "com.mysql.cj.jdbc.Driver";

    private final MySqlNamingConvention namingConvention;
    private final MySqlConnectionUrlGenerator connectionUrlGenerator;

    @Inject private MySqlDbmsMetadataHandler metadataHandler;
    @Inject private MySqlDbmsOperationHandler operationHandler;
    @Inject private MySqlSpeedmentPredicateView fieldPredicateView;

    private MySqlDbmsType() {
        namingConvention = new MySqlNamingConvention();
        connectionUrlGenerator = new MySqlConnectionUrlGenerator();
    }

    @Override
    public String getName() {
        return "MySQL";
    }

    @Override
    public String getDriverManagerName() {
        return "MySQL-AB JDBC Driver";
    }

    @Override
    public int getDefaultPort() {
        return 3306;
    }

    @Override
    public String getDbmsNameMeaning() {
        return "The name of the MySQL Database.";
    }

    @Override
    public boolean hasSchemaNames() {
        return false;
    }

    @Override
    public String getDriverName() {
        return isSupported(NEW_DRIVER) ? NEW_DRIVER : OLD_DRIVER;
    }

    @Override
    public boolean isSupported() {
        // make sure we touch new new driver first.
        return isSupported(NEW_DRIVER) || isSupported(OLD_DRIVER);
    }

    @Override
    public DatabaseNamingConvention getDatabaseNamingConvention() {
        return namingConvention;
    }

    @Override
    public DbmsMetadataHandler getMetadataHandler() {
        return metadataHandler;
    }

    @Override
    public DbmsOperationHandler getOperationHandler() {
        return operationHandler;
    }

    @Override
    public ConnectionUrlGenerator getConnectionUrlGenerator() {
        return connectionUrlGenerator;
    }

    @Override
    public FieldPredicateView getFieldPredicateView() {
        return fieldPredicateView;
    }

    @Override
    public String getInitialQuery() {
        return "select version() as `MySQL version`";
    }

    @Override
    public DbmsColumnHandler getColumnHandler() {
        return new DbmsColumnHandler() {
            @Override
            public Predicate<Column> excludedInInsertStatement() {
                return c -> false; // For MySQL, even autoincrement fields are added to insert statements 
            }

            @Override
            public Predicate<Column> excludedInUpdateStatement() {
                return c -> false;
            }
            
        };
    }

    private final static class MySqlNamingConvention extends AbstractDatabaseNamingConvention {

        private final static String ENCLOSER = "`",
            QUOTE = "'";

        private final static Set<String> EXCLUDE_SET = Stream.of(
            "information_schema"
        ).collect(collectingAndThen(toSet(), Collections::unmodifiableSet));

        @Override
        public Set<String> getSchemaExcludeSet() {
            return EXCLUDE_SET;
        }

        @Override
        protected String getFieldQuoteStart() {
            return QUOTE;
        }

        @Override
        protected String getFieldQuoteEnd() {
            return QUOTE;
        }

        @Override
        protected String getFieldEncloserStart() {
            return ENCLOSER;
        }

        @Override
        protected String getFieldEncloserEnd() {
            return ENCLOSER;
        }
    }

    private final static class MySqlConnectionUrlGenerator implements ConnectionUrlGenerator {

        @Override
        public String from(Dbms dbms) {
            final StringBuilder result = new StringBuilder()
                .append("jdbc:mysql://")
                .append(dbms.getIpAddress().orElse(""));

            dbms.getPort().ifPresent(p -> result.append(":").append(p));

            result/*.append("/").append(dbms.getName())*/ // MySQL treats this as default schema name
                .append("?useUnicode=true&characterEncoding=UTF-8")
                .append("&useServerPrepStmts=true")
                .append("&zeroDateTimeBehavior=")
                .append(driverVersion() >= 8 ? "CONVERT_TO_NULL" : "convertToNull")
                .append("&nullNamePatternMatchesAll=true") // Fix #190
                .append("&useLegacyDatetimeCode=true");    // Fix #190

            if (driverVersion() <= 5) {
                result.append("&useSSL=false");
            } else {
                result.append("&serverTimezone=UTC");
            }

            return result.toString();
        }
    }

    private static int driverVersion() {
        try {
            return ((Driver) Class.forName(NEW_DRIVER).newInstance()).getMajorVersion();
        } catch (ReflectiveOperationException e) {
            try {
                return ((Driver) Class.forName(OLD_DRIVER).newInstance()).getMajorVersion();
            } catch (ReflectiveOperationException e2) {
                throw new SpeedmentException("Error using reflection to read driver version.", e2);
            }
        }
    }
}
