Reproduction of hanging on rollback when a write idle timeout has not been configured for PostgreSQL connections, and the server closes the connection.

Run `main` in `HangOnCloseApp.kt` to reproduce the behavior. The point where it hangs is on line 81 of that same file, and the idle timeout can be enabled by uncommenting line 39.

Not setting a write idle timeout results in the `IdleStateHandler` of Netty (a copy of which is included here), to not unvoid the promise passed to `ctx.write(...)` in `IdleStateHandler.write`. Leaving all idle timeouts as 0, and always unvoiding the promise (see line 305 of `IdleStateHandler`), will also resolve the issue.
