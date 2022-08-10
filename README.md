Reproduction of hanging on rollback when an idle timeout has not been configured for PostgreSQL connections, and the server closes the connection.

Run `main` in `HangOnCloseApp.kt` to reproduce the behavior. The point where it hangs is on line 81 of that same file, and the idle timeout can be enabled by uncommenting line 39.
