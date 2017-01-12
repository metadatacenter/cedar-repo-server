# CEDAR Repo Server

To run the server

    java \
      -Dkeycloak.config.path="$CEDAR_HOME/keycloak.json" \
      -jar $CEDAR_HOME/cedar-repo-server/cedar-repo-server-application/target/cedar-repo-server-application-*.jar \
      server \
      "$CEDAR_HOME/cedar-repo-server/cedar-repo-server-application/config.yml"

To access the application:

[http://localhost:9002/]()

To access the admin port:

[http://localhost:9102/]()