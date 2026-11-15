/**
 * Database backup and restore support. The module talks to PostgreSQL through the command line
 * tools, so it does not need the Docker socket.
 *
 * @author javiersolanop777
 */
@org.springframework.modulith.ApplicationModule(displayName = "Backup")
package com.edu.unicauca.gasstation.backend.backup;
