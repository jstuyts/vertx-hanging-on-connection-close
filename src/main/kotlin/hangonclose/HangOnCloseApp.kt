package hangonclose

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import java.sql.DriverManager
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.testcontainers.containers.GenericContainer

fun main() {
    MaterializeContainer().use { mzContainer ->
        mzContainer.withExposedPorts(6875)
        mzContainer.start()
        val mzPort = mzContainer.getMappedPort(6875)
        DriverManager.getConnection("jdbc:postgresql://localhost:$mzPort/materialize", "materialize", "").use { mzConnection ->
            mzConnection.createStatement().use { statement ->
                statement.executeUpdate("CREATE TABLE theSource (aColumn INTEGER)")
                statement.executeUpdate("CREATE MATERIALIZED VIEW theView AS SELECT aColumn FROM theSource")
            }
        }
        val vertx = Vertx.vertx()
        try {
            val connectOptions = PgConnectOptions()
            connectOptions.host = "localhost"
            connectOptions.port = mzPort
            connectOptions.database = "materialize"
            connectOptions.user = "materialize"
            connectOptions.password = ""
            connectOptions.cachePreparedStatements = true

            // If no idle timeout is set, rolling back the transaction after
            // the query execution has been stopped by Vert.x, will hang
            // (indefinitely). Enabling the idle timeout will cause the
            // rollback to fail immediately.
//            connectOptions.idleTimeout = Integer.MAX_VALUE

            val poolOptions = PoolOptions()
            poolOptions.maxSize = 10
            val mzPool = PgPool.pool(vertx, connectOptions, poolOptions)
            vertx.deployVerticle(WaitingVerticle(mzPool))
            println("Stop the Materialize container after some dots have appeared. No more dots will appear if the idle timeout is not enabled.")
            println("Press ENTER to exit.")
            System.`in`.read()
        } finally {
            vertx.close()
        }
    }
}

private class MaterializeContainer : GenericContainer<MaterializeContainer?>("materialize/materialized:v0.26.1")

private class WaitingVerticle(private val mzPool: PgPool) : CoroutineVerticle() {
    override suspend fun start() {
        launch {
            val connection = mzPool.connection.await()
            try {
                val transaction = connection.begin().await()
                try {
                    connection.query("DECLARE theCursor CURSOR FOR TAIL ( SELECT * FROM theView ) WITH ( PROGRESS = true )").execute().await()
                    val tailQuery = connection.preparedQuery("FETCH 1000 FROM theCursor WITH (timeout = '10ms')")
                    while (isActive) {
                        val rowSet = tailQuery.execute().await()
                        var hasDotBeenOutput = false
                        rowSet.forEach { _ ->
                            if (!hasDotBeenOutput) {
                                print('.')
                                hasDotBeenOutput = true
                            }
                        }
                    }
                } catch (t : Throwable) {
                    println(t)
                    throw t
                } finally {
                    // Without idle timeout, the verticle hangs here.
                    transaction.rollback().await()
                }
            } finally {
                connection.close().await()
            }
        }
    }
}
