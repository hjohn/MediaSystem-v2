package hs.mediasystem.skipscan.db;

import hs.mediasystem.db.DatabaseFactory;
import hs.mediasystem.db.base.DatabaseContentPrintProvider;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.sql.DataSource;

import org.int4.dirk.annotations.Produces;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

public class DatabaseConfig {

  @Produces @Named("database.driverClass")
  public static final String DRIVER_CLASS = "org.postgresql.Driver";

  @Produces @Named("database.user")
  public static final String USER = "testuser";

  @Produces @Named("database.password")
  public static final String PASSWORD = "test";

  @Produces @Named("database.postConnectSql")
  public static final String POST_CONNECT_SQL = "SET search_path = public";

  @Produces @Named("general.basedir")
  public static final String GENERAL_BASEDIR = new File(DatabaseConfig.class.getClassLoader().getResource(".").getFile()).getAbsolutePath() + "/testdata";

  /*
   * The injections below are here to create candidates for interfaces that can't be auto
   * discovered. DatabaseFactory provides an instance of Database and DatabaseContentPrintProvider
   * provides an instance of ContentPrintProvider.
   */

  @Inject DatabaseFactory databaseFactory;
  @Inject DatabaseContentPrintProvider contentPrintProvider;

  @SuppressWarnings("resource")
  @Produces
  @Singleton
  @Named("database.url")
  static String createDatabaseURL() throws IOException {
    EmbeddedPostgres embeddedPostgres = EmbeddedPostgres.builder().start();

    createDatabase(embeddedPostgres.getPostgresDatabase());

    // TODO how to shut down?
    return embeddedPostgres.getJdbcUrl("postgres", "mediasystem_test");
  }

  private static void createDatabase(DataSource dataSource) {
    try(Connection connection = dataSource.getConnection()) {
      try(PreparedStatement ps = connection.prepareStatement("""
          CREATE DATABASE mediasystem_test;
          CREATE ROLE testuser superuser CREATEDB LOGIN PASSWORD 'test';
      """)) {
        ps.execute();
      }
    }
    catch(Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
