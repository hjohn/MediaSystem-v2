package hs.database.schema;

public interface DatabaseStatementTranslator {
  String translate(String statement);
}
