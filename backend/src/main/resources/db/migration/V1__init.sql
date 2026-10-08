-- Tabla para el manejo del bus de eventos 
CREATE TABLE IF NOT EXISTS event_publication (
  id               UUID NOT NULL,
  listener_id      VARCHAR(512) NOT NULL,
  event_type       VARCHAR(512) NOT NULL,
  serialized_event VARCHAR(4000) NOT NULL,
  publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
  completion_date  TIMESTAMP WITH TIME ZONE,
  CONSTRAINT pk_event_publication PRIMARY KEY (id)
);

