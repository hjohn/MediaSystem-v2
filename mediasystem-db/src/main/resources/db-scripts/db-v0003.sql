CREATE TABLE settings (
  id ${SerialType},
  
  system varchar(100) NOT NULL,
  persistlevel varchar(20) NOT NULL,
  name varchar(2000) NOT NULL,
  value varchar(2000) NOT NULL,

  lastupdated timestamp NOT NULL,
  
  CONSTRAINT settings_id PRIMARY KEY (id),
  CONSTRAINT settings_system_key UNIQUE (system, name)
);